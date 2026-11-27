package ui;

import backend.AppService;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/*
Home page connected to the database by Dev Sheth, 11/17/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/** The Home page: an overview of the word database. */
public class HomePage {

    private record Snapshot(AppService.Stats stats, List<AppService.ImportRecord> imports, List<String> sentences) {
    }

    private final Consumer<String> onNavigate;
    private final AppService service;

    private final Label uniqueValue = statValue();
    private final Label totalValue = statValue();
    private final Label pairsValue = statValue();
    private final Label filesValue = statValue();
    private final TableView<AppService.ImportRecord> importsTable = new TableView<>();
    private final ListView<String> sentencesList = new ListView<>();

    private final ScrollPane view;

    /** Keeps the old signature so the existing tests still compile. */
    public HomePage(Consumer<String> onNavigate) {
        this(onNavigate, null);
    }

    public HomePage(Consumer<String> onNavigate, AppService service) {
        this.onNavigate = onNavigate;
        this.service = service;
        view = buildContent();
    }

    public Parent getView() {
        return view;
    }

    private static Label statValue() {
        Label value = new Label("—");
        value.getStyleClass().add("stat-value");
        return value;
    }

    /**
     * Reloads the tiles and both lists from MySQL, off the JavaFX thread. App calls
     * this each time Home is shown.
     */
    public void refresh() {
        if (service == null) {
            return;
        }
        Task<Snapshot> task = new Task<>() {
            @Override
            protected Snapshot call() throws Exception {
                return new Snapshot(service.getStats(), service.getImports(8), service.getRecentSentences(8));
            }
        };
        task.setOnSucceeded(e -> {
            Snapshot s = task.getValue();
            uniqueValue.setText(String.format("%,d", s.stats().uniqueWords()));
            totalValue.setText(String.format("%,d", s.stats().totalWords()));
            pairsValue.setText(String.format("%,d", s.stats().wordPairs()));
            filesValue.setText(String.format("%,d", s.stats().files()));
            importsTable.getItems().setAll(s.imports());
            sentencesList.getItems().setAll(s.sentences());
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private ScrollPane buildContent() {
        VBox page = new VBox(24, header(), statTiles(), new HBox(24, quickActions(), recentImports()),
                recentSentences());
        page.getStyleClass().add("page");

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    private HBox header() {
        Label title = new Label("Home");
        title.getStyleClass().add("page-title");
        Label subtitle = new Label("Overview of the word database built from your imported text.");
        subtitle.getStyleClass().add("page-subtitle");
        VBox titles = new VBox(6, title, subtitle);
        HBox.setHgrow(titles, Priority.ALWAYS);

        Button importButton = new Button("Import text file");
        importButton.getStyleClass().add("primary");
        importButton.setOnAction(e -> onNavigate.accept(Sidebar.IMPORT));

        HBox header = new HBox(16, titles, importButton);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    private HBox statTiles() {
        return new HBox(16,
                statTile("UNIQUE WORDS", uniqueValue),
                statTile("TOTAL WORDS READ", totalValue),
                statTile("WORD PAIRS", pairsValue),
                statTile("FILES IMPORTED", filesValue));
    }

    private VBox statTile(String name, Label value) {
        Label label = new Label(name);
        label.getStyleClass().add("field-label");

        VBox tile = new VBox(8, label, value);
        tile.getStyleClass().add("card");
        tile.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(tile, Priority.ALWAYS);
        return tile;
    }

    private VBox quickActions() {
        Label title = new Label("Quick actions");
        title.getStyleClass().add("section-title");

        VBox card = new VBox(10, title,
                actionButton("Generate a sentence", Sidebar.GENERATE),
                actionButton("Batch generate sentences", Sidebar.BATCH),
                actionButton("Write with auto-complete", Sidebar.AUTOCOMPLETE),
                actionButton("Browse words", Sidebar.WORDS));
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setMinWidth(300);
        return card;
    }

    private Button actionButton(String text, String destination) {
        Button button = new Button(text);
        button.getStyleClass().add("action");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> onNavigate.accept(destination));
        return button;
    }

    private VBox recentImports() {
        importsTable.getColumns().add(importColumn("File", r -> r.file()));
        importsTable.getColumns().add(importColumn("Words", r -> String.format("%,d", r.words())));
        importsTable.getColumns().add(importColumn("Imported", r -> r.imported()));
        importsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        importsTable.setPlaceholder(mutedLabel("No files imported yet."));
        importsTable.setPrefHeight(210);

        VBox card = new VBox(cardHeader("Recent imports", "View all", Sidebar.IMPORT), importsTable);
        card.getStyleClass().addAll("card", "flush");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private TableColumn<AppService.ImportRecord, String> importColumn(String title,
            Function<AppService.ImportRecord, String> value) {
        TableColumn<AppService.ImportRecord, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        return column;
    }

    private VBox recentSentences() {
        sentencesList.setPlaceholder(mutedLabel("No sentences generated yet."));
        sentencesList.setPrefHeight(150);

        HBox top = cardHeader("Recently generated", "All sentences", Sidebar.REPORTS);
        top.getStyleClass().remove("card-header");

        VBox card = new VBox(8, top, sentencesList);
        card.getStyleClass().add("card");
        return card;
    }

    private HBox cardHeader(String title, String linkText, String destination) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button link = new Button(linkText);
        link.getStyleClass().add("link");
        link.setOnAction(e -> onNavigate.accept(destination));

        HBox header = new HBox(titleLabel, spacer, link);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("card-header");
        return header;
    }

    private Label mutedLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }
}