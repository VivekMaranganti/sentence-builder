#!/bin/sh
# Compile and run the UI tests. Run from the repo root.
# Set JAVAFX_HOME to your JavaFX SDK folder (default: ~/javafx-sdk).
# Set JUNIT_DIR to a folder holding the JUnit 5 jars (see README).
JAVAFX_HOME="${JAVAFX_HOME:-$HOME/javafx-sdk}"
JUNIT_DIR="${JUNIT_DIR:-$(ls -d "$HOME"/.vscode/extensions/vscjava.vscode-java-test-*/server 2>/dev/null | tail -1)}"

JUNIT_CP=$(ls "$JUNIT_DIR"/junit-jupiter-api_5*.jar "$JUNIT_DIR"/junit-jupiter-engine_5*.jar \
    "$JUNIT_DIR"/junit-platform-commons_1*.jar "$JUNIT_DIR"/junit-platform-engine_1*.jar \
    "$JUNIT_DIR"/junit-platform-launcher_1*.jar "$JUNIT_DIR"/org.opentest4j_*.jar \
    "$JUNIT_DIR"/org.apiguardian.api_*.jar | tr '\n' ':')
FX="--module-path $JAVAFX_HOME/lib --add-modules javafx.controls"

rm -rf out-test && mkdir out-test &&
javac $FX -cp "$JUNIT_CP" -d out-test src/ui/*.java test/ui/*.java &&
java $FX -cp "out-test:$JUNIT_CP" ui.RunTests
