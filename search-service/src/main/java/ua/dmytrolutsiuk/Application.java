package ua.dmytrolutsiuk;

import lombok.extern.slf4j.Slf4j;
import ua.dmytrolutsiuk.search.*;
import ua.dmytrolutsiuk.server.SearchServer;
import ua.dmytrolutsiuk.threadpool.FixedThreadPool;
import ua.dmytrolutsiuk.threadpool.ThreadPool;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class Application {

    public static void main(String[] args) throws IOException {

        Path indexDir = Path.of("search-service/index-data");

        int httpThreads = Runtime.getRuntime().availableProcessors();
        ThreadPool httpThreadPool = new FixedThreadPool(httpThreads, Integer.MAX_VALUE);

        int indexThreads = Runtime.getRuntime().availableProcessors();
        ThreadPool indexingThreadPool = new FixedThreadPool(indexThreads, Integer.MAX_VALUE);

        ThreadSafeSearchIndex indexService = new ThreadSafeSearchIndex(new InvertedIndex());

        FileIndexer fileIndexer = new FileIndexer(indexService, indexingThreadPool);

        long periodSeconds = 60L;
        IndexScheduler scheduler = new IndexScheduler(fileIndexer, indexDir, periodSeconds);
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
