package ppke.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import ppke.common.dto.UnsubscribeRequest;
import ppke.client.UIManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * A várakozó képernyő (waiting.fxml) vezérlő osztálya.
 * Ezt a képernyőt látja a szavazó, miután sikeresen csatlakozott egy szavazáshoz
 * (de az még nem indult el), vagy miután leadta a szavazatát és az eredményekre vár.
 * Megjelenít egy folyamatjelzőt és egy tájékoztató üzenetet.
 * Lehetőséget ad a várakozás megszakítására és kilépésre (leiratkozással).
 * Implementálja az {@link Initializable} interfészt.
 */
public class WaitingController extends ControllerBase implements Initializable {

    @FXML private ProgressIndicator progressIndicator;
    @FXML private Label messageLabel;
    @FXML private Button cancelButton;

    private String waitingForJoinCode = null;
    private String pollName = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        progressIndicator.setProgress(ProgressIndicator.INDETERMINATE_PROGRESS);
        System.out.println("WaitingController inicializálva.");
    }

    /**
     * Beállítja a várakozási üzenetet a képernyőn.
     * Ezt a {@link UIManager} hívja meg a képernyő betöltésekor.
     * Biztosítja, hogy a frissítés a JavaFX Application Thread-en történjen.
     * @param message A megjelenítendő üzenet. Null esetén alapértelmezett üzenetet használ.
     */
    public void setMessage(String message) {
        Platform.runLater(() -> {
            messageLabel.setText(message != null ? message : "Várakozás...");
            System.out.println("WaitingController: Üzenet beállítva: " + messageLabel.getText().replace("\n", "\\n"));
        });
    }

    /**
     * Beállítja, hogy melyik szavazásra (join kód és név alapján) várunk.
     * Ezt a {@link UIManager} hívja meg a képernyő betöltésekor.
     * @param joinCode A várt szavazás (nagybetűs) kódja.
     * @param name A szavazás neve.
     */
    public void setWaitingFor(String joinCode, String name) {
        this.waitingForJoinCode = joinCode;
        this.pollName = name;
        System.out.println("WaitingController: Várakozás erre a szavazásra: " + joinCode + " ('" + name + "')");
    }

    /**
     * Visszaadja annak a szavazásnak a join kódját, amelyre ez a képernyő vár.
     * @return A join kód (String), vagy null, ha nincs beállítva.
     */
    public String getWaitingForJoinCode() { return waitingForJoinCode; }
    /**
     * Visszaadja annak a szavazásnak a nevét, amelyre ez a képernyő vár.
     * @return A szavazás neve (String), vagy null, ha nincs beállítva.
     */
    public String getPollName() { return pollName; }

    /**
     * Eseménykezelő a "Mégsem / Kilépés" gombra kattintáskor.
     * Megerősítést kér a felhasználótól, majd ha igen, elküld egy leiratkozási kérést
     * a szervernek ({@link UnsubscribeRequest}), és visszanavigál a kezdőképernyőre.
     * @param event Az eseményt kiváltó ActionEvent (nincs használva).
     */
    @FXML
    void handleCancelButtonAction(ActionEvent event) {
        System.out.println("WaitingController: 'Mégsem' gomb megnyomva.");
        boolean confirmed = uiManager.showConfirmation("Biztosan megszakítod a várakozást és kilépsz?", "Megerősítés");
        if(confirmed) {
            System.out.println("WaitingController: Leiratkozás és navigálás a kezdőképernyőre.");
            getServerConnection().sendRequest(new UnsubscribeRequest());
            uiManager.showStartScreen();
        } else {
            System.out.println("WaitingController: Kilépés megszakítva.");
        }
    }
}