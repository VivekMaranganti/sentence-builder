package ui;

import backend.AppService;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/*
Reports connected to the database by Dev Sheth, 11/18/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * Reports: generated sentences, duplicates, and word statistics, with CSV
 * export.
 */
public class ReportsPage {

    private record Data(List<AppService.SentenceRecord> sentences, List<AppService.AlgorithmRate> rates,
            List<AppService.WordRow> top, List<AppService.WordRow> starts,
            List<AppService.WordRow> ends) {
    }

    private final AppService service;
    private final Parent view;
    private final TabPane tabs = new TabPane();

    private final TextField search = new TextField();
    private final ComboBox<String> algorithm = new ComboBox<>();
    private final CheckBox duplicatesOnly = new CheckBox("Duplicates only");
    private final TableView<AppService.SentenceRecord> sentenceTable = new TableView<>();
    private final Label generatedTotal = new Label("0");
    private final Label duplicateTotal = new Label("0");
    private final VBox rateRows = new VBox(18);

    private final TableView<AppService.WordRow> topTable = new TableView<>();
    private final TableView<AppService.WordRow> startsTable = new TableView<>();
    private final TableView<AppService.WordRow> endsTable = new TableView<>();
    private final Label status = Widgets.muted("");
    private int requestSeq;

    /** Keeps the old signature so the existing tests still compile. */
    public ReportsPage() {
        this(null);
    }

    public ReportsPage(AppService service) {
        this.service = service;
        VBox heading = Widgets.header("Reports", "Generated sentences, duplicates, and word statistics.");
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button export = new Button("Export CSV");
        export.setOnAction(e -> export());
        HBox header = new HBox(16, heading, status, export);
        header.setAlignment(Pos.CENTER_LEFT);

        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("Generated sentences", generatedReport()),
                new Tab("Top words", tableCard("Most frequent words", topTable, true)),
                new Tab("Starts & ends", startsAndEnds()));
        view = Widgets.page(header, tabs);
        view.getStyleClass().add("reports-page");
        view.getStylesheets().add(new File("src/ui/css/reports.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    /**
     * Reloads every report from MySQL. App calls this each time the page is shown.
     */
    public void refresh() {
        if (service == null) {
            return;
        }
        int seq = ++requestSeq;
        String text = search.getText();
        String alg = algorithm.getValue();
        boolean dups = duplicatesOnly.isSelected();

        Task<Data> task = new Task<>() {
            @Override
            protected Data call() throws Exception {
                return new Data(service.getReportSentences(text, alg, dups), service.getAlgorithmRates(),
                        service.getTopWords("count", 25), service.getTopWords("start", 15),
                        service.getTopWords("end", 15));
            }
        };
        task.setOnSucceeded(e -> {
            if (seq != requestSeq) {
                return;
            }
            Data d = task.getValue();
            status.setText("");
            sentenceTable.getItems().setAll(d.sentences());
            topTable.getItems().setAll(d.top());
            startsTable.getItems().setAll(d.starts());
            endsTable.getItems().setAll(d.ends());

            int generated = 0;
            int duplicates = 0;
            rateRows.getChildren().clear();
            for (AppService.AlgorithmRate r : d.rates()) {
                generated += r.total();
                duplicates += r.total() - r.distinct();
                rateRows.getChildren().add(rate(r.algorithm(), r.duplicatePercent()));
            }
            if (d.rates().isEmpty()) {
                rateRows.getChildren().add(Widgets.muted("No sentences generated yet."));
            }
            generatedTotal.setText(String.format("%,d", generated));
            duplicateTotal.setText(String.format("%,d", duplicates));
        });
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------- Generated sentences ----------------

    private HBox generatedReport() {
        buildSentenceTable();
        VBox tableCard = new VBox(filters(), sentenceTable);
        tableCard.getStyleClass().addAll("card", "flush");
        tableCard.setMinWidth(570);
        tableCard.setMaxHeight(Region.USE_PREF_SIZE);
        HBox.setHgrow(tableCard, Priority.ALWAYS);
        VBox summary = new VBox(20, totalsCard(), duplicatesCard());
        summary.setPrefWidth(300);
        summary.setMinWidth(270);
        HBox content = new HBox(20, tableCard, summary);
        content.getStyleClass().add("report-content");
        return content;
    }

    private HBox filters() {
        search.setPromptText("Search sentences...");
        search.setMinWidth(140);
        HBox.setHgrow(search, Priority.ALWAYS);
        algorithm.getItems().add("All algorithms");
        algorithm.getItems().addAll(AppService.ALGORITHMS);
        algorithm.getItems().add("Auto-complete");
        algorithm.setValue("All algorithms");

        search.textProperty().addListener((obs, was, now) -> refresh());
        algorithm.valueProperty().addListener((obs, was, now) -> refresh());
        duplicatesOnly.setOnAction(e -> refresh());

        HBox filters = new HBox(12, search, algorithm, duplicatesOnly);
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("card-header");
        return filters;
    }

    private void buildSentenceTable() {
        sentenceTable.getColumns().add(column("Sentence", 4.5, AppService.SentenceRecord::text));
        sentenceTable.getColumns().add(column("Algorithm", 1.8, AppService.SentenceRecord::algorithm));
        sentenceTable.getColumns().add(column("Created", 1, AppService.SentenceRecord::created));
        TableColumn<AppService.SentenceRecord, String> times = column("Times", 0.7,
                sentence -> String.valueOf(sentence.times()));
        times.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                setText(null);
                setGraphic(null);
                if (!empty && value != null) {
                    if (value.equals("1")) {
                        setText(value);
                    } else {
                        setGraphic(Widgets.pill("×" + value, "orange"));
                    }
                }
            }
        });
        sentenceTable.getColumns().add(times);
        sentenceTable.setPlaceholder(Widgets.muted("No sentences generated yet."));
        sentenceTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        sentenceTable.setFixedCellSize(40);
        sentenceTable.setPrefHeight(320);
    }

    private <T> TableColumn<T, String> column(String title, double share, Function<T, String> value) {
        TableColumn<T, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(share * 100);
        return column;
    }

    // ---------------- Summary ----------------

    private HBox totalsCard() {
        generatedTotal.getStyleClass().add("stat-value");
        duplicateTotal.getStyleClass().addAll("stat-value", "duplicate-total");
        VBox left = new VBox(8, Widgets.caption("Generated"), generatedTotal);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox card = new HBox(24, left, new VBox(8, Widgets.caption("Duplicates"), duplicateTotal));
        card.getStyleClass().add("card");
        return card;
    }

    private VBox duplicatesCard() {
        Label title = new Label("Duplicate rate by algorithm");
        title.getStyleClass().add("section-title");
        title.setWrapText(true);
        Label explanation = Widgets.muted("Greedy picks repeat most, since it always takes the top next word.");
        explanation.setWrapText(true);
        VBox card = new VBox(18, title, rateRows, explanation);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox rate(String algorithmName, int percent) {
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label value = new Label(percent + "%");
        value.setStyle("-fx-font-weight: bold;");
        HBox labels = new HBox(new Label(algorithmName), gap, value);
        ProgressBar bar = new ProgressBar(percent / 100.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, labels, bar);
    }

    // ---------------- Word statistics ----------------

    private VBox tableCard(String title, TableView<AppService.WordRow> table, boolean allColumns) {
        table.getColumns().add(column("Word", 2, AppService.WordRow::text));
        table.getColumns().add(column("Count", 1, w -> String.valueOf(w.count())));
        if (allColumns) {
            table.getColumns().add(column("Starts", 1, w -> String.valueOf(w.starts())));
            table.getColumns().add(column("Ends", 1, w -> String.valueOf(w.ends())));
        } else {
            table.getColumns().add(column(title.contains("starters") ? "Starts" : "Ends", 1,
                    w -> String.valueOf(title.contains("starters") ? w.starts() : w.ends())));
        }
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(Widgets.muted("No words yet."));
        table.setFixedCellSize(36);
        table.setPrefHeight(36 * 10 + 30);

        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox card = new VBox(12, heading, table);
        card.getStyleClass().add("card");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox startsAndEnds() {
        // tableCard picks the right count column from these titles
        HBox content = new HBox(20, tableCard("Top sentence starters", startsTable, false),
                tableCard("Top sentence enders", endsTable, false));
        content.getStyleClass().add("report-content");
        return content;
    }

    // ---------------- Export ----------------

    private void export() {
        List<String[]> rows = new ArrayList<>();
        String name;
        switch (tabs.getSelectionModel().getSelectedIndex()) {
            case 0 -> {
                name = "generated_sentences.csv";
                rows.add(new String[] { "Sentence", "Algorithm", "Created", "Times" });
                sentenceTable.getItems().forEach(s -> rows
                        .add(new String[] { s.text(), s.algorithm(), s.created(), String.valueOf(s.times()) }));
            }
            case 1 -> {
                name = "top_words.csv";
                rows.add(new String[] { "Word", "Count", "Starts", "Ends" });
                topTable.getItems().forEach(w -> rows.add(new String[] { w.text(), String.valueOf(w.count()),
                        String.valueOf(w.starts()), String.valueOf(w.ends()) }));
            }
            default -> {
                name = "starts_and_ends.csv";
                rows.add(new String[] { "Type", "Word", "Count" });
                startsTable.getItems().forEach(
                        w -> rows.add(new String[] { "Starts sentences", w.text(), String.valueOf(w.starts()) }));
                endsTable.getItems()
                        .forEach(w -> rows.add(new String[] { "Ends sentences", w.text(), String.valueOf(w.ends()) }));
            }
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export report");
        chooser.setInitialFileName(name);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showSaveDialog(view.getScene().getWindow());
        if (file == null) {
            return;
        }
        StringBuilder csv = new StringBuilder();
        for (String[] row : rows) {
            for (int i = 0; i < row.length; i++) {
                if (i > 0) {
                    csv.append(',');
                }
                csv.append('"').append(row[i].replace("\"", "\"\"")).append('"');
            }
            csv.append("\r\n");
        }
        try {
            Files.writeString(file.toPath(), csv.toString(), StandardCharsets.UTF_8);
            status.setText("Exported " + (rows.size() - 1) + " rows.");
        } catch (IOException ex) {
            status.setText("Export failed: " + ex.getMessage());
        }
    }
}