package ua.dmytrolutsiuk.search;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class IndexScheduler implements AutoCloseable {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final FileIndexer fileIndexer;
    private final Path indexDir;
    private final long periodSeconds;

    public IndexScheduler(FileIndexer fileIndexer, Path indexDir, long periodSeconds) {
        this.fileIndexer = fileIndexer;
        this.indexDir = indexDir;
        this.periodSeconds = periodSeconds;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::runReindexing, 0, periodSeconds, TimeUnit.SECONDS);
    }

    private void runReindexing() {
        long start = System.nanoTime();
        log.info("Starting reindexing process");
        try {
            fileIndexer.indexDirectory(indexDir);
            long duration = System.nanoTime() - start;
            long durationMs = TimeUnit.NANOSECONDS.toMillis(duration);
            log.info("Reindexing process finished in {} ms", durationMs);
        } catch (IOException e) {
            log.error("Error during reindexing process", e);
        }
    }

    @Override
    public void close() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
