package ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;

/**
 * The Generate page: pick a starting word and an algorithm, then see the sentence and how it was built.
 * The sentence and numbers below are sample data until the backend is connected.
 */
public class GeneratePage {

    private final Parent view;

    public GeneratePage() {
        VBox right = new VBox(24, resultCard(), sessionCard());
        HBox.setHgrow(right, Priority.ALWAYS);

        view = Widgets.page(
                Widgets.header("Generate Sentence", "Pick a starting word and an algorithm. The program chooses each next word from the database."),
                new HBox(24, optionsCard(), right));
        view.getStylesheets().add(new File("src/ui/generate.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Left: options ----------------

    private VBox optionsCard() {
        TextField startingWord = new TextField("elizabeth");

        ToggleGroup algorithms = new ToggleGroup();
        VBox algorithmChoices = new VBox(8,
                algorithmChoice(algorithms, "Weighted bigram", "Picks the next word by how often it follows. Rare words still show up sometimes.", true),
                algorithmChoice(algorithms, "Most frequent", "Always takes the most common next word (greedy).", false),
                algorithmChoice(algorithms, "Random follower", "Any word that has followed it, equal chance.", false));

        TextField maxWords = new TextField("15");
        maxWords.setPrefWidth(120);
        maxWords.setMinWidth(120);
        CheckBox stopAtEnd = new CheckBox("Stop at a sentence-ending word");
        stopAtEnd.setSelected(true);
        stopAtEnd.setWrapText(true);
        HBox maxRow = new HBox(16, maxWords, stopAtEnd);
        maxRow.setAlignment(Pos.CENTER_LEFT);

        Button generate = new Button("Generate");
        generate.getStyleClass().add("primary");
        generate.setMaxWidth(Double.MAX_VALUE);
        // TODO: build a sentence from the database

        VBox card = new VBox(8,
                Widgets.caption("Starting word"), startingWord, Widgets.muted("Must already be in the database."),
                spaced(Widgets.caption("Algorithm")), algorithmChoices,
                spaced(Widgets.caption("Max words")), maxRow,
                spaced(generate));
        card.getStyleClass().add("card");
        card.setPrefWidth(400);
        card.setMinWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    /** Adds extra room above a node inside a VBox. */
    private <T extends Region> T spaced(T node) {
        VBox.setMargin(node, new Insets(10, 0, 0, 0));
        return node;
    }

    /** One selectable box: a radio button with its title and a short description. The whole box is clickable. */
    private VBox algorithmChoice(ToggleGroup group, String name, String description, boolean selected) {
        RadioButton radio = new RadioButton(name);
        radio.setToggleGroup(group);
        radio.getStyleClass().add("choice-title");

        Label text = new Label(description);
        text.getStyleClass().add("choice-description");
        text.setWrapText(true);

        VBox choice = new VBox(2, radio, text);
        choice.getStyleClass().add("choice");
        choice.setOnMouseClicked(e -> radio.setSelected(true));
        radio.selectedProperty().addListener((obs, was, isSelected) -> {
            choice.getStyleClass().remove("selected");
            if (isSelected) {
                choice.getStyleClass().add("selected");
            }
        });
        radio.setSelected(selected);
        return choice;
    }

    // ---------------- Right: result and session ----------------

    private VBox resultCard() {
        Label heading = Widgets.caption("Result");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(heading, gap, Widgets.pill("Weighted bigram · 12 words", "blue"));
        top.setAlignment(Pos.CENTER_LEFT);

        Label sentence = new Label("Elizabeth was not been a very well as she had been in the room.");
        sentence.getStyleClass().add("result-sentence");
        sentence.setWrapText(true);

        FlowPane chips = new FlowPane(8, 8,
                chip("elizabeth", "start", false),
                chip("was", "18%", false),
                chip("not", "11%", false),
                chip("been", "2%", true),
                chip("a", "9%", false),
                chip("very", "6%", false),
                chip("well", "7%", false),
                chip("as", "14%", false),
                chip("…", "", false));

        Button again = new Button("Generate again");
        again.getStyleClass().add("primary");
        // TODO: build another sentence with the same settings
        Button copy = new Button("Copy");
        // TODO: copy the sentence to the clipboard
        Button addToCorpus = new Button("Add to corpus");
        // TODO: add the sentence to the database

        VBox card = new VBox(14,
                top,
                sentence,
                Widgets.caption("How it was built — chance of each next word"),
                chips,
                Widgets.muted("Highlighted = a low-probability pick."),
                new HBox(10, again, copy, addToCorpus));
        card.getStyleClass().add("card");
        return card;
    }

    /** One word in the "how it was built" row, with its chance underneath. */
    private VBox chip(String word, String chance, boolean unlikely) {
        Label wordLabel = new Label(word);
        wordLabel.getStyleClass().addAll("mono", "chip-word");
        Label chanceLabel = new Label(chance);
        chanceLabel.getStyleClass().addAll("mono", "chip-chance");

        VBox chip = new VBox(wordLabel, chanceLabel);
        chip.setAlignment(Pos.CENTER);
        chip.getStyleClass().add("chip");
        if (unlikely) {
            chip.getStyleClass().add("unlikely");
        }
        return chip;
    }

    private VBox sessionCard() {
        Label title = new Label("This session");
        title.getStyleClass().add("section-title");

        VBox card = new VBox(8, title,
                sessionRow("Elizabeth had been the subject of the house.", "Most frequent"),
                sessionRow("Elizabeth felt herself so very much to be.", "Random follower"));
        card.getStyleClass().add("card");
        return card;
    }

    private HBox sessionRow(String sentence, String algorithm) {
        Label text = new Label(sentence);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        HBox row = new HBox(text, gap, Widgets.pill(algorithm, "blue"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("session-row");
        return row;
    }
}
