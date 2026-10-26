package ui;

import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.function.Function;

/** The Word Explorer layout. Counts and followers are sample data until the backend is connected. */
public class WordExplorerPage {

    private record Word(String text, int count, int starts, int ends, int next) {
    }

    private final Parent view;

    public WordExplorerPage() {
        view = Widgets.page(
                Widgets.header("Word Explorer", "Every word in the database. Search, sort, filter, and fix counts."),
                filters(), new HBox(20, wordsCard(), detailsCard()));
        view.getStyleClass().add("word-explorer");
        view.getStylesheets().add(new File("src/ui/css/explorer.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    // ---------------- Search and filters ----------------

    private HBox filters() {
        TextField search = new TextField();
        search.setPromptText("Search words...");
        search.setMinWidth(140);
        HBox.setHgrow(search, Priority.ALWAYS);
        ComboBox<String> sort = new ComboBox<>();
        sort.getItems().addAll("A – Z", "Z – A", "Count (highest first)", "Count (lowest first)");
        sort.setValue("A – Z");
        TextField minimum = new TextField("1");
        minimum.setPrefWidth(65);
        minimum.setMinWidth(65);
        // TODO: search, sort, and filter the words from the database
        HBox card = new HBox(12, search, Widgets.muted("Sort by"), sort,
                Widgets.muted("Min count"), minimum,
                new CheckBox("Starts sentences"), new CheckBox("Ends sentences"));
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        return card;
    }

    // ---------------- Word list ----------------

    private VBox wordsCard() {
        TableView<Word> table = new TableView<>();
        table.getColumns().add(column("Word", 1.5, Word::text));
        table.getColumns().add(column("Count", 1, word -> String.valueOf(word.count())));
        table.getColumns().add(column("Starts", 1, word -> String.valueOf(word.starts())));
        table.getColumns().add(column("Ends", 1, word -> String.valueOf(word.ends())));
        table.getColumns().add(column("Next words", 1.2, word -> String.valueOf(word.next())));
        table.getItems().addAll(
                new Word("daily", 18, 0, 1, 12), new Word("damp", 4, 0, 0, 4),
                new Word("dance", 47, 1, 6, 22), new Word("danger", 31, 0, 3, 17),
                new Word("darcy", 418, 96, 37, 84), new Word("dare", 26, 0, 2, 14),
                new Word("dared", 19, 0, 0, 11), new Word("dark", 12, 0, 2, 9),
                new Word("darling", 3, 0, 1, 2), new Word("dashwood", 72, 9, 5, 30));
        table.getSelectionModel().select(4);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(40);
        table.setPrefHeight(440);
        // TODO: load the selected word and its followers into the detail card

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Button previous = new Button("‹");
        previous.setAccessibleText("Previous page");
        Button next = new Button("›");
        next.setAccessibleText("Next page");
        // TODO: load the previous or next page of words
        HBox footer = new HBox(10, Widgets.muted("Showing 1,201 – 1,210 of 14,283 words"), gap, previous, next);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("card-header");
        VBox card = new VBox(table, footer);
        card.getStyleClass().addAll("card", "flush");
        card.setMinWidth(490);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private TableColumn<Word, String> column(String title, double share, Function<Word, String> value) {
        TableColumn<Word, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(share * 100);
        return column;
    }

    // ---------------- Selected word ----------------

    private VBox detailsCard() {
        Label word = new Label("darcy");
        word.getStyleClass().addAll("mono", "stat-value");
        VBox heading = new VBox(4, Widgets.caption("Selected word"), word,
                Widgets.muted("In 3 of 4 files · first added Oct 11"));
        HBox counts = new HBox(12, countField("Count", "418"), countField("Starts", "96"), countField("Ends", "37"));
        Button all = new Button("See all 84 next words");
        all.getStyleClass().add("link");
        // TODO: display all followers of the selected word
        VBox followers = new VBox(12, Widgets.caption("Most common next words"),
                follower("was", 18), follower("had", 9), follower("and", 7),
                follower("said", 6), follower("s", 5), all);
        Button save = new Button("Save changes");
        save.getStyleClass().add("primary");
        Button revert = new Button("Revert");
        // TODO: save edited counts or reload the original values from the database
        VBox card = new VBox(22, heading, counts, followers, new HBox(10, save, revert));
        card.getStyleClass().add("card");
        card.setPrefWidth(360);
        card.setMinWidth(330);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private VBox countField(String title, String value) {
        TextField input = new TextField(value);
        input.setPrefWidth(85);
        VBox field = new VBox(8, Widgets.caption(title), input);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private HBox follower(String text, int percent) {
        Label word = new Label(text);
        word.getStyleClass().add("mono");
        word.setMinWidth(50);
        ProgressBar bar = new ProgressBar(percent / 20.0);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label value = new Label(percent + "%");
        value.setMinWidth(35);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(12, word, bar, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
