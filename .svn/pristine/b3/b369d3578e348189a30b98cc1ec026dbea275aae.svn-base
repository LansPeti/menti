package ppke.client.controller;

import javafx.application.Platform; import javafx.event.ActionEvent; import javafx.fxml.FXML;
import javafx.scene.control.*; import javafx.scene.layout.*;
import ppke.common.dto.*; import ppke.common.model.*;

import java.util.*;

/**
 * A szavazás részleteit és kezelőfelületét megjelenítő képernyő (poll_details.fxml) vezérlő osztálya
 * (a létrehozó számára). Lehetővé teszi az állapotváltást, szerkesztést (név, kérdés),
 * eredmények nullázását és megtekintését (átnavigálással).
 */
public class PollDetailsController extends ControllerBase {

    @FXML private Label pollNameLabel; @FXML private Label pollQuestionLabel; @FXML private Label pollTypeLabel;
    @FXML private Label pollJoinCodeLabel; @FXML private Label pollStateLabel;
    @FXML private Button openButton; @FXML private Button startButton; @FXML private Button finishButton; @FXML private Button closeButton;
    @FXML private Button editButton; @FXML private Button resetResultsButton; @FXML private Button showResultsButton;
    @FXML private Button backButton;
    @FXML private VBox editPane; @FXML private TextField editNameField; @FXML private TextField editQuestionField;
    @FXML private Button saveEditButton; @FXML private Button cancelEditButton;
    @FXML private VBox resultsPane; @FXML private TextArea resultsTextArea;
    private PollDTO currentPoll; private boolean isEditMode = false;

    /**
     * Inicializálja a kontrollert a szavazás adataival.
     * @param poll A megjelenítendő szavazás DTO-ja (nem lehet null).
     */
    public void initializeData(PollDTO poll) { this.currentPoll = Objects.requireNonNull(poll); System.out.println("PollDetailsController: Inicializálás: " + poll.joinCode()); updateUI(); }
    /** Frissíti a UI elemeket az aktuális állapot alapján. */
    private void updateUI() { if (currentPoll == null) return; System.out.println("PollDetailsController: UI frissítése. Szerk mód: " + isEditMode + ", Állapot: " + currentPoll.state()); editPane.setVisible(isEditMode); editPane.setManaged(isEditMode); resultsPane.setVisible(!isEditMode && currentPoll.state() == PollState.RESULTS); resultsPane.setManaged(!isEditMode && currentPoll.state() == PollState.RESULTS); if (isEditMode) { editNameField.setText(currentPoll.name()); editQuestionField.setText(currentPoll.question()); pollNameLabel.setText("Szerkesztés: " + currentPoll.name()); } else { pollNameLabel.setText(currentPoll.name()); pollQuestionLabel.setText(currentPoll.question()); pollTypeLabel.setText("Típus: " + currentPoll.type().getDisplayName()); pollJoinCodeLabel.setText("Kód: " + currentPoll.joinCode()); pollStateLabel.setText("Állapot: " + currentPoll.state().getDisplayName()); if (resultsPane.isVisible()) displayResultsLocally(currentPoll.results()); } setButtonsVisibility(currentPoll.state()); }
    /** Beállítja a gombok láthatóságát az állapot és szerkesztési mód alapján. */
    private void setButtonsVisibility(PollState state) { boolean nullS=state==null, cl=state==PollState.CLOSED, op=state==PollState.OPEN_FOR_JOINING, vo=state==PollState.VOTING, re=state==PollState.RESULTS; openButton.setVisible(!isEditMode&&cl); openButton.setManaged(!isEditMode&&cl); startButton.setVisible(!isEditMode&&op); startButton.setManaged(!isEditMode&&op); finishButton.setVisible(!isEditMode&&vo); finishButton.setManaged(!isEditMode&&vo); closeButton.setVisible(!isEditMode&&(op||re)); closeButton.setManaged(!isEditMode&&(op||re)); editButton.setVisible(!isEditMode&&cl); editButton.setManaged(!isEditMode&&cl); resetResultsButton.setVisible(!isEditMode&&(cl||re)); resetResultsButton.setManaged(!isEditMode&&(cl||re)); showResultsButton.setVisible(!isEditMode&&re); showResultsButton.setManaged(!isEditMode&&re); backButton.setVisible(!isEditMode); backButton.setManaged(!isEditMode); saveEditButton.setVisible(isEditMode); saveEditButton.setManaged(isEditMode); cancelEditButton.setVisible(isEditMode); cancelEditButton.setManaged(isEditMode); if(nullS){ openButton.setVisible(false);openButton.setManaged(false);startButton.setVisible(false);startButton.setManaged(false);finishButton.setVisible(false);finishButton.setManaged(false);closeButton.setVisible(false);closeButton.setManaged(false);editButton.setVisible(false);editButton.setManaged(false);resetResultsButton.setVisible(false);resetResultsButton.setManaged(false);showResultsButton.setVisible(false);showResultsButton.setManaged(false);backButton.setVisible(true);backButton.setManaged(true);saveEditButton.setVisible(false);saveEditButton.setManaged(false);cancelEditButton.setVisible(false);cancelEditButton.setManaged(false); } }
    /** Megjeleníti az eredményeket a helyi TextArea-ban. */
    private void displayResultsLocally(Object resultsData) { if (resultsData == null) { resultsTextArea.setText("Nincs eredmény."); return; } StringBuilder sb = new StringBuilder("Összesített Eredmények:\n--------------------\n"); try { if (currentPoll.type() == PollType.WORD_CLOUD && resultsData instanceof Map<?,?> map) { safeCastMapStringInt(map).entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).forEach(e -> sb.append(String.format("%s: %d\n", e.getKey(), e.getValue()))); } else if (currentPoll.type() == PollType.MULTIPLE_CHOICE && resultsData instanceof Map<?,?> map) { safeCastMapStringInt(map).forEach((k, v) -> sb.append(String.format("%s: %d szavazat\n", k, v))); } else sb.append("Ismeretlen formátum.\n").append(resultsData); } catch (Exception e) { sb.append("Hiba a formázáskor: ").append(e.getMessage()); } resultsTextArea.setText(sb.toString()); }
    /** Eseménykezelő az állapotváltó gombokhoz. */
    @FXML void handleOpenButtonAction(ActionEvent e) { sendStateChangeRequest(PollState.OPEN_FOR_JOINING); } @FXML void handleStartButtonAction(ActionEvent e) { sendStateChangeRequest(PollState.VOTING); } @FXML void handleFinishButtonAction(ActionEvent e) { sendStateChangeRequest(PollState.RESULTS); } @FXML void handleCloseButtonAction(ActionEvent e) { sendStateChangeRequest(PollState.CLOSED); }
    /** Segédfüggvény állapotváltási kérés küldéséhez. */
    private void sendStateChangeRequest(PollState newState) { if (currentPoll != null) { System.out.println("PollDetailsController: Állapotváltás kérése -> " + newState.getDisplayName()); getServerConnection().sendRequest(new ChangePollStateRequest(currentPoll.joinCode(), newState)); } }
    /** Eseménykezelő a "Szerkesztés" gombra. */
    @FXML void handleEditButtonAction(ActionEvent event) { if (currentPoll != null && currentPoll.state() == PollState.CLOSED) { System.out.println("PollDetailsController: Szerkesztő módba lépés: " + currentPoll.joinCode()); isEditMode = true; updateUI(); } else { System.err.println("PollDetailsController: Szerkesztés nem lehetséges. Állapot: " + (currentPoll != null ? currentPoll.state() : "null")); uiManager.showError("A szavazás csak 'Lezárva' állapotban szerkeszthető.", "Szerkesztés Nem Lehetséges"); } }
    /** Eseménykezelő az "Eredmények Nullázása" gombra. */
    @FXML void handleResetResultsButtonAction(ActionEvent event) { if (currentPoll != null) { if (uiManager.showConfirmation("Biztosan nullázod az eredményeket?", "Megerősítés")) { System.out.println("PollDetailsController: Eredmény nullázási kérés küldése: " + currentPoll.joinCode()); getServerConnection().sendRequest(new ResetResultsRequest(currentPoll.joinCode())); } else System.out.println("PollDetailsController: Eredmény nullázás megszakítva."); } }
    /** Eseménykezelő az "Eredmények mutatása" gombra. */
    @FXML void handleShowResultsButtonAction(ActionEvent event) { if (currentPoll != null && currentPoll.state() == PollState.RESULTS) { System.out.println("PollDetailsController: Navigálás az eredmények képernyőre (HOST): " + currentPoll.joinCode()); uiManager.showResultsScreen(currentPoll, true); } else { System.err.println("PollDetailsController: Eredmények nem mutathatók. Állapot: " + (currentPoll != null ? currentPoll.state() : "null")); uiManager.showError("Az eredmények csak 'Eredmények' állapotban tekinthetők meg.", "Eredmények Nem Elérhetők"); } }
    /** Eseménykezelő a "Mentés" gombra (szerkesztő módban). */
    @FXML void handleSaveEditButtonAction(ActionEvent event) { if (currentPoll == null || !isEditMode) return; System.out.println("PollDetailsController: Szerkesztések mentése: " + currentPoll.joinCode()); String name = editNameField.getText().trim(); String q = editQuestionField.getText().trim(); if(name.isEmpty() || q.isEmpty()){ uiManager.showError("Név és kérdés kötelező!", "Hiba"); return; } PollData data = new PollData().setType(currentPoll.type()).setName(name).setQuestion(q).setOptions(currentPoll.options()).setAspects(currentPoll.aspects()).setScaleMin(currentPoll.scaleMin()).setScaleMax(currentPoll.scaleMax()); System.out.println("PollDetailsController: EditPollRequest küldése: " + currentPoll.joinCode()); getServerConnection().sendRequest(new EditPollRequest(currentPoll.joinCode(), data)); }
    /** Eseménykezelő a "Mégsem" gombra (szerkesztő módban). */
    @FXML void handleCancelEditButtonAction(ActionEvent event) { System.out.println("PollDetailsController: Szerkesztés megszakítva."); isEditMode = false; updateUI(); }
    /** Eseménykezelő a "Vissza a Profilhoz" gombra. */
    @FXML void handleBackButtonAction(ActionEvent event) { System.out.println("PollDetailsController: Visszalépés a profilhoz."); uiManager.showProfileScreenAgain(); }
    /** Frissíti a nézetet szerverről érkező PollUpdateNotification alapján. */
    public void updatePollData(PollDTO updatedPoll) { Platform.runLater(() -> { if (updatedPoll != null && currentPoll != null && Objects.equals(updatedPoll.joinCode(), currentPoll.joinCode())) { System.out.println("PollDetailsController: Poll adatok frissítése érkezett: " + updatedPoll.joinCode()); this.currentPoll = updatedPoll; if (isEditMode) { System.out.println("PollDetailsController: Kilépés a szerkesztő módból frissítés után."); isEditMode = false; } updateUI(); } else { System.err.println("PollDetailsController: Poll adat frissítés érkezett, de nem egyezik a kóddal vagy null."); } }); }
    /** Frissíti a nézet állapotát szerverről érkező PollStateChangedNotification alapján. */
    public void updatePollState(PollState newState, Object resultsData) { Platform.runLater(() -> { if (currentPoll != null && newState != null) { System.out.println("PollDetailsController: Poll állapot frissítése érkezett -> " + newState + " (" + currentPoll.joinCode() + ")"); this.currentPoll = new PollDTO( currentPoll.joinCode(), currentPoll.name(), currentPoll.question(), currentPoll.type(), newState, currentPoll.options(), currentPoll.aspects(), currentPoll.scaleMin(), currentPoll.scaleMax(), resultsData ); if (isEditMode) { System.out.println("PollDetailsController: Kilépés a szerkesztő módból állapotváltás miatt."); isEditMode = false; } updateUI(); } else { System.err.println("PollDetailsController: Poll állapot frissítés érkezett null adatokkal."); } }); }
    /** @return Az aktuálisan megjelenített szavazás join kódja, vagy null. */
    public String getCurrentPollJoinCode() { return (currentPoll != null) ? currentPoll.joinCode() : null; }
    /** Biztonságosan kasztol egy Map-et Map<String, Integer>-re. */
    @SuppressWarnings("unchecked") private Map<String, Integer> safeCastMapStringInt(Map<?, ?> map) throws ClassCastException { if (map == null) return Collections.emptyMap(); for(Map.Entry<?,?> e:map.entrySet()) { if(!(e.getKey() instanceof String) || !(e.getValue() instanceof Integer)) throw new ClassCastException("Nem Map<String, Integer>"); } return (Map<String, Integer>) map; }
    /** Biztonságosan kasztol egy Map-et Map<String, Double>-re. */
    @SuppressWarnings("unchecked") private Map<String, Double> safeCastMapStringDouble(Map<?, ?> map) throws ClassCastException { if (map == null) return Collections.emptyMap(); for(Map.Entry<?,?> e:map.entrySet()) { if(!(e.getKey() instanceof String) || !(e.getValue() instanceof Double)) throw new ClassCastException("Nem Map<String, Double>"); } return (Map<String, Double>) map; }
}