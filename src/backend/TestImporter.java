package backend;

import java.io.FileNotFoundException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;

public class TestImporter {
    public static void main(String[] args) throws IOException, SQLException {
        if (args.length > 1) {
            throw new IllegalArgumentException("Usage: java TestImporter [path-to-text-file]");
        }

        Path inputPath = args.length == 1
                ? Path.of(args[0])
                : Path.of("data", "PracticeImport.txt");
        if (!Files.isRegularFile(inputPath)) {
            throw new FileNotFoundException("Input file not found: " + inputPath.toAbsolutePath());
        }

        File file = inputPath.toFile();
        try (Connection conn = DatabaseManager.getConnection()) {
            FileImporter importer = new FileImporter();
            long startTime = System.nanoTime();

            importer.importFile(conn, file);

            double elapsedSeconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
            System.out.printf("Import of %s completed in %.2f seconds.%n",
                    file.getName(), elapsedSeconds);
        }
    }
}