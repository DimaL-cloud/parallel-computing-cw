package ua.dmytrolutsiuk.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import ua.dmytrolutsiuk.search.ThreadSafeSearchIndex;
import ua.dmytrolutsiuk.threadpool.ThreadPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class HttpServer implements AutoCloseable {

    private final int port;
    private final ThreadSafeSearchIndex threadSafeSearchIndex;
    private final ThreadPool threadPool;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpResponseWriter responseWriter = new HttpResponseWriter(objectMapper);

    private volatile boolean running = false;
    private ServerSocket serverSocket;

    public HttpServer(int port, ThreadSafeSearchIndex threadSafeSearchIndex, ThreadPool threadPool) {
        this.port = port;
        this.threadSafeSearchIndex = threadSafeSearchIndex;
        this.threadPool = threadPool;
    }

    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        log.info("HTTP search server started on port {}", port);

        while (running) {
            try {
                Socket clientSocket = serverSocket.accept();
                threadPool.submit(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (running) {
                    log.error("Error accepting connection", e);
                } else {
                    log.info("Server socket closed");
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             OutputStream out = socket.getOutputStream()
        ) {
            String requestLine = reader.readLine();
            if (StringUtils.isBlank(requestLine)) {
                return;
            }

            log.debug("Incoming request: {}", requestLine);

            String[] parts = requestLine.split(" ");
            if (parts.length < 3) {
                responseWriter.writeBadRequest(out, "Malformed request line");
                return;
            }

            String method = parts[0];
            String rawTarget = parts[1];

            while (true) {
                String header = reader.readLine();
                if (StringUtils.isBlank(header)) break;
            }

            if (!"GET".equalsIgnoreCase(method)) {
                responseWriter.writeMethodNotAllowed(out, "Only GET is supported");
                return;
            }

            URI uri;
            try {
                uri = new URI(rawTarget);
            } catch (URISyntaxException _) {
                responseWriter.writeBadRequest(out, "Invalid URI");
                return;
            }

            if (!"/filePaths".equals(uri.getPath())) {
                responseWriter.writeNotFound(out, "Endpoint not found");
                return;
            }

            Map<String, String> queryParams = splitQuery(uri.getRawQuery());
            String searchParam = queryParams.get("search");

            if (StringUtils.isBlank(searchParam)) {
                responseWriter.writeBadRequest(out, "Missing parameter 'search'");
                return;
            }

            searchParam = URLDecoder.decode(searchParam, StandardCharsets.UTF_8);

            List<String> results = threadSafeSearchIndex.search(searchParam);

            responseWriter.writeJson(out, results);

        } catch (IOException e) {
            log.error("Error in handleClient()", e);
        }
    }

    private Map<String, String> splitQuery(String query) {
        if (StringUtils.isBlank(query)) return Map.of();

        return Arrays.stream(query.split("&"))
                .map(param -> param.split("=", 2))
                .collect(Collectors.toMap(
                        arr -> arr[0],
                        arr -> arr.length > 1 ? arr[1] : ""
                ));
    }

    @Override
    public void close() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            log.error("Error closing server", e);
        }
    }
}
