package ui;

import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * The Batch Generate page: make several sentences at once, tick the ones to keep, and add them to the database.
 * The sentences below are sample data until the backend is connected.
 */
public class BatchPage {

    private record Sentence(String startWord, String text, boolean duplicate) {
    }

    private final Parent view;
    private final List<CheckBox> rowChecks = new ArrayList<>();
    private final List<CheckBox> newRowChecks = new ArrayList<>(); // rows that aren't already in the database
    private final CheckBox selectAll = new CheckBox("Select all");
    private final Label summary = new Label();
    private final Button addToCorpus = new Button();
    private boolean updatingSelectAll; // true while select-all is being changed by the code, not the user

    public BatchPage() {
        view = Widgets.page(
                Widgets.header("Batch Generate", "Make several sentences from random starting words, then feed the ones you keep back into the database."),
                settingsCard(),
                resultsCard());
        view.getStylesheets().add(new File("src/ui/batch.css").toURI().toString());
        updateSummary();
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Settings ----------------

    private HBox settingsCard() {
        TextField howMany = new TextField("10");
        howMany.setPrefWidth(150);

        ComboBox<String> algorithm = new ComboBox<>();
        algorithm.getItems().addAll("Weighted bigram", "Most frequent", "Random follower");
        algorithm.setValue("Weighted bigram");
        algorithm.setMaxWidth(Double.MAX_VALUE);

        TextField maxWords = new TextField("15");
        maxWords.setPrefWidth(150);

        Button generate = new Button("Generate batch");
        generate.getStyleClass().add("primary");
        // TODO: build the sentences from random starting words

        VBox algorithmField = field("Algorithm", algorithm);
        HBox.setHgrow(algorithmField, Priority.ALWAYS);

        HBox card = new HBox(16, field("How many", howMany), algorithmField, field("Max words", maxWords), generate);
        card.setAlignment(Pos.BOTTOM_LEFT);
        card.getStyleClass().add("card");
        return card;
    }

    private VBox field(String caption, Region input) {
        return new VBox(6, Widgets.caption(caption), input);
    }

    // ---------------- Results ----------------

    private VBox resultsCard() {
        List<Sentence> sentences = List.of(
                new Sentence("darcy", "Darcy had been so much as to the subject.", false),
                new Sentence("morning", "Morning was over the rest of her own.", false),
                new Sentence("she", "She could not help it was a very good opinion.", false),
                new Sentence("the", "The same time, and the room, and the room.", true),
                new Sentence("captain", "Captain Wentworth was in the first.", false),
                new Sentence("mr", "Mr. Knightley was very much to be sure.", false),
                new Sentence("letter", "Letter from her sister was not know.", false),
                new Sentence("nothing", "Nothing could be in the world.", false),
                new Sentence("anne", "Anne was to be the evening.", false),
                new Sentence("happiness", "Happiness of the family had been.", false));

        selectAll.getStyleClass().add("select-all");
        selectAll.setSelected(true);
        selectAll.selectedProperty().addListener((obs, was, isSelected) -> {
            if (!updatingSelectAll) {
                newRowChecks.forEach(check -> check.setSelected(isSelected));
            }
        });
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        summary.getStyleClass().add("muted");
        HBox top = new HBox(selectAll, gap, summary);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("batch-top");

        VBox card = new VBox(top);
        sentences.forEach(sentence -> card.getChildren().add(row(sentence)));
        card.getChildren().add(footer());
        card.getStyleClass().addAll("card", "flush");
        return card;
    }

    /** One generated sentence with a checkbox. Sentences already in the database start unticked. */
    private HBox row(Sentence sentence) {
        CheckBox check = new CheckBox();
        check.setSelected(!sentence.duplicate());
        check.selectedProperty().addListener((obs, was, isSelected) -> updateSummary());
        rowChecks.add(check);
        if (!sentence.duplicate()) {
            newRowChecks.add(check);
        }

        Label word = new Label(sentence.startWord());
        word.getStyleClass().addAll("mono", "word-chip");

        HBox row = new HBox(14, check, word, new Label(sentence.text()));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("batch-row");

        if (sentence.duplicate()) {
            row.getStyleClass().add("duplicate");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            row.getChildren().addAll(gap, Widgets.pill("Duplicate", "orange"));
        }
        return row;
    }

    private HBox footer() {
        Button discard = new Button("Discard all");
        // TODO: clear the generated sentences
        Button saveToHistory = new Button("Save to history only");
        // TODO: save the sentences without adding their words to the database
        addToCorpus.getStyleClass().add("primary");
        // TODO: add the ticked sentences to the database

        HBox footer = new HBox(10, discard, saveToHistory, addToCorpus);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.getStyleClass().add("batch-footer");
        return footer;
    }

    /** Refreshes the "N of M selected" text, the Add button, and the Select all box (ticked when every new sentence is ticked). */
    private void updateSummary() {
        long selected = rowChecks.stream().filter(CheckBox::isSelected).count();
        long total = rowChecks.size();
        long duplicates = total - newRowChecks.size();

        summary.setText(selected + " of " + total + " selected · " + duplicates + " already exists");
        addToCorpus.setText("Add " + selected + (selected == 1 ? " sentence" : " sentences") + " to corpus");
        addToCorpus.setDisable(selected == 0);

        updatingSelectAll = true;
        selectAll.setSelected(newRowChecks.stream().allMatch(CheckBox::isSelected));
        updatingSelectAll = false;
    }
}
