package ui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;

/** Starts the app and opens the Home page. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        HomePage home = new HomePage();
        Scene scene = new Scene(home.getView(), 1280, 820);

        // Load the styles. This path works when you run from the project root folder.
        scene.getStylesheets().add(new File("src/ui/styles.css").toURI().toString());

        stage.setTitle("Sentence Builder");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
