package ua.dmytrolutsiuk.search;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InvertedIndex implements SearchIndex {

    private static final String TOKEN_DELIMITER_PATTERN = "\\W+";

    private final Map<String, Set<String>> index = new HashMap<>();
    private final Lock writeLock = new ReentrantLock();

    @Override
    public List<String> search(String query) {
        String term = query.toLowerCase(Locale.ROOT);
        Set<String> files = index.get(term);
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        return List.copyOf(files);
    }

    void addDocument(String filePath, String content) {
        String[] tokens = content.split(TOKEN_DELIMITER_PATTERN);
        for (String token : tokens) {
            if (token.isBlank()) {
                continue;
            }
            String term = token.toLowerCase(Locale.ROOT);

            writeLock.lock();
            try {
                Set<String> files = index.computeIfAbsent(term, _ -> new HashSet<>());
                files.add(filePath);
            } finally {
                writeLock.unlock();
            }
        }
    }
}
