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

    @Override
    public void start(Stage stage) {
        pages.put(Sidebar.HOME, () -> new HomePage(this::show, service).getView());
        pages.put(Sidebar.IMPORT, () -> new ImportPage().getView());
        pages.put(Sidebar.GENERATE, () -> new GeneratePage(service).getView());
        pages.put(Sidebar.BATCH, () -> new BatchPage().getView());
        pages.put(Sidebar.AUTOCOMPLETE, () -> new AutoCompletePage().getView());
        pages.put(Sidebar.WORDS, () -> new WordExplorerPage().getView());
        pages.put(Sidebar.REPORTS, () -> new ReportsPage().getView());

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
    }

    public static void main(String[] args) {
        launch(args);
    }
}