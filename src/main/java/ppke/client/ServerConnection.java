package ppke.client;

import javafx.application.Platform; // JavaFX Platform műveletekhez
import ppke.common.dto.Request; // Közös Request interfész

import java.io.*; // Input/Output streamek és kivételek
import java.net.Socket; // TCP Socket
import java.net.SocketException; // Specifikus socket kivétel
import java.net.UnknownHostException; // Kivétel ismeretlen hoszthoz
import java.util.Objects; // Segédprogram null ellenőrzésekhez

/**
 * Kezeli a hálózati kapcsolatot és kommunikációt a szerverrel a kliens alkalmazás számára.
 * Felelős a kapcsolódásért, bontásért, kérések ({@link Request} objektumok) küldéséért,
 * valamint a szerver válaszainak ({@link ppke.common.dto.Response} objektumok) aszinkron fogadásáért
 * és továbbításáért a {@link UIManager} felé, hogy az feldolgozza őket a JavaFX Application Thread-en.
 */
public class ServerConnection {
    /** A TCP socket kapcsolat a szerverrel. */
    private Socket socket;
    /** Kimeneti stream szerializált objektumok küldéséhez a szervernek. */
    private ObjectOutputStream out;
    /** Bemeneti stream szerializált objektumok fogadásához a szerverről. */
    private ObjectInputStream in;
    /** Jelzi, hogy a kapcsolat jelenleg aktív-e. Volatile a szálak közötti láthatóságért. */
    private volatile boolean connected = false;
    /** A dedikált szál, amely a szerverről érkező üzeneteket figyeli. */
    private Thread listeningThread;
    /** Referencia a UIManager-re a szerver válaszainak és UI frissítések kezeléséhez. */
    private UIManager uiManager;

    /**
     * Beállítja a {@link UIManager} példányt, amely kezelni fogja a szerverről kapott válaszokat.
     * Ezt a metódust maga az UIManager hívja meg az inicializálása során.
     *
     * @param uiManager A {@link UIManager} példány (nem lehet null).
     * @throws NullPointerException ha az uiManager null.
     */
    public void setUiManager(UIManager uiManager) {
        this.uiManager = Objects.requireNonNull(uiManager, "Az UIManager nem lehet null a ServerConnection-ben");
    }

    /**
     * Megpróbál kapcsolatot létesíteni a megadott címen és porton lévő szerverrel.
     * Ha a kapcsolat sikeres, elindít egy dedikált szálat a szerver válaszainak figyelésére.
     *
     * @param address A szerver hosztneve vagy IP címe.
     * @param port A szerver portszáma.
     * @return {@code true}, ha a kapcsolat sikeresen létrejött, {@code false} egyébként.
     */
    public boolean connect(String address, int port) {
        if (connected) {
            System.out.println("ServerConnection: Már kapcsolódva.");
            return true; // Már kapcsolódtunk
        }
        if (address == null || address.isBlank() || port <= 0 || port > 65535) {
            System.err.println("ServerConnection: Érvénytelen cím vagy port a kapcsolódáshoz.");
            return false;
        }
        try {
            System.out.println("ServerConnection: Kapcsolódás a szerverhez: " + address + ":" + port + "...");
            socket = new Socket(address, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;
            startListeningThread(); // Figyelő szál indítása sikeres kapcsolat esetén
            System.out.println("ServerConnection: Sikeresen kapcsolódva a szerverhez.");
            return true;
        } catch (UnknownHostException e) {
            System.err.println("ServerConnection: Ismeretlen hoszt - " + address);
            if(uiManager != null) Platform.runLater(() -> uiManager.showError("Ismeretlen szerver cím: " + address, "Kapcsolódási Hiba"));
        } catch (IOException e) {
            System.err.println("ServerConnection: Kapcsolódási hiba - " + e.getMessage());
            if(uiManager != null) Platform.runLater(() -> uiManager.showError("Nem sikerült kapcsolódni a szerverhez:\n" + address + ":" + port + "\n" + e.getMessage(), "Kapcsolódási Hiba"));
        }
        // Hiba esetén biztosítjuk az erőforrások visszaállítását
        connected = false;
        closeResources(); // Eltakarítjuk a részlegesen megnyitott erőforrásokat
        return false;
    }

    /**
     * Elindít egy új daemon szálat, amely dedikáltan figyeli a szerverről érkező objektumokat (Response).
     * A kapott objektumokat továbbítja a {@link UIManager#handleServerResponse(Object)} metódusnak
     * feldolgozásra a JavaFX Application Thread-en, a {@link Platform#runLater(Runnable)} segítségével.
     */
    private void startListeningThread() {
        listeningThread = new Thread(() -> {
            System.out.println("ServerConnection: Figyelő szál elindult.");
            try {
                // Folyamatosan olvas objektumokat, amíg kapcsolódva vagyunk és a szál nincs megszakítva
                while (connected && !Thread.currentThread().isInterrupted()) {
                    Object serverResponse = in.readObject();
                    if (uiManager != null) {
                        // Válaszkezelés delegálása az UIManager-nek a JavaFX szálon
                        Platform.runLater(() -> uiManager.handleServerResponse(serverResponse));
                    } else {
                        System.err.println("ServerConnection Figyelmeztetés: Válasz érkezett, de az UIManager null!");
                    }
                }
            } catch (EOFException | SocketException e) {
                // Várható kivételek normális kapcsolatbontáskor vagy hálózati hiba esetén
                if (connected) { // Csak akkor jelezzük, ha nem vártuk a bontást
                    System.out.println("ServerConnection: A szerver kapcsolat bontódott vagy hiba történt: " + e.getMessage());
                    handleDisconnection("Megszakadt a kapcsolat a szerverrel.");
                }
            } catch (IOException e) {
                // Egyéb I/O hiba olvasás közben
                if (connected) {
                    System.err.println("ServerConnection: I/O hiba olvasás közben a szerverről: " + e.getMessage());
                    handleDisconnection("Hiba a szerverrel való kommunikáció során.");
                }
            } catch (ClassNotFoundException e) {
                if (connected) {
                    System.err.println("ServerConnection: ClassNotFound hiba (inkompatibilis szerver verzió?): " + e.getMessage());
                    handleDisconnection("Inkompatibilis válasz érkezett a szerverről.");
                }
            } catch (Exception e) {
                // Bármilyen más váratlan hiba elkapása a figyelő ciklusban
                if (connected) {
                    System.err.println("!!! ServerConnection: Váratlan hiba a figyelő szálban !!!");
                    e.printStackTrace();
                    handleDisconnection("Váratlan belső hiba történt a kliensben.");
                }
            } finally {
                System.out.println("ServerConnection: Figyelő szál leállt.");
                // Biztosítjuk a kapcsolat bontását, ha a ciklus váratlanul ér véget, de a 'connected' még true
                if (connected) {
                    handleDisconnection("A szerver figyelő szál váratlanul leállt.");
                }
            }
        }, "Kliens-Figyelő-Szál"); // Név a szálnak a könnyebb debuggoláshoz

        listeningThread.setDaemon(true); // Lehetővé teszi az alkalmazás kilépését akkor is, ha ez a szál még fut
        listeningThread.start();
    }

    /**
     * Elküld egy {@link Request} objektumot a szervernek a meglévő kapcsolaton keresztül.
     * Ez a metódus szinkronizált a kimeneti streamre, hogy megelőzze a potenciális problémákat,
     * ha több szálból hívnák (bár tipikusan a JavaFX szálból hívjuk).
     * Ha a küldés sikertelen, elindítja a kapcsolatbontási folyamatot.
     *
     * @param request Az elküldendő {@link Request} objektum (nem lehet null).
     * @throws NullPointerException ha a request null.
     */
    public void sendRequest(Request request) {
        Objects.requireNonNull(request, "A küldendő kérés nem lehet null");
        if (connected && out != null) {
            try {
                // Szinkronizálás a kimeneti streamre a szálbiztosságért
                synchronized (out) {
                    out.reset(); // Törli a stream objektum gyorsítótárát, fontos a frissített objektumok küldéséhez
                    out.writeObject(request);
                    out.flush(); // Biztosítja, hogy az objektum azonnal elküldésre kerüljön
                }
            } catch (IOException e) {
                System.err.println("ServerConnection: Hiba a kérés küldése közben (" + request.getClass().getSimpleName() + "): " + e.getMessage());
                // Ha a küldés sikertelen, feltételezzük, hogy a kapcsolat megszakadt
                handleDisconnection("Hiba a kérés küldése közben.");
            }
        } else {
            System.err.println("ServerConnection: Nem lehet kérést küldeni (" + request.getClass().getSimpleName() + "), nincs aktív kapcsolat.");
            // Azonnal jelezzük a hibát a UI-on, ha lehetséges
            if (uiManager != null) {
                Platform.runLater(() -> uiManager.showError("Nem sikerült elküldeni a kérést.\nNincs kapcsolat a szerverrel.", "Küldés Hiba"));
            }
        }
    }

    /**
     * Kezeli a kapcsolat bontását kecsesen.
     * Beállítja a 'connected' flag-et false-ra, megszakítja a figyelő szálat,
     * lezárja a hálózati erőforrásokat (socket, streamek), és értesíti a {@link UIManager}-t,
     * hogy frissítse a UI-t (pl. mutasson hibaüzenetet és lépjen vissza a kezdőképernyőre).
     * Biztosítja, hogy a bontási logika csak egyszer fusson le.
     *
     * @param reason Egy felhasználóbarát üzenet, ami leírja a kapcsolat bontásának okát.
     */
    private void handleDisconnection(String reason) {
        // Biztosítjuk, hogy a bontási logika csak egyszer fusson le
        if (connected) {
            System.out.println("ServerConnection: Kapcsolat bontása folyamatban... Ok: " + reason);
            connected = false; // Flag átállítása azonnal

            // Figyelő szál megszakítása, ha még fut
            if (listeningThread != null && listeningThread.isAlive()) {
                System.out.println("ServerConnection: Figyelő szál megszakítása...");
                listeningThread.interrupt();
            }

            // Streamek és socket lezárása
            closeResources();

            // UI értesítése a JavaFX szálon
            if (uiManager != null) {
                Platform.runLater(() -> uiManager.showDisconnectedScreen(reason));
            }
            System.out.println("ServerConnection: Kapcsolat bontva.");
        } else {
            System.out.println("ServerConnection: Már bontva volt a kapcsolat.");
        }
    }

    /**
     * Segédmetódus a socket és az I/O streamek biztonságos lezárására.
     * Elkapja és figyelmen kívül hagyja a lezárás közbeni esetleges IO kivételeket.
     * Szinkronizált, hogy megelőzze a párhuzamos lezárási kísérleteket.
     */
    private synchronized void closeResources() {
        System.out.println("ServerConnection: Hálózati erőforrások lezárása...");
        // Streamek lezárása először, majd a socket
        try { if (in != null) in.close(); } catch (IOException e) { /* Ignoráljuk */ } finally { in = null; }
        try { if (out != null) out.close(); } catch (IOException e) { /* Ignoráljuk */ } finally { out = null; }
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) { /* Ignoráljuk */ } finally { socket = null; }
        System.out.println("ServerConnection: Hálózati erőforrások lezárva.");
    }

    /**
     * Explicit módon bontja a kapcsolatot a szerverrel.
     * Tipikusan akkor hívjuk meg, amikor a felhasználó kijelentkezik vagy bezárja az alkalmazást.
     */
    public void disconnect() {
        handleDisconnection("Kapcsolat bontva a kliens által.");
    }

    /**
     * Ellenőrzi, hogy a kliens jelenleg kapcsolódva van-e a szerverhez.
     * @return {@code true}, ha kapcsolódva van, {@code false} egyébként.
     */
    public boolean isConnected() {
        // Ellenőrizzük a flag-et és a socket állapotát is a biztonság kedvéért
        return connected && socket != null && socket.isConnected() && !socket.isClosed();
    }
}