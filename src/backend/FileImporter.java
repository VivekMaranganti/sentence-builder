package backend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/*
Original importer by the database/import team.
Options, Gutenberg stripping, lower-casing, progress, cancel and transaction added
by Dev Sheth, 11/17/2026, with AI assistance (Claude).
*/
public class FileImporter {

    public record Options(boolean stripGutenberg, boolean lowerCase, boolean keepContractions) {
        public static Options defaults() {
            return new Options(false, true, true);
        }
    }

    /** fraction runs from 0.0 to 1.0 over the whole import. */
    public record Progress(String phase, double fraction, int words, int sentences, int uniqueWords) {
    }

    public interface ProgressListener {
        void onProgress(Progress progress);
    }

    public record Result(int words, int uniqueWords, int sentences, double seconds) {
    }

    public static class CancelledException extends Exception {
        public CancelledException() {
            super("Import cancelled.");
        }
    }

    // "Mr." and friends end in a period but don't end a sentence
    private static final Set<String> ABBREVIATIONS = Set.of("mr", "mrs", "ms", "dr", "st", "vs", "mt");

    private final WordDAO wordDAO;

    public FileImporter() {
        this.wordDAO = new WordDAO();
    }

    /** Old entry point, kept for TestImporter. */
    public void importFile(Connection conn, File file) throws IOException, SQLException {
        try {
            importFile(conn, file, Options.defaults(), null, new AtomicBoolean(false));
        } catch (CancelledException e) {
            // can't happen: nothing sets the cancel flag
        }
    }

    public Result importFile(Connection conn, File file, Options options, ProgressListener listener,
            AtomicBoolean cancel) throws IOException, SQLException, CancelledException {
        long startTime = System.nanoTime();
        List<String> lines = readLines(file);

        // Optionally keep only the text between the Gutenberg START and END marker
        // lines
        int from = 0;
        int to = lines.size();
        if (options.stripGutenberg()) {
            int startMarker = indexOfMarker(lines, "*** START OF", 0);
            if (startMarker >= 0) {
                from = startMarker + 1;
                int endMarker = indexOfMarker(lines, "*** END OF", from);
                if (endMarker >= 0) {
                    to = endMarker;
                }
            }
        }
        int span = Math.max(1, to - from);

        Map<String, WordDAO.WordStats> wordMap = new HashMap<>();
        Map<String, Integer> rawTransitions = new HashMap<>(); // "prevWord<TAB>nextWord" -> count
        int totalWords = 0;
        int sentences = 0;
        String prevWord = null; // null means the next word starts a sentence

        for (int i = from; i < to; i++) {
            if (cancel.get()) {
                throw new CancelledException();
            }
            if ((i - from) % 500 == 0) {
                report(listener, "Reading text", 0.8 * (i - from) / span, totalWords, sentences, wordMap.size());
            }
            String line = lines.get(i).replaceAll("[\u2014\u2013]|--+", " ").trim();
            if (line.isEmpty()) {
                continue;
            }

            for (String raw : line.split("\\s+")) {
                String token = raw.replace('\u2019', '\'').replace('\u2018', '\'');
                String core = token.replaceAll("^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$", "");
                if (core.isEmpty()) {
                    continue;
                }
                boolean tokenEnds = endsSentence(token, core);
                String[] parts = options.keepContractions() ? new String[] { core } : core.split("'+");
                int last = parts.length - 1;

                for (int j = 0; j <= last; j++) {
                    if (parts[j].isEmpty()) {
                        continue;
                    }
                    String word = options.lowerCase() ? parts[j].toLowerCase() : parts[j];
                    if (word.length() > 100) {
                        continue;
                    }
                    totalWords++;
                    boolean isEnd = tokenEnds && j == last;
                    boolean isStart = prevWord == null;

                    WordDAO.WordStats old = wordMap.getOrDefault(word, new WordDAO.WordStats(0, 0, 0));
                    wordMap.put(word, new WordDAO.WordStats(
                            old.getFrequency() + 1,
                            old.getStartCount() + (isStart ? 1 : 0),
                            old.getEndCount() + (isEnd ? 1 : 0)));

                    if (prevWord != null) {
                        rawTransitions.merge(prevWord + "\t" + word, 1, Integer::sum);
                    }
                    prevWord = isEnd ? null : word;
                    if (isEnd) {
                        sentences++;
                    }
                }
            }
        }

        if (totalWords == 0) {
            throw new IOException("No words were found in " + file.getName() + ".");
        }

        // Everything below is one transaction, so a cancel or error leaves the database
        // unchanged
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            report(listener, "Saving words", 0.82, totalWords, sentences, wordMap.size());
            wordDAO.upsertWordsBatch(conn, wordMap);
            checkCancel(cancel);

            report(listener, "Saving word pairs", 0.90, totalWords, sentences, wordMap.size());
            Map<String, Integer> ids = new HashMap<>();
            for (Map.Entry<String, Integer> e : wordDAO.getWordIdMap(conn).entrySet()) {
                ids.put(e.getKey().toLowerCase(), e.getValue()); // MySQL treats "The" and "the" as one row
            }
            Map<String, Integer> idTransitions = new HashMap<>();
            for (Map.Entry<String, Integer> e : rawTransitions.entrySet()) {
                String[] pair = e.getKey().split("\t");
                Integer a = ids.get(pair[0].toLowerCase());
                Integer b = ids.get(pair[1].toLowerCase());
                if (a != null && b != null) {
                    idTransitions.merge(a + ":" + b, e.getValue(), Integer::sum);
                }
            }
            wordDAO.upsertTransitionsBatch(conn, idTransitions);
            checkCancel(cancel);

            wordDAO.recordImportedFile(conn, file.getName(), totalWords);
            conn.commit();
        } catch (CancelledException | SQLException | RuntimeException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }

        report(listener, "Done", 1.0, totalWords, sentences, wordMap.size());
        double seconds = (System.nanoTime() - startTime) / 1_000_000_000.0;
        return new Result(totalWords, wordMap.size(), sentences, seconds);
    }

    private static List<String> readLines(File file) throws IOException {
        try {
            return Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        } catch (CharacterCodingException e) {
            return Files.readAllLines(file.toPath(), StandardCharsets.ISO_8859_1);
        }
    }

    private static int indexOfMarker(List<String> lines, String marker, int fromIndex) {
        for (int i = fromIndex; i < lines.size(); i++) {
            if (lines.get(i).contains(marker)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean endsSentence(String token, String core) {
        String t = token.replaceAll("[\"')\\]\u201C\u201D]+$", "");
        if (!(t.endsWith(".") || t.endsWith("!") || t.endsWith("?"))) {
            return false;
        }
        return !(t.endsWith(".") && ABBREVIATIONS.contains(core.toLowerCase()));
    }

    private static void checkCancel(AtomicBoolean cancel) throws CancelledException {
        if (cancel.get()) {
            throw new CancelledException();
        }
    }

    private static void report(ProgressListener listener, String phase, double fraction,
            int words, int sentences, int unique) {
        if (listener != null) {
            listener.onProgress(new Progress(phase, fraction, words, sentences, unique));
        }
    }
}