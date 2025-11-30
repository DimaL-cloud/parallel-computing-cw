package ua.dmytrolutsiuk.server;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class HttpResponseWriter {

    private final ObjectMapper objectMapper;

    public HttpResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void writeJson(OutputStream out, Object obj) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(obj);

        out.write((
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: " + body.length + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET\r\n" +
                        "Access-Control-Allow-Headers: Content-Type\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes(StandardCharsets.UTF_8));

        out.write(body);
        out.flush();
    }

    public void writeError(OutputStream out, int status, String statusText, String message) throws IOException {
        byte[] body = objectMapper.writeValueAsBytes(Map.of("error", message));

        out.write((
                "HTTP/1.1 " + status + " " + statusText + "\r\n" +
                        "Content-Type: application/json; charset=utf-8\r\n" +
                        "Content-Length: " + body.length + "\r\n" +
                        "Access-Control-Allow-Origin: *\r\n" +
                        "Access-Control-Allow-Methods: GET\r\n" +
                        "Access-Control-Allow-Headers: Content-Type\r\n" +
                        "Connection: close\r\n\r\n"
        ).getBytes(StandardCharsets.UTF_8));

        out.write(body);
        out.flush();
    }

    public void writeBadRequest(OutputStream out, String message) throws IOException {
        writeError(out, 400, "Bad Request", message);
    }

    public void writeNotFound(OutputStream out, String message) throws IOException {
        writeError(out, 404, "Not Found", message);
    }

    public void writeMethodNotAllowed(OutputStream out, String message) throws IOException {
        writeError(out, 405, "Method Not Allowed", message);
    }
}
