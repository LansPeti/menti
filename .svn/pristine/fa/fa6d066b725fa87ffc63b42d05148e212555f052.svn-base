package ppke.client.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import ppke.common.dto.*;

import java.text.DecimalFormat;
import java.util.*;

/**
 * Az eredményeket megjelenítő képernyő (poll_results.fxml) vezérlő osztálya.
 * Felelős az eredményadatok fogadásáért és a szavazás típusának megfelelő
 * vizuális megjelenítésért (pl. oszlopdiagram, szófelhő, skála átlagok).
 * Megkülönbözteti, hogy a host vagy egy szavazó nézi-e, és ennek megfelelően
 * kezeli a "Bezárás/Vissza" gomb működését.
 */
public class PollResultsController extends ControllerBase {

    @FXML private Label pollNameLabel;
    @FXML private Label pollQuestionLabel;
    @FXML private ScrollPane scrollPane; // Konténer a resultsArea-hoz, hogy görgethető legyen
    @FXML private VBox resultsArea; // Ide kerülnek a diagramok/eredmények
    @FXML private Button closeButton;

    private PollDTO currentPollWithResults;
    private boolean isHostViewing;
    private static final DecimalFormat df = new DecimalFormat("#.##"); // Formátum az átlagokhoz

    /**
     * Inicializálja a kontrollert és megjeleníti az eredményeket.
     * @param poll A szavazás DTO-ja az eredményekkel (nem lehet null).
     * @param isHostView true, ha a host nézi, false egyébként.
     */
    public void initializeData(PollDTO poll, boolean isHostView) {
        this.currentPollWithResults = Objects.requireNonNull(poll);
        this.isHostViewing = isHostView;
        System.out.println("PollResultsController: Inicializálás: " + poll.joinCode() + ", Host nézet: " + isHostView);

        pollNameLabel.setText("Eredmények: " + poll.name());
        pollQuestionLabel.setText(poll.question());
        pollQuestionLabel.setWrapText(true);

        scrollPane.setFitToWidth(true); // A ScrollPane szélessége igazodjon a VBox-hoz

        if (isHostViewing) {
            closeButton.setText("Vissza a Részletekhez");
        } else {
            closeButton.setText("Bezárás / Vissza a Kezdőlapra");
        }
        displayResults(poll);
    }

    /** Megjeleníti az eredményeket a {@code resultsArea} VBox-ban a szavazás típusa alapján. */
    private void displayResults(PollDTO poll) {
        resultsArea.getChildren().clear();
        resultsArea.setSpacing(20); // Térköz a diagramok/elemek között
        resultsArea.setPadding(new Insets(10)); // Belső margó

        if (poll.results() == null) {
            System.out.println("PollResultsController: Nincsenek eredmény adatok.");
            resultsArea.getChildren().add(createCenteredLabel("Az eredmények még nem elérhetők vagy nincsenek feldolgozott adatok."));
            return;
        }

        System.out.println("PollResultsController: Eredmények megjelenítése: " + poll.type());
        try {
            Node resultsNode = null;
            switch (poll.type()) {
                case MULTIPLE_CHOICE:
                    if (poll.results() instanceof Map<?, ?> map) {
                        resultsNode = createBarChart(safeCastMapStringInt(map), "Szavazatok Eloszlása");
                    } else {
                        throw new IllegalArgumentException("Hibás Multiple Choice eredmény formátum. Várt: Map<String, Integer>");
                    }
                    break;
                case WORD_CLOUD:
                    if (poll.results() instanceof Map<?, ?> map) {
                        resultsNode = createWordCloud(safeCastMapStringInt(map), "Gyakori Szavak");
                    } else {
                        throw new IllegalArgumentException("Hibás Word Cloud eredmény formátum. Várt: Map<String, Integer>");
                    }
                    break;
                case SCALE:
                    if (poll.results() instanceof Map<?, ?> map) {
                        // A PollDTO-ból vesszük a min/max értékeket a tengelyhez
                        resultsNode = createScaleResultsView(safeCastMapStringDouble(map), "Átlagos Értékelések Szempontonként", poll.scaleMin(), poll.scaleMax());
                    } else {
                        throw new IllegalArgumentException("Hibás Scale eredmény formátum. Várt: Map<String, Double>, Kapott: " + (poll.results() != null ? poll.results().getClass().getName() : "null"));
                    }
                    break;
                default:
                    resultsNode = createCenteredLabel("Ismeretlen szavazástípus eredményei.");
            }
            if (resultsNode != null) {
                resultsArea.getChildren().add(resultsNode);
            }
        } catch (Exception e) {
            System.err.println("Eredmény megjelenítési hiba: " + e.getMessage());
            e.printStackTrace();
            resultsArea.getChildren().add(createCenteredLabel("Hiba az eredmények megjelenítésekor.\nRészletek a konzolon."));
        }
    }

    /** Segédfüggvény központosított Label létrehozásához. */
    private Label createCenteredLabel(String text) {
        Label label = new Label(text);
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        label.setMaxWidth(Double.MAX_VALUE); // Hogy kitöltse a rendelkezésre álló szélességet
        label.setAlignment(Pos.CENTER); // Középre igazítás a Label-en belül
        return label;
    }

    /** Biztonságosan kasztol egy Map-et Map<String, Integer>-re. Dob ClassCastException-t hiba esetén. */
    @SuppressWarnings("unchecked")
    private Map<String, Integer> safeCastMapStringInt(Map<?, ?> map) throws ClassCastException {
        if (map == null) return Collections.emptyMap();
        for (Map.Entry<?, ?> currentEntry : map.entrySet()) {
            if (!(currentEntry.getKey() instanceof String) || !(currentEntry.getValue() instanceof Integer)) {
                throw new ClassCastException("Nem Map<String, Integer> a bemeneti map. Kulcs: " + (currentEntry.getKey()!=null?currentEntry.getKey().getClass():"null") + ", Érték: " + (currentEntry.getValue()!=null?currentEntry.getValue().getClass():"null"));
            }
        }
        return (Map<String, Integer>) map;
    }

    /** Biztonságosan kasztol egy Map-et Map<String, Double>-re. Dob ClassCastException-t hiba esetén. */
    @SuppressWarnings("unchecked")
    private Map<String, Double> safeCastMapStringDouble(Map<?, ?> map) throws ClassCastException {
        if (map == null) return Collections.emptyMap();
        for (Map.Entry<?, ?> currentEntry : map.entrySet()) {
            if (!(currentEntry.getKey() instanceof String) || !(currentEntry.getValue() instanceof Double)) {
                throw new ClassCastException("Nem Map<String, Double> a bemeneti map. Kulcs: " + (currentEntry.getKey()!=null?currentEntry.getKey().getClass():"null") + ", Érték: " + (currentEntry.getValue()!=null?currentEntry.getValue().getClass():"null"));
            }
        }
        return (Map<String, Double>) map;
    }

    /** Létrehoz egy oszlopdiagramot Multiple Choice eredményekhez. */
    private Node createBarChart(Map<String, Integer> data, String title) {
        System.out.println("PollResultsController: Oszlopdiagram létrehozása...");
        VBox container = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        container.getChildren().add(titleLabel);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Opciók");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Szavazatok Száma");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, null)); // Egész számokhoz
        yAxis.setMinorTickCount(0); // Nincsenek al-osztások
        yAxis.setAutoRanging(true); // Automatikus tengelybeállítás

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false); // Animáció kikapcsolása
        chart.setLegendVisible(false); // Jelmagyarázat nem szükséges egy adatsorhoz
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER)) // Opciók ABC sorrendben
                .forEach(entry -> series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue())));

        chart.getData().add(series);
        chart.setCategoryGap(15); // Térköz az oszlopok között
        chart.setMinHeight(250);
        chart.setPrefHeight(Math.max(250, data.size() * 35 + 80)); // Dinamikus magasság
        chart.setMaxHeight(800); // Maximális magasság korlátozása

        container.getChildren().add(chart);
        System.out.println("PollResultsController: Oszlopdiagram létrehozva.");
        return container;
    }


    /**
     * Létrehoz egy vizuálisan gazdagabb szófelhő megjelenítést FlowPane-nel.
     * A szavak mérete a gyakoriságukkal arányos.
     *
     * @param data A szógyakoriságokat tartalmazó Map (kulcs: szó, érték: gyakoriság).
     * @param title A szófelhő címe (pl. "Gyakori Szavak").
     * @return Egy Node (VBox), amely tartalmazza a címet és a generált szófelhőt (FlowPane).
     */
    private Node createWordCloud(Map<String, Integer> data, String title) {
        System.out.println("PollResultsController: Szófelhő létrehozása (dinamikus méretezéssel)...");
        VBox container = new VBox(5); // Térköz a cím és a szófelhő között
        container.setAlignment(Pos.CENTER_LEFT); // Igazítás, ha szükséges

        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        container.getChildren().add(titleLabel);

        FlowPane flowPane = new FlowPane();
        flowPane.setPadding(new Insets(10)); // Belső margó a FlowPane-en belül
        flowPane.setHgap(10); // Vízszintes térköz a szavak (Text node-ok) között
        flowPane.setVgap(6);  // Függőleges térköz a sorok között

        if (data == null || data.isEmpty()) {
            Label emptyLabel = new Label("Nincs megjeleníthető szó az adatokban.");
            emptyLabel.setFont(Font.font("System", FontPosture.ITALIC, 12));
            flowPane.getChildren().add(emptyLabel);
            container.getChildren().add(flowPane);
            System.out.println("PollResultsController: Szófelhő létrehozása - nincsenek adatok.");
            return container;
        }

        // 1. Maximális gyakoriság meghatározása a skálázáshoz
        // Ha a data üres, a Collections.max hibát dobna, ezért az ellenőrzés fontos.
        int maxFrequency = 1; // Alapértelmezett, hogy elkerüljük a nullával való osztást
        if (!data.isEmpty()) { // Csak akkor keressük, ha van tartalom
            maxFrequency = Collections.max(data.values());
            if (maxFrequency == 0) { // Ha minden szó gyakorisága 0
                maxFrequency = 1;
            }
        }

        // 2. Betűméret határainak és egyéb vizuális paramétereknek a definiálása
        final double minFontSize = 11.0;  // Minimális betűméret
        final double maxFontSize = 28.0;  // Maximális betűméret a leggyakoribb szónak


        // 3. Szavak rendezése (opcionális, de ajánlott a konzisztens megjelenésért)
        // Például gyakoriság szerint csökkenő, majd ábécérendben.
        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(data.entrySet());
        sortedEntries.sort((e1, e2) -> {
            int freqCompare = e2.getValue().compareTo(e1.getValue()); // Csökkenő gyakoriság
            if (freqCompare == 0) {
                return e1.getKey().compareTo(e2.getKey()); // Azonos gyakoriságnál ábécérend
            }
            return freqCompare;
        });


        // 4. Szavak hozzáadása a FlowPane-hez dinamikus mérettel és esetleg színnel
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            Text textNode = new Text(entry.getKey()); // Csak a szót jelenítjük meg, a számot nem a felhőben
            int frequency = entry.getValue();

            // Betűméret kiszámítása lineáris skálázással
            double normalizedFrequency = (maxFrequency == 1 && frequency == 1) ? 1.0 : (double) frequency / maxFrequency; // Ha csak 1 a max, és a freq is 1, akkor is max méret
            if (maxFrequency == 1 && frequency ==0) normalizedFrequency = 0.0; // Ha a max 1, de a freq 0

            double fontSize = minFontSize + (normalizedFrequency * (maxFontSize - minFontSize));

            // Biztosítjuk, hogy a betűméret a definiált határokon belül maradjon
            fontSize = Math.max(minFontSize, Math.min(fontSize, maxFontSize));

            textNode.setFont(Font.font("System", FontWeight.BOLD, fontSize));

            // Színek hozzáadása a gyakoriság alapján a jobb vizuális elkülönítésért
            if (normalizedFrequency > 0.66) {
                textNode.setFill(Color.rgb(220, 50, 50)); // Erőteljesebb szín a leggyakoribbaknak
            } else if (normalizedFrequency > 0.33) {
                textNode.setFill(Color.rgb(50, 130, 220)); // Közepes
            } else {
                textNode.setFill(Color.rgb(70, 70, 70));    // Halványabb a kevésbé gyakoriaknak
            }
            // Tooltip hozzáadása, ami mutatja a pontos gyakoriságot
            Tooltip.install(textNode, new Tooltip(entry.getKey() + ": " + frequency + " db"));


            flowPane.getChildren().add(textNode);
        }
        container.getChildren().add(flowPane);
        System.out.println("PollResultsController: Szófelhő létrehozva (dinamikus) " + data.size() + " egyedi szóval.");
        return container;
    }
    /** Létrehoz egy nézetet a Scale típusú eredményekhez (pl. oszlopdiagram az átlagoknak). */
    private Node createScaleResultsView(Map<String, Double> data, String title, int scaleMin, int scaleMax) {
        System.out.println("PollResultsController: Skála eredmények nézetének létrehozása...");
        VBox container = new VBox(5);
        Label titleLabel = new Label(title);
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        container.getChildren().add(titleLabel);

        if (data.isEmpty()) {
            container.getChildren().add(createCenteredLabel("Nincsenek megjeleníthető átlagok."));
            return container;
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Szempontok");
        NumberAxis yAxis = new NumberAxis(scaleMin, scaleMax, Math.max(1.0, (scaleMax - scaleMin) / 10.0));
        yAxis.setLabel("Átlagos Értékelés");
        yAxis.setTickLabelFormatter(new NumberAxis.DefaultFormatter(yAxis, null, null));

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setAnimated(false);
        chart.setLegendVisible(false);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        data.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String.CASE_INSENSITIVE_ORDER))
                .forEach(entry -> {
                    XYChart.Data<String, Number> chartData = new XYChart.Data<>(entry.getKey(), entry.getValue());
                    series.getData().add(chartData);
                });

        chart.getData().add(series);
        chart.setCategoryGap(15);
        chart.setMinHeight(250);
        chart.setPrefHeight(Math.max(250, data.size() * 40 + 80));
        chart.setMaxHeight(800);

        for(XYChart.Series<String, Number> s : chart.getData()){
            for (XYChart.Data<String, Number> d : s.getData()) {
                Node node = d.getNode();
                if (node != null) {
                    String tooltipText = d.getXValue() + ": " + df.format(d.getYValue().doubleValue());
                    Tooltip.install(node, new Tooltip(tooltipText));
                }
            }
        }

        container.getChildren().add(chart);
        System.out.println("PollResultsController: Skála eredmények nézete (BarChart) létrehozva.");
        return container;
    }


    /** Eseménykezelő a Bezárás/Vissza gombra. */
    @FXML
    void handleCloseButtonAction(ActionEvent event) {
        System.out.println("PollResultsController: 'Bezárás/Vissza' gomb megnyomva. Host nézet: " + isHostViewing);
        if (isHostViewing) {
            System.out.println("PollResultsController: Navigálás vissza a profilhoz (Host).");
            uiManager.showProfileScreenAgain();
        } else {
            System.out.println("PollResultsController: Leiratkozás és navigálás a kezdőképernyőre (Szavazó).");
            if (getServerConnection() != null) {
                getServerConnection().sendRequest(new UnsubscribeRequest());
            }
            uiManager.showStartScreen();
        }
    }

    /** @return Az aktuálisan megjelenített szavazás join kódja, vagy null. */
    public String getCurrentPollJoinCode() {
        return (currentPollWithResults != null) ? currentPollWithResults.joinCode() : null;
    }

    /** @return true, ha a host nézi ezt a képernyőt, false egyébként. */
    public boolean isHostViewing() {
        return isHostViewing;
    }
}
