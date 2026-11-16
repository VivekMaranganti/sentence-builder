package ui;

import backend.AppService;
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

import java.util.function.Consumer;

/*
Home page stat tiles connected to the database by Dev Sheth, 11/10/2026, with AI assistance (Claude).
*/
/**
 * The Home page: an overview of the word database.
 */
public class HomePage {

    private final Consumer<String> onNavigate;
    private final AppService service;
    private final ScrollPane view;
    // Edited by Dev Sheth: (with help from Claude) 11/10/2026: Added the four
    // labels so they can be updated when the stats are loaded.
    private final Label uniqueValue = statValue();
    private final Label totalValue = statValue();
    private final Label pairsValue = statValue();
    private final Label filesValue = statValue();

    /** Keeps the old signature so the existing tests still compile. */
    public HomePage(Consumer<String> onNavigate) {
        this(onNavigate, null);
    }

    public HomePage(Consumer<String> onNavigate, AppService service) {
        this.onNavigate = onNavigate;
        this.service = service;
        view = buildContent();
        loadStats();
    }

    private static Label statValue() {
        Label value = new Label("—");
        value.getStyleClass().add("stat-value");
        return value;
    }

    /** Fills the four tiles from MySQL, off the JavaFX thread. */
    private void loadStats() {
        if (service == null) {
            return;
        }
        Task<AppService.Stats> task = new Task<>() {
            @Override
            protected AppService.Stats call() throws Exception {
                return service.getStats();
            }
        };
        task.setOnSucceeded(e -> {
            AppService.Stats s = task.getValue();
            uniqueValue.setText(String.format("%,d", s.uniqueWords()));
            totalValue.setText(String.format("%,d", s.totalWords()));
            pairsValue.setText(String.format("%,d", s.wordPairs()));
            filesValue.setText(String.format("%,d", s.files()));
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    public Parent getView() {
        return view;
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

    /** A white card with a label and a number. */
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
        TableView<String> table = new TableView<>();
        table.getColumns().add(new TableColumn<String, String>("File"));
        table.getColumns().add(new TableColumn<String, String>("Words"));
        table.getColumns().add(new TableColumn<String, String>("Imported"));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(mutedLabel("No files imported yet."));
        table.setPrefHeight(210);

        VBox card = new VBox(cardHeader("Recent imports", "View all", Sidebar.IMPORT), table);
        card.getStyleClass().addAll("card", "flush");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox recentSentences() {
        ListView<String> list = new ListView<>();
        list.setPlaceholder(mutedLabel("No sentences generated yet."));
        list.setPrefHeight(150);

        HBox top = cardHeader("Recently generated", "All sentences", Sidebar.REPORTS);
        top.getStyleClass().remove("card-header"); // this card already has padding

        VBox card = new VBox(8, top, list);
        card.getStyleClass().add("card");
        return card;
    }

    /** A card title on the left and a small text link on the right. */
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
