package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * The Auto-complete page: write a sentence and choose a suggested next word.
 * Suggestions and session numbers are sample data until the backend is connected.
 */
public class AutoCompletePage {

    private record Suggestion(String word, int percent) {
    }

    private final Parent view;

    public AutoCompletePage() {
        VBox right = new VBox(16, sessionCard(),
                notice("New word added", "“bingley's” wasn't in the database, so it was saved and linked after “mr”.", "green"),
                notice("Counting your choices", "Each word you pick adds one to how often it follows the previous word, so future suggestions learn from you.", "blue"));
        right.setPrefWidth(310);
        right.setMinWidth(280);

        view = Widgets.page(header(), new HBox(24, editorCard(), right));
        view.getStyleClass().add("autocomplete-page");
        view.getStylesheets().add(new File("src/ui/css/autocomplete.css").toURI().toString());

    }

    public Parent getView() {
        return view;
    }

    // ---------------- Heading and editor ----------------

    private HBox header() {
        VBox heading = Widgets.header("Auto-complete", "Type normally. Suggestions appear after a space, period, or comma.");
        ((Label) heading.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button clear = new Button("Clear");
        // TODO: clear the editor
        Button save = new Button("Save sentence");
        save.getStyleClass().add("primary");
        // TODO: save the sentence to the database
        HBox actions = new HBox(10, clear, save);
        actions.setMinWidth(Region.USE_PREF_SIZE);
        HBox header = new HBox(20, heading, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox editorCard() {
        TextArea text = new TextArea("It is a truth universally");
        text.setWrapText(true);
        text.setAccessibleText("Your text");
        text.getStyleClass().add("autocomplete-editor");
        text.setPrefHeight(320);

        ListView<Suggestion> suggestions = new ListView<>();
        suggestions.getItems().addAll(new Suggestion("acknowledged", 62),
                new Suggestion("known", 14), new Suggestion("admitted", 9),
                new Suggestion("felt", 6), new Suggestion("beloved", 3));
        suggestions.setAccessibleText("Suggested next words");
        suggestions.setFocusTraversable(false);
        suggestions.setFixedCellSize(42);
        suggestions.setPrefHeight(210);
        suggestions.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Suggestion suggestion, boolean empty) {
                super.updateItem(suggestion, empty);
                setText(null);
                setGraphic(empty || suggestion == null ? null : suggestionRow(suggestion));
            }
        });
        suggestions.getSelectionModel().selectFirst();
        // TODO: load suggestions and accept the selected word
        Label after = new Label("After universally");
        after.getStyleClass().add("suggestion-heading");
        VBox dropdown = new VBox(after, suggestions);
        dropdown.getStyleClass().add("suggestion-dropdown");
        dropdown.setMaxSize(260, Region.USE_PREF_SIZE);

        StackPane editor = new StackPane(text, dropdown);
        StackPane.setAlignment(dropdown, Pos.TOP_RIGHT);
        StackPane.setMargin(dropdown, new Insets(64, 18, 0, 18));
        Label help = Widgets.muted("↑  ↓ move    Tab or Enter accept    Esc keep typing your own word");
        help.setWrapText(true);
        VBox card = new VBox(14, Widgets.caption("Your text"), editor, help);
        card.getStyleClass().add("card");
        card.setMinWidth(350);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox suggestionRow(Suggestion suggestion) {
        Label word = new Label(suggestion.word());
        word.getStyleClass().add("mono");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(10, word, gap, new Label(suggestion.percent() + "%"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------- Session and learning notes ----------------

    private VBox sessionCard() {
        Label title = new Label("This session");
        title.getStyleClass().add("section-title");
        VBox card = new VBox(14, title, statRow("Suggestions accepted", new Label("23")),
                statRow("Typed your own word", new Label("7")), statRow("New words added", new Label("2")));
        card.getStyleClass().add("card");
        return card;
    }

    private HBox statRow(String title, Label value) {
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        value.getStyleClass().add("session-count");
        HBox row = new HBox(10, Widgets.muted(title), gap, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private VBox notice(String title, String description, String color) {
        Label heading = new Label(title);
        heading.getStyleClass().add("notice-title");
        Label body = new Label(description);
        body.setWrapText(true);
        VBox card = new VBox(8, heading, body);
        card.getStyleClass().addAll("autocomplete-notice", color);
        return card;
    }
}
