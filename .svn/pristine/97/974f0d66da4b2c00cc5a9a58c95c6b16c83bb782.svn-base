package ppke.server;

import ppke.common.dto.*;
import ppke.common.model.PollState;
import ppke.server.model.Poll;
import ppke.server.model.User;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.List;
import java.util.Objects;
import java.util.Locale;
/**
 * Egyedi klienskapcsolat kezelése külön szálon a szerver oldalon.
 * Felelős a klienstől érkező kérések ({@link Request} objektumok) fogadásáért, feldolgozásáért
 * (a {@link UserManager}, {@link PollManager} segítségével), és a válaszok/értesítések
 * ({@link Response} objektumok) visszaküldéséért a kliensnek.
 * Kezeli a bejelentkezett felhasználó állapotát és a szavazásokra való feliratkozást.
 * {@link Runnable} interfészt implementál, hogy {@link java.util.concurrent.ExecutorService}-ben futtatható legyen.
 */
public class ClientHandler implements Runnable {
    /** A klienshez tartozó socket kapcsolat. */
    private final Socket clientSocket;
    /** Kimeneti stream objektumok küldéséhez. */
    private ObjectOutputStream out;
    /** Bemeneti stream objektumok fogadásához. */
    private ObjectInputStream in;
    /** Referencia a felhasználókat kezelő managerre. */
    private final UserManager userManager;
    /** Referencia a szavazásokat kezelő managerre. */
    private final PollManager pollManager;
    /** Referencia a perzisztenciát kezelő managerre. */
    private final PersistenceManager persistenceManager;
    /** Referencia a fő szerver objektumra (pl. broadcast üzenetek küldéséhez). */
    private final ServerMain server;
    /** Az ehhez a handlerhez tartozó bejelentkezett felhasználó. `volatile` a szálak közötti láthatóságért. */
    private volatile User loggedInUser = null;
    /** Jelzi, hogy a handler futó állapotban van-e. `volatile` a szálak közötti láthatóságért. */
    private volatile boolean running = true;
    /**
     * Annak a szavazásnak a (nagybetűs) join kódja, amelyre ez a kliens (mint szavazó) feliratkozott.
     * `volatile` a szálak közötti láthatóságért.
     */
    private volatile String subscribedPollJoinCode = null;

    /**
     * Konstruktor a klienskezelő inicializálásához.
     * @param socket A klienshez tartozó socket (nem lehet null).
     * @param um A felhasználókat kezelő manager (nem lehet null).
     * @param pm A szavazásokat kezelő manager (nem lehet null).
     * @param psm A perzisztenciát kezelő manager (nem lehet null).
     * @param server A fő szerver objektum (nem lehet null).
     */
    public ClientHandler(Socket socket, UserManager um, PollManager pm, PersistenceManager psm, ServerMain server) {
        this.clientSocket = Objects.requireNonNull(socket, "A kliens socket nem lehet null");
        this.userManager = Objects.requireNonNull(um, "A UserManager nem lehet null");
        this.pollManager = Objects.requireNonNull(pm, "A PollManager nem lehet null");
        this.persistenceManager = Objects.requireNonNull(psm, "A PersistenceManager nem lehet null");
        this.server = Objects.requireNonNull(server, "A ServerMain nem lehet null");
    }

    /**
     * A klienskezelő szál fő logikája.
     * Létrehozza a kommunikációs streameket, majd egy ciklusban olvassa a bejövő
     * {@link Request} objektumokat, feldolgozza őket a {@link #processRequest(Request)}
     * metódussal, és kezeli a kapcsolat bontását vagy a hibákat.
     */
    @Override
    public void run() {
        String clientInfo = "ismeretlen"; // Kezdeti info
        try {
            clientInfo = clientSocket.getRemoteSocketAddress().toString();
            out = new ObjectOutputStream(clientSocket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(clientSocket.getInputStream());
            System.out.println("ClientHandler elindult: " + clientInfo);

            Object requestObject;
            while (running && !clientSocket.isClosed() && (requestObject = in.readObject()) != null) {
                if (requestObject instanceof Request request) {
                    processRequest(request);
                } else {
                    System.err.println("ClientHandler (" + getLoggedInUsernameSafe() + "): Nem Request típusú objektum érkezett: " + requestObject.getClass().getName());
                }
            }
            System.out.println("ClientHandler (" + getLoggedInUsernameSafe() + "): A bemeneti stream lezárult vagy a handler leállt.");

        } catch (EOFException | SocketException e) {
            System.out.println("ClientHandler (" + getLoggedInUsernameSafe() + "): A kapcsolat lezárult vagy hiba történt: " + e.getMessage());
        } catch (IOException e) {
            if(running) System.err.println("ClientHandler I/O Hiba (" + getLoggedInUsernameSafe() + "): " + e.getMessage());
        } catch (ClassNotFoundException e) {
            if(running) System.err.println("ClientHandler ClassNotFound Hiba (" + getLoggedInUsernameSafe() + "): " + e.getMessage());
        } catch (Exception e) {
            if(running) {
                System.err.println("!!! ClientHandler Váratlan Hiba (" + getLoggedInUsernameSafe() + ") !!!");
                e.printStackTrace();
            }
        }
        finally {
            closeConnection();
        }
    }

    /**
     * Feldolgozza a klienstől érkezett {@link Request} objektumot.
     * Azonosítja a kérés típusát hagyományos `if-else if` és `instanceof` segítségével,
     * majd meghívja a megfelelő al-kezelő metódust.
     * Kezeli a belső kivételeket és hiba esetén {@link ErrorResponse}-t küld.
     *
     * @param request A klienstől kapott {@link Request} objektum.
     */
    private void processRequest(Request request) {
        Object response = null;
        String requestType = request.getClass().getSimpleName();
        System.out.println("ClientHandler (" + getLoggedInUsernameSafe() + "): Kérés feldolgozása: " + requestType);

        try {
            if (request instanceof LoginRequest) {
                response = handleLogin((LoginRequest) request);
            } else if (request instanceof RegisterRequest) {
                response = handleRegister((RegisterRequest) request);
            } else if (request instanceof LogoutRequest) {
                response = handleLogout(); // Nincs paraméter
            } else if (request instanceof CreatePollRequest) {
                response = handleCreatePoll((CreatePollRequest) request);
            } else if (request instanceof EditPollRequest) {
                response = handleEditPoll((EditPollRequest) request);
            } else if (request instanceof DeletePollRequest) {
                response = handleDeletePoll((DeletePollRequest) request);
            } else if (request instanceof ResetResultsRequest) {
                response = handleResetResults((ResetResultsRequest) request);
            } else if (request instanceof JoinPollRequest) {
                response = handleJoinPoll((JoinPollRequest) request);
            } else if (request instanceof SubmitVoteRequest) {
                response = handleSubmitVote((SubmitVoteRequest) request);
            } else if (request instanceof ChangePollStateRequest) {
                response = handleChangePollState((ChangePollStateRequest) request);
            } else if (request instanceof SubscribeToPollRequest) {
                response = handleSubscribe((SubscribeToPollRequest) request);
            } else if (request instanceof UnsubscribeRequest) {
                response = handleUnsubscribe(); // Nincs paraméter
            }
            // Ismeretlen kérés
            else {
                System.err.println("ClientHandler (" + getLoggedInUsernameSafe() + "): Nem kezelt kérés típus: " + requestType);
                response = new ErrorResponse("Ismeretlen szerver oldali kérés típus.");
            }
        } catch (ClassCastException cce) {
            // Ez nem fordulhatna elő, ha az instanceof helyes, de biztonság kedvéért
            System.err.println("!!! ClientHandler HIBA a(z) " + requestType + " feldolgozása közben (típuskonverzió) !!!");
            cce.printStackTrace();
            response = new ErrorResponse("Szerver oldali belső hiba történt a kérés feldolgozása közben.");
        } catch (Exception e) {
            // Váratlan hiba a specifikus handler metódusokban
            System.err.println("!!! ClientHandler HIBA a(z) " + requestType + " feldolgozása közben (" + getLoggedInUsernameSafe() + ") !!!");
            e.printStackTrace();
            response = new ErrorResponse("Szerver oldali váratlan hiba történt a kérés feldolgozása közben.");
        }

        if (response != null) {
            sendMessage(response);
        } else {
            System.out.println("ClientHandler (" + getLoggedInUsernameSafe() + "): Nem generáltunk közvetlen választ a(z) " + requestType + " kérésre.");
        }
    }

    // --- Specifikus Kéréskezelő Metódusok ---

    private Response handleLogin(LoginRequest req) {
        if (loggedInUser != null) return new ErrorResponse("Már be van jelentkezve!");
        User user = userManager.loginUser(req.username(), req.password());
        if (user != null) {
            this.loggedInUser = user;
            // Itt már a PollDTO típust kell visszaadnia a PollManagernek
            List<ppke.common.dto.PollDTO> userPolls = pollManager.getPollsForUserDTO(user);
            return new LoginSuccessResponse(user.getUsername(), userPolls);
        } else return new ErrorResponse("Hibás felhasználónév vagy jelszó!");
    }

    private Response handleRegister(RegisterRequest req) {
        if (loggedInUser != null) return new ErrorResponse("Regisztrációhoz ki kell jelentkezni!");
        boolean success = userManager.registerUser(req.username(), req.password());
        if (success) {
            persistenceManager.saveUsers(userManager);
            return new SuccessResponse("Sikeres regisztráció!");
        } else return new ErrorResponse("Felhasználónév már foglalt!");
    }

    private Response handleLogout() {
        if (loggedInUser != null) {
            System.out.println("Felhasználó kijelentkezik: " + loggedInUser.getUsername());
            this.loggedInUser = null;
            this.subscribedPollJoinCode = null;
            return new SuccessResponse("Sikeres kijelentkezés.");
        } else return new ErrorResponse("Nincs bejelentkezett felhasználó.");
    }

    private Response handleCreatePoll(CreatePollRequest req) {
        if (loggedInUser == null) return new ErrorResponse("Szavazás létrehozásához be kell jelentkezni!");
        String joinCode = req.joinCode();
        if (joinCode == null || joinCode.isBlank()) {
            if (req.autoGenerateCode()) joinCode = pollManager.generateUniqueJoinCode();
            else return new ErrorResponse("Join kód megadása vagy automatikus generálás kérése szükséges!");
        }
        Poll createdPoll = pollManager.createPoll(loggedInUser, req.pollData(), joinCode);
        if (createdPoll != null) {
            persistenceManager.savePolls(pollManager);
            persistenceManager.saveUsers(userManager);
            // Itt már a PollDTO típust kell visszaadnia a PollManagernek
            sendMessage(new PollListUpdateResponse(pollManager.getPollsForUserDTO(loggedInUser)));
            return new SuccessResponse("Szavazás sikeresen létrehozva. Kód: " + createdPoll.getJoinCode());
        } else return new ErrorResponse("Hiba a szavazás létrehozásakor (pl. kód foglalt, érvénytelen adat).");
    }

    private Response handleEditPoll(EditPollRequest req) {
        if (loggedInUser == null) return new ErrorResponse("Szavazás szerkesztéséhez be kell jelentkezni!");
        boolean success = pollManager.editPoll(loggedInUser, req.joinCode(), req.pollData());
        if (success) {
            persistenceManager.savePolls(pollManager);
            Poll updatedPoll = pollManager.getPollByJoinCode(req.joinCode());
            if (updatedPoll != null) sendMessage(new PollUpdateNotification(updatedPoll.toDTO()));
            return new SuccessResponse("Szavazás sikeresen módosítva.");
        } else return new ErrorResponse("Szavazás módosítása sikertelen (pl. nincs jogosultság, rossz állapot, érvénytelen adat).");
    }

    private Response handleDeletePoll(DeletePollRequest req) {
        if (loggedInUser == null) return new ErrorResponse("Szavazás törléséhez be kell jelentkezni!");
        boolean success = pollManager.deletePoll(loggedInUser, req.joinCode());
        if (success) {
            persistenceManager.savePolls(pollManager);
            persistenceManager.saveUsers(userManager);
            // Itt már a PollDTO típust kell visszaadnia a PollManagernek
            sendMessage(new PollListUpdateResponse(pollManager.getPollsForUserDTO(loggedInUser)));
            return new SuccessResponse("Szavazás sikeresen törölve.");
        } else return new ErrorResponse("Szavazás törlése sikertelen (pl. nem található, nincs jogosultság).");
    }

    private Response handleResetResults(ResetResultsRequest req) {
        if (loggedInUser == null) return new ErrorResponse("Eredmények nullázásához be kell jelentkezni!");
        boolean success = pollManager.resetPollResults(loggedInUser, req.joinCode());
        if (success) {
            persistenceManager.savePolls(pollManager);
            Poll updatedPoll = pollManager.getPollByJoinCode(req.joinCode());
            if (updatedPoll != null) sendMessage(new PollUpdateNotification(updatedPoll.toDTO()));
            return new SuccessResponse("Szavazás eredményei sikeresen nullázva.");
        } else return new ErrorResponse("Eredmények nullázása sikertelen (pl. nem található, nincs jogosultság).");
    }

    private Response handleJoinPoll(JoinPollRequest req) {
        Poll poll = pollManager.validateJoinCodeForJoining(req.joinCode());
        if (poll != null) {
            return new JoinSuccessResponse(poll.getJoinCode(), poll.getName(), poll.getCurrentState());
        } else return new ErrorResponse("Érvénytelen vagy jelenleg nem elérhető csatlakozási kód!");
    }

    private Response handleSubmitVote(SubmitVoteRequest req) {
        boolean success = pollManager.submitVote(req.joinCode(), req.voteData());
        if (success) {
            persistenceManager.savePolls(pollManager);
            return new SuccessResponse("Szavazat sikeresen leadva!");
        } else return new ErrorResponse("Szavazat leadása sikertelen (pl. lejárt az idő, érvénytelen adat, rossz állapot).");
    }

    private Response handleChangePollState(ChangePollStateRequest req) {
        if (loggedInUser == null) return new ErrorResponse("Állapotváltoztatáshoz be kell jelentkezni!");
        boolean success = pollManager.changePollState(loggedInUser, req.joinCode(), req.newState());
        if (success) {
            persistenceManager.savePolls(pollManager);
            Poll updatedPoll = pollManager.getPollByJoinCode(req.joinCode());
            if (updatedPoll != null) {
                sendMessage(new PollUpdateNotification(updatedPoll.toDTO()));
                Object resultsForNotify = (req.newState() == PollState.RESULTS) ? updatedPoll.getFormattedResults() : null;
                server.broadcastPollStateChange(updatedPoll.getJoinCode(), req.newState(), resultsForNotify, this);
                return new SuccessResponse("Szavazás állapota sikeresen módosítva: " + req.newState().getDisplayName());
            } else {
                System.err.println("KRITIKUS: Szavazás nem található sikeres állapotváltás után! Kód: " + req.joinCode().toUpperCase(Locale.ROOT));
                return new ErrorResponse("Belső hiba: Szavazás nem található az állapotváltás után.");
            }
        } else return new ErrorResponse("Állapotváltoztatás sikertelen (pl. nincs jogosultság, érvénytelen állapotátmenet).");
    }

    private Response handleSubscribe(SubscribeToPollRequest req) {
        Poll poll = pollManager.getPollByJoinCode(req.joinCode());
        if (poll != null) {
            this.subscribedPollJoinCode = poll.getJoinCode();
            System.out.println("Kliens (" + getLoggedInUsernameSafe() + ") feliratkozott a szavazásra: " + this.subscribedPollJoinCode);
            return new SuccessResponse("Feliratkozva: " + this.subscribedPollJoinCode);
        } else {
            System.err.println("Feliratkozási kísérlet sikertelen: Szavazás (" + req.joinCode().toUpperCase(Locale.ROOT) + ") nem található.");
            return new ErrorResponse("Feliratkozás sikertelen: A szavazás nem található.");
        }
    }

    private Response handleUnsubscribe() {
        if (this.subscribedPollJoinCode != null) {
            System.out.println("Kliens (" + getLoggedInUsernameSafe() + ") leiratkozott a szavazásról: " + this.subscribedPollJoinCode);
            this.subscribedPollJoinCode = null;
            return new SuccessResponse("Leiratkozva.");
        } else {
            return new SuccessResponse("Nem volt aktív feliratkozás.");
        }
    }

    /**
     * Elküld egy válasz/értesítés objektumot a kliensnek.
     * Szálbiztos módon kezeli a kimeneti streamet. Ha a küldés sikertelen, bontja a kapcsolatot.
     * @param response A küldendő {@link Response} objektum.
     */
    public synchronized void sendMessage(Object response) {
        if (!running || out == null || clientSocket.isClosed()) {
            System.err.println("Nem lehet üzenetet küldeni, a kapcsolat nem aktív: " + getLoggedInUsernameSafe());
            return;
        }
        try {
            out.reset();
            out.writeObject(response);
            out.flush();
        } catch (IOException e) {
            System.err.println("Hiba üzenet küldésekor a kliensnek ("+ getLoggedInUsernameSafe() +"): " + e.getMessage());
            closeConnection();
        }
    }

    /**
     * Lezárja a klienskapcsolatot és felszabadítja az erőforrásokat.
     * Eltávolítja a handlert a szerver aktív kliensei közül.
     * Beállítja a futás jelzőt false-ra, hogy a fő ciklus leálljon.
     * Szálbiztos (bár általában a handler saját szála hívja meg).
     */
    private void closeConnection() {
        if (running) {
            running = false;
            String clientInfo = getLoggedInUsernameSafe();
            System.out.println("Kapcsolat bontása: " + clientInfo);
            server.removeClient(this);
            subscribedPollJoinCode = null;

            try { if (in != null) in.close(); } catch (IOException e) { System.err.println("Hiba a bemeneti stream lezárásakor ("+clientInfo+"): "+e.getMessage()); } finally { in = null; }
            try { if (out != null) out.close(); } catch (IOException e) { System.err.println("Hiba a kimeneti stream lezárásakor ("+clientInfo+"): "+e.getMessage()); } finally { out = null; }
            try { if (clientSocket != null && !clientSocket.isClosed()) clientSocket.close(); } catch (IOException e) { System.err.println("Hiba a socket lezárásakor ("+clientInfo+"): "+e.getMessage()); }

            System.out.println("ClientHandler leállt: " + clientInfo);
        }
    }

    /**
     * Visszaadja a bejelentkezett felhasználó nevét vagy a kliens címét a logoláshoz.
     * @return A felhasználónév vagy az IP cím/hostname.
     */
    private String getLoggedInUsernameSafe() {
        User user = loggedInUser;
        if (user != null) {
            return user.getUsername();
        } else {
            try {
                return (clientSocket != null && !clientSocket.isClosed()) ? clientSocket.getRemoteSocketAddress().toString() : "ismeretlen-kliens";
            } catch (Exception e) {
                return "ismeretlen-kliens-hiba";
            }
        }
    }

    /**
     * Visszaadja a bejelentkezett felhasználó nevét.
     * @return A felhasználónév, vagy null, ha nincs bejelentkezve senki ezen a kapcsolaton.
     */
    public String getLoggedInUsername() {
        User user = loggedInUser;
        return (user != null) ? user.getUsername() : null;
    }

    /**
     * Visszaadja annak a szavazásnak a (nagybetűs) join kódját, amelyre ez a kliens
     * (mint szavazó) feliratkozott.
     * @return A join kód, vagy null, ha nincs feliratkozva.
     */
    public String getSubscribedPollJoinCode() {
        return subscribedPollJoinCode;
    }
}
