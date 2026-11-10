#!/bin/sh
# Compile and launch the JavaFX UI. Run from the repo root.
# Set JAVAFX_HOME to your JavaFX SDK folder (default: ~/javafx-sdk).
#!/bin/sh
# Compile and launch the JavaFX UI. Run from the repo root.
# Set JAVAFX_HOME to your JavaFX SDK folder (default: ~/javafx-sdk).
# Set JDBC_JAR to the MySQL Connector/J jar (default: first one found in lib/).
# Set DB_PASSWORD to your MySQL root password before running.
JAVAFX_HOME="${JAVAFX_HOME:-$HOME/javafx-sdk}"
JDBC_JAR="${JDBC_JAR:-$(ls lib/mysql-connector-j*.jar 2>/dev/null | head -1)}"

javac --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls -cp "$JDBC_JAR" -d out src/backend/*.java src/ui/*.java &&
java --module-path "$JAVAFX_HOME/lib" --add-modules javafx.controls -cp "out:$JDBC_JAR" ui.App