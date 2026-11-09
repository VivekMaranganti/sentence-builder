import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class WordDAO {

    // Helper class to store word counts before flushing to DB
    public static class WordStats {
        private int frequency;
        private int startCount;
        private int endCount;

        public WordStats(int frequency, int startCount, int endCount) {
            this.frequency = frequency;
            this.startCount = startCount;
            this.endCount = endCount;
        }

        public int getFrequency() { return frequency; }
        public int getStartCount() { return startCount; }
        public int getEndCount() { return endCount; }
    }

    // Function 1: Batch Insert / Update Words
    public void upsertWordsBatch(Connection conn, Map<String, WordStats> wordMap) throws SQLException {
        String sql = "INSERT INTO words (word_text, frequency, start_count, end_count) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "frequency = frequency + VALUES(frequency), " +
                "start_count = start_count + VALUES(start_count), " +
                "end_count = end_count + VALUES(end_count)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (Map.Entry<String, WordStats> entry : wordMap.entrySet()) {
                WordStats stats = entry.getValue();
                stmt.setString(1, entry.getKey());
                stmt.setInt(2, stats.getFrequency());
                stmt.setInt(3, stats.getStartCount());
                stmt.setInt(4, stats.getEndCount());
                stmt.addBatch();

                if (++count % 1000 == 0) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    // Function 2: Batch Insert / Update Bigram Transitions
    // Expects key format "prev_word_id:next_word_id" mapped to transition count
    public void upsertTransitionsBatch(Connection conn, Map<String, Integer> transitionMap) throws SQLException {
        String sql = "INSERT INTO word_transitions (previous_word_id, next_word_id, transition_count) " +
                "VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE " +
                "transition_count = transition_count + VALUES(transition_count)";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            int count = 0;
            for (Map.Entry<String, Integer> entry : transitionMap.entrySet()) {
                // Splits "12:45" -> prevId: 12, nextId: 45
                String[] ids = entry.getKey().split(":");
                int prevId = Integer.parseInt(ids[0]);
                int nextId = Integer.parseInt(ids[1]);

                stmt.setInt(1, prevId);
                stmt.setInt(2, nextId);
                stmt.setInt(3, entry.getValue());
                stmt.addBatch();

                if (++count % 1000 == 0) {
                    stmt.executeBatch();
                }
            }
            stmt.executeBatch();
        }
    }

    // Function 3: Record Imported File Metadata
    public void recordImportedFile(Connection conn, String fileName, int wordCount) throws SQLException {
        String sql = "INSERT INTO imported_files (file_name, word_count) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, fileName);
            stmt.setInt(2, wordCount);
            stmt.executeUpdate();
        }
    }

    // Function 4: Fetch Candidate Words for Auto-Complete / Sentence Generation
    public ResultSet getNextWordCandidates(Connection conn, int previousWordId) throws SQLException {
        String sql = "SELECT w.word_id, w.word_text, t.transition_count " +
                "FROM word_transitions t " +
                "JOIN words w ON t.next_word_id = w.word_id " +
                "WHERE t.previous_word_id = ? " +
                "ORDER BY t.transition_count DESC";

        PreparedStatement stmt = conn.prepareStatement(sql);
        stmt.setInt(1, previousWordId);
        return stmt.executeQuery(); // Note: Caller is responsible for closing this statement/resultset
    }

    // Helper: Map word strings to word_ids after inserting words
    public Map<String, Integer> getWordIdMap(Connection conn) throws SQLException {
        Map<String, Integer> wordMap = new HashMap<>();
        String sql = "SELECT word_id, word_text FROM words";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                wordMap.put(rs.getString("word_text"), rs.getInt("word_id"));
            }
        }
        return wordMap;
    }

    // Helper: Log generated sentence to DB for duplicate checking
    public void saveGeneratedSentence(Connection conn, String sentenceText, String algorithm) throws SQLException {
        String sql = "INSERT INTO generated_sentences (sentence_text, algorithm) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, sentenceText);
            stmt.setString(2, algorithm);
            stmt.executeUpdate();
        }
    }
}