package ui;

import backend.AppService;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/** Starts the app: a sidebar on the left and the current page in the middle. */
public class App extends Application {

    private final BorderPane root = new BorderPane();
    private final Sidebar sidebar = new Sidebar(this::show);
    private final AppService service = new AppService();

    // How to build each page. A page is built the first time it's opened, then
    // reused.
    private final Map<String, Supplier<Parent>> pages = new HashMap<>();
    private final Map<String, Parent> openedPages = new HashMap<>();
    // Pages that reload their data every time they are shown
    private final Map<String, Runnable> refreshers = new HashMap<>();

    @Override
    public void start(Stage stage) {
        pages.put(Sidebar.HOME, () -> {
            HomePage p = new HomePage(this::show, service);
            refreshers.put(Sidebar.HOME, p::refresh);
            return p.getView();
        });
        pages.put(Sidebar.IMPORT, () -> new ImportPage(service).getView());
        pages.put(Sidebar.GENERATE, () -> new GeneratePage(service).getView());
        pages.put(Sidebar.BATCH, () -> new BatchPage(service).getView());
        pages.put(Sidebar.AUTOCOMPLETE, () -> new AutoCompletePage(service).getView());
        pages.put(Sidebar.WORDS, () -> {
            WordExplorerPage p = new WordExplorerPage(service);
            refreshers.put(Sidebar.WORDS, p::refresh);
            return p.getView();
        });
        pages.put(Sidebar.REPORTS, () -> {
            ReportsPage p = new ReportsPage(service);
            refreshers.put(Sidebar.REPORTS, p::refresh);
            return p.getView();
        });

        root.setLeft(sidebar.getView());
        show(Sidebar.HOME);

        Scene scene = new Scene(root, 1280, 820);
        scene.getStylesheets().add(new File("src/ui/css/styles.css").toURI().toString());

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();

        Task<Boolean> check = new Task<>() {
            @Override
            protected Boolean call() {
                return service.isConnected();
            }
        };
        check.setOnSucceeded(e -> sidebar.setConnected(check.getValue()));
        Thread thread = new Thread(check);
        thread.setDaemon(true);
        thread.start();
    }

    private void show(String page) {
        Supplier<Parent> builder = pages.get(page);
        if (builder == null) {
            return;
        }
        sidebar.setActive(page);
        root.setCenter(openedPages.computeIfAbsent(page, name -> builder.get()));
        Runnable refresh = refreshers.get(page);
        if (refresh != null) {
            refresh.run();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}