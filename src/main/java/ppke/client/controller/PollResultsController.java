package ppke.client.controller;

import javafx.event.ActionEvent; import javafx.fxml.FXML; import javafx.geometry.Insets; import javafx.scene.Node;
import javafx.scene.chart.*; import javafx.scene.control.*; import javafx.scene.layout.*; import javafx.scene.paint.Color;
import javafx.scene.text.*; import ppke.common.dto.*; import ppke.common.model.PollType;
import ppke.client.UIManager; // <-- IMPORT ITT IS KELL!

import java.util.*; import java.util.stream.Collectors;

/**
 * Az eredményeket megjelenítő képernyő (poll_results.fxml) vezérlő osztálya.
 * Felelős az eredményadatok fogadásáért és a szavazás típusának megfelelő
 * vizuális megjelenítésért (pl. oszlopdiagram, szófelhő).
 * Megkülönbözteti, hogy a host vagy egy szavazó nézi-e, és ennek megfelelően
 * kezeli a "Bezárás/Vissza" gomb működését.
 */
public class PollResultsController extends ControllerBase {

    @FXML private Label pollNameLabel; @FXML private Label pollQuestionLabel; @FXML private ScrollPane scrollPane;
    @FXML private VBox resultsArea; @FXML private Button closeButton;
    private PollDTO currentPollWithResults; private boolean isHostViewing;

    /**
     * Inicializálja a kontrollert és megjeleníti az eredményeket.
     * @param poll A szavazás DTO-ja az eredményekkel (nem lehet null).
     * @param isHostView true, ha a host nézi, false egyébként.
     */
    public void initializeData(PollDTO poll, boolean isHostView) { this.currentPollWithResults = Objects.requireNonNull(poll); this.isHostViewing = isHostView; System.out.println("PollResultsController: Inicializálás: " + poll.joinCode() + ", Host nézet: " + isHostView); pollNameLabel.setText("Eredmények: " + poll.name()); pollQuestionLabel.setText(poll.question()); pollQuestionLabel.setWrapText(true); scrollPane.setFitToWidth(true); if (isHostViewing) closeButton.setText("Vissza a Részletekhez"); else closeButton.setText("Bezárás / Vissza a Kezdőlapra"); displayResults(poll); }
    /** Megjeleníti az eredményeket a {@code resultsArea} VBox-ban a szavazás típusa alapján. */
    private void displayResults(PollDTO poll) { resultsArea.getChildren().clear(); resultsArea.setSpacing(20); resultsArea.setPadding(new Insets(10)); if (poll.results() == null) { System.out.println("PollResultsController: Nincsenek eredmény adatok."); resultsArea.getChildren().add(createCenteredLabel("Az eredmények még nem elérhetők vagy nincsenek feldolgozott adatok.")); return; } System.out.println("PollResultsController: Eredmények megjelenítése: " + poll.type()); try { Node node = null; switch (poll.type()) {
        case MULTIPLE_CHOICE: if (poll.results() instanceof Map<?,?> map) node = createBarChart(safeCastMapStringInt(map), "Szavazatok Eloszlása"); else throw new IllegalArgumentException("Hibás MC eredmény formátum."); break;
        case WORD_CLOUD: if (poll.results() instanceof Map<?,?> map) node = createWordCloud(safeCastMapStringInt(map), "Gyakori Szavak"); else throw new IllegalArgumentException("Hibás WC eredmény formátum."); break;
        case SCALE: node = createCenteredLabel("Skála eredmények megjelenítése nincs implementálva."); break; default: node = createCenteredLabel("Ismeretlen típus eredményei.");
    } if (node != null) resultsArea.getChildren().add(node); } catch (Exception e) { System.err.println("Eredmény megjelenítési hiba: "+e.getMessage()); e.printStackTrace(); resultsArea.getChildren().add(createCenteredLabel("Hiba az eredmények megjelenítésekor.")); }
    }
    /** Segédfüggvény központosított Label létrehozásához. */
    private Label createCenteredLabel(String text) { Label l = new Label(text); l.setWrapText(true); l.setTextAlignment(TextAlignment.CENTER); l.setMaxWidth(Double.MAX_VALUE); l.setAlignment(javafx.geometry.Pos.CENTER); return l; }
    /** Biztonságosan kasztol egy Map-et Map<String, Integer>-re. */
    @SuppressWarnings("unchecked") private Map<String, Integer> safeCastMapStringInt(Map<?, ?> map) throws ClassCastException { if (map == null) return Collections.emptyMap(); for(Map.Entry<?,?> e:map.entrySet()) if(!(e.getKey() instanceof String) || !(e.getValue() instanceof Integer)) throw new ClassCastException("Nem Map<String, Integer>"); return (Map<String, Integer>) map; }
    /** Biztonságosan kasztol egy Map-et Map<String, Double>-re. */
    @SuppressWarnings("unchecked") private Map<String, Double> safeCastMapStringDouble(Map<?, ?> map) throws ClassCastException { if (map == null) return Collections.emptyMap(); for(Map.Entry<?,?> e:map.entrySet()) if(!(e.getKey() instanceof String) || !(e.getValue() instanceof Double)) throw new ClassCastException("Nem Map<String, Double>"); return (Map<String, Double>) map; }
    /** Létrehoz egy oszlopdiagramot. */
    private Node createBarChart(Map<String, Integer> data, String title) { System.out.println("PollResultsController: Oszlopdiagram létrehozása..."); VBox cont = new VBox(5); Label t = new Label(title); t.setFont(Font.font("System", FontWeight.BOLD, 14)); cont.getChildren().add(t); CategoryAxis x = new CategoryAxis(); NumberAxis y = new NumberAxis(); y.setLabel("Szavazatok"); y.setTickLabelFormatter(new NumberAxis.DefaultFormatter(y, null, null)); y.setMinorTickCount(0); y.setAutoRanging(true); BarChart<String, Number> chart = new BarChart<>(x, y); chart.setAnimated(false); chart.setLegendVisible(false); chart.setHorizontalGridLinesVisible(true); chart.setVerticalGridLinesVisible(false); XYChart.Series<String, Number> series = new XYChart.Series<>(); data.entrySet().stream().sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)).forEach(e -> series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()))); chart.getData().add(series); chart.setCategoryGap(15); chart.setMinHeight(250); chart.setPrefHeight(Math.max(250, data.size() * 35 + 80)); chart.setMaxHeight(800); cont.getChildren().add(chart); System.out.println("PollResultsController: Oszlopdiagram létrehozva."); return cont; }
    /** Létrehoz egy egyszerű szófelhő megjelenítést FlowPane-nel. */
    private Node createWordCloud(Map<String, Integer> data, String title) { System.out.println("PollResultsController: Szófelhő létrehozása..."); VBox cont = new VBox(5); Label t = new Label(title); t.setFont(Font.font("System", FontWeight.BOLD, 14)); cont.getChildren().add(t); FlowPane fp = new FlowPane(); fp.setPadding(new Insets(10)); fp.setHgap(12); fp.setVgap(8); if(data.isEmpty()){ fp.getChildren().add(new Label("Nincs adat.")); cont.getChildren().add(fp); return cont; } LinkedHashMap<String, Integer> sorted = data.entrySet().stream().sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1,e2)->e1, LinkedHashMap::new)); int max = sorted.values().stream().findFirst().orElse(1); final double BASE=11.0, INC=28.0; for(Map.Entry<String, Integer> e : sorted.entrySet()){ Text text = new Text(e.getKey() + " [" + e.getValue() + "]"); double scale = (max == 1) ? 1.0 : Math.log1p(e.getValue()) / Math.log1p(max); double size = BASE + (INC * scale); text.setFont(Font.font("System", FontWeight.BOLD, Math.max(BASE, size))); fp.getChildren().add(text); } cont.getChildren().add(fp); System.out.println("PollResultsController: Szófelhő létrehozva " + data.size() + " szóval."); return cont; }
    /** Eseménykezelő a Bezárás/Vissza gombra. */
    @FXML void handleCloseButtonAction(ActionEvent event) { System.out.println("PollResultsController: 'Bezárás/Vissza' gomb megnyomva. Host nézet: " + isHostViewing); if (isHostViewing) { System.out.println("PollResultsController: Navigálás vissza a profilhoz (Host)."); uiManager.showProfileScreenAgain(); } else { System.out.println("PollResultsController: Leiratkozás és navigálás a kezdőképernyőre (Szavazó)."); getServerConnection().sendRequest(new UnsubscribeRequest()); uiManager.showStartScreen(); } }
    /** @return Az aktuálisan megjelenített szavazás join kódja, vagy null. */
    public String getCurrentPollJoinCode() { return (currentPollWithResults != null) ? currentPollWithResults.joinCode() : null; }
    /** @return true, ha a host nézi ezt a képernyőt, false egyébként. */
    public boolean isHostViewing() { return isHostViewing; }
}