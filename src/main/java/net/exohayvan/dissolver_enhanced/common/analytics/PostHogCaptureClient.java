package net.exohayvan.dissolver_enhanced.common.analytics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

public final class PostHogCaptureClient implements AutoCloseable {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final URI endpoint;
    private final String apiKey;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final BiConsumer<String, Throwable> warningLogger;

    public PostHogCaptureClient(String endpoint, String apiKey, BiConsumer<String, Throwable> warningLogger) {
        this.endpoint = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.warningLogger = warningLogger;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "dissolver-enhanced-posthog");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .executor(executor)
            .build();
    }

    public CompletableFuture<Void> capture(String event, String distinctId, Map<String, ?> properties) {
        Map<String, Object> eventProperties = new LinkedHashMap<>();
        if (properties != null) {
            eventProperties.putAll(properties);
        }
        eventProperties.put("distinct_id", distinctId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("api_key", apiKey);
        body.put("event", event);
        body.put("properties", eventProperties);
        body.put("timestamp", Instant.now().toString());

        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(PostHogJson.toJson(body)))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                int statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    logWarning("PostHog capture failed with HTTP " + statusCode + ".", null);
                }
            })
            .exceptionally(exception -> {
                logWarning("PostHog capture failed.", exception);
                return null;
            });
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private void logWarning(String message, Throwable throwable) {
        if (warningLogger != null) {
            warningLogger.accept(message, throwable);
        }
    }

}
