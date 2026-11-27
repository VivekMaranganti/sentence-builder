package ui;

import backend.AppService;
import javafx.beans.binding.Bindings;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/*
Auto-complete page connected to the database by Dev Sheth, 11/18/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * The Auto-complete page: write a sentence and choose a suggested next word.
 */
public class AutoCompletePage {

    private final AppService service;
    private final Parent view;
    // One worker thread, so database writes happen in the order the words were
    // typed
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    private final TextArea text = new TextArea();
    private final ListView<AppService.Suggestion> suggestions = new ListView<>();
    private final Label after = new Label("");
    private final VBox dropdown = new VBox();
    private final Label acceptedLabel = new Label("0");
    private final Label typedLabel = new Label("0");
    private final Label newLabel = new Label("0");
    private final Label newWordNote = new Label("Words you type that aren't in the database are saved automatically.");
    private final Label status = Widgets.muted("");
    private int accepted;
    private int typedOwn;
    private int newWords;
    private boolean suppress; // true while the code (not the user) changes the text

    /** Keeps the old signature so the existing tests still compile. */
    public AutoCompletePage() {
        this(null);
    }

    public AutoCompletePage(AppService service) {
        this.service = service;
        VBox right = new VBox(16, sessionCard(),
                notice("New word added", newWordNote, "green"),
                notice("Counting your choices", new Label(
                        "Each word you finish adds one to how often it follows the previous word, so future suggestions learn from you."),
                        "blue"));
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
        VBox heading = Widgets.header("Auto-complete",
                "Type normally. Suggestions appear after a space, period, or comma.");
        ((Label) heading.getChildren().get(1)).setWrapText(true);
        HBox.setHgrow(heading, Priority.ALWAYS);
        Button clear = new Button("Clear");
        clear.setOnAction(e -> {
            text.clear();
            status.setText("");
        });
        Button save = new Button("Save sentence");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> saveSentence());
        HBox actions = new HBox(10, clear, save);
        actions.setMinWidth(Region.USE_PREF_SIZE);
        HBox header = new HBox(20, heading, actions);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private VBox editorCard() {
        text.setWrapText(true);
        text.setPromptText("Start typing a sentence...");
        text.setAccessibleText("Your text");
        text.getStyleClass().add("autocomplete-editor");
        text.setPrefHeight(320);
        text.textProperty().addListener((obs, old, now) -> onTextChanged(old, now));
        text.addEventFilter(KeyEvent.KEY_PRESSED, this::onKey);

        suggestions.setAccessibleText("Suggested next words");
        suggestions.setFocusTraversable(false);
        suggestions.setFixedCellSize(42);
        suggestions.setPrefHeight(210);
        suggestions.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(AppService.Suggestion suggestion, boolean empty) {
                super.updateItem(suggestion, empty);
                setText(null);
                setGraphic(empty || suggestion == null ? null : suggestionRow(suggestion));
            }
        });
        suggestions.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                acceptSelected();
            }
        });

        after.getStyleClass().add("suggestion-heading");
        dropdown.getChildren().addAll(after, suggestions);
        dropdown.getStyleClass().add("suggestion-dropdown");
        dropdown.setMaxSize(260, Region.USE_PREF_SIZE);
        dropdown.visibleProperty().bind(Bindings.isNotEmpty(suggestions.getItems()));

        StackPane editor = new StackPane(text, dropdown);
        StackPane.setAlignment(dropdown, Pos.TOP_RIGHT);
        StackPane.setMargin(dropdown, new Insets(64, 18, 0, 18));
        Label help = Widgets.muted("↑  ↓ move    Tab or Enter accept    Esc keep typing your own word");
        help.setWrapText(true);
        VBox card = new VBox(14, Widgets.caption("Your text"), editor, help, status);
        card.getStyleClass().add("card");
        card.setMinWidth(350);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private HBox suggestionRow(AppService.Suggestion suggestion) {
        Label word = new Label(suggestion.word());
        word.getStyleClass().add("mono");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(10, word, gap, new Label(suggestion.percent() + "%"));
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    // ---------------- Typing and choosing ----------------

    private void onKey(KeyEvent e) {
        if (!dropdown.isVisible()) {
            return;
        }
        switch (e.getCode()) {
            case DOWN -> {
                move(1);
                e.consume();
            }
            case UP -> {
                move(-1);
                e.consume();
            }
            case TAB, ENTER -> {
                acceptSelected();
                e.consume();
            }
            case ESCAPE -> {
                suggestions.getItems().clear();
                e.consume();
            }
            default -> {
            }
        }
    }

    private void move(int delta) {
        int size = suggestions.getItems().size();
        if (size == 0) {
            return;
        }
        int index = Math.max(0, Math.min(size - 1, suggestions.getSelectionModel().getSelectedIndex() + delta));
        suggestions.getSelectionModel().select(index);
        suggestions.scrollTo(index);
    }

    /**
     * Puts the chosen suggestion into the text (replacing a half-typed word) and
     * records the choice.
     */
    private void acceptSelected() {
        AppService.Suggestion pick = suggestions.getSelectionModel().getSelectedItem();
        if (pick == null) {
            return;
        }
        String current = text.getText();
        int i = current.length();
        while (i > 0 && Character.isLetterOrDigit(current.charAt(i - 1))) {
            i--;
        }
        String filled = current.substring(0, i) + pick.word();
        suppress = true;
        text.setText(filled + " ");
        text.positionCaret(text.getLength());
        suppress = false;
        accepted++;
        acceptedLabel.setText(String.valueOf(accepted));
        completeWord(filled, false, true);
    }

    private void onTextChanged(String old, String now) {
        if (suppress) {
            return;
        }
        if (now.isEmpty()) {
            suggestions.getItems().clear();
            return;
        }
        // Only react when exactly one character was typed at the end
        if (now.length() != old.length() + 1 || !now.startsWith(old)) {
            return;
        }
        char typed = now.charAt(now.length() - 1);
        if (typed == ' ' || typed == '.' || typed == ',') {
            completeWord(now.substring(0, now.length() - 1), typed == '.', false);
        }
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '\'';
    }

    /**
     * Returns {word, previousWord} for the last word in the text, or null if it
     * doesn't end in a word.
     */
    private static String[] wordAndPrevious(String before) {
        int end = before.length();
        if (end == 0 || !isWordChar(before.charAt(end - 1))) {
            return null;
        }
        int start = end;
        while (start > 0 && isWordChar(before.charAt(start - 1))) {
            start--;
        }
        String word = before.substring(start, end).replaceAll("^'+|'+$", "").toLowerCase();
        if (word.isEmpty()) {
            return null;
        }

        // The previous word counts only if it is in the same sentence
        String head = before.substring(0, start);
        int cut = Math.max(head.lastIndexOf('.'), Math.max(head.lastIndexOf('!'), head.lastIndexOf('?')));
        String sentence = head.substring(cut + 1);
        int e = sentence.length();
        while (e > 0 && !isWordChar(sentence.charAt(e - 1))) {
            e--;
        }
        String previous = null;
        if (e > 0) {
            int s = e;
            while (s > 0 && isWordChar(sentence.charAt(s - 1))) {
                s--;
            }
            String p = sentence.substring(s, e).replaceAll("^'+|'+$", "").toLowerCase();
            previous = p.isEmpty() ? null : p;
        }
        return new String[] { word, previous };
    }

    /**
     * Saves a finished word to MySQL, then loads suggestions for what could come
     * next.
     */
    private void completeWord(String before, boolean sentenceEnd, boolean fromSuggestion) {
        String[] pair = wordAndPrevious(before);
        if (pair == null || service == null) {
            return;
        }
        String word = pair[0];
        String previous = pair[1];
        if (!fromSuggestion) {
            typedOwn++;
            typedLabel.setText(String.valueOf(typedOwn));
        }
        boolean[] added = new boolean[1];
        Task<List<AppService.Suggestion>> task = new Task<>() {
            @Override
            protected List<AppService.Suggestion> call() throws Exception {
                added[0] = service.recordChoice(previous, word);
                return sentenceEnd ? List.of() : service.suggestNext(word);
            }
        };
        task.setOnSucceeded(e -> {
            status.setText("");
            suggestions.getItems().setAll(task.getValue());
            suggestions.getSelectionModel().selectFirst();
            after.setText("After " + word);
            if (added[0]) {
                newWords++;
                newLabel.setText(String.valueOf(newWords));
                newWordNote.setText("“" + word + "” wasn't in the database, so it was saved"
                        + (previous != null ? " and linked after “" + previous + "”." : "."));
            }
        });
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        worker.execute(task);
    }

    private void saveSentence() {
        String sentence = text.getText().trim();
        if (sentence.isEmpty() || service == null) {
            return;
        }
        // Count the last word too if the user didn't finish it with a space, period, or
        // comma
        completeWord(sentence, false, false);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.logSentences(List.of(sentence), "Auto-complete");
                return null;
            }
        };
        task.setOnSucceeded(e -> status.setText("Sentence saved."));
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        worker.execute(task);
    }

    // ---------------- Session and learning notes ----------------

    private VBox sessionCard() {
        Label title = new Label("This session");
        title.getStyleClass().add("section-title");
        VBox card = new VBox(14, title, statRow("Suggestions accepted", acceptedLabel),
                statRow("Typed your own word", typedLabel), statRow("New words added", newLabel));
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

    private VBox notice(String title, Label body, String color) {
        Label heading = new Label(title);
        heading.getStyleClass().add("notice-title");
        body.setWrapText(true);
        VBox card = new VBox(8, heading, body);
        card.getStyleClass().addAll("autocomplete-notice", color);
        return card;
    }
}