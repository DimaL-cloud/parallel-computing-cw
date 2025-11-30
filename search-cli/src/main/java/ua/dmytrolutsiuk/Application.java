package ua.dmytrolutsiuk;

import lombok.extern.slf4j.Slf4j;
import ua.dmytrolutsiuk.client.SearchClient;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

@Slf4j
public class Application {

    private static final String BASE_URL = "http://localhost:8080";

    public static void main(String[] args) {
        var client = new SearchClient(BASE_URL);
        System.out.println("Enter a word to search (empty line = exit).");
        try (var scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("> ");
                String query = scanner.nextLine().trim();
                if (query.isEmpty()) {
                    return;
                }
                try {
                    List<String> results = client.search(query);
                    printResults(results);
                } catch (IOException | InterruptedException e) {
                    log.error("Search request failed", e);
                }
            }
        }
    }

    private static void printResults(List<String> results) {
        if (results.isEmpty()) {
            System.out.println("No results.");
            return;
        }
        System.out.println("Found " + results.size() + " file(s):");
        for (int i = 0; i < results.size(); i++) {
            System.out.printf("%2d. %s%n", i + 1, results.get(i));
        }
    }
}
