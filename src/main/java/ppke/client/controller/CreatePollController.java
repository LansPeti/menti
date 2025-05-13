package ppke.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import ppke.common.dto.*;
import ppke.common.model.PollType;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Az új szavazás létrehozása képernyő (create_poll.fxml) vezérlő osztálya.
 * Lehetővé teszi a szavazás nevének, kérdésének, típusának megadását,
 * valamint a típus-specifikus beállítások (pl. opciók, szempontok, skála) konfigurálását.
 * Kezeli a join kód manuális megadását vagy automatikus generálását.
 * Elküldi a {@link CreatePollRequest}-et a szervernek.
 * Implementálja az {@link Initializable} interfészt.
 */
public class CreatePollController extends ControllerBase implements Initializable {

    @FXML private TextField pollNameField;
    @FXML private TextField pollQuestionField;
    @FXML private ComboBox<PollType> pollTypeComboBox;
    @FXML private TextField joinCodeField;
    @FXML private CheckBox autoGenerateCheckBox;

    // Multiple Choice specifikus
    @FXML private VBox optionsVBox;
    @FXML private TextArea optionsTextArea;

    // Word Cloud specifikus
    @FXML private VBox wordCloudVBox;

    // Scale specifikus
    @FXML private VBox scaleOptionsVBox;
    @FXML private ListView<String> scaleAspectsListView;
    @FXML private TextField newAspectTextField;
    @FXML private Spinner<Integer> scaleMinSpinner;
    @FXML private Spinner<Integer> scaleMaxSpinner;


    private ObservableList<String> scaleAspectsData = FXCollections.observableArrayList();
    private final int MAX_ASPECTS_CLIENT = 5; // Kliens oldali limit a szempontokra
    private final int MAX_MC_OPTIONS_CLIENT = 10; // Kliens oldali limit az MC opciókra

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        pollTypeComboBox.getItems().setAll(PollType.values());
        pollTypeComboBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateSpecificOptionsVisibility(newVal));

        if (scaleAspectsListView != null) {
            scaleAspectsListView.setItems(scaleAspectsData);
            scaleAspectsListView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        } else {
            System.err.println("Figyelmeztetés: scaleAspectsListView nem lett injektálva. Skála típusú szavazásnál problémát okozhat.");
        }

        if (scaleMinSpinner != null && scaleMaxSpinner != null) {
            SpinnerValueFactory.IntegerSpinnerValueFactory minFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0, 1);
            SpinnerValueFactory.IntegerSpinnerValueFactory maxFactory =
                    new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, 10, 1);

            scaleMinSpinner.setValueFactory(minFactory);
            scaleMaxSpinner.setValueFactory(maxFactory);

            scaleMinSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue >= scaleMaxSpinner.getValue()) {
                    if (newValue < maxFactory.getMax()) {
                        scaleMaxSpinner.getValueFactory().setValue(newValue + 1);
                    } else {
                        scaleMinSpinner.getValueFactory().setValue(oldValue);
                        if (uiManager != null) {
                            uiManager.showError("A minimum érték nem lehet nagyobb vagy egyenlő a maximum érték felső határával (" + maxFactory.getMax() + ").", "Skála Hiba");
                        }
                    }
                }
            });
            scaleMaxSpinner.valueProperty().addListener((obs, oldValue, newValue) -> {
                if (newValue <= scaleMinSpinner.getValue()) {
                    if (newValue > minFactory.getMin()) {
                        scaleMinSpinner.getValueFactory().setValue(newValue - 1);
                    } else {
                        scaleMaxSpinner.getValueFactory().setValue(oldValue);
                        if (uiManager != null) {
                            uiManager.showError("A maximum érték nem lehet kisebb vagy egyenlő a minimum érték alsó határával (" + minFactory.getMin() + ").", "Skála Hiba");
                        }
                    }
                }
            });
        } else {
            System.err.println("Figyelmeztetés: scaleMinSpinner vagy scaleMaxSpinner nem lett injektálva. Skála típusú szavazásnál problémát okozhat.");
        }

        updateSpecificOptionsVisibility(null);
        autoGenerateCheckBox.setSelected(true);
        handleAutoGenerateToggle(null);
    }

    private void updateSpecificOptionsVisibility(PollType type) {
        boolean isMC = type == PollType.MULTIPLE_CHOICE;
        boolean isWC = type == PollType.WORD_CLOUD;
        boolean isScale = type == PollType.SCALE;

        if (optionsVBox != null) { optionsVBox.setVisible(isMC); optionsVBox.setManaged(isMC); }
        if (wordCloudVBox != null) { wordCloudVBox.setVisible(isWC); wordCloudVBox.setManaged(isWC); }

        if (scaleOptionsVBox != null) {
            scaleOptionsVBox.setVisible(isScale);
            scaleOptionsVBox.setManaged(isScale);
            if (isScale && newAspectTextField != null) {
                newAspectTextField.requestFocus();
            }
        } else if (isScale) {
            System.err.println("HIBA: scaleOptionsVBox is NULL, de Skála típus van kiválasztva. Ellenőrizd az FXML fájlt ('create_poll.fxml').");
            if (uiManager != null) {
                uiManager.showError("Belső hiba: A skála opciók megjelenítése nem sikerült. Kérjük, ellenőrizze az alkalmazás konfigurációját.", "Konfigurációs Hiba");
            }
        }
    }

    @FXML
    void handleAutoGenerateToggle(ActionEvent event) {
        boolean autoSelected = autoGenerateCheckBox.isSelected();
        joinCodeField.setDisable(autoSelected);
        if (autoSelected) {
            joinCodeField.clear();
        } else {
            joinCodeField.requestFocus();
        }
    }

    @FXML
    void handleAddAspectButtonAction(ActionEvent event) {
        if (newAspectTextField == null || scaleAspectsData == null) {
            if (uiManager != null) uiManager.showError("Belső hiba: A szempontok kezeléséhez szükséges elemek nem érhetők el.", "Hiba");
            return;
        }
        String newAspect = newAspectTextField.getText().trim();
        if (!newAspect.isEmpty() && scaleAspectsData.size() < MAX_ASPECTS_CLIENT && !scaleAspectsData.contains(newAspect)) {
            scaleAspectsData.add(newAspect);
            newAspectTextField.clear();
        } else if (newAspect.isEmpty()) {
            if (uiManager != null) uiManager.showError("A szempont neve nem lehet üres.", "Hiba");
        } else if (scaleAspectsData.contains(newAspect)) {
            if (uiManager != null) uiManager.showError("Ez a szempont már szerepel a listában.", "Hiba");
        } else if (scaleAspectsData.size() >= MAX_ASPECTS_CLIENT) {
            if (uiManager != null) uiManager.showError("Maximum " + MAX_ASPECTS_CLIENT + " szempont adható meg.", "Hiba");
        }
        newAspectTextField.requestFocus();
    }

    @FXML
    void handleRemoveAspectButtonAction(ActionEvent event) {
        if (scaleAspectsListView == null || scaleAspectsData == null) {
            if (uiManager != null) uiManager.showError("Belső hiba: A szempontlista nem elérhető.", "Hiba");
            return;
        }
        String selectedAspect = scaleAspectsListView.getSelectionModel().getSelectedItem();
        if (selectedAspect != null) {
            scaleAspectsData.remove(selectedAspect);
        } else {
            if (uiManager != null) uiManager.showError("Nincs kijelölt szempont a törléshez.", "Hiba");
        }
        if (newAspectTextField != null) newAspectTextField.requestFocus();
    }

    @FXML
    void handleCreateButtonAction(ActionEvent event) {
        if (uiManager == null) { // Kritikus hiba, ha a uiManager nincs beállítva
            System.err.println("Kritikus hiba: UIManager nincs beállítva a CreatePollController-ben!");
            return;
        }

        String name = pollNameField.getText().trim();
        String question = pollQuestionField.getText().trim();
        PollType type = pollTypeComboBox.getValue();
        String joinCodeInput = joinCodeField.getText().trim();
        boolean autoGenerate = autoGenerateCheckBox.isSelected();

        if (name.isEmpty()) { uiManager.showError("A szavazás nevének megadása kötelező!", "Hiányzó Adat"); pollNameField.requestFocus(); return; }
        if (question.isEmpty()) { uiManager.showError("A kérdés megadása kötelező!", "Hiányzó Adat"); pollQuestionField.requestFocus(); return; }
        if (type == null) { uiManager.showError("A szavazás típusának kiválasztása kötelező!", "Hiányzó Adat"); pollTypeComboBox.requestFocus(); return; }

        String finalJoinCode = null;
        if (!autoGenerate) {
            if (joinCodeInput.isEmpty() || !joinCodeInput.matches("^[a-zA-Z0-9]{8}$")) {
                uiManager.showError("Érvénytelen join kód!\n8 alfanumerikus karakter szükséges, vagy válaszd az automatikus generálást.", "Hibás Kód");
                joinCodeField.requestFocus();
                return;
            }
            finalJoinCode = joinCodeInput.toUpperCase(Locale.ROOT);
        }

        PollData pollData = new PollData().setType(type).setName(name).setQuestion(question);

        try {
            switch (type) {
                case MULTIPLE_CHOICE:
                    if (optionsTextArea == null) {
                        throw new IllegalStateException("A feleletválasztós opciók beviteli mezője (optionsTextArea) nincs megfelelően beállítva az FXML-ben.");
                    }
                    List<String> options = Arrays.stream(optionsTextArea.getText().split("\\r?\\n"))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .limit(MAX_MC_OPTIONS_CLIENT)
                            .collect(Collectors.toList());
                    if (options.size() < 2) {
                        throw new IllegalArgumentException("Minimum 2 különböző, nem üres opció megadása kötelező (maximum " + MAX_MC_OPTIONS_CLIENT + ").");
                    }
                    pollData.setOptions(options);
                    break;
                case WORD_CLOUD:
                    // Nincs specifikus opció a létrehozásnál
                    break;
                case SCALE:
                    if (scaleAspectsData == null) throw new IllegalStateException("A skála szempontok listája (scaleAspectsData) nincs inicializálva.");
                    if (scaleMinSpinner == null) throw new IllegalStateException("A skála minimum érték választó (scaleMinSpinner) nincs megfelelően beállítva az FXML-ben.");
                    if (scaleMaxSpinner == null) throw new IllegalStateException("A skála maximum érték választó (scaleMaxSpinner) nincs megfelelően beállítva az FXML-ben.");

                    List<String> aspects = new ArrayList<>(scaleAspectsData);
                    if (aspects.isEmpty() || aspects.size() > MAX_ASPECTS_CLIENT) {
                        throw new IllegalArgumentException("Legalább 1 és maximum " + MAX_ASPECTS_CLIENT + " szempontot kell megadni.");
                    }
                    int minVal = scaleMinSpinner.getValue();
                    int maxVal = scaleMaxSpinner.getValue();
                    if (minVal >= maxVal) {
                        throw new IllegalArgumentException("A skála minimum értékének (" + minVal + ") kisebbnek kell lennie a maximum értéknél (" + maxVal + ").");
                    }
                    pollData.setAspects(aspects);
                    pollData.setScaleMin(minVal);
                    pollData.setScaleMax(maxVal);
                    break;
                default:
                    // Ez elvileg nem fordulhat elő, ha a ComboBox csak érvényes PollType értékeket tartalmaz.
                    throw new IllegalArgumentException("Ismeretlen szavazás típus: " + type);
            }
        } catch (IllegalArgumentException | IllegalStateException e) {
            uiManager.showError("Hiba a típus-specifikus adatokban:\n" + e.getMessage(), "Érvénytelen Beállítás");
            if (type == PollType.MULTIPLE_CHOICE && optionsTextArea != null) optionsTextArea.requestFocus();
            else if (type == PollType.SCALE && scaleAspectsListView != null) scaleAspectsListView.requestFocus();
            return;
        } catch (Exception e) { // Bármilyen egyéb, nem várt hiba
            uiManager.showError("Hiba a beállítások feldolgozása közben:\n" + e.getMessage(), "Feldolgozási Hiba");
            return;
        }

        CreatePollRequest request = new CreatePollRequest(pollData, autoGenerate ? null : finalJoinCode, autoGenerate);
        getServerConnection().sendRequest(request); // A ControllerBase biztosítja, hogy uiManager (és általa a ServerConnection) létezik.
    }

    @FXML
    void handleCancelButtonAction(ActionEvent event) {
        if (uiManager != null) {
            uiManager.showProfileScreenAgain();
        }
    }
}