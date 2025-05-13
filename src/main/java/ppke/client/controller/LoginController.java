package ppke.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ppke.common.dto.LoginRequest;
import ppke.client.UIManager;

/**
 * A bejelentkezési képernyő (login.fxml) vezérlő osztálya.
 * Kezeli a felhasználónév és jelszó beviteli mezőket, valamint a
 * "Bejelentkezés", "Regisztráció" és "Csatlakozás" gombok/linkek eseményeit.
 */
public class LoginController extends ControllerBase {

    /** Felhasználónév beviteli mező. Az FXML fájlban lévő {@code fx:id="usernameField"} elemhez kapcsolódik. */
    @FXML private TextField usernameField;
    /** Jelszó beviteli mező. Az FXML fájlban lévő {@code fx:id="passwordField"} elemhez kapcsolódik. */
    @FXML private PasswordField passwordField;
    /** Regisztrációra ugró hyperlink. Az FXML fájlban lévő {@code fx:id="registerLink"} elemhez kapcsolódik. */
    @FXML private Hyperlink registerLink;
    /** Csatlakozásra ugró hyperlink. Az FXML fájlban lévő {@code fx:id="joinLink"} elemhez kapcsolódik. */
    @FXML private Hyperlink joinLink;

    /**
     * Eseménykezelő a "Bejelentkezés" gomb megnyomásakor.
     * Összegyűjti a felhasználónevet és jelszót, validálja őket (nem üresek-e),
     * majd elküld egy {@link LoginRequest}-et a szervernek a {@link #getServerConnection()} segítségével.
     * Hiba esetén tájékoztatja a felhasználót a {@link UIManager#showError(String, String)} metódussal.
     * Sikeres bejelentkezés esetén a szerver válaszát ({@link ppke.common.dto.LoginSuccessResponse})
     * az {@link UIManager} kezeli, ami átnavigál a profil képernyőre.
     *
     * @param event Az eseményt kiváltó ActionEvent (itt nincs közvetlenül felhasználva).
     */
    @FXML
    void handleLoginButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField.getText();

        // Alap validálás
        if (username.isBlank()) {
            uiManager.showError("A felhasználónév megadása kötelező!", "Hiányzó Adat");
            usernameField.requestFocus(); // Fókusz vissza a hibás mezőre
            return;
        }
        if (password.isEmpty()) { // Üres jelszót nem fogadunk el
            uiManager.showError("A jelszó megadása kötelező!", "Hiányzó Adat");
            passwordField.requestFocus();
            return;
        }

        System.out.println("LoginController: Bejelentkezési kísérlet: " + username);
        LoginRequest request = new LoginRequest(username, password);
        getServerConnection().sendRequest(request);
        // A választ az UIManager kezeli
    }

    /**
     * Eseménykezelő a "Regisztráció" hyperlinkre kattintáskor.
     * Átirányítja a felhasználót a regisztrációs képernyőre a {@link UIManager#showRegisterScreen()} segítségével.
     *
     * @param event Az eseményt kiváltó ActionEvent (itt nincs közvetlenül felhasználva).
     */
    @FXML
    void handleRegisterLinkAction(ActionEvent event) {
        System.out.println("LoginController: Navigálás a regisztrációs képernyőre.");
        uiManager.showRegisterScreen();
    }

    /**
     * Eseménykezelő a "Csatlakozás szavazáshoz" hyperlinkre kattintáskor.
     * Átirányítja a felhasználót a csatlakozási képernyőre a {@link UIManager#showJoinPollScreen()} segítségével.
     *
     * @param event Az eseményt kiváltó ActionEvent (itt nincs közvetlenül felhasználva).
     */
    @FXML
    void handleGoToJoinAction(ActionEvent event) {
        System.out.println("LoginController: Navigálás a csatlakozási képernyőre.");
        uiManager.showJoinPollScreen();
    }
}