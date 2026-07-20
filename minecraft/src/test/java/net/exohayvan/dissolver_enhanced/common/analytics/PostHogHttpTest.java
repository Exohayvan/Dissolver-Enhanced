package net.exohayvan.dissolver_enhanced.common.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

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

        List<String> messages = new ArrayList<>();
        List<Throwable> throwables = new ArrayList<>();
        captureWithServer(server, messages, throwables);

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

    @Test
    void preservesOperationSpecificWarningsForTransportFailures() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> exchange.close());

        List<String> messages = new ArrayList<>();
        List<Throwable> throwables = new ArrayList<>();
        captureWithServer(server, messages, throwables);

        assertEquals(
            List.of(
                "PostHog capture failed.",
                "PostHog exception capture failed."
            ),
            messages
        );
        assertEquals(2, throwables.size());
        assertNotNull(throwables.get(0));
        assertNotNull(throwables.get(1));
    }

    private static void captureWithServer(
        HttpServer server,
        List<String> messages,
        List<Throwable> throwables
    ) {
        server.start();
        try {
            captureWithBothClients(
                "http://127.0.0.1:" + server.getAddress().getPort() + "/",
                messages,
                throwables
            );
        } finally {
            server.stop(0);
        }
    }

    private static void captureWithBothClients(
        String endpoint,
        List<String> messages,
        List<Throwable> throwables
    ) {
        BiConsumer<String, Throwable> warningLogger = (message, throwable) -> {
            messages.add(message);
            throwables.add(throwable);
        };
        try (
            PostHogCaptureClient capture = new PostHogCaptureClient(endpoint, "token", warningLogger);
            PostHogErrorReporter reporter = new PostHogErrorReporter(
                endpoint,
                "token",
                "player",
                "net.exohayvan",
                Map::of,
                warningLogger
            )
        ) {
            capture.capture("event", "player", Map.of()).join();
            reporter.capture(inAppFailure(), true).join();
        }
    }

    private static RuntimeException inAppFailure() {
        RuntimeException failure = new RuntimeException("boom");
        failure.setStackTrace(new StackTraceElement[] {
            new StackTraceElement("net.exohayvan.Test", "run", "Test.java", 1)
        });
        return failure;
    }
}
