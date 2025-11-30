package ua.dmytrolutsiuk.search;

import lombok.extern.slf4j.Slf4j;
import ua.dmytrolutsiuk.threadpool.ThreadPool;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class FileIndexer {

    private final ThreadSafeSearchIndex threadSafeIndex;
    private final ThreadPool threadPool;

    public FileIndexer(ThreadSafeSearchIndex threadSafeIndex, ThreadPool threadPool) {
        this.threadSafeIndex = threadSafeIndex;
        this.threadPool = threadPool;
    }

    public void indexDirectory(Path rootDir) throws IOException {
        log.info("Building new index for directory: {}", rootDir.toAbsolutePath());

        List<Path> files = collectFiles(rootDir);
        log.info("Found {} files to index", files.size());

        if (files.isEmpty()) {
            threadSafeIndex.swapIndex(new InvertedIndex());
            log.info("No files found, swapped empty index");
            return;
        }

        InvertedIndex newIndex = new InvertedIndex();
        CountDownLatch latch = new CountDownLatch(files.size());

        for (Path file : files) {
            Runnable task = () -> {
                try {
                    indexFile(file, newIndex);
                } catch (IOException e) {
                    log.error("Failed to index file {}", file, e);
                } finally {
                    latch.countDown();
                }
            };
            threadPool.submit(task);
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while waiting for indexing tasks to finish", e);
            return;
        }

        threadSafeIndex.swapIndex(newIndex);
        log.info("New index swapped successfully");
    }

    private List<Path> collectFiles(Path rootDir) throws IOException {
        List<Path> result = new ArrayList<>();
        try (var stream = Files.walk(rootDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(result::add);
        }
        return result;
    }

    private void indexFile(Path filePath, InvertedIndex targetIndex) throws IOException {
        String content = Files.readString(filePath, StandardCharsets.UTF_8);
        targetIndex.addDocument(filePath.toString(), content);
    }
}
