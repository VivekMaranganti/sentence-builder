package ui;

import backend.AppService;
import backend.FileImporter;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/*
Import page connected to the importer by Dev Sheth, 11/17/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * The Import page: pick text files, choose how to read them, and see the import
 * progress and history.
 */
public class ImportPage {

    private static final String FILE_ICON = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5M12 17v-6M9 13l3-3 3 3";

    private record ImportRow(String file, String words, String uniqueWords, String imported, String timeTaken,
            String status) {
    }

    private final AppService service;
    private final Parent view;
    private final AtomicBoolean cancel = new AtomicBoolean(false);
    private final Map<String, FileImporter.Result> results = new HashMap<>(); // this session's imports, by file name
    private boolean running;

    private final CheckBox stripHeader = new CheckBox("Strip Project Gutenberg header & footer");
    private final CheckBox lowerCase = new CheckBox("Convert words to lower case");
    private final CheckBox contractions = new CheckBox("Keep contractions as one word (e.g. \"don't\")");
    private final Button choose = new Button("Choose files...");
    private final Button cancelButton = new Button("Cancel import");
    private final Label currentFile = new Label("No import running");
    private final ProgressBar progress = new ProgressBar(0);
    private final Label percent = new Label("0%");
    private final Label progressText = Widgets.muted("Choose a .txt file to begin.");
    private final Label wordsFound = new Label("0");
    private final Label uniqueFound = new Label("0");
    private final Label sentencesFound = new Label("0");
    private final Label status = Widgets.muted("");
    private final TableView<ImportRow> table = new TableView<>();
    private final Label fileCount = Widgets.muted("0 files");

    /** Keeps the old signature so the existing tests still compile. */
    public ImportPage() {
        this(null);
    }

    public ImportPage(AppService service) {
        this.service = service;
        view = Widgets.page(
                Widgets.header("Import Text Files",
                        "Read a plain .txt file, split it into words, and add the counts to the database."),
                new HBox(24, pickFilesCard(), importingNowCard()),
                historyCard());
        view.getStylesheets().add(new File("src/ui/css/import.css").toURI().toString());
        loadHistory();
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Top row ----------------

    private VBox pickFilesCard() {
        VBox card = new VBox(20, dropZone(), options());
        card.getStyleClass().add("card");
        card.setPrefWidth(520);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox dropZone() {
        SVGPath icon = new SVGPath();
        icon.setContent(FILE_ICON);
        icon.getStyleClass().add("drop-icon");

        Label title = new Label("Drag .txt files here");
        title.getStyleClass().add("section-title");

        choose.getStyleClass().add("primary");
        choose.setOnAction(e -> chooseFiles());

        VBox zone = new VBox(10, icon, title, Widgets.muted("or"), choose,
                Widgets.muted("Plain text only. PDF and EPUB are not supported."));
        zone.setAlignment(Pos.CENTER);
        zone.getStyleClass().add("drop-zone");

        zone.setOnDragOver(e -> {
            if (e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
            }
            e.consume();
        });
        zone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles().stream()
                    .filter(f -> f.getName().toLowerCase().endsWith(".txt")).toList();
            e.setDropCompleted(!files.isEmpty());
            e.consume();
            if (!files.isEmpty()) {
                startImport(files);
            }
        });
        return zone;
    }

    private VBox options() {
        stripHeader.setSelected(true);
        lowerCase.setSelected(true);
        contractions.setSelected(true);
        return new VBox(14, Widgets.caption("Options"), stripHeader, lowerCase, contractions);
    }

    private VBox importingNowCard() {
        currentFile.getStyleClass().addAll("mono", "section-title");
        progress.setMaxWidth(Double.MAX_VALUE);
        percent.getStyleClass().add("percent");

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox progressRow = new HBox(progressText, gap, percent);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        HBox found = new HBox(24, countBlock("Words read", wordsFound),
                countBlock("Unique words", uniqueFound), countBlock("Sentences", sentencesFound));

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        cancelButton.setDisable(true);
        cancelButton.setOnAction(e -> {
            cancel.set(true);
            status.setText("Cancelling...");
        });

        VBox card = new VBox(12, Widgets.caption("Importing now"), currentFile, progress, progressRow,
                found, spacer, status, cancelButton);
        card.getStyleClass().add("card");
        card.setPrefWidth(380);
        card.setMinWidth(340);
        return card;
    }

    private VBox countBlock(String name, Label number) {
        number.getStyleClass().add("count");
        return new VBox(2, Widgets.muted(name), number);
    }

    // ---------------- Importing ----------------

    private void chooseFiles() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose text files to import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        File data = new File("data");
        if (data.isDirectory()) {
            chooser.setInitialDirectory(data);
        }
        List<File> files = chooser.showOpenMultipleDialog(view.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            startImport(files);
        }
    }

    private void startImport(List<File> files) {
        if (running) {
            return;
        }
        if (service == null) {
            status.setText("The database isn't connected.");
            return;
        }
        FileImporter.Options options = new FileImporter.Options(
                stripHeader.isSelected(), lowerCase.isSelected(), contractions.isSelected());
        cancel.set(false);
        setRunning(true);
        status.setText("");

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (int i = 0; i < files.size(); i++) {
                    File file = files.get(i);
                    String label = files.size() > 1 ? file.getName() + " (" + (i + 1) + " of " + files.size() + ")"
                            : file.getName();
                    Platform.runLater(() -> currentFile.setText(label));
                    FileImporter.Result result = service.importFile(file, options,
                            p -> Platform.runLater(() -> showProgress(p)), cancel);
                    Platform.runLater(() -> results.put(file.getName(), result));
                }
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            setRunning(false);
            status.setText("Import complete.");
            loadHistory();
        });
        task.setOnFailed(e -> {
            setRunning(false);
            Throwable ex = task.getException();
            if (ex instanceof FileImporter.CancelledException) {
                status.setText("Import cancelled. Nothing was added.");
            } else {
                status.setText("Import failed: " + ex.getMessage());
            }
            loadHistory();
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showProgress(FileImporter.Progress p) {
        progress.setProgress(p.fraction());
        percent.setText(Math.round(p.fraction() * 100) + "%");
        progressText.setText(p.phase());
        wordsFound.setText(String.format("%,d", p.words()));
        uniqueFound.setText(String.format("%,d", p.uniqueWords()));
        sentencesFound.setText(String.format("%,d", p.sentences()));
    }

    private void setRunning(boolean value) {
        running = value;
        choose.setDisable(value);
        cancelButton.setDisable(!value);
    }

    // ---------------- History ----------------

    private VBox historyCard() {
        table.getColumns().add(column("File", 3, ImportRow::file, "mono"));
        table.getColumns().add(column("Words", 1, ImportRow::words));
        table.getColumns().add(column("Unique words", 1, ImportRow::uniqueWords));
        table.getColumns().add(column("Imported", 1.6, ImportRow::imported));
        table.getColumns().add(column("Time taken", 1, ImportRow::timeTaken));
        table.getColumns().add(statusColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(Widgets.muted("No files imported yet."));
        table.setFixedCellSize(44);
        table.setPrefHeight(38 + 44 * 2 + 2);

        Label title = new Label("Import history");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(title, spacer, fileCount);
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("card-header");

        VBox card = new VBox(top, table);
        card.getStyleClass().addAll("card", "flush");
        return card;
    }

    private void loadHistory() {
        if (service == null) {
            return;
        }
        Task<List<AppService.ImportRecord>> task = new Task<>() {
            @Override
            protected List<AppService.ImportRecord> call() throws Exception {
                return service.getImports(50);
            }
        };
        task.setOnSucceeded(e -> showHistory(task.getValue()));
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showHistory(List<AppService.ImportRecord> records) {
        List<ImportRow> rows = new ArrayList<>();
        Set<String> used = new HashSet<>();
        for (AppService.ImportRecord r : records) {
            FileImporter.Result res = results.get(r.file());
            // Unique words and time are only known for imports made in this session (newest
            // row per file)
            boolean known = res != null && used.add(r.file());
            rows.add(new ImportRow(r.file(),
                    String.format("%,d", r.words()),
                    known ? String.format("%,d", res.uniqueWords()) : "—",
                    r.imported(),
                    known ? String.format("%.1f s", res.seconds()) : "—",
                    r.file().startsWith("batch_") ? "From batch" : "Done"));
        }
        table.getItems().setAll(rows);
        fileCount.setText(rows.size() + (rows.size() == 1 ? " file" : " files"));
        table.setPrefHeight(38 + 44 * Math.min(Math.max(rows.size(), 2), 10) + 2);
    }

    /**
     * A text column. The width is a share of the table, so 2 is twice as wide as 1.
     */
    private TableColumn<ImportRow, String> column(String title, double share, Function<ImportRow, String> value,
            String... cellStyles) {
        TableColumn<ImportRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(share * 100);
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);
                setText(empty ? null : text);
                getStyleClass().removeAll(cellStyles);
                if (!empty) {
                    getStyleClass().addAll(cellStyles);
                }
            }
        });
        return column;
    }

    /** The status column shows a colored tag instead of plain text. */
    private TableColumn<ImportRow, String> statusColumn() {
        TableColumn<ImportRow, String> column = new TableColumn<>("Status");
        column.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().status()));
        column.setPrefWidth(100);
        column.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(Widgets.pill(status, status.equals("Done") ? "green" : "blue"));
                }
            }
        });
        return column;
    }
}