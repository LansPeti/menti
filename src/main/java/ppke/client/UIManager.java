package ppke.client;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Stage;
import ppke.client.controller.*;
import ppke.common.dto.*;
import ppke.common.model.PollState;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * A kliensoldali felhasználói felület (GUI) és a nézetek közötti navigáció kezelése JavaFX alatt.
 * Ez az osztály felelős:
 * <ul>
 * <li>Az FXML fájlok betöltéséért és a Scene-ek létrehozásáért/cseréjéért a fő {@link Stage}-en.</li>
 * <li>A {@link ServerConnection}-től érkező szerverválaszok ({@link ppke.common.dto.Response}) fogadásáért és feldolgozásáért.</li>
 * <li>A válaszok alapján a megfelelő UI frissítések elindításáért (mindig a JavaFX Application Thread-en, {@link Platform#runLater(Runnable)} használatával).</li>
 * <li>A szerver adatainak továbbításáért a megfelelő UI Controller felé megjelenítésre.</li>
 * <li>Általános UI visszajelzések (pl. hibaüzenetek, megerősítő dialógusok - {@link Alert}) megjelenítéséért.</li>
 * <li>Navigációs metódusok biztosításáért a különböző képernyők (pl. Login, Profil, Szavazás) közötti váltáshoz.</li>
 * <li>Az aktuális Controller referencia tárolásáért és a Controllereknek való UIManager referencia átadásáért.</li>
 * <li>Az ablak magasságának automatikus növeléséért a jobb láthatóság érdekében.</li>
 * </ul>
 */
public class UIManager {

    /** Referencia a szerverkapcsolatot kezelő objektumra. */
    private final ServerConnection serverConnection;
    /** Az alkalmazás fő ablaka (Stage). */
    private final Stage primaryStage;
    /** Referencia az aktuálisan megjelenített nézet kontrollerére. */
    private Object currentController;
    /** A jelenleg bejelentkezett felhasználó adatai (beleértve a szavazáslistát), frissítve tárolva. */
    private LoginSuccessResponse currentUserLoginData = null;
    /** Bázis elérési út az FXML fájlokhoz az erőforrás mappán belül. */
    private static final String VIEW_PATH = "/ppke/client/view/";
    /** Szorzó az ablakmagasság növeléséhez (1.4 = 40% növekedés). */
    private static final double HEIGHT_INCREASE_FACTOR = 1.4;

    /**
     * Konstruktor a UI Manager inicializálásához.
     * @param connection A {@link ServerConnection} példány (nem lehet null).
     * @param stage Az alkalmazás fő {@link Stage}-e (nem lehet null).
     */
    public UIManager(ServerConnection connection, Stage stage) {
        this.serverConnection = Objects.requireNonNull(connection, "A ServerConnection nem lehet null");
        this.primaryStage = Objects.requireNonNull(stage, "A primaryStage nem lehet null");
        this.serverConnection.setUiManager(this);
        this.primaryStage.setTitle("Mentimeter Klón");
        this.primaryStage.setMinHeight(400);
        this.primaryStage.setMinWidth(500);
        System.out.println("UIManager inicializálva.");
    }

    /**
     * Betölt egy FXML fájlt, beállítja a hozzá tartozó Scene-t a primaryStage-re,
     * eltárolja a Controller referenciáját, és automatikusan növeli az ablak magasságát.
     * Hibát dob, ha a betöltés sikertelen.
     *
     * @param fxmlFile Az FXML fájl neve (pl. "login.fxml") a VIEW_PATH alatt.
     * @param title Az ablak címe ehhez a nézethez.
     * @return A betöltött Controller példánya.
     * @throws RuntimeException Ha az FXML betöltése vagy a Scene beállítása sikertelen.
     */
    private Object loadScene(String fxmlFile, String title) {
        System.out.println("UIManager: Kísérlet a scene betöltésére: " + fxmlFile);
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(VIEW_PATH + fxmlFile));
            if (loader.getLocation() == null) {
                throw new IOException("FXML fájl nem található az erőforrás útvonalon: " + VIEW_PATH + fxmlFile);
            }
            Parent root = loader.load();

            currentController = loader.getController();
            if (currentController instanceof ControllerBase controller) {
                controller.setUIManager(this);
                System.out.println("UIManager: Controller (" + currentController.getClass().getSimpleName() + ") beállítva ehhez: " + fxmlFile);
            } else if (currentController != null) {
                System.err.println("UIManager Figyelmeztetés: A(z) " + fxmlFile + " kontrollere nem ControllerBase típusú!");
            } else {
                System.err.println("UIManager Figyelmeztetés: Nem található controller a(z) " + fxmlFile + " fájlhoz.");
            }

            Scene scene = primaryStage.getScene();
            if (scene == null) {
                scene = new Scene(root);
                primaryStage.setScene(scene);
                System.out.println("UIManager: Új scene beállítva: " + fxmlFile);
            } else {
                scene.setRoot(root);
                System.out.println("UIManager: Scene gyökér frissítve: " + fxmlFile);
            }

            primaryStage.setTitle(title != null ? "Mentimeter Klón - " + title : "Mentimeter Klón");

            primaryStage.sizeToScene();
            double currentHeight = primaryStage.getHeight();
            double newHeight = Math.max(primaryStage.getMinHeight(), currentHeight * HEIGHT_INCREASE_FACTOR);
            primaryStage.setHeight(newHeight);
            System.out.println("UIManager: Ablakmagasság beállítva (" + fxmlFile + "): " + currentHeight + " -> " + newHeight);

            primaryStage.centerOnScreen();
            if (!primaryStage.isShowing()) {
                primaryStage.show();
                System.out.println("UIManager: Stage megjelenítve.");
            }

            System.out.println("UIManager: Scene sikeresen betöltve: " + fxmlFile);
            return currentController;

        } catch (Exception e) {
            System.err.println("!!! UIManager KRITIKUS HIBA FXML betöltésekor: " + fxmlFile + " !!!");
            e.printStackTrace();
            Platform.runLater(() -> showError("Kritikus hiba a(z) '" + fxmlFile + "' felület betöltése közben.\nAz alkalmazás instabil lehet.\n\nHiba: " + e.getMessage(), "UI Betöltési Hiba"));
            throw new RuntimeException("Nem sikerült betölteni a scene-t: " + fxmlFile, e);
        }
    }

    // --- Navigációs Metódusok (Hibakezeléssel) ---
    private void navigateTo(Runnable sceneLoader) {
        try {
            sceneLoader.run();
        } catch (Exception e) {
            System.err.println("UIManager: Navigáció sikertelen!");
            try {
                System.err.println("UIManager: Kísérlet a kezdőképernyőre való visszanavigálásra a hiba után.");
                showStartScreen();
            } catch (Exception ignored) {
                System.err.println("UIManager: KRITIKUS - Nem sikerült visszanavigálni a kezdőképernyőre a hiba után.");
            }
        }
    }

    /** Megjeleníti a kezdőképernyőt (jelenleg a Csatlakozás képernyő). */
    public void showStartScreen() {
        navigateTo(() -> showJoinPollScreen());
    }
    /** Megjeleníti a szavazáshoz csatlakozás képernyőt. */
    public void showJoinPollScreen() { navigateTo(() -> loadScene("join_poll.fxml", "Csatlakozás Szavazáshoz")); }
    /** Megjeleníti a bejelentkezési képernyőt. */
    public void showLoginScreen() { navigateTo(() -> loadScene("login.fxml", "Bejelentkezés")); }
    /** Megjeleníti a regisztrációs képernyőt. */
    public void showRegisterScreen() { navigateTo(() -> loadScene("register.fxml", "Regisztráció")); }
    /** Megjeleníti a felhasználói profilt. */
    public void showProfileScreen(LoginSuccessResponse loginData) { navigateTo(() -> { this.currentUserLoginData = loginData; Object ctrl = loadScene("profile.fxml", "Profil - " + loginData.username()); if (ctrl instanceof ProfileController pc) pc.initializeData(loginData.username(), loginData.userPolls()); }); }
    /** Újra megjeleníti a profil képernyőt a tárolt adatokkal. */
    public void showProfileScreenAgain() { navigateTo(() -> { if (currentUserLoginData != null) showProfileScreen(currentUserLoginData); else { System.err.println("UIManager: showProfileScreenAgain hívva, de nincs user adat!"); showLoginScreen(); } }); }
    /** Megjeleníti az új szavazás létrehozása képernyőt. */
    public void showCreatePollScreen() { navigateTo(() -> loadScene("create_poll.fxml", "Új Szavazás Létrehozása")); }
    /** Megjeleníti a szavazás részleteit/kezelő felületét. */
    public void showPollDetailsScreen(PollDTO poll) { navigateTo(() -> { Object ctrl = loadScene("poll_details.fxml", "Szavazás Részletei - " + poll.name()); if (ctrl instanceof PollDetailsController pdc) pdc.initializeData(poll); }); }
    /** Megjeleníti a várakozó képernyőt csatlakozás után. */
    public void showJoiningWaitingScreen(String joinCode, String pollName) { navigateTo(() -> { Object ctrl = loadScene("waiting.fxml", "Várakozás - " + pollName); if (ctrl instanceof WaitingController wc) { wc.setWaitingFor(joinCode, pollName); wc.setMessage("Csatlakozva ("+joinCode+"). Várakozás a szavazás indítására..."); } }); }
    /** Megjeleníti a szavazási felületet. */
    public void showVotingScreen(PollDTO poll) { navigateTo(() -> { Object ctrl = loadScene("voting.fxml", "Szavazás - " + poll.name()); if (ctrl instanceof VotingController vc) vc.initializeData(poll); }); }
    /** Megjeleníti a várakozás az eredményekre képernyőt. */
    public void showWaitingForResultsScreen(String joinCode, String pollName) { navigateTo(() -> { Object ctrl = loadScene("waiting.fxml", "Várakozás az eredményre - " + pollName); if (ctrl instanceof WaitingController wc) { wc.setWaitingFor(joinCode, pollName); wc.setMessage("Szavazat leadva. Várakozás az eredményekre..."); } }); }
    /** Megjeleníti az eredményeket. */
    public void showResultsScreen(PollDTO pollWithResults, boolean isHostView) { navigateTo(() -> { Object ctrl = loadScene("poll_results.fxml", "Eredmények - " + pollWithResults.name()); if (ctrl instanceof PollResultsController prc) prc.initializeData(pollWithResults, isHostView); }); }
    /** Megjeleníti a kapcsolat megszakadását jelző képernyőt. */
    public void showDisconnectedScreen(String reason) { showError(reason, "Kapcsolat Megszakadt"); navigateTo(this::showStartScreen); }

    // --- Szerver Válasz Kezelése ---
    /** Kezeli a szerverről érkező válaszokat/értesítéseket. */
    public void handleServerResponse(Object response) {
        Platform.runLater(() -> {
            String controllerName = (currentController != null ? currentController.getClass().getSimpleName() : "null");
            String responseName = (response != null ? response.getClass().getSimpleName() : "null");
            System.out.println("UI: Válasz kezelése: " + responseName + ", Aktuális controller: " + controllerName);
            try {
                if (response instanceof ErrorResponse res) { showError(res.errorMessage(), "Szerver Hiba"); }
                else if (response instanceof SuccessResponse res) { handleSuccessResponse(res); }
                else if (response instanceof LoginSuccessResponse res) { handleLoginSuccess(res); }
                else if (response instanceof JoinSuccessResponse res) { handleJoinSuccess(res); }
                else if (response instanceof PollListUpdateResponse res) { handlePollListUpdate(res); }
                else if (response instanceof PollUpdateNotification res) { handlePollUpdate(res); }
                else if (response instanceof PollStateChangedNotification res) { handlePollStateChange(res); }
                else if (response != null) { System.err.println("UI FIGYELMEZTETÉS: Nem kezelt szerver válasz típus: " + responseName); }
                else { System.err.println("UI FIGYELMEZTETÉS: Null válasz érkezett a szerverről."); }
            } catch (Exception e) { System.err.println("!!! UIManager KRITIKUS HIBA szerver válaszának feldolgozása közben: " + responseName + " !!!"); e.printStackTrace(); showError("Kritikus hiba történt a szerver válaszának feldolgozása közben.\nAz alkalmazás instabil lehet.\n\nHiba: " + e.getMessage(), "Belső Kliens Hiba"); try { if (currentUserLoginData != null) showProfileScreenAgain(); else showStartScreen(); } catch (Exception ignored) {} }
        });
    }

    // --- Segédmetódusok a válaszkezeléshez ---
    private void handleSuccessResponse(SuccessResponse res) { String msg = res.message(); System.out.println("UI: SuccessResponse kezelése: " + msg); if (msg != null && !msg.isBlank() && !msg.contains("Feliratkozva") && !msg.contains("Leiratkozva") && !msg.contains("Nem volt aktív")) { showInfo(msg, "Siker"); } if (currentController instanceof CreatePollController && msg != null && msg.contains("Szavazás sikeresen létrehozva")) { System.out.println("UI: Szavazás létrehozás sikeres, vissza a profilhoz."); showProfileScreenAgain(); } else if (msg != null && msg.contains("kijelentkezés")) { currentUserLoginData = null; showLoginScreen(); } else if (msg != null && msg.contains("regisztráció")) { showLoginScreen(); } else if (currentController instanceof VotingController vc && msg != null && msg.contains("Szavazat sikeresen leadva")) { System.out.println("UI: Szavazat leadva, várakozás az eredményre."); showWaitingForResultsScreen(vc.getCurrentPollJoinCode(), vc.getCurrentPollName()); } }
    private void handleLoginSuccess(LoginSuccessResponse res) { System.out.println("UI: LoginSuccessResponse kezelése: " + res.username()); this.currentUserLoginData = res; showProfileScreen(res); }
    private void handleJoinSuccess(JoinSuccessResponse res) { System.out.println("UI: JoinSuccessResponse kezelése: " + res.joinCode()); serverConnection.sendRequest(new SubscribeToPollRequest(res.joinCode())); showJoiningWaitingScreen(res.joinCode(), res.pollName()); }
    private void handlePollListUpdate(PollListUpdateResponse res) { System.out.println("UI: PollListUpdateResponse kezelése: " + res.userPolls().size() + " szavazat."); if (currentUserLoginData != null) { this.currentUserLoginData = new LoginSuccessResponse(currentUserLoginData.username(), res.userPolls()); System.out.println("UI: Tárolt LoginSuccessResponse frissítve."); if (currentController instanceof ProfileController pc) { System.out.println("UI: Aktív ProfileController lista frissítése."); pc.updatePollList(res.userPolls()); } } else { System.out.println("UI: PollListUpdateResponse érkezett, de nincs bejelentkezett felhasználó."); } }
    private void handlePollUpdate(PollUpdateNotification res) { PollDTO updatedPoll = res.updatedPoll(); System.out.println("UI: PollUpdateNotification kezelése: " + updatedPoll.joinCode() + ", új állapot: " + updatedPoll.state()); String currentJoinCode = getCurrentControllerJoinCode(); if (Objects.equals(currentJoinCode, updatedPoll.joinCode())) { System.out.println("UI: Poll frissítés egyezik az aktuális képernyővel (" + currentJoinCode + ")"); if (currentController instanceof PollDetailsController pdc) { pdc.updatePollData(updatedPoll); } else if (currentController instanceof WaitingController wc) { if (updatedPoll.state() == PollState.VOTING) { System.out.println("UI: Váltás: Várakozás -> Szavazás"); showVotingScreen(updatedPoll); } else if (updatedPoll.state() == PollState.RESULTS) { System.out.println("UI: Váltás: Várakozás -> Eredmények (Szavazó)"); showResultsScreen(updatedPoll, false); } } else if (currentController instanceof PollResultsController prc) { prc.initializeData(updatedPoll, prc.isHostViewing()); } else if (currentController instanceof VotingController vc) { System.out.println("UI: PollUpdate érkezett szavazás közben - állapotváltást figyelmen kívül hagyjuk."); } } else { System.out.println("UI: Poll frissítés nem egyezik az aktuális képernyővel (Aktuális: " + currentJoinCode + ", Frissítés: " + updatedPoll.joinCode() + ")"); } if (currentController instanceof ProfileController pc) { pc.updateSinglePollInList(updatedPoll); } }
    private void handlePollStateChange(PollStateChangedNotification res) { String changedJoinCode = res.joinCode(); PollState newState = res.newState(); Object resultsData = res.results(); System.out.println("UI: PollStateChangedNotification kezelése: " + changedJoinCode + " -> " + newState); String currentJoinCode = getCurrentControllerJoinCode(); if (Objects.equals(currentJoinCode, changedJoinCode)) { if (currentController instanceof PollDetailsController pdc) { pdc.updatePollState(newState, resultsData); } } if (currentController instanceof ProfileController pc) { pc.updatePollStateInList(changedJoinCode, newState); } if (newState == PollState.CLOSED && Objects.equals(currentJoinCode, changedJoinCode)) { boolean wasOnRelevantScreen = (currentController instanceof WaitingController || currentController instanceof VotingController || currentController instanceof PollResultsController); if(wasOnRelevantScreen){ System.out.println("UI: Szavazás (" + changedJoinCode + ") lezárva. Leiratkozás és vissza a kezdőképernyőre."); serverConnection.sendRequest(new UnsubscribeRequest()); showError("A szavazást lezárták.", "Szavazás Vége"); showStartScreen(); } } }

    /** Segédfüggvény az aktuális kontrollerhez tartozó join kód lekérésére. */
    private String getCurrentControllerJoinCode() { if (currentController instanceof WaitingController wc) return wc.getWaitingForJoinCode(); if (currentController instanceof VotingController vc) return vc.getCurrentPollJoinCode(); if (currentController instanceof PollResultsController prc) return prc.getCurrentPollJoinCode(); if (currentController instanceof PollDetailsController pdc) return pdc.getCurrentPollJoinCode(); return null; }

    // --- UI Segédmetódusok (Alerts) ---
    public void showError(String message, String title) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.ERROR); a.setTitle(title != null ? title : "Hiba"); a.setHeaderText(null); a.setContentText(message != null ? message : "Ismeretlen hiba."); a.initOwner(primaryStage); a.showAndWait(); }); }
    public void showInfo(String message, String title) { Platform.runLater(() -> { Alert a = new Alert(Alert.AlertType.INFORMATION); a.setTitle(title != null ? title : "Info"); a.setHeaderText(null); a.setContentText(message != null ? message : ""); a.initOwner(primaryStage); a.showAndWait(); }); }
    public boolean showConfirmation(String message, String title) { Alert a = new Alert(Alert.AlertType.CONFIRMATION); a.setTitle(title != null ? title : "Megerősítés"); a.setHeaderText(null); a.setContentText(message != null ? message : "Folytatja?"); a.initOwner(primaryStage); Optional<ButtonType> res = a.showAndWait(); return res.isPresent() && res.get() == ButtonType.OK; }

    // --- Getterek ---
    public ServerConnection getServerConnection() { return serverConnection; }
    public Stage getPrimaryStage() { return primaryStage; }
}