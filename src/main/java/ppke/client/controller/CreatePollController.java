package ppke.client.controller;

import javafx.event.ActionEvent; import javafx.fxml.*; import javafx.scene.control.*; import javafx.scene.layout.VBox;
import ppke.common.dto.*; import ppke.common.model.PollType;
import ppke.client.UIManager;

import java.net.URL; import java.util.*; import java.util.stream.Collectors;

/**
 * Az új szavazás létrehozása képernyő (create_poll.fxml) vezérlő osztálya.
 * Lehetővé teszi a szavazás nevének, kérdésének, típusának megadását,
 * valamint a típus-specifikus beállítások (pl. opciók) konfigurálását.
 * Kezeli a join kód manuális megadását vagy automatikus generálását.
 * Elküldi a {@link CreatePollRequest}-et a szervernek.
 * Implementálja az {@link Initializable} interfészt.
 */
public class CreatePollController extends ControllerBase implements Initializable {

    @FXML private TextField pollNameField; @FXML private TextField pollQuestionField; @FXML private ComboBox<PollType> pollTypeComboBox;
    @FXML private TextField joinCodeField; @FXML private CheckBox autoGenerateCheckBox;
    @FXML private VBox optionsVBox; @FXML private TextArea optionsTextArea; // MC
    @FXML private VBox wordCloudVBox; // WC
    @FXML private Button createButton; @FXML private Button cancelButton;

    @Override public void initialize(URL url, ResourceBundle rb) { pollTypeComboBox.getItems().setAll(PollType.values()); pollTypeComboBox.getItems().remove(PollType.SCALE); pollTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateSpecificOptionsVisibility(n)); updateSpecificOptionsVisibility(null); autoGenerateCheckBox.setSelected(true); handleAutoGenerateToggle(null); System.out.println("CreatePollController inicializálva."); }
    private void updateSpecificOptionsVisibility(PollType type) { boolean isMC = type == PollType.MULTIPLE_CHOICE; boolean isWC = type == PollType.WORD_CLOUD; optionsVBox.setVisible(isMC); optionsVBox.setManaged(isMC); wordCloudVBox.setVisible(isWC); wordCloudVBox.setManaged(isWC); System.out.println("CreatePollController: Specifikus opciók láthatósága frissítve: " + type); }
    @FXML void handleAutoGenerateToggle(ActionEvent event) { boolean autoSelected = autoGenerateCheckBox.isSelected(); joinCodeField.setDisable(autoSelected); if (autoSelected) { joinCodeField.clear(); System.out.println("CreatePollController: Auto-generálás bekapcsolva, join kód mező letiltva."); } else { System.out.println("CreatePollController: Auto-generálás kikapcsolva, join kód mező engedélyezve."); joinCodeField.requestFocus(); } }
    @FXML void handleCreateButtonAction(ActionEvent event) { System.out.println("CreatePollController: 'Létrehozás' gomb megnyomva."); String name = pollNameField.getText().trim(); String question = pollQuestionField.getText().trim(); PollType type = pollTypeComboBox.getValue(); String joinCodeInput = joinCodeField.getText().trim(); boolean autoGenerate = autoGenerateCheckBox.isSelected(); if (name.isEmpty()) { uiManager.showError("A szavazás nevének megadása kötelező!", "Hiányzó Adat"); pollNameField.requestFocus(); return; } if (question.isEmpty()) { uiManager.showError("A kérdés megadása kötelező!", "Hiányzó Adat"); pollQuestionField.requestFocus(); return; } if (type == null) { uiManager.showError("A szavazás típusának kiválasztása kötelező!", "Hiányzó Adat"); pollTypeComboBox.requestFocus(); return; } String finalJoinCode = null; if (!autoGenerate) { if (joinCodeInput.isEmpty() || !joinCodeInput.matches("^[a-zA-Z0-9]{8}$")) { uiManager.showError("Érvénytelen join kód!\n8 alfanumerikus karakter szükséges, vagy válaszd az automatikus generálást.", "Hibás Kód"); joinCodeField.requestFocus(); return; } finalJoinCode = joinCodeInput; } PollData pollData = new PollData().setType(type).setName(name).setQuestion(question); try { System.out.println("CreatePollController: Specifikus opciók validálása: " + type); switch (type) { case MULTIPLE_CHOICE: List<String> options = Arrays.stream(optionsTextArea.getText().split("\\r?\\n")).map(String::trim).filter(s->!s.isEmpty()).distinct().limit(10).toList(); System.out.println("CreatePollController: Feldolgozott MC opciók: " + options); if (options.size() < 2) throw new IllegalArgumentException("Minimum 2 különböző, nem üres opció megadása kötelező!"); pollData.setOptions(options); break; case WORD_CLOUD: System.out.println("CreatePollController: Nincs specifikus opció Word Cloudhoz."); break; case SCALE: throw new IllegalArgumentException("A Scale típus még nincs implementálva."); } } catch (IllegalArgumentException | NullPointerException e) { uiManager.showError("Hiba a típus-specifikus adatokban:\n" + e.getMessage(), "Érvénytelen Beállítás"); if (type == PollType.MULTIPLE_CHOICE) optionsTextArea.requestFocus(); return; } catch (Exception e){ uiManager.showError("Hiba a beállítások feldolgozása közben:\n" + e.getMessage(), "Feldolgozási Hiba"); return; } CreatePollRequest request = new CreatePollRequest(pollData, autoGenerate ? null : finalJoinCode, autoGenerate); System.out.println("CreatePollController: CreatePollRequest küldése. Auto-gen: " + autoGenerate + ", Manuális kód: " + finalJoinCode); getServerConnection().sendRequest(request); }
    @FXML void handleCancelButtonAction(ActionEvent event) { System.out.println("CreatePollController: 'Mégsem' gomb megnyomva, vissza a profilhoz."); uiManager.showProfileScreenAgain(); }
}