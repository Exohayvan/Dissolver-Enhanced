package net.exohayvan.dissolver_enhanced.common.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

class PostHogHttpTest {
    @Test
    void preservesOperationSpecificWarningsForNonSuccessResponses() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();

        String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        List<String> messages = new ArrayList<>();
        List<Throwable> throwables = new ArrayList<>();

        try (
            PostHogCaptureClient capture = new PostHogCaptureClient(
                endpoint,
                "token",
                (message, throwable) -> {
                    messages.add(message);
                    throwables.add(throwable);
                }
            );
            PostHogErrorReporter reporter = new PostHogErrorReporter(
                endpoint,
                "token",
                "player",
                "net.exohayvan",
                Map::of,
                (message, throwable) -> {
                    messages.add(message);
                    throwables.add(throwable);
                }
            )
        ) {
            capture.capture("event", "player", Map.of()).join();

            RuntimeException failure = new RuntimeException("boom");
            failure.setStackTrace(new StackTraceElement[] {
                new StackTraceElement("net.exohayvan.Test", "run", "Test.java", 1)
            });
            reporter.capture(failure, true).join();
        } finally {
            server.stop(0);
        }

        assertEquals(
            List.of(
                "PostHog capture failed with HTTP 503.",
                "PostHog exception capture failed with HTTP 503."
            ),
            messages
        );
        assertEquals(2, throwables.size());
        assertNull(throwables.get(0));
        assertNull(throwables.get(1));
    }
}
