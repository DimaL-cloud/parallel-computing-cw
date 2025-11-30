package ua.dmytrolutsiuk;

import lombok.extern.slf4j.Slf4j;
import ua.dmytrolutsiuk.search.*;
import ua.dmytrolutsiuk.server.SearchServer;
import ua.dmytrolutsiuk.threadpool.FixedThreadPool;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class SearchServiceApplication {

    public static void main(String[] args) throws IOException {
        var indexDir = Path.of("search-service/index-data");
        int httpThreads = Runtime.getRuntime().availableProcessors();
        var httpThreadPool = new FixedThreadPool(httpThreads, Integer.MAX_VALUE);
        int indexThreads = Runtime.getRuntime().availableProcessors();
        var indexingThreadPool = new FixedThreadPool(indexThreads, Integer.MAX_VALUE);
        var indexService = new ThreadSafeSearchIndex(new InvertedIndex());
        var fileIndexer = new FileIndexer(indexService, indexingThreadPool);
        long periodSeconds = 60L;
        var scheduler = new IndexScheduler(fileIndexer, indexDir, periodSeconds);
        scheduler.start();
        var searchServer = new SearchServer(8080, indexService, httpThreadPool);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down...");
            scheduler.close();
            searchServer.close();
            httpThreadPool.shutdown();
            indexingThreadPool.shutdown();
        }));
        searchServer.start();
    }
}
