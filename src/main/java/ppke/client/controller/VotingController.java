package ppke.client.controller;

import javafx.event.ActionEvent; import javafx.fxml.FXML; import javafx.geometry.Insets;
import javafx.scene.Node; import javafx.scene.control.*; import javafx.scene.layout.VBox;
import javafx.scene.text.Font; import javafx.scene.text.FontWeight;
import ppke.common.dto.*; import ppke.common.model.PollType;
import ppke.client.UIManager; // <-- IMPORT ITT IS KELL!

import java.util.*; import java.util.stream.Collectors;

/**
 * A szavazási képernyő (voting.fxml) vezérlő osztálya.
 * Felelős a szavazás típusának megfelelő UI dinamikus felépítéséért,
 * a felhasználói input összegyűjtéséért és a szavazat elküldéséért a szervernek.
 */
public class VotingController extends ControllerBase {

    @FXML private Label pollNameLabel; @FXML private Label pollQuestionLabel; @FXML private VBox votingArea;
    @FXML private Button submitButton; @FXML private Button cancelButton;
    private PollDTO currentPoll; private Object voteInputControl;

    /**
     * Adatok inicializálása és a UI felépítése a szavazás típusa alapján.
     * Ezt a {@link UIManager} hívja meg a nézet betöltésekor.
     * @param poll A szavazás adatai (DTO). Nem lehet null.
     */
    public void initializeData(PollDTO poll) { this.currentPoll = Objects.requireNonNull(poll); System.out.println("VotingController: Inicializálás: " + poll.joinCode() + " - Típus: " + poll.type()); pollNameLabel.setText(poll.name()); pollQuestionLabel.setText(poll.question()); pollQuestionLabel.setWrapText(true); buildVotingUI(poll); }
    /** Felépíti a szavazási UI elemeket a {@code votingArea} VBox-ba a poll típusa alapján. */
    private void buildVotingUI(PollDTO poll) { votingArea.getChildren().clear(); votingArea.setSpacing(10); voteInputControl = null; submitButton.setDisable(false); System.out.println("VotingController: UI építése: " + poll.type()); try { Label instr; switch (poll.type()) {
        case MULTIPLE_CHOICE: instr = new Label("Válassz egyet:"); votingArea.getChildren().add(instr); Node mcNode = buildMultipleChoiceUI(poll.options()); if (mcNode == null) submitButton.setDisable(true); else votingArea.getChildren().add(mcNode); break;
        case WORD_CLOUD: instr = new Label("Írj max 3 szót/kifejezést (max 25 kar/sor), új sorba:"); instr.setWrapText(true); votingArea.getChildren().add(instr); Node wcNode = buildWordCloudUI(); if (wcNode != null) votingArea.getChildren().add(wcNode); else submitButton.setDisable(true); break;
        case SCALE: Label scaleNI = new Label("Skála típus nincs implementálva."); votingArea.getChildren().add(scaleNI); submitButton.setDisable(true); break;
        default: Label unsupp = new Label("Nem támogatott típus."); votingArea.getChildren().add(unsupp); submitButton.setDisable(true);
    } } catch (Exception e) { handleUIBuildError("UI építési hiba.", e); }
    }
    /** Létrehozza a Multiple Choice UI elemeket (RadioButton-ok). */
    private Node buildMultipleChoiceUI(List<String> options) { if (options == null || options.isEmpty()) { votingArea.getChildren().add(new Label("Hiba: Nincsenek opciók.")); this.voteInputControl = null; return null; } VBox box = new VBox(8); box.setPadding(new Insets(5, 0, 5, 10)); ToggleGroup group = new ToggleGroup(); this.voteInputControl = group; options.forEach(opt -> { RadioButton rb = new RadioButton(opt); rb.setToggleGroup(group); rb.setWrapText(true); rb.setPadding(new Insets(2)); box.getChildren().add(rb); }); System.out.println("VotingController: MC UI felépítve " + options.size() + " opcióval."); return box; }
    /** Létrehozza a Word Cloud UI elemet (TextArea). */
    private Node buildWordCloudUI() { TextArea ta = new TextArea(); ta.setPromptText("Szó1\nSzó2\nSzó3..."); ta.setPrefRowCount(4); ta.setWrapText(true); ta.setTextFormatter(new TextFormatter<>(c -> c.getControlNewText().length() <= 80 ? c : null)); this.voteInputControl = ta; System.out.println("VotingController: WC UI (TextArea) felépítve."); return ta; }
    /** Segédfüggvény UI építési hiba kezelésére. */
    private void handleUIBuildError(String msg, Exception e) { System.err.println(msg + ": " + e.getMessage()); e.printStackTrace(); uiManager.showError(msg, "UI Hiba"); submitButton.setDisable(true); votingArea.getChildren().add(new Label("Hiba a felület betöltésekor.")); }
    /** Eseménykezelő a "Szavazat Leadása" gombra. */
    @FXML void handleSubmitButtonAction(ActionEvent event) { System.out.println("VotingController: 'Szavazat Leadása' gomb megnyomva."); if (currentPoll == null) { uiManager.showError("Hiba: Nincs aktuális szavazás.", "Hiba"); return; } if (voteInputControl == null) { uiManager.showError("Hiba: Nem található a szavazó elem.", "UI Hiba"); return; } System.out.println("VotingController: Szavazat összegyűjtése: " + currentPoll.type()); VoteData voteData = null; String validationError = null; try { switch (currentPoll.type()) {
        case MULTIPLE_CHOICE: if (voteInputControl instanceof ToggleGroup group) { Toggle selTog = group.getSelectedToggle(); if (selTog != null) { String selOpt = ((RadioButton)selTog).getText(); System.out.println("VotingController: Kiválasztott MC opció: " + selOpt); voteData = VoteData.forMultipleChoice(selOpt); } else { validationError = "Válassz egy opciót!"; System.out.println("VotingController: Nincs MC opció kiválasztva."); } } else throw new IllegalStateException("Input kontrol nem ToggleGroup MC esetén!"); break;
        case WORD_CLOUD: if (voteInputControl instanceof TextArea ta) { String raw = ta.getText(); System.out.println("VotingController: Nyers WC szöveg: \"" + raw.replace("\n", "\\n") + "\""); List<String> words = Arrays.stream(raw.split("\\r?\\n")).map(String::trim).filter(s->!s.isEmpty() && s.length()<=25).limit(3).toList(); System.out.println("VotingController: Feldolgozott WC szavak: " + words); if(words.isEmpty()) { validationError = "Írj be legalább egy érvényes szót (max 25 kar)!"; System.out.println("VotingController: Nem adott meg érvényes WC szót."); } else voteData = VoteData.forWordCloud(words); } else throw new IllegalStateException("Input kontrol nem TextArea WC esetén!"); break;
        case SCALE: validationError = "Skála típus nincs implementálva."; break; default: validationError = "Ismeretlen típus."; }
    } catch (Exception e) { System.err.println("Hiba a szavazat összegyűjtése közben: " + e.getMessage()); e.printStackTrace(); uiManager.showError("Hiba történt a szavazat feldolgozása közben.\nRészletek a konzolon.", "Kliens Hiba"); return; }
        if (validationError != null) { uiManager.showError(validationError, "Hiányzó/Hibás Válasz"); return; }
        if (voteData != null && !voteData.isEmpty()) { System.out.println("VotingController: SubmitVoteRequest küldése, adat: " + voteData); SubmitVoteRequest req = new SubmitVoteRequest(currentPoll.joinCode(), voteData); getServerConnection().sendRequest(req); submitButton.setDisable(true); cancelButton.setDisable(true); }
        else { System.err.println("VotingController: Nem sikerült érvényes VoteData objektumot létrehozni, vagy üres maradt."); uiManager.showError("Nem sikerült a szavazatot előkészíteni a küldéshez.", "Hiba"); }
    }
    /** Eseménykezelő a Mégsem/Kilépés gombra. */
    @FXML void handleCancelButtonAction(ActionEvent event) { System.out.println("VotingController: 'Mégsem' gomb megnyomva."); if (uiManager.showConfirmation("Biztosan megszakítod a szavazást és kilépsz?", "Megerősítés")) { System.out.println("VotingController: Leiratkozás és navigálás a kezdőképernyőre."); getServerConnection().sendRequest(new UnsubscribeRequest()); uiManager.showStartScreen(); } else { System.out.println("VotingController: Kilépés megszakítva."); } }
    /** @return Az aktuálisan megjelenített szavazás join kódja, vagy null. */
    public String getCurrentPollJoinCode() { return (currentPoll != null) ? currentPoll.joinCode() : null; }
    /** @return Az aktuálisan megjelenített szavazás neve, vagy null. */
    public String getCurrentPollName() { return (currentPoll != null) ? currentPoll.name() : null; }
}