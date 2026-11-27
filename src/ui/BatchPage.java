package ui;

import backend.AppService;
import javafx.concurrent.Task;
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

/*
Batch page connected to the database by Dev Sheth, 11/18/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * The Batch Generate page: make several sentences at once, tick the ones to
 * keep, and add them to the database.
 */
public class BatchPage {

    private final AppService service;
    private final Parent view;
    private final List<AppService.BatchItem> items = new ArrayList<>();
    private final List<CheckBox> rowChecks = new ArrayList<>();
    private final List<CheckBox> newRowChecks = new ArrayList<>(); // rows that aren't already in the database
    private final VBox rows = new VBox();
    private final CheckBox selectAll = new CheckBox("Select all");
    private final Label summary = new Label();
    private final Label status = Widgets.muted("");
    private final Button addToCorpus = new Button();
    private final Button saveToHistory = new Button("Save to history only");
    private final TextField howMany = new TextField("10");
    private final ComboBox<String> algorithm = new ComboBox<>();
    private final TextField maxWords = new TextField("15");
    private boolean updatingSelectAll;
    private String batchAlgorithm = "Weighted bigram";

    /** Keeps the old signature so the existing tests still compile. */
    public BatchPage() {
        this(null);
    }

    public BatchPage(AppService service) {
        this.service = service;
        view = Widgets.page(
                Widgets.header("Batch Generate",
                        "Make several sentences from random starting words, then feed the ones you keep back into the database."),
                settingsCard(),
                resultsCard());
        view.getStylesheets().add(new File("src/ui/css/batch.css").toURI().toString());
        updateSummary();
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Settings ----------------

    private HBox settingsCard() {
        howMany.setPrefWidth(150);
        algorithm.getItems().addAll(AppService.ALGORITHMS);
        algorithm.setValue("Weighted bigram");
        algorithm.setMaxWidth(Double.MAX_VALUE);
        maxWords.setPrefWidth(150);

        Button generate = new Button("Generate batch");
        generate.getStyleClass().add("primary");
        generate.setOnAction(e -> generate());

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

    private static int parse(String text, int fallback, int min, int max) {
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(text.trim())));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    // ---------------- Generating ----------------

    private void generate() {
        if (service == null) {
            status.setText("The database isn't connected.");
            return;
        }
        int count = parse(howMany.getText(), 10, 1, 100);
        int max = parse(maxWords.getText(), 15, 1, 100);
        String alg = algorithm.getValue();
        status.setText("Generating...");

        Task<List<AppService.BatchItem>> task = new Task<>() {
            @Override
            protected List<AppService.BatchItem> call() throws Exception {
                return service.generateBatch(count, alg, max);
            }
        };
        task.setOnSucceeded(e -> {
            batchAlgorithm = alg;
            showItems(task.getValue());
            status.setText(task.getValue().isEmpty() ? "Nothing to generate. Import some text first." : "");
        });
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        start(task);
    }

    private void showItems(List<AppService.BatchItem> generated) {
        items.clear();
        items.addAll(generated);
        rows.getChildren().clear();
        rowChecks.clear();
        newRowChecks.clear();
        generated.forEach(item -> rows.getChildren().add(row(item)));
        updateSummary();
    }

    private void clearResults() {
        showItems(List.of());
    }

    private List<String> selectedTexts() {
        List<String> texts = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            if (rowChecks.get(i).isSelected()) {
                texts.add(items.get(i).text());
            }
        }
        return texts;
    }

    private void save(boolean addWords) {
        List<String> texts = selectedTexts();
        if (texts.isEmpty() || service == null) {
            return;
        }
        String alg = batchAlgorithm;
        status.setText("Saving...");
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                if (addWords) {
                    service.addToCorpus(texts);
                }
                service.logSentences(texts, alg);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            clearResults();
            status.setText(addWords
                    ? "Added " + texts.size() + " sentences to the corpus."
                    : "Saved " + texts.size() + " sentences to history.");
        });
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        start(task);
    }

    private static void start(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------- Results ----------------

    private VBox resultsCard() {
        selectAll.getStyleClass().add("select-all");
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

        VBox card = new VBox(top, rows, footer());
        card.getStyleClass().addAll("card", "flush");
        return card;
    }

    /**
     * One generated sentence with a checkbox. Sentences already in the database
     * start unticked.
     */
    private HBox row(AppService.BatchItem item) {
        CheckBox check = new CheckBox();
        check.setSelected(!item.duplicate());
        check.selectedProperty().addListener((obs, was, isSelected) -> updateSummary());
        rowChecks.add(check);
        if (!item.duplicate()) {
            newRowChecks.add(check);
        }

        Label word = new Label(item.startWord());
        word.getStyleClass().addAll("mono", "word-chip");

        HBox row = new HBox(14, check, word, new Label(item.text()));
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("batch-row");

        if (item.duplicate()) {
            row.getStyleClass().add("duplicate");
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            row.getChildren().addAll(gap, Widgets.pill("Duplicate", "orange"));
        }
        return row;
    }

    private HBox footer() {
        Button discard = new Button("Discard all");
        discard.setOnAction(e -> {
            clearResults();
            status.setText("");
        });
        saveToHistory.setOnAction(e -> save(false));
        addToCorpus.getStyleClass().add("primary");
        addToCorpus.setOnAction(e -> save(true));

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox footer = new HBox(10, status, gap, discard, saveToHistory, addToCorpus);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("batch-footer");
        return footer;
    }

    private void updateSummary() {
        long selected = rowChecks.stream().filter(CheckBox::isSelected).count();
        long total = rowChecks.size();
        long duplicates = total - newRowChecks.size();

        summary.setText(total == 0 ? "No sentences yet"
                : selected + " of " + total + " selected · " + duplicates + " already exists");
        addToCorpus.setText("Add " + selected + (selected == 1 ? " sentence" : " sentences") + " to corpus");
        addToCorpus.setDisable(selected == 0);
        saveToHistory.setDisable(selected == 0);

        updatingSelectAll = true;
        selectAll.setSelected(!newRowChecks.isEmpty() && newRowChecks.stream().allMatch(CheckBox::isSelected));
        updatingSelectAll = false;
    }
}