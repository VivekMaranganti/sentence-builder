# sentence-builder

## Running the UI

The UI is a JavaFX desktop app. There is no Maven/Gradle build, so you run it with the JavaFX SDK.

1. Install Java 17 or newer.
2. Download the JavaFX SDK from [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/) and unzip it (e.g. to `~/javafx-sdk`).
3. From the repo root (the app loads `src/ui/styles.css` by relative path), run:

```bash
./run-ui.sh
```

If the SDK is somewhere else, set `JAVAFX_HOME`:

```bash
JAVAFX_HOME=/path/to/javafx-sdk ./run-ui.sh
```

Or run the commands directly:

```bash
javac --module-path ~/javafx-sdk/lib --add-modules javafx.controls -d out src/ui/*.java
java --module-path ~/javafx-sdk/lib --add-modules javafx.controls -cp out ui.App
```
