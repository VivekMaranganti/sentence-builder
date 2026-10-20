package ui;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Small pieces that every page uses, so the pages look the same. */
final class Widgets {

    private Widgets() {
    }

    /** The big page title with a one-line description under it. */
    static VBox header(String title, String subtitle) {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("page-title");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.getStyleClass().add("page-subtitle");
        return new VBox(6, titleLabel, subtitleLabel);
    }

    /** Puts the sections in a padded column that scrolls when the window is short. */
    static ScrollPane page(Node... sections) {
        VBox page = new VBox(24, sections);
        page.getStyleClass().add("page");

        ScrollPane scroll = new ScrollPane(page);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("page-scroll");
        return scroll;
    }

    /** A small uppercase label above a field, like "OPTIONS". */
    static Label caption(String text) {
        Label label = new Label(text.toUpperCase());
        label.getStyleClass().add("field-label");
        return label;
    }

    /** A small rounded tag. Color is "blue", "green" or "orange". */
    static Label pill(String text, String color) {
        Label label = new Label(text);
        label.getStyleClass().addAll("pill", color);
        return label;
    }

    static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("muted");
        return label;
    }
}
