package ua.dmytrolutsiuk.threadpool;

public interface ThreadPool {

    boolean submit(Runnable task);

    void shutdown();

    void shutdownNow();

    void pause();

    void resume();
}
