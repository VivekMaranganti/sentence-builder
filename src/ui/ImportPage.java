package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.SVGPath;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/**
 * The Import page: pick text files, choose how to read them, and see the import progress and history.
 * The numbers below are sample data until the backend is connected.
 */
public class ImportPage {

    private static final String FILE_ICON = "M14 3H7a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V8zM14 3v5h5M12 17v-6M9 13l3-3 3 3";

    private record ImportRow(String file, String words, String uniqueWords, String imported, String timeTaken, String status) {
    }

    private final Parent view;

    public ImportPage() {
        view = Widgets.page(
                Widgets.header("Import Text Files", "Read a plain .txt file, split it into words, and add the counts to the database."),
                new HBox(24, pickFilesCard(), importingNowCard()),
                historyCard());
        view.getStylesheets().add(new File("src/ui/css/import.css").toURI().toString());
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

        Button choose = new Button("Choose files...");
        choose.getStyleClass().add("primary");
        // TODO: open a file chooser and start the import

        VBox zone = new VBox(10, icon, title, Widgets.muted("or"), choose,
                Widgets.muted("Plain text only. PDF and EPUB are not supported."));
        zone.setAlignment(Pos.CENTER);
        zone.getStyleClass().add("drop-zone");
        return zone;
    }

    private VBox options() {
        CheckBox stripHeader = new CheckBox("Strip Project Gutenberg header & footer");
        stripHeader.setSelected(true);
        CheckBox lowerCase = new CheckBox("Convert words to lower case");
        lowerCase.setSelected(true);
        CheckBox contractions = new CheckBox("Keep contractions as one word (e.g. \"don't\")");

        return new VBox(14, Widgets.caption("Options"), stripHeader, lowerCase, contractions);
    }

    private VBox importingNowCard() {
        Label file = new Label("mansfield_park.txt");
        file.getStyleClass().addAll("mono", "section-title");

        ProgressBar progress = new ProgressBar(0.62);
        progress.setMaxWidth(Double.MAX_VALUE);

        Label percent = new Label("62%");
        percent.getStyleClass().add("percent");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox progressText = new HBox(Widgets.muted("99,410 of 160,342 words"), gap, percent);
        progressText.setAlignment(Pos.CENTER_LEFT);

        HBox found = new HBox(32, countBlock("New words found", "1,206"), countBlock("Sentences parsed", "4,873"));

        // Pushes the cancel button to the bottom of the card
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button cancel = new Button("Cancel import");
        // TODO: stop the running import

        VBox card = new VBox(12, Widgets.caption("Importing now"), file, progress, progressText, found, spacer, cancel);
        card.getStyleClass().add("card");
        card.setPrefWidth(380);
        card.setMinWidth(340);
        return card;
    }

    private VBox countBlock(String name, String value) {
        Label number = new Label(value);
        number.getStyleClass().add("count");
        return new VBox(2, Widgets.muted(name), number);
    }

    // ---------------- History ----------------

    private VBox historyCard() {
        List<ImportRow> rows = List.of(
                new ImportRow("pride_and_prejudice.txt", "121,533", "6,712", "Oct 12, 4:18 PM", "14.2 s", "Done"),
                new ImportRow("emma.txt", "158,921", "7,340", "Oct 12, 4:02 PM", "18.9 s", "Done"),
                new ImportRow("persuasion.txt", "83,309", "5,488", "Oct 11, 9:47 PM", "9.6 s", "Done"),
                new ImportRow("batch_2026-10-11.txt", "142", "0", "Oct 11, 9:30 PM", "0.1 s", "From batch"));

        TableView<ImportRow> table = new TableView<>();
        table.getColumns().add(column("File", 3, ImportRow::file, "mono"));
        table.getColumns().add(column("Words", 1, ImportRow::words));
        table.getColumns().add(column("Unique words", 1, ImportRow::uniqueWords));
        table.getColumns().add(column("Imported", 1.6, ImportRow::imported));
        table.getColumns().add(column("Time taken", 1, ImportRow::timeTaken));
        table.getColumns().add(statusColumn());
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPlaceholder(Widgets.muted("No files imported yet."));
        table.getItems().addAll(rows);

        // Show every row without an inner scroll bar: one header + the rows
        table.setFixedCellSize(44);
        table.setPrefHeight(38 + 44 * rows.size() + 2);

        Label title = new Label("Import history");
        title.getStyleClass().add("section-title");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(title, spacer, Widgets.muted(rows.size() + " files"));
        top.setAlignment(Pos.CENTER_LEFT);
        top.getStyleClass().add("card-header");

        VBox card = new VBox(top, table);
        card.getStyleClass().addAll("card", "flush");
        return card;
    }

    /** A text column. The width is a share of the table, so 2 is twice as wide as 1. */
    private TableColumn<ImportRow, String> column(String title, double share, Function<ImportRow, String> value, String... cellStyles) {
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
