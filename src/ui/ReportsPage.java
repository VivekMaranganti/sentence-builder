package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.function.Function;

/** Reports layout with sample sentences. Database queries and export will be connected later. */
public class ReportsPage {

    private record Sentence(String text, String algorithm, String created, int times) {
    }

    private final Parent view;

    public ReportsPage() {
        VBox heading = Widgets.header("Reports", "Generated sentences, duplicates, and word statistics.");
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button export = new Button("Export CSV");
        // TODO: export the current report to CSV
        HBox header = new HBox(16, heading, export);
        header.setAlignment(Pos.CENTER_LEFT);

        TabPane tabs = new TabPane();
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getTabs().addAll(new Tab("Generated sentences", generatedReport()),
                new Tab("Top words", pendingReport("Top words")),
                new Tab("Starts & ends", pendingReport("Starts & ends")));
        view = Widgets.page(header, tabs);
        view.getStyleClass().add("reports-page");
        view.getStylesheets().add(new File("src/ui/css/reports.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Generated sentences ----------------

    private HBox generatedReport() {
        VBox tableCard = new VBox(filters(), sentenceTable());
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
        TextField search = new TextField();
        search.setPromptText("Search sentences...");
        search.setMinWidth(140);
        HBox.setHgrow(search, Priority.ALWAYS);
        ComboBox<String> algorithm = new ComboBox<>();
        algorithm.getItems().addAll("All algorithms", "Weighted bigram", "Most frequent", "Random follower");
        algorithm.setValue("All algorithms");
        // TODO: filter the report by search text, algorithm, and duplicates
        HBox filters = new HBox(12, search, algorithm, new CheckBox("Duplicates only"));
        filters.setAlignment(Pos.CENTER_LEFT);
        filters.getStyleClass().add("card-header");
        return filters;
    }

    private TableView<Sentence> sentenceTable() {
        TableView<Sentence> table = new TableView<>();
        table.getColumns().add(column("Sentence", 4.5, Sentence::text));
        table.getColumns().add(column("Algorithm", 1.8, Sentence::algorithm));
        table.getColumns().add(column("Created", 1, Sentence::created));
        TableColumn<Sentence, String> times = column("Times", 0.7, sentence -> String.valueOf(sentence.times()));
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
        table.getColumns().add(times);
        table.getItems().addAll(
                new Sentence("The same time, and the room, and the room.", "Most frequent", "Oct 12", 4),
                new Sentence("Elizabeth was not been a very well as she had been.", "Weighted bigram", "Oct 12", 1),
                new Sentence("Her sister was quite so much attention to Netherfield.", "Weighted bigram", "Oct 12", 1),
                new Sentence("Darcy had been so much as to the subject.", "Weighted bigram", "Oct 12", 1),
                new Sentence("Mr. Knightley was very much to be sure.", "Most frequent", "Oct 11", 2),
                new Sentence("Captain Wentworth was in the first.", "Random follower", "Oct 11", 1),
                new Sentence("Nothing could be in the world.", "Weighted bigram", "Oct 11", 1));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(40);
        table.setPrefHeight(320);
        return table;
    }

    private TableColumn<Sentence, String> column(String title, double share, Function<Sentence, String> value) {
        TableColumn<Sentence, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(share * 100);
        return column;
    }

    // ---------------- Summary ----------------

    private HBox totalsCard() {
        Label generated = new Label("148");
        generated.getStyleClass().add("stat-value");
        Label duplicates = new Label("6");
        duplicates.getStyleClass().addAll("stat-value", "duplicate-total");
        VBox left = new VBox(8, Widgets.caption("Generated"), generated);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox card = new HBox(24, left, new VBox(8, Widgets.caption("Duplicates"), duplicates));
        card.getStyleClass().add("card");
        return card;
    }

    private VBox duplicatesCard() {
        Label title = new Label("Duplicate rate by algorithm");
        title.getStyleClass().add("section-title");
        title.setWrapText(true);
        Label explanation = Widgets.muted("Greedy picks repeat most, since it always takes the top next word.");
        explanation.setWrapText(true);
        VBox card = new VBox(18, title, rate("Most frequent", 11), rate("Weighted bigram", 2),
                rate("Random follower", 0), explanation);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox rate(String algorithm, int percent) {
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label value = new Label(percent + "%");
        value.setStyle("-fx-font-weight: bold;");
        HBox labels = new HBox(new Label(algorithm), gap, value);
        ProgressBar bar = new ProgressBar(percent / 11.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(6, labels, bar);
    }

    /** The mockup only shows the Generated sentences tab; other report bodies await a design. */
    private VBox pendingReport(String title) {
        Label heading = new Label(title);
        heading.getStyleClass().add("section-title");
        VBox card = new VBox(12, heading, Widgets.muted("No report data yet."));
        card.getStyleClass().add("card");
        VBox content = new VBox(card);
        content.getStyleClass().add("report-content");
        // TODO: display word statistics when the report data is connected
        return content;
    }
}
