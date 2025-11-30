package ua.dmytrolutsiuk.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class SearchClient {

    @NonNull
    private final String baseUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<String> search(String query) throws IOException, InterruptedException {
        if (query.isBlank()) {
            throw new IllegalArgumentException("Query must not be empty");
        }
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        var uri = URI.create(baseUrl).resolve("/filePaths?search=" + encodedQuery);
        log.debug("Sending GET {}", uri);
        var request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(10))
                .build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("Server returned HTTP {}: {}", response.statusCode(), response.body());
            throw new IOException("Unexpected status: " + response.statusCode());
        }
        return objectMapper.readValue(response.body(), new TypeReference<>() {});
    }
}
