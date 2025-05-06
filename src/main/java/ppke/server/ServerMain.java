package ppke.server;

import ppke.common.dto.PollDTO;
import ppke.common.dto.PollStateChangedNotification;
import ppke.common.dto.PollUpdateNotification; // Import
import ppke.common.model.PollState;
import ppke.server.model.Poll; // Import

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects; // Import
import ppke.common.dto.Response;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit; // Import TimeUnit

/**
 * A Mentimeter Klón szerver alkalmazás fő osztálya és belépési pontja.
 * Felelős a szerver indításáért, a bejövő klienskapcsolatok fogadásáért,
 * a kliensek kezelésének delegálásáért a {@link ClientHandler} példányoknak
 * (egy {@link ExecutorService} segítségével), az adatok betöltéséért és mentéséért
 * (a {@link PersistenceManager} révén), valamint a célzott broadcast üzenetek
 * koordinálásáért a {@link ServerMain#broadcastPollStateChange} metódussal.
 * Biztosítja a szerver kecses leállítását (shutdown hook).
 */
public class ServerMain {
    /** A portszám, amelyen a szerver figyel a bejövő kapcsolatokra. */
    private static final int PORT = 12345; // Konfigurálhatóvá tehető lenne
    /** A klienskezelő szálak maximális száma a thread pool-ban. */
    private static final int MAX_THREADS = 50; // Igény szerint állítható

    /** Felhasználók kezeléséért felelős manager. */
    private UserManager userManager;
    /** Szavazások kezeléséért felelős manager. */
    private PollManager pollManager;
    /** Adatok perzisztálásáért felelős manager. */
    private PersistenceManager persistenceManager;
    /**
     * Az aktív klienskapcsolatokat kezelő {@link ClientHandler} példányok halmaza.
     * {@link ConcurrentHashMap#newKeySet()} biztosítja a szálbiztosságot.
     */
    private final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();
    /** Szál-pool a {@link ClientHandler} feladatok aszinkron futtatására. */
    private final ExecutorService clientExecutor;
    /** Jelzi, hogy a szerver futó állapotban van-e. `volatile` a szálak közötti láthatóságért. */
    private volatile boolean running = true;
    /** A szerver socket a kapcsolatok fogadásához. */
    private ServerSocket serverSocket; // Hogy le tudjuk zárni a shutdown hook-ban

    /**
     * Konstruktor. Inicializálja a manager osztályokat, betölti a mentett adatokat,
     * és létrehozza a fix méretű szál-poolt a kliensek kezelésére.
     */
    public ServerMain() {
        System.out.println("Szerver inicializálása...");
        persistenceManager = new PersistenceManager();
        // Adatok betöltése (a PersistenceManager kezeli, ha a fájlok nem léteznek)
        userManager = persistenceManager.loadUsers();
        pollManager = persistenceManager.loadPolls(userManager);
        // Fix méretű thread pool létrehozása
        clientExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("Szerver inicializálva. Szál-pool mérete: " + MAX_THREADS);
    }

    /**
     * Elindítja a szervert. Létrehozza a {@link ServerSocket}-ot, figyel a bejövő kapcsolatokra,
     * és minden új kapcsolatot átad egy új {@link ClientHandler} feladatnak a szál-poolba.
     * Regisztrál egy shutdown hook-ot a kecses leállításhoz.
     * A metódus addig fut, amíg a szervert le nem állítják (a `running` flag `false` nem lesz).
     */
    public void startServer() {
        System.out.println("Szerver indítása a(z) " + PORT + " porton...");
        // Shutdown Hook regisztrálása
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownServer, "ServerShutdownHook"));

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Szerver sikeresen elindult. Várakozás kapcsolatokra...");

            // Fő ciklus: kapcsolatok fogadása
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept(); // Blokkoló hívás
                    if (!running) { // Ellenőrzés újra
                        try { clientSocket.close(); } catch (IOException ignored) {}
                        break;
                    }
                    System.out.println("Kliens csatlakozott: " + clientSocket.getRemoteSocketAddress());

                    ClientHandler handler = new ClientHandler(clientSocket, userManager, pollManager, persistenceManager, this);
                    activeClients.add(handler);
                    clientExecutor.submit(handler);

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Hiba kliens kapcsolat elfogadása közben: " + e.getMessage());
                    } else {
                        System.out.println("ServerSocket lezárva, kapcsolatok fogadása leállt.");
                        break; // Kilépés a ciklusból
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("KRITIKUS HIBA: Nem sikerült elindítani vagy futtatni a szervert a(z) " + PORT + " porton. Port foglalt?");
                e.printStackTrace();
                shutdownServer(); // Próbáljuk meg leállítani rendesen
            }
        } finally {
            if (running) {
                System.err.println("Szerver fő ciklusa váratlanul véget ért futás közben. Leállítás kezdeményezése...");
                shutdownServer();
            }
            System.out.println("Szerver fő szála leállt.");
        }
    }

    /**
     * Kecsesen leállítja a szervert.
     * Beállítja a `running` flag-et `false`-ra, leállítja a kapcsolatok fogadását
     * a {@link ServerSocket} bezárásával, leállítja a szál-poolt (megvárja a futó taskokat),
     * és elmenti az aktuális adatokat a {@link PersistenceManager} segítségével.
     * Szinkronizált, hogy ne fusson párhuzamosan többször.
     */
    public synchronized void shutdownServer() {
        if (!running) {
            return; // Már leállítás alatt
        }
        System.out.println("Szerver leállítása...");
        running = false;

        // ServerSocket bezárása
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("ServerSocket lezárása...");
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Hiba a ServerSocket lezárása közben: " + e.getMessage());
        }

        // Szál-pool leállítása
        System.out.println("Kliens Executor leállítása...");
        clientExecutor.shutdown(); // Nem fogad új taskokat
        try {
            // Várakozás a futó taskok befejezésére (max 5 mp)
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Kliens Executor nem állt le időben, kényszerített leállítás...");
                clientExecutor.shutdownNow(); // Megszakítja a futó taskokat
            } else {
                System.out.println("Kliens Executor sikeresen leállt.");
            }
        } catch (InterruptedException e) {
            System.err.println("Várakozás megszakítva a kliens Executor leállásakor.");
            clientExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Adatok mentése
        System.out.println("Adatok mentése leállításkor...");
        try {
            persistenceManager.saveAll(userManager, pollManager);
            System.out.println("Adatok sikeresen mentve.");
        } catch (Exception e) {
            System.err.println("!!! KRITIKUS HIBA adatmentés közben leállításkor !!!");
            e.printStackTrace();
        }

        System.out.println("Szerver leállítási folyamat befejeződött.");
    }


    /**
     * Eltávolít egy kliens kezelőt az aktív kliensek halmazából.
     * Ezt a {@link ClientHandler} hívja meg a {@code closeConnection} metódusában.
     * @param handler Az eltávolítandó {@link ClientHandler}.
     */
    public void removeClient(ClientHandler handler) {
        if (handler != null) {
            boolean removed = activeClients.remove(handler);
            if (removed) {
                System.out.println("Kliens kezelő eltávolítva. Aktív kliensek: " + activeClients.size());
            }
        }
    }

    /**
     * Célzott broadcast üzenetet küld egy szavazás állapotváltozásáról.
     * Elküldi a {@link PollStateChangedNotification}-t ÉS (ha az új állapot VOTING vagy RESULTS)
     * egy {@link PollUpdateNotification}-t is a szavazás létrehozójának és a feliratkozott szavazóknak.
     * Nem küldi vissza az üzenetet annak a kliensnek, aki a változást kezdeményezte.
     *
     * @param joinCode Az érintett szavazás (nagybetűs) kódja.
     * @param newState Az új állapot ({@link PollState}).
     * @param results Az eredmények (ha az új állapot RESULTS), lehet null.
     * @param senderHandler A változást kiváltó kérést küldő {@link ClientHandler} (neki nem küldjük).
     */
    public void broadcastPollStateChange(String joinCode, PollState newState, Object results, ClientHandler senderHandler) {
        if (joinCode == null || newState == null) {
            System.err.println("Érvénytelen paraméterek a broadcastPollStateChange hívásakor.");
            return;
        }

        PollStateChangedNotification stateNotification = new PollStateChangedNotification(joinCode, newState, results);
        System.out.println("Célzott broadcast kezdeményezve: Szavazás " + joinCode + " -> " + newState.getDisplayName());

        Poll poll = pollManager.getPollByJoinCode(joinCode);
        if (poll == null) {
            System.err.println("Nem lehet broadcastolni állapotváltozást nem létező szavazáshoz: " + joinCode);
            return;
        }
        String creatorUsername = poll.getCreatorUsername();
        PollDTO updatedPollDTO = null;

        boolean sendUpdateNotification = (newState == PollState.VOTING || newState == PollState.RESULTS);
        if (sendUpdateNotification) {
            try {
                updatedPollDTO = poll.toDTO(); // Friss DTO generálása
            } catch (Exception e) {
                System.err.println("Hiba a PollDTO létrehozásakor az értesítéshez (Szavazás: " + joinCode + "): " + e.getMessage());
                sendUpdateNotification = false; // Nem tudjuk elküldeni a DTO-t
            }
        }

        int recipients = 0;
        // Végigmegyünk az aktív klienseken
        for (ClientHandler client : activeClients) {
            if (client == senderHandler) continue; // Küldő kihagyása

            String clientUser = client.getLoggedInUsername();
            String subscribedCode = client.getSubscribedPollJoinCode();

            boolean isCreator = (clientUser != null && creatorUsername != null && clientUser.equals(creatorUsername));
            boolean isSubscribed = (subscribedCode != null && subscribedCode.equals(joinCode));

            if (isCreator || isSubscribed) {
                String recipientType = isCreator ? "Létrehozó ("+clientUser+")" : "Feliratkozott";
                System.out.println(" -> Állapotváltozás küldése: " + recipientType + " (Szavazás: " + joinCode + ")");
                client.sendMessage(stateNotification);
                recipients++;

                if (sendUpdateNotification && updatedPollDTO != null) {
                    client.sendMessage(new PollUpdateNotification(updatedPollDTO));
                    System.out.println(" -> PollUpdateNotification küldve: " + recipientType + " (Szavazás: " + joinCode + ")");
                }
            }
        }
        System.out.println("Broadcast befejezve: Szavazás " + joinCode + " (" + recipients + " címzett)");
    }

    /**
     * Üzenetet küld egy specifikus felhasználónak, ha az be van jelentkezve és aktív.
     * Megkeresi a felhasználóhoz tartozó {@link ClientHandler}-t és annak {@code sendMessage} metódusát hívja.
     * @param username A célfelhasználó neve (nem lehet null).
     * @param message Az elküldendő {@link Response} objektum (nem lehet null).
     */
    public void sendMessageToUser(String username, Object message) {
        if (username == null || message == null) {
            System.err.println("Kísérlet üzenet küldésére null usernek vagy null üzenettel.");
            return;
        }
        boolean sent = false;
        for (ClientHandler client : activeClients) {
            if (username.equals(client.getLoggedInUsername())) { // getLoggedInUsername kezeli a null-t
                client.sendMessage(message);
                // System.out.println("Direkt üzenet elküldve: " + username);
                sent = true;
                break; // Feltételezzük, egy user csak egyszer aktív
            }
        }
        if (!sent) {
            System.out.println("Nem sikerült direkt üzenetet küldeni: Felhasználó (" + username + ") nem aktív.");
        }
    }


    /**
     * A szerver alkalmazás belépési pontja.
     * Létrehoz egy {@link ServerMain} példányt és elindítja a szervert.
     * @param args Parancssori argumentumok (nem használt).
     */
    public static void main(String[] args) {
        System.out.println("Szerver alkalmazás indítása...");
        ServerMain server = new ServerMain();
        server.startServer(); // Elindítja a szerver fő ciklusát
        System.out.println("Szerver alkalmazás leállt."); // Ez csak akkor íródik ki, ha a startServer visszatér
    }
}