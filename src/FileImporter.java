import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class FileImporter {

    private final WordDAO wordDAO;

    public FileImporter() {
        this.wordDAO = new WordDAO();
    }

    public void importFile(Connection conn, File file) throws IOException, SQLException {
        Map<String, WordDAO.WordStats> wordMap = new HashMap<>();
        Map<String, Integer> rawTransitions = new HashMap<>(); // Key: "prevWord:nextWord"

        int totalWordCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            String prevWord = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;

                // Split line into words using whitespace while keeping punctuation structure
                String[] rawTokens = line.split("\\s+");

                for (int i = 0; i < rawTokens.length; i++) {
                    String token = rawTokens[i];

                    // Clean word: keep letters, apostrophes, and hyphens (strips leading/trailing punctuation)
                    String cleanedWord = token.replaceAll("^[^a-zA-Z0-9]+|[^a-zA-Z0-9]+$", "");
                    if (cleanedWord.isEmpty()) continue;

                    totalWordCount++;

                    // Determine sentence start/end boundaries based on punctuation
                    boolean isStart = (prevWord == null || i == 0 && (token.startsWith("\"") || token.startsWith("(")));
                    boolean isEnd = token.endsWith(".") || token.endsWith("!") || token.endsWith("?");

                    // 1. Update word frequencies in memory
                    WordDAO.WordStats stats = wordMap.getOrDefault(cleanedWord, new WordDAO.WordStats(0, 0, 0));
                    int newFreq = stats.getFrequency() + 1;
                    int newStart = stats.getStartCount() + (isStart ? 1 : 0);
                    int newEnd = stats.getEndCount() + (isEnd ? 1 : 0);
                    wordMap.put(cleanedWord, new WordDAO.WordStats(newFreq, newStart, newEnd));

                    // 2. Track bigram transitions
                    if (prevWord != null) {
                        String bigramKey = prevWord + ":" + cleanedWord;
                        rawTransitions.put(bigramKey, rawTransitions.getOrDefault(bigramKey, 0) + 1);
                    }

                    // Reset previous word if sentence ends
                    prevWord = isEnd ? null : cleanedWord;
                }
            }
        }

        System.out.println("Finished reading file. Total words: " + totalWordCount);
        System.out.println("Unique words to insert: " + wordMap.size());

        // Step 2: Flush unique words to MySQL
        System.out.println("Uploading words to database...");
        wordDAO.upsertWordsBatch(conn, wordMap);

        // Step 3: Fetch updated word IDs to build transition mapping
        System.out.println("Fetching word IDs...");
        Map<String, Integer> wordIdMap = wordDAO.getWordIdMap(conn);

        // Step 4: Map raw word pairs to ID pairs ("prevId:nextId")
        Map<String, Integer> idTransitions = new HashMap<>();
        for (Map.Entry<String, Integer> entry : rawTransitions.entrySet()) {
            String[] parts = entry.getKey().split(":");
            Integer prevId = wordIdMap.get(parts[0]);
            Integer nextId = wordIdMap.get(parts[1]);

            if (prevId != null && nextId != null) {
                String idKey = prevId + ":" + nextId;
                idTransitions.put(idKey, idTransitions.getOrDefault(idKey, 0) + entry.getValue());
            }
        }

        // Step 5: Flush bigrams to MySQL
        System.out.println("Uploading transitions to database...");
        wordDAO.upsertTransitionsBatch(conn, idTransitions);

        // Step 6: Log imported file record
        wordDAO.recordImportedFile(conn, file.getName(), totalWordCount);
        System.out.println("File import complete!");
    }
}