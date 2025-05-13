/**
 * Modul definíció
 * Meghatározza a szükséges függőségeket és
 * megnyitja a szükséges csomagokat a JavaFX FXML betöltő és indító számára.
 */
module ppke.mentimeter { // A modul neve (konzisztens a pom.xml artifactId-vel)

    // Szükséges JavaFX modulok
    requires javafx.controls; // Alap vezérlők
    requires javafx.fxml;    // FXML betöltéshez
    requires javafx.graphics; // Alap grafika, Scene, Stage, Charts


    // Csomagok, amiket megnyitunk a JavaFX FXML betöltő számára (reflexióhoz)
    opens ppke.client.controller to javafx.fxml; // Controller osztályok és @FXML mezők eléréséhez

    // Csomagok, amiket exportálunk (hogy más modulok is elérhessék az osztályokat)
    exports ppke.client; // Szükséges, hogy a javafx.graphics indítani tudja a ClientMain-t
    exports ppke.common.dto; // DTO-k elérhetővé tétele
    exports ppke.common.model; // Enumok elérhetővé tétele
}