package net.exohayvan.dissolver_enhanced.common.analytics;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public final class PostHogErrorReporter implements AutoCloseable {
    private static final String EXCEPTION_EVENT = "$exception";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final URI endpoint;
    private final String token;
    private final String distinctId;
    private final String inAppPackagePrefix;
    private final Supplier<Map<String, Object>> baseProperties;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final BiConsumer<String, Throwable> warningLogger;

    public PostHogErrorReporter(
        String endpoint,
        String token,
        String distinctId,
        String inAppPackagePrefix,
        Supplier<Map<String, Object>> baseProperties,
        BiConsumer<String, Throwable> warningLogger
    ) {
        this.endpoint = URI.create(Objects.requireNonNull(endpoint, "endpoint"));
        this.token = Objects.requireNonNull(token, "token");
        this.distinctId = Objects.requireNonNull(distinctId, "distinctId");
        this.inAppPackagePrefix = Objects.requireNonNull(inAppPackagePrefix, "inAppPackagePrefix");
        this.baseProperties = baseProperties;
        this.warningLogger = warningLogger;
        this.executor = Executors.newSingleThreadExecutor(task -> {
            Thread thread = new Thread(task, "dissolver-enhanced-posthog-errors");
            thread.setDaemon(true);
            return thread;
        });
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(REQUEST_TIMEOUT)
            .executor(executor)
            .build();
    }

    public CompletableFuture<Void> capture(Throwable throwable, boolean handled) {
        if (!hasInAppFrame(throwable)) {
            return CompletableFuture.completedFuture(null);
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        if (baseProperties != null) {
            properties.putAll(baseProperties.get());
        }
        properties.put("distinct_id", distinctId);
        properties.put("$exception_list", List.of(exceptionObject(throwable, handled, inAppPackagePrefix)));
        properties.put("$exception_fingerprint", fingerprint(throwable));
        properties.put("$exception_message", exceptionMessage(throwable));
        properties.put("$exception_type", throwable.getClass().getName());
        properties.put("$exception_stack_trace_raw", rawStackTrace(throwable));
        properties.put("exception_handled", handled);
        properties.put("exception_causes", exceptionCauses(throwable));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", token);
        body.put("event", EXCEPTION_EVENT);
        body.put("properties", properties);
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
                    logWarning("PostHog exception capture failed with HTTP " + statusCode + ".", null);
                }
            })
            .exceptionally(exception -> {
                logWarning("PostHog exception capture failed.", exception);
                return null;
            });
    }

    @Override
    public void close() {
        executor.shutdown();
    }

    private boolean hasInAppFrame(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            for (StackTraceElement element : current.getStackTrace()) {
                if (isInAppFrame(element, inAppPackagePrefix)) {
                    return true;
                }
            }
            current = current.getCause();
        }

        return false;
    }

    private static Map<String, Object> exceptionObject(Throwable throwable, boolean handled, String inAppPackagePrefix) {
        Map<String, Object> exception = new LinkedHashMap<>();
        exception.put("type", throwable.getClass().getName());
        exception.put("value", exceptionMessage(throwable));
        exception.put("mechanism", mechanism(handled));
        exception.put("stacktrace", stacktrace(throwable, inAppPackagePrefix));
        return exception;
    }

    private static Map<String, Object> mechanism(boolean handled) {
        Map<String, Object> mechanism = new LinkedHashMap<>();
        mechanism.put("handled", handled);
        mechanism.put("synthetic", false);
        return mechanism;
    }

    private static Map<String, Object> stacktrace(Throwable throwable, String inAppPackagePrefix) {
        Map<String, Object> stacktrace = new LinkedHashMap<>();
        stacktrace.put("type", "raw");
        stacktrace.put("frames", frames(throwable, inAppPackagePrefix));
        return stacktrace;
    }

    private static List<Map<String, Object>> frames(Throwable throwable, String inAppPackagePrefix) {
        List<Map<String, Object>> frames = new ArrayList<>();
        for (StackTraceElement element : throwable.getStackTrace()) {
            Map<String, Object> frame = new LinkedHashMap<>();
            frame.put("platform", "custom");
            frame.put("lang", "java");
            frame.put("function", element.getMethodName());
            frame.put("filename", element.getFileName());
            frame.put("lineno", element.getLineNumber());
            frame.put("module", element.getClassName());
            frame.put("resolved", true);
            frame.put("in_app", isInAppFrame(element, inAppPackagePrefix));
            frames.add(frame);
        }
        return frames;
    }

    private static String exceptionMessage(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.toString() : throwable.getMessage();
    }

    private static String rawStackTrace(Throwable throwable) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        return writer.toString();
    }

    private static List<Map<String, Object>> exceptionCauses(Throwable throwable) {
        List<Map<String, Object>> causes = new ArrayList<>();
        Throwable current = throwable.getCause();
        while (current != null) {
            Map<String, Object> cause = new LinkedHashMap<>();
            cause.put("type", current.getClass().getName());
            cause.put("message", exceptionMessage(current));
            causes.add(cause);
            current = current.getCause();
        }
        return causes;
    }

    private static boolean isInAppFrame(StackTraceElement element, String inAppPackagePrefix) {
        return element.getClassName().startsWith(inAppPackagePrefix);
    }

    private static String fingerprint(Throwable throwable) {
        StackTraceElement[] stackTrace = throwable.getStackTrace();
        String topFrame = stackTrace.length == 0 ? "unknown" : stackTrace[0].toString();
        String rawFingerprint = throwable.getClass().getName() + ":" + topFrame;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawFingerprint.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(rawFingerprint.hashCode());
        }
    }

    private void logWarning(String message, Throwable throwable) {
        if (warningLogger != null) {
            warningLogger.accept(message, throwable);
        }
    }
}
