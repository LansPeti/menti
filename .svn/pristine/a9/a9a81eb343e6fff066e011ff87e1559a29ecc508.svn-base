package ppke.server;

import ppke.common.dto.PollStateChangedNotification;
import ppke.common.dto.PollUpdateNotification;
import ppke.common.dto.Response;
import ppke.common.model.PollState;
import ppke.server.model.Poll;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Fő belépési pont
 */
public class ServerMain {
    private static final int PORT = 12345;
    private static final int MAX_THREADS = 50;

    private UserManager userManager;
    private PollManager pollManager;
    private PersistenceManager persistenceManager;
    private final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();
    private final ExecutorService clientExecutor;
    private volatile boolean running = true;
    private ServerSocket serverSocket;

    public ServerMain() {
        System.out.println("Szerver inicializálása...");
        persistenceManager = new PersistenceManager();
        userManager = persistenceManager.loadUsers();
        pollManager = persistenceManager.loadPolls(userManager);
        clientExecutor = Executors.newFixedThreadPool(MAX_THREADS);
        System.out.println("Szerver inicializálva. Betöltött userek: " + userManager.getUsers().size() + ", Betöltött pollok: " + pollManager.getAllPolls().size());
    }

    public void startServer() {
        System.out.println("Szerver indítása a(z) " + PORT + " porton...");
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdownServer, "ServerShutdownHook"));

        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Szerver sikeresen elindult. Várakozás kapcsolatokra...");

            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    if (!running) {
                        try { clientSocket.close(); } catch (IOException ignored) {}
                        break;
                    }
                    System.out.println("Kliens csatlakozott: " + clientSocket.getRemoteSocketAddress());

                    // A ClientHandler konstruktorának átadjuk a persistenceManager-t és a server (this) példányt is
                    ClientHandler handler = new ClientHandler(clientSocket, userManager, pollManager, persistenceManager, this);
                    activeClients.add(handler);
                    clientExecutor.submit(handler);

                } catch (IOException e) {
                    if (running) {
                        System.err.println("Hiba kliens kapcsolat elfogadása közben: " + e.getMessage());
                    } else {
                        System.out.println("ServerSocket lezárva, kapcsolatok fogadása leállt.");
                        break;
                    }
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("KRITIKUS HIBA: Nem sikerült elindítani vagy futtatni a szervert a(z) " + PORT + " porton.");
                e.printStackTrace();
                shutdownServer();
            }
        } finally {
            if (running) { // Ha a ciklusból váratlanul lépett ki
                System.err.println("Szerver fő ciklusa váratlanul véget ért futás közben. Leállítás kezdeményezése...");
                shutdownServer(); // Biztos, ami biztos
            }
            System.out.println("Szerver fő szála leállt.");
        }
    }

    public synchronized void shutdownServer() {
        if (!running) {
            return;
        }
        System.out.println("Szerver leállítása...");
        running = false;

        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                System.out.println("ServerSocket lezárása...");
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Hiba a ServerSocket lezárása közben: " + e.getMessage());
        }

        System.out.println("Kliens Executor leállítása...");
        clientExecutor.shutdown();
        try {
            if (!clientExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                System.err.println("Kliens Executor nem állt le időben, kényszerített leállítás...");
                clientExecutor.shutdownNow();
            } else {
                System.out.println("Kliens Executor sikeresen leállt.");
            }
        } catch (InterruptedException e) {
            System.err.println("Várakozás megszakítva a kliens Executor leállásakor.");
            clientExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Adatok mentése leállításkor...");
        try {
            if (persistenceManager != null && userManager != null && pollManager != null) {
                persistenceManager.saveAll(userManager, pollManager);
                System.out.println("Adatok sikeresen mentve.");
            } else {
                System.err.println("Nem sikerült menteni az adatokat, mert valamelyik manager null.");
            }
        } catch (Exception e) {
            System.err.println("!!! KRITIKUS HIBA adatmentés közben leállításkor !!!");
            e.printStackTrace();
        }

        System.out.println("Szerver leállítási folyamat befejeződött.");
    }

    public void removeClient(ClientHandler handler) {
        if (handler != null) {
            if(activeClients.remove(handler)) {
                System.out.println("Kliens kezelő eltávolítva. Aktív kliensek: " + activeClients.size());
            }
        }
    }

    public void broadcastPollStateChange(String joinCode, PollState newState, Object results, ClientHandler senderHandler) {
        if (joinCode == null || newState == null) {
            System.err.println("Érvénytelen paraméterek a broadcastPollStateChange hívásakor.");
            return;
        }
        PollStateChangedNotification stateNotification = new PollStateChangedNotification(joinCode, newState, results);
        System.out.println("Célzott broadcast: Szavazás " + joinCode + " -> " + newState.getDisplayName());

        Poll poll = pollManager.getPollByJoinCode(joinCode);
        if (poll == null) {
            System.err.println("Broadcast hiba: Szavazás nem található: " + joinCode);
            return;
        }
        String creatorUsername = poll.getCreatorUsername();
        ppke.common.dto.PollDTO updatedPollDTO = null;

        boolean sendUpdateNotification = (newState == PollState.VOTING || newState == PollState.RESULTS);
        if (sendUpdateNotification) {
            try {
                updatedPollDTO = poll.toDTO();
            } catch (Exception e) {
                System.err.println("Hiba a PollDTO létrehozásakor az értesítéshez (Szavazás: " + joinCode + "): " + e.getMessage());
                sendUpdateNotification = false;
            }
        }

        int recipients = 0;
        for (ClientHandler client : activeClients) {
            if (client == senderHandler) continue;

            String clientUser = client.getLoggedInUsername();
            String subscribedCode = client.getSubscribedPollJoinCode();

            boolean isCreator = (clientUser != null && creatorUsername != null && clientUser.equals(creatorUsername));
            boolean isSubscribed = (subscribedCode != null && subscribedCode.equals(joinCode));

            if (isCreator || isSubscribed) {
                client.sendMessage(stateNotification);
                recipients++;
                if (sendUpdateNotification && updatedPollDTO != null) {
                    client.sendMessage(new PollUpdateNotification(updatedPollDTO));
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
                sent = true;
                break; // Feltételezzük, egy user csak egyszer aktív
            }
        }
        if (!sent) {
            System.out.println("Nem sikerült direkt üzenetet küldeni: Felhasználó (" + username + ") nem aktív.");
        }
    }

    public static void main(String[] args) {
        System.out.println("Szerver alkalmazás indítása...");
        ServerMain server = new ServerMain();
        server.startServer();
        System.out.println("Szerver alkalmazás leállt.");
    }
}
