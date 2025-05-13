package ppke.client.controller;

import javafx.event.ActionEvent; import javafx.fxml.FXML;
import javafx.scene.control.Button; import javafx.scene.control.TextField;
import ppke.common.dto.JoinPollRequest;
import ppke.client.UIManager;

import java.util.Locale;

/**
 * A szavazáshoz csatlakozás képernyő (join_poll.fxml) vezérlő osztálya.
 * Lehetővé teszi a felhasználó számára, hogy megadjon egy 8 karakteres join kódot,
 * és elküldje azt a szervernek csatlakozási szándékkal.
 * Lehetőséget ad a bejelentkezési képernyőre való átlépésre is.
 */
public class JoinPollController extends ControllerBase {

    /** Join kód beviteli mező. FXML: fx:id="joinCodeField". */
    @FXML private TextField joinCodeField;
    /** Csatlakozás gomb. FXML: fx:id="joinButton". */
    @FXML private Button joinButton;
    /** Bejelentkezés gomb. FXML: fx:id="loginButton". */
    @FXML private Button loginButton;

    /**
     * Eseménykezelő a "Csatlakozás" gomb megnyomásakor.
     * Kiolvassa a beírt kódot, validálja a formátumát (8 alfanumerikus karakter),
     * nagybetűssé alakítja, majd elküld egy {@link JoinPollRequest}-et a szervernek.
     * Hiba esetén tájékoztatja a felhasználót.
     * Sikeres válasz ({@link ppke.common.dto.JoinSuccessResponse}) esetén az {@link UIManager}
     * átnavigál a várakozó képernyőre.
     *
     * @param event Az eseményt kiváltó ActionEvent (nincs használva).
     */
    @FXML
    void handleJoinButtonAction(ActionEvent event) {
        String joinCode = joinCodeField.getText().trim();

        if (joinCode.isEmpty()) {
            uiManager.showError("A csatlakozási kód megadása kötelező!", "Hiányzó Kód");
            joinCodeField.requestFocus();
            return;
        }
        if (!joinCode.matches("^[a-zA-Z0-9]{8}$")) {
            uiManager.showError("Érvénytelen kód formátum!\nPontosan 8 betű vagy szám karakter szükséges.", "Hibás Kód");
            joinCodeField.requestFocus();
            return;
        }

        String upperCaseCode = joinCode.toUpperCase(Locale.ROOT);
        System.out.println("JoinPollController: Csatlakozási kísérlet a következő kóddal: " + upperCaseCode);

        JoinPollRequest request = new JoinPollRequest(upperCaseCode);
        getServerConnection().sendRequest(request);
    }

    /**
     * Eseménykezelő a "Bejelentkezés / Saját Szavazások" gombra kattintáskor.
     * Átnavigál a bejelentkezési képernyőre.
     *
     * @param event Az eseményt kiváltó ActionEvent (nincs használva).
     */
    @FXML
    void handleLoginButtonAction(ActionEvent event) {
        System.out.println("JoinPollController: Navigálás a bejelentkezési képernyőre.");
        uiManager.showLoginScreen();
    }
}