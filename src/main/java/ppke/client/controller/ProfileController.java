package ppke.client.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import ppke.common.dto.*;
import ppke.common.model.PollState;
import ppke.client.UIManager;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;

/**
 * A felhasználói profil képernyő (profile.fxml) vezérlő osztálya.
 * Megjeleníti a bejelentkezett felhasználó üdvözlését és a saját maga által létrehozott
 * szavazások listáját. Lehetőséget biztosít új szavazás létrehozására,
 * meglévő szavazás részleteinek megtekintésére/kezelésére és törlésére,
 * valamint kijelentkezésre.
 * Implementálja az {@link Initializable} interfészt az FXML betöltése utáni inicializáláshoz.
 */
public class ProfileController extends ControllerBase implements Initializable {

    @FXML private Label welcomeLabel;
    @FXML private ListView<PollDTO> pollListView;
    @FXML private Button detailsButton;
    @FXML private Button deleteButton;
    @FXML private Button newPollButton;
    @FXML private Button logoutButton;

    private String currentUsername;
    private final ObservableList<PollDTO> pollList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        pollListView.setItems(pollList);

        pollListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PollDTO item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null); setStyle("");
                } else {
                    setText(String.format("%s [%s] (%s) - %s", item.name(), item.joinCode(), item.type().getDisplayName(), item.state().getDisplayName()));
                    setStyle(getStyleForPollState(item.state()));
                }
            }
        });

        pollListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            boolean itemSelected = (newSelection != null);
            detailsButton.setDisable(!itemSelected);
            deleteButton.setDisable(!itemSelected);
        });

        pollListView.setOnMouseClicked(this::handleListDoubleClick);
        System.out.println("ProfileController inicializálva.");
    }

    private String getStyleForPollState(PollState state) {
        return switch (state) {
            case OPEN_FOR_JOINING -> "-fx-control-inner-background: #c8e6c9;";
            case VOTING -> "-fx-control-inner-background: #bbdefb;";
            case RESULTS -> "-fx-control-inner-background: #fff9c4;";
            case CLOSED -> "-fx-control-inner-background: #eeeeee;";
        };
    }

    public void initializeData(String username, List<PollDTO> initialPolls) {
        this.currentUsername = username;
        welcomeLabel.setText("Üdvözöllek, " + username + "!");
        System.out.println("ProfileController: Adatok inicializálása: " + username + ", Szavazások: " + (initialPolls != null ? initialPolls.size() : 0));
        updatePollList(initialPolls);
    }

    public void updatePollList(List<PollDTO> polls) {
        Platform.runLater(() -> {
            pollList.setAll(Objects.requireNonNullElseGet(polls, Collections::emptyList));
            System.out.println("ProfileController: Szavazás lista frissítve a UI szálon. Méret: " + pollList.size());
            pollListView.getSelectionModel().clearSelection();
        });
    }

    public void updateSinglePollInList(PollDTO updatedPoll) {
        Objects.requireNonNull(updatedPoll, "A frissített szavazás nem lehet null");
        Platform.runLater(() -> {
            int index = -1;
            for (int i = 0; i < pollList.size(); i++) {
                if (Objects.equals(pollList.get(i).joinCode(), updatedPoll.joinCode())) { index = i; break; }
            }
            if (index != -1) { pollList.set(index, updatedPoll); System.out.println("ProfileController: Szavazás frissítve a listában: " + updatedPoll.joinCode()); }
            else { pollList.add(updatedPoll); System.out.println("ProfileController: Új szavazás hozzáadva a listához: " + updatedPoll.joinCode()); }
        });
    }

    public void updatePollStateInList(String joinCode, PollState newState) {
        if (joinCode == null || newState == null) return;
        Platform.runLater(() -> {
            pollList.stream()
                    .filter(poll -> Objects.equals(poll.joinCode(), joinCode))
                    .findFirst()
                    .ifPresent(oldPoll -> {
                        PollDTO updated = new PollDTO( oldPoll.joinCode(), oldPoll.name(), oldPoll.question(), oldPoll.type(), newState, oldPoll.options(), oldPoll.aspects(), oldPoll.scaleMin(), oldPoll.scaleMax(), oldPoll.results() );
                        int index = pollList.indexOf(oldPoll);
                        if (index != -1) { pollList.set(index, updated); System.out.println("ProfileController: Szavazás állapot frissítve a listában: " + joinCode + " -> " + newState.getDisplayName()); }
                    });
        });
    }

    @FXML void handleNewPollButtonAction(ActionEvent event) { System.out.println("ProfileController: 'Új Szavazás' gomb megnyomva."); uiManager.showCreatePollScreen(); }
    @FXML void handleDetailsButtonAction(ActionEvent event) { PollDTO sel = pollListView.getSelectionModel().getSelectedItem(); if (sel != null) { System.out.println("ProfileController: 'Részletek' gomb megnyomva ehhez: " + sel.joinCode()); uiManager.showPollDetailsScreen(sel); } else { System.err.println("ProfileController: 'Részletek' gomb megnyomva, de nincs kiválasztott elem!"); uiManager.showError("Nincs kiválasztott szavazás a részletek megtekintéséhez.", "Kiválasztási Hiba"); } }
    @FXML void handleDeleteButtonAction(ActionEvent event) { PollDTO sel = pollListView.getSelectionModel().getSelectedItem(); if (sel != null) { System.out.println("ProfileController: 'Törlés' gomb megnyomva ehhez: " + sel.joinCode()); boolean confirmed = uiManager.showConfirmation( "Biztosan törölni szeretnéd a(z) '" + sel.name() + "' [" + sel.joinCode() + "] szavazást?\n" + "Ez a művelet nem vonható vissza!", "Törlés Megerősítése"); if (confirmed) { System.out.println("ProfileController: Törlési kérés küldése: " + sel.joinCode()); DeletePollRequest request = new DeletePollRequest(sel.joinCode()); getServerConnection().sendRequest(request); } else { System.out.println("ProfileController: Törlés megszakítva."); } } else { System.err.println("ProfileController: 'Törlés' gomb megnyomva, de nincs kiválasztott elem!"); uiManager.showError("Nincs kiválasztott szavazás a törléshez.", "Kiválasztási Hiba"); } }
    @FXML void handleLogoutButtonAction(ActionEvent event) { System.out.println("ProfileController: 'Kijelentkezés' gomb megnyomva."); getServerConnection().sendRequest(new LogoutRequest()); }
    private void handleListDoubleClick(MouseEvent event) { if (event.getClickCount() >= 2) { PollDTO sel = pollListView.getSelectionModel().getSelectedItem(); if (sel != null) { System.out.println("ProfileController: Dupla kattintás észlelve: " + sel.joinCode()); handleDetailsButtonAction(null); } } }
}