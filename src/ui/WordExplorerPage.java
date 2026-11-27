package ui;

import backend.AppService;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.util.List;
import java.util.function.Function;

/*
Word Explorer connected to the database by Dev Sheth, 11/18/2026, with AI assistance (Claude).
Layout and styling by the UI team.
*/
/**
 * The Word Explorer: every word in the database. Search, sort, filter, and fix
 * counts.
 */
public class WordExplorerPage {

    private static final int PAGE_SIZE = 10;

    private final AppService service;
    private final Parent view;

    private final TextField search = new TextField();
    private final ComboBox<String> sort = new ComboBox<>();
    private final TextField minimum = new TextField("1");
    private final CheckBox startsOnly = new CheckBox("Starts sentences");
    private final CheckBox endsOnly = new CheckBox("Ends sentences");

    private final TableView<AppService.WordRow> table = new TableView<>();
    private final Label showing = Widgets.muted("");
    private final Button previous = new Button("‹");
    private final Button next = new Button("›");

    private final Label wordLabel = new Label("—");
    private final Label nextInfo = Widgets.muted("Select a word from the list.");
    private final TextField countField = new TextField();
    private final TextField startsField = new TextField();
    private final TextField endsField = new TextField();
    private final VBox followersBox = new VBox(12);
    private final Button seeAll = new Button();
    private final Label status = Widgets.muted("");

    private AppService.WordRow current;
    private int offset;
    private int total;
    private int requestSeq;
    private int followerSeq;
    private boolean showAllFollowers;

    /** Keeps the old signature so the existing tests still compile. */
    public WordExplorerPage() {
        this(null);
    }

    public WordExplorerPage(AppService service) {
        this.service = service;
        view = Widgets.page(
                Widgets.header("Word Explorer", "Every word in the database. Search, sort, filter, and fix counts."),
                filters(), new HBox(20, wordsCard(), detailsCard()));
        view.getStyleClass().add("word-explorer");
        view.getStylesheets().add(new File("src/ui/css/explorer.css").toURI().toString());
    }

    public Parent getView() {
        return view;
    }

    /** Reloads the list from MySQL. App calls this each time the page is shown. */
    public void refresh() {
        load();
    }

    // ---------------- Search and filters ----------------

    private HBox filters() {
        search.setPromptText("Search words...");
        search.setMinWidth(140);
        HBox.setHgrow(search, Priority.ALWAYS);
        sort.getItems().addAll("A – Z", "Z – A", "Count (highest first)", "Count (lowest first)");
        sort.setValue("A – Z");
        minimum.setPrefWidth(65);
        minimum.setMinWidth(65);

        search.textProperty().addListener((obs, was, now) -> reloadFromStart());
        sort.valueProperty().addListener((obs, was, now) -> reloadFromStart());
        minimum.setOnAction(e -> reloadFromStart());
        minimum.focusedProperty().addListener((obs, was, focused) -> {
            if (!focused) {
                reloadFromStart();
            }
        });
        startsOnly.setOnAction(e -> reloadFromStart());
        endsOnly.setOnAction(e -> reloadFromStart());

        HBox card = new HBox(12, search, Widgets.muted("Sort by"), sort,
                Widgets.muted("Min count"), minimum, startsOnly, endsOnly);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("card");
        return card;
    }

    private void reloadFromStart() {
        offset = 0;
        load();
    }

    private static int parseCount(String text, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(text.trim()));
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private void load() {
        if (service == null) {
            return;
        }
        int seq = ++requestSeq;
        String searchText = search.getText();
        String sortValue = sort.getValue();
        int min = parseCount(minimum.getText(), 1);
        boolean starts = startsOnly.isSelected();
        boolean ends = endsOnly.isSelected();
        int from = offset;
        Integer keep = current == null ? null : current.id();

        Task<AppService.WordPage> task = new Task<>() {
            @Override
            protected AppService.WordPage call() throws Exception {
                return service.searchWords(searchText, sortValue, min, starts, ends, from, PAGE_SIZE);
            }
        };
        task.setOnSucceeded(e -> {
            if (seq != requestSeq) {
                return; // a newer search has started
            }
            AppService.WordPage page = task.getValue();
            total = page.total();
            table.getItems().setAll(page.rows());
            showing.setText(total == 0 ? "No words match."
                    : String.format("Showing %,d – %,d of %,d words", from + 1, from + page.rows().size(), total));
            previous.setDisable(from == 0);
            next.setDisable(from + PAGE_SIZE >= total);

            AppService.WordRow pick = null;
            for (AppService.WordRow row : page.rows()) {
                if (keep != null && row.id() == keep) {
                    pick = row;
                }
            }
            if (pick == null && !page.rows().isEmpty()) {
                pick = page.rows().get(0);
            }
            if (pick != null) {
                table.getSelectionModel().select(pick);
            } else {
                clearDetails();
            }
        });
        task.setOnFailed(e -> showing.setText("Database error: " + task.getException().getMessage()));
        start(task);
    }

    private static void start(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ---------------- Word list ----------------

    private VBox wordsCard() {
        table.getColumns().add(column("Word", 1.5, AppService.WordRow::text));
        table.getColumns().add(column("Count", 1, word -> String.valueOf(word.count())));
        table.getColumns().add(column("Starts", 1, word -> String.valueOf(word.starts())));
        table.getColumns().add(column("Ends", 1, word -> String.valueOf(word.ends())));
        table.getColumns().add(column("Next words", 1.2, word -> String.valueOf(word.next())));
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setFixedCellSize(40);
        table.setPrefHeight(440);
        table.getSelectionModel().selectedItemProperty().addListener((obs, was, row) -> {
            if (row != null) {
                showDetails(row);
            }
        });

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        previous.setAccessibleText("Previous page");
        previous.setDisable(true);
        previous.setOnAction(e -> {
            offset = Math.max(0, offset - PAGE_SIZE);
            load();
        });
        next.setAccessibleText("Next page");
        next.setDisable(true);
        next.setOnAction(e -> {
            offset += PAGE_SIZE;
            load();
        });
        HBox footer = new HBox(10, showing, gap, previous, next);
        footer.setAlignment(Pos.CENTER_LEFT);
        footer.getStyleClass().add("card-header");
        VBox card = new VBox(table, footer);
        card.getStyleClass().addAll("card", "flush");
        card.setMinWidth(490);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private TableColumn<AppService.WordRow, String> column(String title, double share,
            Function<AppService.WordRow, String> value) {
        TableColumn<AppService.WordRow, String> column = new TableColumn<>(title);
        column.setCellValueFactory(data -> new SimpleStringProperty(value.apply(data.getValue())));
        column.setPrefWidth(share * 100);
        return column;
    }

    // ---------------- Selected word ----------------

    private VBox detailsCard() {
        wordLabel.getStyleClass().addAll("mono", "stat-value");
        VBox heading = new VBox(4, Widgets.caption("Selected word"), wordLabel, nextInfo);
        HBox counts = new HBox(12, countField("Count", countField), countField("Starts", startsField),
                countField("Ends", endsField));

        seeAll.getStyleClass().add("link");
        seeAll.setOnAction(e -> {
            showAllFollowers = !showAllFollowers;
            loadFollowers();
        });

        Button save = new Button("Save changes");
        save.getStyleClass().add("primary");
        save.setOnAction(e -> saveCounts());
        Button revert = new Button("Revert");
        revert.setOnAction(e -> {
            if (current != null) {
                fillCounts(current);
                status.setText("");
            }
        });

        VBox card = new VBox(22, heading, counts, followersBox, new HBox(10, save, revert), status);
        card.getStyleClass().add("card");
        card.setPrefWidth(360);
        card.setMinWidth(330);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        return card;
    }

    private VBox countField(String title, TextField input) {
        input.setPrefWidth(85);
        VBox field = new VBox(8, Widgets.caption(title), input);
        HBox.setHgrow(field, Priority.ALWAYS);
        return field;
    }

    private void fillCounts(AppService.WordRow row) {
        countField.setText(String.valueOf(row.count()));
        startsField.setText(String.valueOf(row.starts()));
        endsField.setText(String.valueOf(row.ends()));
    }

    private void clearDetails() {
        current = null;
        wordLabel.setText("—");
        nextInfo.setText("No word selected.");
        countField.clear();
        startsField.clear();
        endsField.clear();
        followersBox.getChildren().clear();
    }

    private void showDetails(AppService.WordRow row) {
        current = row;
        showAllFollowers = false;
        status.setText("");
        wordLabel.setText(row.text());
        nextInfo.setText(row.next() + " different next words");
        fillCounts(row);
        loadFollowers();
    }

    private void loadFollowers() {
        if (service == null || current == null) {
            return;
        }
        int seq = ++followerSeq;
        int id = current.id();
        int limit = showAllFollowers ? 50 : 5;
        int totalNext = current.next();

        Task<List<AppService.FollowerRow>> task = new Task<>() {
            @Override
            protected List<AppService.FollowerRow> call() throws Exception {
                return service.getFollowers(id, limit);
            }
        };
        task.setOnSucceeded(e -> {
            if (seq != followerSeq) {
                return;
            }
            followersBox.getChildren().clear();
            followersBox.getChildren().add(Widgets.caption("Most common next words"));
            List<AppService.FollowerRow> rows = task.getValue();
            if (rows.isEmpty()) {
                followersBox.getChildren().add(Widgets.muted("This word has no next words."));
                return;
            }
            int top = Math.max(1, rows.get(0).percent());
            rows.forEach(r -> followersBox.getChildren().add(follower(r.word(), r.percent(), top)));
            if (totalNext > 5) {
                seeAll.setText(showAllFollowers ? "Show fewer" : "See all " + totalNext + " next words");
                followersBox.getChildren().add(seeAll);
            }
        });
        start(task);
    }

    private HBox follower(String text, int percent, int top) {
        Label word = new Label(text);
        word.getStyleClass().add("mono");
        word.setMinWidth(50);
        ProgressBar bar = new ProgressBar(percent / (double) top);
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label value = new Label(percent + "%");
        value.setMinWidth(35);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(12, word, bar, value);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private void saveCounts() {
        if (service == null || current == null) {
            return;
        }
        int id = current.id();
        int count;
        int starts;
        int ends;
        try {
            count = Integer.parseInt(countField.getText().trim());
            starts = Integer.parseInt(startsField.getText().trim());
            ends = Integer.parseInt(endsField.getText().trim());
            if (count < 0 || starts < 0 || ends < 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            status.setText("Counts must be whole numbers, 0 or more.");
            return;
        }
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                service.updateWordCounts(id, count, starts, ends);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            status.setText("Saved.");
            load();
        });
        task.setOnFailed(e -> status.setText("Database error: " + task.getException().getMessage()));
        start(task);
    }
}