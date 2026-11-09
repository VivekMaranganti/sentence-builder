#!/bin/sh
# Compile and launch the JavaFX UI. Run from the repo root.
# Set JAVAFX_HOME to your JavaFX SDK folder (default: ~/javafx-sdk).
JAVAFX_HOME="${JAVAFX_HOME:-$HOME/javafx-sdk}"

javac --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls -d out src/ui/*.java &&
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls -cp out ui.App
