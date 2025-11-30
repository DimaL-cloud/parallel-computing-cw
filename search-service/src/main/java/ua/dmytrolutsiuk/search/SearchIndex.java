package ua.dmytrolutsiuk.search;

import java.util.List;

public interface SearchIndex {

    List<String> search(String text);
}
