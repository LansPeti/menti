package ppke.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import ppke.common.dto.RegisterRequest;
import ppke.client.UIManager;

/**
 * A regisztrációs képernyő (register.fxml) vezérlő osztálya.
 * Kezeli a felhasználónév, jelszó és jelszó megerősítés beviteli mezőit,
 * valamint a "Regisztráció" és "Mégsem" gombok eseményeit.
 */
public class RegisterController extends ControllerBase {

    /** Felhasználónév beviteli mező. Az FXML-ben fx:id="usernameField". */
    @FXML private TextField usernameField;
    /** Első jelszó beviteli mező. Az FXML-ben fx:id="passwordField1". */
    @FXML private PasswordField passwordField1;
    /** Jelszó megerősítése beviteli mező. Az FXML-ben fx:id="passwordField2". */
    @FXML private PasswordField passwordField2;
    /** Regisztráció gomb. Az FXML-ben fx:id="registerButton". */
    @FXML private Button registerButton;
    /** Mégsem gomb. Az FXML-ben fx:id="cancelButton". */
    @FXML private Button cancelButton;

    /**
     * Eseménykezelő a "Regisztráció" gomb megnyomásakor.
     * Összegyűjti a beírt adatokat, elvégzi az alapvető validálást (nem üres, jelszavak egyeznek),
     * majd elküld egy {@link RegisterRequest}-et a szervernek a {@link #getServerConnection()} segítségével.
     * Hiba esetén tájékoztatja a felhasználót a {@link UIManager#showError(String, String)} metódussal.
     * Sikeres regisztráció esetén a szerver válaszát ({@link ppke.common.dto.SuccessResponse})
     * az {@link UIManager} kezeli, ami átnavigál a bejelentkezési képernyőre.
     *
     * @param event Az eseményt kiváltó ActionEvent (itt nincs közvetlenül felhasználva).
     */
    @FXML
    void handleRegisterButtonAction(ActionEvent event) {
        String username = usernameField.getText();
        String password = passwordField1.getText();
        String passwordConfirm = passwordField2.getText();

        // Validálás
        if (username.isBlank()) {
            uiManager.showError("A felhasználónév megadása kötelező!", "Hiányzó Adat");
            usernameField.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            uiManager.showError("A jelszó megadása kötelező!", "Hiányzó Adat");
            passwordField1.requestFocus();
            return;
        }
        if (passwordConfirm.isEmpty()) {
            uiManager.showError("A jelszó megerősítése kötelező!", "Hiányzó Adat");
            passwordField2.requestFocus();
            return;
        }
        if (!password.equals(passwordConfirm)) {
            uiManager.showError("A két jelszó nem egyezik!", "Jelszó Hiba");
            passwordField1.clear();
            passwordField2.clear();
            passwordField1.requestFocus();
            return;
        }

        System.out.println("RegisterController: Regisztrációs kísérlet: " + username);
        RegisterRequest request = new RegisterRequest(username, password);
        getServerConnection().sendRequest(request);
    }

    /**
     * Eseménykezelő a "Mégsem" gombra kattintáskor.
     * Visszanavigál a bejelentkezési képernyőre a {@link UIManager#showLoginScreen()} segítségével.
     *
     * @param event Az eseményt kiváltó ActionEvent (itt nincs közvetlenül felhasználva).
     */
    @FXML
    void handleCancelButtonAction(ActionEvent event) {
        System.out.println("RegisterController: Regisztráció megszakítva, vissza a bejelentkezéshez.");
        uiManager.showLoginScreen();
    }
}