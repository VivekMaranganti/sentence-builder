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

### Database setup

The app talks to a local MySQL server, so before running it:

1. Install MySQL Server and run the schema script to create the `sentence_builder` database.
2. Download MySQL Connector/J (platform independent zip) from dev.mysql.com and copy the `.jar` into a `lib/` folder at the repo root. Do not commit it.
3. Set your MySQL root password in the terminal before launching. Never put it in the code.

```bash
export DB_PASSWORD=your_password
```

**Windows (PowerShell):** `run-ui.sh` uses `:` in the classpath, which only works on Mac and Linux. On Windows, run:

```powershell
$env:DB_PASSWORD = "your_password"
javac --module-path $FX --add-modules javafx.controls -cp $JDBC -d out src\backend\*.java src\ui\*.java
java --module-path $FX --add-modules javafx.controls -cp "out;$JDBC" ui.App
```

where `$FX` is your JavaFX `lib` folder and `$JDBC` is the path to the connector jar.

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
