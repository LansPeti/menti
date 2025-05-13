package ppke.client.controller;

import ppke.client.ServerConnection;
import ppke.client.UIManager;

/**
 * Opcionális absztrakt ősosztály a JavaFX kontrollerek számára ebben az alkalmazásban.
 * Közös funkcionalitást biztosít, mint például a {@link UIManager} referencia tárolása
 * és egy kényelmi metódus a {@link ServerConnection} eléréséhez.
 * A konkrét képernyőkhöz tartozó kontrollereknek ebből kell származniuk.
 */
public abstract class ControllerBase {

    /**
     * Referencia a {@link UIManager} példányra, amely ezt a kontrollert kezeli.
     * A UIManager állítja be az FXML betöltése után a {@link #setUIManager(UIManager)} metóduson keresztül.
     * Védett (protected), hogy a leszármazott kontrollerek közvetlenül hozzáférhessenek.
     */
    protected UIManager uiManager;

    /**
     * Beállítja a {@link UIManager} referenciáját ehhez a kontrollerhez.
     * Ezt a metódust az {@link UIManager} hívja meg, miután sikeresen betöltötte
     * az FXML fájlt és példányosította ezt a kontrollert (az {@code UIManager.loadScene}
     * metódusának részeként).
     *
     * @param uiManager A UI Manager példány (nem lehet null).
     */
    public void setUIManager(UIManager uiManager) {
        this.uiManager = uiManager;
    }

    /**
     * Kényelmi metódus a {@link ServerConnection} példány eléréséhez a {@link UIManager}-en keresztül.
     * A leszármazott kontrollerek használhatják ezt a metódust a szerverrel való kommunikációhoz
     * (kérések küldése).
     *
     * @return A {@link ServerConnection} példány.
     * @throws IllegalStateException ha a {@code uiManager} még nincs beállítva (ez normál működés mellett nem fordulhat elő).
     */
    protected ServerConnection getServerConnection() {
        if (uiManager == null) {
            // Ez a hiba nem fordulhat elő, ha az UIManager helyesen működik, de biztonsági ellenőrzésként jó.
            throw new IllegalStateException("Az UIManager még nincs beállítva ehhez a kontrollerhez.");
        }
        return uiManager.getServerConnection();
    }

    // Leszármazott osztályok ide implementálhatják az @FXML annotált mezőket és eseménykezelő metódusokat.
    // Az initialize(URL, ResourceBundle) metódust is implementálhatják, ha szükséges az FXML betöltése utáni inicializáció.
}
