package ua.dmytrolutsiuk.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class ThreadSafeSearchIndex implements SearchIndex {

    private final AtomicReference<SearchIndex> currentIndex;

    public ThreadSafeSearchIndex(SearchIndex initialIndex) {
        this.currentIndex = new AtomicReference<>(initialIndex);
    }

    @Override
    public List<String> search(String text) {
        return currentIndex.get().search(text);
    }

    void swapIndex(SearchIndex newIndex) {
        currentIndex.set(newIndex);
    }
}
