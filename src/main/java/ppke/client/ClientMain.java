package ppke.client;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.TextInputDialog;
import javafx.stage.Stage;
import java.util.Optional;

/**
 * A JavaFX kliens alkalmazás fő belépési pontja.
 * Inicializálja a {@link ServerConnection}-t és a {@link UIManager}-t,
 * opcionálisan bekéri a felhasználótól a szerver adatait, megpróbál kapcsolódni,
 * és megjeleníti a kezdő UI képernyőt a UIManager segítségével.
 * Kezeli az alkalmazás bezárását a szerverkapcsolat bontásával.
 */
public class ClientMain extends Application {

    /** Kezeli a szerver kommunikációt. */
    private ServerConnection serverConnection;
    /** Kezeli a felhasználói felületet és a navigációt. */
    private UIManager uiManager;

    /**
     * A JavaFX alkalmazás indítási metódusa.
     * Ezt a metódust a JavaFX futtatókörnyezet hívja meg az inicializálás után.
     * Beállítja a fő komponenseket, megpróbál kapcsolódni a szerverhez, és megjeleníti a kezdő UI-t.
     *
     * @param primaryStage Az alkalmazás fő ablaka (Stage), amit a JavaFX futtatókörnyezet ad át.
     */
    @Override
    public void start(Stage primaryStage) {
        System.out.println("Mentimeter Kliens indítása...");

        // Fő komponensek inicializálása
        serverConnection = new ServerConnection();
        uiManager = new UIManager(serverConnection, primaryStage); // Átadjuk a Stage-et

        // Kecses leállítás beállítása az ablak bezárásakor
        primaryStage.setOnCloseRequest(event -> {
            System.out.println("Alkalmazás bezárása kérése. Kapcsolat bontása...");
            serverConnection.disconnect(); // Bontjuk a kapcsolatot
            Platform.exit(); // Biztosítja a JavaFX szál leállását
            System.exit(0); // Biztosítja a teljes JVM folyamat leállását
        });

        // Szerver cím és port bekérése (dialógusablakkal)
        String serverAddress = getServerAddressFromUser("localhost"); // Alapértelmezett cím
        int serverPort = getServerPortFromUser(12345); // Alapértelmezett port

        // Ha a felhasználó megszakította a cím/port megadását
        if (serverAddress == null || serverPort == -1) {
            System.out.println("Szerver adatok megadása megszakítva. Kilépés.");
            Platform.exit(); // Kilépés az alkalmazásból
            return;
        }

        // Kapcsolódási kísérlet
        System.out.println("Kísérlet a kapcsolódásra: " + serverAddress + ":" + serverPort + "...");
        boolean connected = serverConnection.connect(serverAddress, serverPort);

        if (connected) {
            // Sikeres kapcsolat esetén kezdőképernyő megjelenítése
            System.out.println("Kapcsolódás sikeres. Kezdőképernyő megjelenítése.");
            uiManager.showStartScreen();
        } else {
            // Sikertelen kapcsolat esetén hibaüzenet (már megtörtént a connect-ben) és kilépés
            System.err.println("Kapcsolódás sikertelen. Az alkalmazás leáll.");
            // Az UIManager már mutatott hibaüzenetet a connect() hívásakor
            Platform.exit(); // Kiléptetjük az alkalmazást
        }
    }

    /**
     * Bekéri a felhasználótól a szerver címét egy dialógusablak segítségével.
     * @param defaultValue Az alapértelmezett érték, ami megjelenik a beviteli mezőben.
     * @return A felhasználó által beírt cím, vagy null, ha megszakította vagy üresen hagyta.
     */
    private String getServerAddressFromUser(String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        dialog.setTitle("Szerver Kapcsolat");
        dialog.setHeaderText("Adja meg a szerver címét (hostname vagy IP):");
        dialog.setContentText("Cím:");
        dialog.initOwner(null); // Független dialógusablak

        Optional<String> result = dialog.showAndWait();
        // Csak akkor fogadjuk el, ha nem üres és nem csak whitespace
        return result.filter(s -> !s.isBlank()).orElse(null);
    }

    /**
     * Bekéri a felhasználótól a szerver portját egy dialógusablak segítségével.
     * Ellenőrzi, hogy a beírt érték érvényes portszám-e (1-65535).
     * @param defaultValue Az alapértelmezett érték, ami megjelenik a beviteli mezőben.
     * @return A felhasználó által beírt portszám, vagy -1, ha érvénytelen vagy megszakította.
     */
    private int getServerPortFromUser(int defaultValue) {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(defaultValue));
        dialog.setTitle("Szerver Kapcsolat");
        dialog.setHeaderText("Adja meg a szerver portját (1-65535):");
        dialog.setContentText("Port:");
        dialog.initOwner(null);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            try {
                int port = Integer.parseInt(result.get().trim()); // Trim whitespace
                if (port > 0 && port <= 65535) {
                    return port; // Érvényes port
                } else {
                    System.err.println("Érvénytelen portszám: " + port);
                    showPortErrorAlert("Érvénytelen portszám: " + port + "\nKérlek, 1 és 65535 közötti számot adj meg.");
                    return -1; // Érvénytelen tartomány
                }
            } catch (NumberFormatException e) {
                System.err.println("Érvénytelen számformátum a portnál: " + result.get());
                showPortErrorAlert("Érvénytelen számformátum a portnál: '" + result.get() + "'");
                return -1; // Nem szám
            }
        } else {
            return -1; // Felhasználó megszakította (Cancel)
        }
    }

    /** Segédfüggvény hibaüzenet megjelenítésére port hiba esetén. */
    private void showPortErrorAlert(String message) {
        // Biztosítjuk, hogy a UI szálon fusson, ha esetleg korai fázisban hívódna meg
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Hibás Port");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }


    /**
     * Az alkalmazás fő (main) metódusa, amely elindítja a JavaFX alkalmazást.
     * @param args Parancssori argumentumok (nem használt).
     */
    public static void main(String[] args) {
        // Elindítja a JavaFX futtatókörnyezetet és meghívja a start() metódust
        launch(args);
    }
}