package ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** The dark navigation sidebar shown on every page. */
public class Sidebar {

    // Page names. These are the button labels and also the keys App uses to find each page.
    public static final String HOME = "Home";
    public static final String IMPORT = "Import Files";
    public static final String GENERATE = "Generate";
    public static final String BATCH = "Batch Generate";
    public static final String AUTOCOMPLETE = "Auto-complete";
    public static final String WORDS = "Word Explorer";
    public static final String REPORTS = "Reports";

    // Icons (SVG path drawings, 24x24)
    private static final String HOME_ICON = "M3 11l9-8 9 8M5 10v10h14V10";
    private static final String IMPORT_ICON = "M12 3v12M7 10l5 5 5-5M4 21h16";
    private static final String GENERATE_ICON = "M4 20l4-1 11-11-3-3L5 16l-1 4z";
    private static final String BATCH_ICON = "M12 3l9 5-9 5-9-5 9-5zM3 13l9 5 9-5";
    private static final String AUTOCOMPLETE_ICON = "M5 6h14a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2zM7 10h1M11 10h1M15 10h1M7 14h10";
    private static final String WORDS_ICON = "M4 5h16M4 10h16M4 15h10M4 20h7";
    private static final String REPORTS_ICON = "M4 20V10M10 20V4M16 20v-7M22 20H2";

    private final Map<String, Button> buttons = new LinkedHashMap<>();
    private final VBox view;

    /** @param onNavigate called with the page name when a nav button is clicked */
    public Sidebar(Consumer<String> onNavigate) {
        Label logo = new Label("SB");
        logo.getStyleClass().add("brand-logo");
        Label appName = new Label("Sentence Builder");
        appName.getStyleClass().add("brand-name");
        appName.setMinWidth(Region.USE_PREF_SIZE); // don't cut the name off with "..."
        HBox brand = new HBox(10, logo, appName);
        brand.setAlignment(Pos.CENTER_LEFT);
        brand.getStyleClass().add("brand");

        view = new VBox(4, brand);
        addNavButton(HOME, HOME_ICON, onNavigate);
        addNavButton(IMPORT, IMPORT_ICON, onNavigate);
        addNavButton(GENERATE, GENERATE_ICON, onNavigate);
        addNavButton(BATCH, BATCH_ICON, onNavigate);
        addNavButton(AUTOCOMPLETE, AUTOCOMPLETE_ICON, onNavigate);
        addNavButton(WORDS, WORDS_ICON, onNavigate);
        addNavButton(REPORTS, REPORTS_ICON, onNavigate);

        // Pushes the database status to the bottom of the sidebar
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Region statusDot = new Region();
        statusDot.getStyleClass().add("status-dot");
        HBox dbStatus = new HBox(8, statusDot, new Label("MySQL · not connected"));
        dbStatus.setAlignment(Pos.CENTER_LEFT);
        dbStatus.getStyleClass().add("db-status");

        view.getChildren().addAll(spacer, dbStatus);
        view.getStyleClass().add("sidebar");
        view.setPrefWidth(240);
        view.setMinWidth(240);
    }

    public Parent getView() {
        return view;
    }

    /** Highlights the button for the given page. */
    public void setActive(String page) {
        buttons.forEach((name, button) -> {
            button.getStyleClass().remove("active");
            if (name.equals(page)) {
                button.getStyleClass().add("active");
            }
        });
    }

    private void addNavButton(String text, String icon, Consumer<String> onNavigate) {
        SVGPath iconShape = new SVGPath();
        iconShape.setContent(icon);
        iconShape.getStyleClass().add("nav-icon");

        Button button = new Button(text, iconShape);
        button.getStyleClass().add("nav-button");
        button.setMaxWidth(Double.MAX_VALUE);
        button.setOnAction(e -> onNavigate.accept(text));

        buttons.put(text, button);
        view.getChildren().add(button);
    }
}
