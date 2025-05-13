package ppke.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import ppke.common.dto.*;
import ppke.common.model.PollType;
import ppke.client.UIManager;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A szavazási képernyő (voting.fxml) vezérlő osztálya.
 * Felelős a szavazás típusának megfelelő UI dinamikus felépítéséért,
 * a felhasználói input összegyűjtéséért és a szavazat elküldéséért a szervernek.
 */
public class VotingController extends ControllerBase {

    @FXML private Label pollNameLabel;
    @FXML private Label pollQuestionLabel;
    @FXML private VBox votingArea;
    @FXML private Button submitButton;
    @FXML private Button cancelButton;

    private PollDTO currentPoll;
    private Object voteInputControl; // Általános referencia a fő input konténerre/csoportra
    private Map<String, Node> aspectInputControls; // Scale típushoz: Szempont neve -> Input elem (pl. Slider)

    /**
     * Adatok inicializálása és a UI felépítése a szavazás típusa alapján.
     * Ezt a {@link UIManager} hívja meg a nézet betöltésekor.
     * @param poll A szavazás adatai (DTO). Nem lehet null.
     */
    public void initializeData(PollDTO poll) {
        this.currentPoll = Objects.requireNonNull(poll);
        System.out.println("VotingController: Inicializálás: " + poll.joinCode() + " - Típus: " + poll.type());
        pollNameLabel.setText(poll.name());
        pollQuestionLabel.setText(poll.question());
        pollQuestionLabel.setWrapText(true);
        buildVotingUI(poll);
    }

    /** Felépíti a szavazási UI elemeket a {@code votingArea} VBox-ba a poll típusa alapján. */
    private void buildVotingUI(PollDTO poll) {
        votingArea.getChildren().clear();
        votingArea.setSpacing(10);
        voteInputControl = null;
        aspectInputControls = new HashMap<>(); // Minden UI építéskor ürítjük
        submitButton.setDisable(false);
        System.out.println("VotingController: UI építése: " + poll.type());
        try {
            Label instr;
            switch (poll.type()) {
                case MULTIPLE_CHOICE:
                    instr = new Label("Válassz egyet:");
                    votingArea.getChildren().add(instr);
                    Node mcNode = buildMultipleChoiceUI(poll.options());
                    if (mcNode == null) submitButton.setDisable(true);
                    else votingArea.getChildren().add(mcNode);
                    break;
                case WORD_CLOUD:
                    instr = new Label("Írj max 3 szót/kifejezést (max 25 kar/sor), új sorba:");
                    instr.setWrapText(true);
                    votingArea.getChildren().add(instr);
                    Node wcNode = buildWordCloudUI();
                    if (wcNode != null) votingArea.getChildren().add(wcNode);
                    else submitButton.setDisable(true);
                    break;
                case SCALE:
                    if (poll.aspects() == null || poll.aspects().isEmpty()) {
                        votingArea.getChildren().add(new Label("Hiba: Nincsenek szempontok definiálva ehhez a skála szavazáshoz."));
                        submitButton.setDisable(true);
                        return;
                    }
                    instr = new Label("Add meg az értékeléseidet az alábbi szempontokra (" + poll.scaleMin() + "-" + poll.scaleMax() + " skálán):");
                    instr.setWrapText(true);
                    votingArea.getChildren().add(instr);
                    Node scaleNode = buildScaleUI(poll.aspects(), poll.scaleMin(), poll.scaleMax());
                    if (scaleNode != null) votingArea.getChildren().add(scaleNode);
                    else submitButton.setDisable(true);
                    break;
                default:
                    Label unsupp = new Label("Nem támogatott szavazástípus.");
                    votingArea.getChildren().add(unsupp);
                    submitButton.setDisable(true);
            }
        } catch (Exception e) {
            handleUIBuildError("UI építési hiba.", e);
        }
    }

    /** Létrehozza a Multiple Choice UI elemeket (RadioButton-ok). */
    private Node buildMultipleChoiceUI(List<String> options) {
        if (options == null || options.isEmpty()) {
            votingArea.getChildren().add(new Label("Hiba: Nincsenek opciók a feleletválasztós szavazáshoz."));
            this.voteInputControl = null;
            return null;
        }
        VBox box = new VBox(8);
        box.setPadding(new Insets(5, 0, 5, 10));
        ToggleGroup group = new ToggleGroup();
        this.voteInputControl = group; // A ToggleGroup lesz az input kontroll
        options.forEach(opt -> {
            RadioButton rb = new RadioButton(opt);
            rb.setToggleGroup(group);
            rb.setWrapText(true);
            rb.setPadding(new Insets(2));
            box.getChildren().add(rb);
        });
        System.out.println("VotingController: MC UI felépítve " + options.size() + " opcióval.");
        return box;
    }

    /** Létrehozza a Word Cloud UI elemet (TextArea). */
    private Node buildWordCloudUI() {
        TextArea ta = new TextArea();
        ta.setPromptText("Szó1\nSzó2\nSzó3...");
        ta.setPrefRowCount(4); // Max 3 sor + egy kis hely
        ta.setWrapText(true);
        // Karakter limit per sor (kb. 25*3 + 2 sortörés = 77)
        ta.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().length() <= 80 ? change : null));
        this.voteInputControl = ta; // A TextArea lesz az input kontroll
        System.out.println("VotingController: WC UI (TextArea) felépítve.");
        return ta;
    }

    /** Létrehozza a Scale (Skála) UI elemeket (pl. Sliderek). */
    private Node buildScaleUI(List<String> aspects, int min, int max) {
        VBox aspectContainer = new VBox(10); // Konténer az összes szempontnak
        aspectContainer.setPadding(new Insets(5, 0, 5, 10));
        this.voteInputControl = aspectContainer; // A VBox maga lesz a "fő" input kontroll a validációhoz

        for (String aspect : aspects) {
            HBox itemBox = new HBox(10); // Konténer egy szempontnak (label + slider + érték)
            itemBox.setAlignment(Pos.CENTER_LEFT);

            Label aspectLabel = new Label(aspect + ":");
            aspectLabel.setMinWidth(150); // Szélesség, hogy a sliderek egy vonalban kezdődjenek
            aspectLabel.setWrapText(true);

            Slider slider = new Slider(min, max, min + (max - min) / 2.0); // Kezdeti érték középen
            slider.setMajorTickUnit(Math.max(1, (max - min) / 10.0)); // Kb. 10 fő osztás
            slider.setMinorTickCount(0); // Nincsenek alosztások
            slider.setShowTickMarks(true);
            slider.setShowTickLabels(true);
            slider.setSnapToTicks(true); // Egész értékekre ugrik
            HBox.setHgrow(slider, javafx.scene.layout.Priority.ALWAYS); // Slider töltse ki a helyet

            Label sliderValueLabel = new Label(String.valueOf((int) slider.getValue()));
            sliderValueLabel.setMinWidth(30); // Hely az értéknek
            slider.valueProperty().addListener((obs, oldVal, newVal) -> {
                sliderValueLabel.setText(String.valueOf(newVal.intValue()));
            });

            itemBox.getChildren().addAll(aspectLabel, slider, sliderValueLabel);
            aspectContainer.getChildren().add(itemBox);
            aspectInputControls.put(aspect, slider); // Eltároljuk a slidert a szempont nevével
        }
        System.out.println("VotingController: Scale UI felépítve " + aspects.size() + " szemponttal.");
        return aspectContainer;
    }

    /** Segédfüggvény UI építési hiba kezelésére. */
    private void handleUIBuildError(String msg, Exception e) {
        System.err.println(msg + ": " + e.getMessage());
        e.printStackTrace();
        uiManager.showError(msg, "UI Hiba");
        submitButton.setDisable(true);
        votingArea.getChildren().add(new Label("Hiba a felület betöltésekor."));
    }

    /** Eseménykezelő a "Szavazat Leadása" gombra. */
    @FXML
    void handleSubmitButtonAction(ActionEvent event) {
        System.out.println("VotingController: 'Szavazat Leadása' gomb megnyomva.");
        if (currentPoll == null) {
            uiManager.showError("Hiba: Nincs aktuális szavazás.", "Hiba");
            return;
        }
        if (voteInputControl == null && (currentPoll.type() != PollType.SCALE || aspectInputControls.isEmpty())) {
            // Skálánál az aspectInputControls-t is ellenőrizzük
            uiManager.showError("Hiba: Nem található a szavazó elem.", "UI Hiba");
            return;
        }

        System.out.println("VotingController: Szavazat összegyűjtése: " + currentPoll.type());
        VoteData voteData = null;
        String validationError = null;

        try {
            switch (currentPoll.type()) {
                case MULTIPLE_CHOICE:
                    if (voteInputControl instanceof ToggleGroup group) {
                        Toggle selectedToggle = group.getSelectedToggle();
                        if (selectedToggle != null) {
                            String selectedOption = ((RadioButton) selectedToggle).getText();
                            System.out.println("VotingController: Kiválasztott MC opció: " + selectedOption);
                            voteData = VoteData.forMultipleChoice(selectedOption);
                        } else {
                            validationError = "Válassz egy opciót!";
                            System.out.println("VotingController: Nincs MC opció kiválasztva.");
                        }
                    } else {
                        throw new IllegalStateException("Input kontrol nem ToggleGroup Multiple Choice esetén!");
                    }
                    break;
                case WORD_CLOUD:
                    if (voteInputControl instanceof TextArea ta) {
                        String rawText = ta.getText();
                        System.out.println("VotingController: Nyers WC szöveg: \"" + rawText.replace("\n", "\\n") + "\"");
                        List<String> words = Arrays.stream(rawText.split("\\r?\\n"))
                                .map(String::trim)
                                .filter(s -> !s.isEmpty() && s.length() <= 25) // Max 25 karakter/szó(sor)
                                .limit(3) // Max 3 szó(sor)
                                .collect(Collectors.toList());
                        System.out.println("VotingController: Feldolgozott WC szavak: " + words);
                        if (words.isEmpty()) {
                            validationError = "Írj be legalább egy érvényes szót (max 25 kar)!";
                            System.out.println("VotingController: Nem adott meg érvényes WC szót.");
                        } else {
                            voteData = VoteData.forWordCloud(words);
                        }
                    } else {
                        throw new IllegalStateException("Input kontrol nem TextArea Word Cloud esetén!");
                    }
                    break;
                case SCALE:
                    if (aspectInputControls != null && !aspectInputControls.isEmpty()) {
                        Map<String, Integer> aspectRatingsMap = new HashMap<>();
                        boolean allAspectsRated = true; // Feltételezzük, hogy minden rendben
                        for (Map.Entry<String, Node> entry : aspectInputControls.entrySet()) {
                            String aspectName = entry.getKey();
                            Node inputNode = entry.getValue();
                            int rating;

                            if (inputNode instanceof Slider slider) {
                                rating = (int) slider.getValue();
                            } else {
                                // Ha más input típust is használnánk (pl. Spinner), itt kellene kezelni
                                throw new IllegalStateException("Ismeretlen input kontrol típus a Scale szavazásnál: " + inputNode.getClass().getName());
                            }
                            // Validáció (bár a Slider korlátoz, de duplaellenőrzés)
                            if (rating < currentPoll.scaleMin() || rating > currentPoll.scaleMax()) {
                                validationError = "Értékelés '" + aspectName + "'-hez (" + rating + ") kívül esik a megengedett tartományon [" + currentPoll.scaleMin() + "-" + currentPoll.scaleMax() + "].";
                                allAspectsRated = false;
                                break;
                            }
                            aspectRatingsMap.put(aspectName, rating);
                        }

                        if (allAspectsRated) {
                            // Ellenőrizzük, hogy minden definiált szempontra van-e értékelés
                            if (aspectRatingsMap.size() == currentPoll.aspects().size()) {
                                voteData = VoteData.forScale(aspectRatingsMap);
                                System.out.println("VotingController: Feldolgozott SCALE értékelések: " + aspectRatingsMap);
                            } else {
                                // Ez nem fordulhatna elő, ha az UI helyesen épül fel és minden szempontra van slider
                                validationError = "Nem minden szempontra adtál meg értékelést.";
                                System.err.println("VotingController: Inkonzisztencia! Nem minden SCALE szempont lett értékelve a Map-ben, pedig az UI-nak tartalmaznia kellett volna mindet.");
                            }
                        }
                        // Ha validationError != null, a külső if kezeli
                    } else {
                        validationError = "Hiba: Nem találhatók a skála értékelő elemek.";
                        System.err.println("VotingController: aspectInputControls null vagy üres Scale szavazásnál.");
                    }
                    break;
                default:
                    validationError = "Ismeretlen szavazástípus.";
            }
        } catch (Exception e) {
            System.err.println("Hiba a szavazat összegyűjtése közben: " + e.getMessage());
            e.printStackTrace();
            uiManager.showError("Hiba történt a szavazat feldolgozása közben.\nRészletek a konzolon.", "Kliens Hiba");
            return;
        }

        if (validationError != null) {
            uiManager.showError(validationError, "Hiányzó/Hibás Válasz");
            return;
        }

        if (voteData != null && !voteData.isEmpty()) {
            System.out.println("VotingController: SubmitVoteRequest küldése, adat: " + voteData);
            SubmitVoteRequest request = new SubmitVoteRequest(currentPoll.joinCode(), voteData);
            getServerConnection().sendRequest(request);
            submitButton.setDisable(true); // Küldés után letiltjuk a gombokat
            cancelButton.setDisable(true);
        } else {
            System.err.println("VotingController: Nem sikerült érvényes VoteData objektumot létrehozni, vagy üres maradt.");
            uiManager.showError("Nem sikerült a szavazatot előkészíteni a küldéshez.", "Hiba");
        }
    }

    /** Eseménykezelő a Mégsem/Kilépés gombra. */
    @FXML
    void handleCancelButtonAction(ActionEvent event) {
        System.out.println("VotingController: 'Mégsem' gomb megnyomva.");
        if (uiManager.showConfirmation("Biztosan megszakítod a szavazást és kilépsz?", "Megerősítés")) {
            System.out.println("VotingController: Leiratkozás és navigálás a kezdőképernyőre.");
            getServerConnection().sendRequest(new UnsubscribeRequest());
            uiManager.showStartScreen();
        } else {
            System.out.println("VotingController: Kilépés megszakítva.");
        }
    }

    /** @return Az aktuálisan megjelenített szavazás join kódja, vagy null. */
    public String getCurrentPollJoinCode() {
        return (currentPoll != null) ? currentPoll.joinCode() : null;
    }

    /** @return Az aktuálisan megjelenített szavazás neve, vagy null. */
    public String getCurrentPollName() {
        return (currentPoll != null) ? currentPoll.name() : null;
    }
}
