package net.exohayvan.dissolver_enhanced.common.analytics;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

final class PostHogHttp {
    private PostHogHttp() {
    }

    static CompletableFuture<Void> postJson(
        HttpClient httpClient,
        URI endpoint,
        Duration timeout,
        Map<String, Object> body,
        String operation,
        BiConsumer<String, Throwable> warningLogger
    ) {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
            .timeout(timeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(PostHogJson.toJson(body)))
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            .thenAccept(response -> {
                int statusCode = response.statusCode();
                if (statusCode < 200 || statusCode >= 300) {
                    logWarning(warningLogger, operation + " failed with HTTP " + statusCode + ".", null);
                }
            })
            .exceptionally(exception -> {
                logWarning(warningLogger, operation + " failed.", exception);
                return null;
            });
    }

    private static void logWarning(
        BiConsumer<String, Throwable> warningLogger,
        String message,
        Throwable throwable
    ) {
        if (warningLogger != null) {
            warningLogger.accept(message, throwable);
        }
    }
}
