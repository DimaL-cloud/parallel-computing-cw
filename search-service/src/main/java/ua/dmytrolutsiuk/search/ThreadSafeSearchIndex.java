package ua.dmytrolutsiuk.search;

import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ThreadSafeSearchIndex implements SearchIndex {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private SearchIndex currentIndex;

    public ThreadSafeSearchIndex(SearchIndex initialIndex) {
        this.currentIndex = initialIndex;
    }

    @Override
    public List<String> search(String text) {
        lock.readLock().lock();
        try {
            return currentIndex.search(text);
        } finally {
            lock.readLock().unlock();
        }
    }

    void swapIndex(SearchIndex newIndex) {
        lock.writeLock().lock();
        try {
            this.currentIndex = newIndex;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
