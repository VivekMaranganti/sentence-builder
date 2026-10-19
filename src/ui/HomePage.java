package ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

/**
 * The Home page: a sidebar on the left and the overview in the middle.
 * Buttons for the other pages are here but don't do anything yet.
 */
public class HomePage {

    // Sidebar icons (SVG path drawings, 24x24)
    private static final String HOME_ICON = "M3 11l9-8 9 8M5 10v10h14V10";
    private static final String IMPORT_ICON = "M12 3v12M7 10l5 5 5-5M4 21h16";
    private static final String GENERATE_ICON = "M4 20l4-1 11-11-3-3L5 16l-1 4z";
    private static final String BATCH_ICON = "M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5";
    private static final String AUTOCOMPLETE_ICON = "M5 6h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2zM7 10h1M11 10h1M15 10h1M7 14h10";
    private static final String WORDS_ICON = "M4 5h16M4 10h16M4 15h10M4 20h7";
    private static final String REPORTS_ICON = "M4 20V10M10 20V4M16 20v-7M22 20H2";

    private final BorderPane root = new BorderPane();

    public HomePage() {
        root.setLeft(buildSidebar());
        root.setCenter(buildContent());
    }

    public Parent getView() {
        return root;
    }

    // ---------------- Sidebar ----------------

    private VBox buildSidebar() {
        Label logo = new Label("SB");
        logo.getStyleClass().add("brand-logo");
        Label appName = new Label("Sentence Builder");
        appName.getStyleClass().add("brand-name");
        appName.setMinWidth(Region.USE_PREF_SIZE); // don't cut the name off with "..."
        HBox brand = new HBox(10, logo, appName);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        // Pushes the database status to the bottom of the sidebar
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region statusDot = new Region();
        statusDot.getStyleClass().add("status-dot");
        HBox dbStatus = new HBox(8, statusDot, new Label("MySQL · not connected"));
        dbStatus.setAlignment(Pos.CENTER_LEFT);
        dbStatus.getStyleClass().add("db-status");

        VBox sidebar = new VBox(4,
                brand,
                navButton("Home", HOME_ICON, true),
                navButton("Import Files", IMPORT_ICON, false),
                navButton("Generate", GENERATE_ICON, false),
                navButton("Batch Generate", BATCH_ICON, false),
                navButton("Auto-complete", AUTOCOMPLETE_ICON, false),
                navButton("Word Explorer", WORDS_ICON, false),
                navButton("Reports", REPORTS_ICON, false),
                spacer,
                dbStatus);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(240);
        sidebar.setMinWidth(240);
        return sidebar;
    }

    private Button navButton(String text, String icon, boolean active) {
        SVGPath iconShape = new SVGPath();
        iconShape.setContent(icon);
        iconShape.getStyleClass().add("nav-icon");

        Button button = new Button(text, iconShape);
        button.getStyleClass().add("nav-button");
        if (active) {
            button.getStyleClass().add("active");
        }
        button.setMaxWidth(Double.MAX_VALUE);
        // TODO: open this page once it's built
        return button;
    }

    // ---------------- Main content ----------------

    private ScrollPane buildContent() {
        VBox page = new VBox(24, header(), statTiles(), new HBox(24, quickActions(), recentImports()), recentSentences());
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
        // TODO: open the Import page once it's built

        HBox header = new HBox(16, titles, importButton);
        header.setAlignment(Pos.BOTTOM_LEFT);
        return header;
    }

    private HBox statTiles() {
        return new HBox(16,
                statTile("UNIQUE WORDS"),
                statTile("TOTAL WORDS READ"),
                statTile("WORD PAIRS"),
                statTile("FILES IMPORTED"));
    }

    /** A white card with a label and a number. Shows "—" until real data is connected. */
    private VBox statTile(String name) {
        Label label = new Label(name);
        label.getStyleClass().add("field-label");
        Label value = new Label("—");
        value.getStyleClass().add("stat-value");

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
                actionButton("Generate a sentence"),
                actionButton("Batch generate sentences"),
                actionButton("Write with auto-complete"),
                actionButton("Browse words"));
        card.getStyleClass().add("card");
        card.setPrefWidth(300);
        card.setMinWidth(300);
        return card;
    }

    private Button actionButton(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("action");
        button.setMaxWidth(Double.MAX_VALUE);
        // TODO: open the matching page once it's built
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

        VBox card = new VBox(cardHeader("Recent imports", "View all"), table);
        card.getStyleClass().addAll("card", "flush");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox recentSentences() {
        ListView<String> list = new ListView<>();
        list.setPlaceholder(mutedLabel("No sentences generated yet."));
        list.setPrefHeight(150);

        HBox top = cardHeader("Recently generated", "All sentences");
        top.getStyleClass().remove("card-header"); // this card already has padding

        VBox card = new VBox(8, top, list);
        card.getStyleClass().add("card");
        return card;
    }

    /** A card title on the left and a small text link on the right. */
    private HBox cardHeader(String title, String linkText) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Button link = new Button(linkText);
        link.getStyleClass().add("link");
        // TODO: open the matching page once it's built

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
