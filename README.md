# sentence-builder

## Running the UI

The UI is a JavaFX desktop app. There is no Maven/Gradle build, so you run it with the JavaFX SDK.

1. Install Java 17 or newer.
2. Download the JavaFX SDK from [gluonhq.com/products/javafx](https://gluonhq.com/products/javafx/) and unzip it (e.g. to `~/javafx-sdk`).
3. From the repo root (the app loads `src/ui/css/styles.css` by relative path), run:

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

## Running the tests

The UI tests use JUnit 5 and the same JavaFX SDK as above.

1. Put the JUnit 5 jars in one folder: `junit-jupiter-api`, `junit-jupiter-engine`, `junit-platform-commons`, `junit-platform-engine`, `junit-platform-launcher`, `opentest4j` and `apiguardian` (the versions bundled with the VS Code "Test Runner for Java" extension work).
2. From the repo root, run:

```bash
./run-tests.sh
```

By default the script looks for the jars in the VS Code Java Test extension folder. Otherwise set `JUNIT_DIR` (and `JAVAFX_HOME` if the SDK is not in `~/javafx-sdk`):

```bash
JUNIT_DIR=/path/to/junit-jars JAVAFX_HOME=/path/to/javafx-sdk ./run-tests.sh
```

## More docs

- [UI guide](docs/ui.md): pages and navigation, styling, backend integration points, tests.
