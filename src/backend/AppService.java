package backend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/*
Written by Dev Sheth, 11/16/2026, with AI assistance (Claude).
The one class the UI talks to. Pages call this instead of writing SQL.
*/
//I had to paste all the earlier files of the ui and backend into claude that my teammates had made for better context and it took me a few times so I wouldnt run out of tokens
public class AppService {

    public static final List<String> ALGORITHMS = List.of("Weighted bigram", "Most frequent", "Random follower");

    public record Stats(int uniqueWords, long totalWords, int wordPairs, int files) {
    }

    public record ImportRecord(String file, int words, String imported) {
    }

    public record SentenceRecord(String text, String algorithm, String created, int times) {
    }

    public record BatchItem(String startWord, String text, boolean duplicate) {
    }

    public record Suggestion(String word, int percent) {
    }

    public record WordRow(int id, String text, int count, int starts, int ends, int next) {
    }

    public record WordPage(List<WordRow> rows, int total) {
    }

    public record FollowerRow(String word, int count, int percent) {
    }

    public record AlgorithmRate(String algorithm, int total, int distinct) {
        public int duplicatePercent() {
            return total == 0 ? 0 : (int) Math.round(100.0 * (total - distinct) / total);
        }
    }

    private final WordDAO dao = new WordDAO();
    private SentenceGenerator generator; // loaded from MySQL on first use, reloaded after changes

    // ---------------- Connection and generator cache ----------------

    public boolean isConnected() {
        try (Connection conn = DatabaseManager.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private synchronized SentenceGenerator generator() throws SQLException {
        if (generator == null) {
            try (Connection conn = DatabaseManager.getConnection()) {
                generator = new SentenceGenerator(conn);
            }
        }
        return generator;
    }

    /**
     * Call after anything changes words or transitions, so the next generation
     * reloads.
     */
    public synchronized void invalidateGenerator() {
        generator = null;
    }

    // ---------------- Home ----------------

    public Stats getStats() throws SQLException {
        String sql = "SELECT (SELECT COUNT(*) FROM words), "
                + "(SELECT COALESCE(SUM(word_count), 0) FROM imported_files), "
                + "(SELECT COUNT(*) FROM word_transitions), "
                + "(SELECT COUNT(*) FROM imported_files)";
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return new Stats(rs.getInt(1), rs.getLong(2), rs.getInt(3), rs.getInt(4));
        }
    }

    public List<ImportRecord> getImports(int limit) throws SQLException {
        String sql = "SELECT file_name, word_count, DATE_FORMAT(imported_at, '%b %e, %l:%i %p') "
                + "FROM imported_files ORDER BY imported_at DESC, file_id DESC LIMIT ?";
        List<ImportRecord> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new ImportRecord(rs.getString(1), rs.getInt(2), rs.getString(3)));
                }
            }
        }
        return rows;
    }

    public List<String> getRecentSentences(int limit) throws SQLException {
        String sql = "SELECT sentence_text FROM generated_sentences "
                + "ORDER BY generated_at DESC, sentence_id DESC LIMIT ?";
        List<String> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(rs.getString(1));
                }
            }
        }
        return rows;
    }

    // ---------------- Generate and Batch ----------------

    /**
     * Builds one sentence word by word. Throws IllegalArgumentException if the seed
     * isn't in the database.
     */
    public List<SentenceGenerator.Step> generate(String algorithm, String seedWord, int maxWords,
            boolean stopAtEnd) throws SQLException {
        return generator().generateSteps(algorithm, seedWord, maxWords, stopAtEnd);
    }

    /**
     * Joins the steps into display text: lower-case, first letter capitalized,
     * ending in a period.
     */
    public static String formatSentence(List<SentenceGenerator.Step> steps) {
        StringBuilder sb = new StringBuilder();
        for (SentenceGenerator.Step step : steps) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(step.word().toLowerCase());
        }
        if (sb.length() == 0) {
            return "";
        }
        sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        sb.append('.');
        return sb.toString();
    }

    public List<BatchItem> generateBatch(int howMany, String algorithm, int maxWords) throws SQLException {
        SentenceGenerator g = generator();
        List<BatchItem> items = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try (Connection conn = DatabaseManager.getConnection()) {
            for (int i = 0; i < howMany; i++) {
                String start = g.randomStartWord();
                if (start == null) {
                    break;
                }
                String text = formatSentence(g.generateSteps(algorithm, start, maxWords, true));
                boolean duplicate = !seen.add(text) || existsInHistory(conn, text);
                items.add(new BatchItem(start.toLowerCase(), text, duplicate));
            }
        }
        return items;
    }

    private boolean existsInHistory(Connection conn, String text) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM generated_sentences WHERE sentence_text = ?")) {
            ps.setString(1, text);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Saves sentences to the generated_sentences history (used for duplicate
     * checking).
     */
    public void logSentences(List<String> texts, String algorithm) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            for (String text : texts) {
                dao.saveGeneratedSentence(conn, text, algorithm);
            }
        }
    }

    /**
     * Feeds sentences back into the word and transition counts, and logs them as a
     * "batch" import.
     */
    public void addToCorpus(List<String> sentences) throws SQLException {
        Map<String, WordDAO.WordStats> stats = new HashMap<>();
        Map<String, Integer> pairs = new HashMap<>();
        int total = 0;

        for (String sentence : sentences) {
            List<String> tokens = tokenize(sentence);
            String previous = null;
            for (int i = 0; i < tokens.size(); i++) {
                String word = tokens.get(i);
                total++;
                WordDAO.WordStats old = stats.getOrDefault(word, new WordDAO.WordStats(0, 0, 0));
                stats.put(word, new WordDAO.WordStats(
                        old.getFrequency() + 1,
                        old.getStartCount() + (i == 0 ? 1 : 0),
                        old.getEndCount() + (i == tokens.size() - 1 ? 1 : 0)));
                if (previous != null) {
                    pairs.merge(previous + "\t" + word, 1, Integer::sum);
                }
                previous = word;
            }
        }
        if (total == 0) {
            return;
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            dao.upsertWordsBatch(conn, stats);
            Map<String, Integer> ids = lowerCaseIds(conn);
            Map<String, Integer> idPairs = new HashMap<>();
            for (Map.Entry<String, Integer> e : pairs.entrySet()) {
                String[] p = e.getKey().split("\t");
                Integer a = ids.get(p[0]);
                Integer b = ids.get(p[1]);
                if (a != null && b != null) {
                    idPairs.merge(a + ":" + b, e.getValue(), Integer::sum);
                }
            }
            dao.upsertTransitionsBatch(conn, idPairs);
            dao.recordImportedFile(conn, "batch_" + LocalDate.now() + ".txt", total);
        }
        invalidateGenerator();
    }

    private static List<String> tokenize(String sentence) {
        List<String> tokens = new ArrayList<>();
        for (String raw : sentence.toLowerCase().split("\\s+")) {
            String word = raw.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
            if (!word.isEmpty()) {
                tokens.add(word);
            }
        }
        return tokens;
    }

    /**
     * word -> id with lower-case keys, because MySQL treats "The" and "the" as the
     * same row.
     */
    private Map<String, Integer> lowerCaseIds(Connection conn) throws SQLException {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Integer> e : dao.getWordIdMap(conn).entrySet()) {
            result.put(e.getKey().toLowerCase(), e.getValue());
        }
        return result;
    }

    // ---------------- Auto-complete ----------------

    /** Top 5 next words after the given word, with their share of all followers. */
    public List<Suggestion> suggestNext(String previousWord) throws SQLException {
        String sql = "SELECT w.word_text, t.transition_count FROM word_transitions t "
                + "JOIN words w ON w.word_id = t.next_word_id "
                + "WHERE t.previous_word_id = (SELECT word_id FROM words WHERE word_text = ?) "
                + "ORDER BY t.transition_count DESC";
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        long total = 0;
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, previousWord.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                    counts.add(rs.getInt(2));
                    total += rs.getInt(2);
                }
            }
        }
        List<Suggestion> result = new ArrayList<>();
        for (int i = 0; i < Math.min(5, names.size()); i++) {
            result.add(new Suggestion(names.get(i), (int) Math.round(100.0 * counts.get(i) / total)));
        }
        return result;
    }

    /**
     * The user chose or typed a word after previousWord (null at the start of a
     * sentence).
     * Adds the word if it is new, and adds one to how often it follows
     * previousWord.
     * Returns true if the word was new to the database.
     */
    public boolean recordChoice(String previousWord, String typedWord) throws SQLException {
        String typed = typedWord.trim().toLowerCase();
        try (Connection conn = DatabaseManager.getConnection()) {
            boolean isNew = wordId(conn, typed) == null;
            Map<String, WordDAO.WordStats> stats = new HashMap<>();
            stats.put(typed, new WordDAO.WordStats(1, previousWord == null ? 1 : 0, 0));
            dao.upsertWordsBatch(conn, stats);

            if (previousWord != null) {
                Integer prevId = wordId(conn, previousWord.trim().toLowerCase());
                Integer newId = wordId(conn, typed);
                if (prevId != null && newId != null) {
                    dao.upsertTransitionsBatch(conn, Map.of(prevId + ":" + newId, 1));
                }
            }
            invalidateGenerator();
            return isNew;
        }
    }

    private Integer wordId(Connection conn, String word) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT word_id FROM words WHERE word_text = ?")) {
            ps.setString(1, word);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : null;
            }
        }
    }

    // ---------------- Word Explorer ----------------

    public WordPage searchWords(String search, String sort, int minCount, boolean startsOnly,
            boolean endsOnly, int offset, int limit) throws SQLException {
        StringBuilder where = new StringBuilder(" WHERE w.frequency >= ?");
        List<Object> params = new ArrayList<>();
        params.add(minCount);
        if (search != null && !search.isBlank()) {
            where.append(" AND w.word_text LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (startsOnly) {
            where.append(" AND w.start_count > 0");
        }
        if (endsOnly) {
            where.append(" AND w.end_count > 0");
        }

        String order;
        if (sort != null && sort.startsWith("Z")) {
            order = "w.word_text DESC";
        } else if (sort != null && sort.contains("highest")) {
            order = "w.frequency DESC, w.word_text ASC";
        } else if (sort != null && sort.contains("lowest")) {
            order = "w.frequency ASC, w.word_text ASC";
        } else {
            order = "w.word_text ASC";
        }

        try (Connection conn = DatabaseManager.getConnection()) {
            int total;
            try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM words w" + where)) {
                bind(ps, params);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }

            String sql = "SELECT w.word_id, w.word_text, w.frequency, w.start_count, w.end_count, "
                    + "(SELECT COUNT(*) FROM word_transitions t WHERE t.previous_word_id = w.word_id) "
                    + "FROM words w" + where + " ORDER BY " + order + " LIMIT ? OFFSET ?";
            List<Object> pageParams = new ArrayList<>(params);
            pageParams.add(limit);
            pageParams.add(offset);
            List<WordRow> rows = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                bind(ps, pageParams);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        rows.add(new WordRow(rs.getInt(1), rs.getString(2), rs.getInt(3),
                                rs.getInt(4), rs.getInt(5), rs.getInt(6)));
                    }
                }
            }
            return new WordPage(rows, total);
        }
    }

    public List<FollowerRow> getFollowers(int wordId, int limit) throws SQLException {
        String sql = "SELECT w.word_text, t.transition_count FROM word_transitions t "
                + "JOIN words w ON w.word_id = t.next_word_id WHERE t.previous_word_id = ? "
                + "ORDER BY t.transition_count DESC";
        List<String> names = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        long total = 0;
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, wordId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    names.add(rs.getString(1));
                    counts.add(rs.getInt(2));
                    total += rs.getInt(2);
                }
            }
        }
        List<FollowerRow> rows = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, names.size()); i++) {
            rows.add(new FollowerRow(names.get(i), counts.get(i),
                    (int) Math.round(100.0 * counts.get(i) / total)));
        }
        return rows;
    }

    public void updateWordCounts(int wordId, int count, int starts, int ends) throws SQLException {
        if (count < 0 || starts < 0 || ends < 0) {
            throw new IllegalArgumentException("Counts can't be negative.");
        }
        String sql = "UPDATE words SET frequency = ?, start_count = ?, end_count = ? WHERE word_id = ?";
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, count);
            ps.setInt(2, starts);
            ps.setInt(3, ends);
            ps.setInt(4, wordId);
            ps.executeUpdate();
        }
        invalidateGenerator();
    }

    // ---------------- Reports ----------------

    public List<SentenceRecord> getReportSentences(String search, String algorithm,
            boolean duplicatesOnly) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT sentence_text, MIN(algorithm), DATE_FORMAT(MAX(generated_at), '%b %e'), COUNT(*) "
                        + "FROM generated_sentences WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND sentence_text LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (algorithm != null && !algorithm.startsWith("All")) {
            sql.append(" AND algorithm = ?");
            params.add(algorithm);
        }
        sql.append(" GROUP BY sentence_text");
        if (duplicatesOnly) {
            sql.append(" HAVING COUNT(*) > 1");
        }
        sql.append(" ORDER BY MAX(generated_at) DESC LIMIT 500");

        List<SentenceRecord> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new SentenceRecord(rs.getString(1), rs.getString(2), rs.getString(3), rs.getInt(4)));
                }
            }
        }
        return rows;
    }

    /**
     * Per algorithm: how many sentences were generated and how many of those were
     * distinct.
     */
    public List<AlgorithmRate> getAlgorithmRates() throws SQLException {
        String sql = "SELECT algorithm, COUNT(*), COUNT(DISTINCT sentence_text) "
                + "FROM generated_sentences GROUP BY algorithm";
        List<AlgorithmRate> rows = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                rows.add(new AlgorithmRate(rs.getString(1), rs.getInt(2), rs.getInt(3)));
            }
        }
        return rows;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            ps.setObject(i + 1, params.get(i));
        }
    }
}