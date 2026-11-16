package ui;

import backend.AppService;
import backend.SentenceGenerator;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.io.File;
import java.util.List;

/*
Generate page connected to the database by Dev Sheth, 11/16/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * The Generate page: pick a starting word and an algorithm, then see the
 * sentence and how it was built.
 */
public class GeneratePage {

    private record Result(String text, List<SentenceGenerator.Step> steps, String algorithm) {
    }

    private final AppService service;
    private final Parent view;

    private final TextField startingWord = new TextField("the");
    private final ToggleGroup algorithms = new ToggleGroup();
    private final TextField maxWords = new TextField("15");
    private final CheckBox stopAtEnd = new CheckBox("Stop at a sentence-ending word");

    private final Label sentence = new Label("Press Generate to build a sentence.");
    private final Label algorithmPill = Widgets.pill("—", "blue");
    private final FlowPane chips = new FlowPane(8, 8);
    private final Label status = Widgets.muted("");
    private final VBox sessionRows = new VBox(8);

    private String lastText;
    private String lastAlgorithm;

    public GeneratePage(AppService service) {
        this.service = service;
        VBox right = new VBox(24, resultCard(), sessionCard());
        HBox.setHgrow(right, Priority.ALWAYS);

        view = Widgets.page(
                Widgets.header("Generate Sentence",
                        "Pick a starting word and an algorithm. The program chooses each next word from the database."),
                new HBox(24, optionsCard(), right));
        view.getStylesheets().add(new File("src/ui/css/generate.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Left: options ----------------

    private VBox optionsCard() {
        VBox algorithmChoices = new VBox(8,
                algorithmChoice("Weighted bigram",
                        "Picks the next word by how often it follows. Rare words still show up sometimes.", true),
                algorithmChoice("Most frequent", "Always takes the most common next word (greedy).", false),
                algorithmChoice("Random follower", "Any word that has followed it, equal chance.", false));

        maxWords.setPrefWidth(120);
        maxWords.setMinWidth(120);
        stopAtEnd.setSelected(true);
        stopAtEnd.setWrapText(true);
        HBox maxRow = new HBox(16, maxWords, stopAtEnd);
        maxRow.setAlignment(Pos.CENTER_LEFT);

        Button generate = new Button("Generate");
        generate.getStyleClass().add("primary");
        generate.setMaxWidth(Double.MAX_VALUE);
        generate.setOnAction(e -> generate());

        VBox card = new VBox(8,
                Widgets.caption("Starting word"), startingWord, Widgets.muted("Must already be in the database."),
                spaced(Widgets.caption("Algorithm")), algorithmChoices,
                spaced(Widgets.caption("Max words")), maxRow,
                spaced(generate), status);
        card.getStyleClass().add("card");
        card.setPrefWidth(400);
        card.setMinWidth(400);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private <T extends Region> T spaced(T node) {
        VBox.setMargin(node, new Insets(10, 0, 0, 0));
        return node;
    }

    private VBox algorithmChoice(String name, String description, boolean selected) {
        RadioButton radio = new RadioButton(name);
        radio.setToggleGroup(algorithms);
        radio.setUserData(name);
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

    // ---------------- Generating ----------------

    private void generate() {
        String seed = startingWord.getText().trim();
        if (seed.isEmpty()) {
            status.setText("Type a starting word first.");
            return;
        }
        int max = 15;
        try {
            max = Math.max(1, Math.min(100, Integer.parseInt(maxWords.getText().trim())));
        } catch (NumberFormatException ignored) {
            maxWords.setText("15");
        }
        String algorithm = (String) algorithms.getSelectedToggle().getUserData();
        boolean stop = stopAtEnd.isSelected();
        int maxLen = max;

        status.setText("Generating...");
        Task<Result> task = new Task<>() {
            @Override
            protected Result call() throws Exception {
                List<SentenceGenerator.Step> steps = service.generate(algorithm, seed, maxLen, stop);
                String text = AppService.formatSentence(steps);
                service.logSentences(List.of(text), algorithm);
                return new Result(text, steps, algorithm);
            }
        };
        task.setOnSucceeded(e -> showResult(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            if (ex instanceof IllegalArgumentException) {
                status.setText("“" + seed + "” isn't in the database.");
            } else {
                status.setText("Database error: " + ex.getMessage());
            }
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showResult(Result result) {
        lastText = result.text();
        lastAlgorithm = result.algorithm();
        status.setText("");
        sentence.setText(result.text());
        algorithmPill.setText(result.algorithm() + " · " + result.steps().size() + " words");

        chips.getChildren().clear();
        for (int i = 0; i < result.steps().size(); i++) {
            SentenceGenerator.Step step = result.steps().get(i);
            String chance = i == 0 ? "start" : Math.round(step.chance() * 100) + "%";
            boolean unlikely = i > 0 && step.chance() < 0.05;
            chips.getChildren().add(chip(step.word().toLowerCase(), chance, unlikely));
        }

        sessionRows.getChildren().add(0, sessionRow(result.text(), result.algorithm()));
    }

    // ---------------- Right: result and session ----------------

    private VBox resultCard() {
        Label heading = Widgets.caption("Result");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox top = new HBox(heading, gap, algorithmPill);
        top.setAlignment(Pos.CENTER_LEFT);

        sentence.getStyleClass().add("result-sentence");
        sentence.setWrapText(true);

        Button again = new Button("Generate again");
        again.getStyleClass().add("primary");
        again.setOnAction(e -> generate());

        Button copy = new Button("Copy");
        copy.setOnAction(e -> {
            if (lastText != null) {
                ClipboardContent content = new ClipboardContent();
                content.putString(lastText);
                Clipboard.getSystemClipboard().setContent(content);
                status.setText("Copied.");
            }
        });

        Button addToCorpus = new Button("Add to corpus");
        addToCorpus.setOnAction(e -> addLastToCorpus());

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

    private void addLastToCorpus() {
        if (lastText == null) {
            return;
        }
        String text = lastText;
        status.setText("Adding to corpus...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.addToCorpus(List.of(text));
                return null;
            }
        };
        task.setOnSucceeded(e -> status.setText("Added to the corpus."));
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

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

        VBox card = new VBox(8, title, sessionRows);
        card.getStyleClass().add("card");
        return card;
    }

    private HBox sessionRow(String text, String algorithm) {
        Label label = new Label(text);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        HBox row = new HBox(label, gap, Widgets.pill(algorithm, "blue"));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("session-row");
        return row;
    }
}