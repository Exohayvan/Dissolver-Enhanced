package net.exohayvan.dissolver_enhanced.common.values;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class DefaultEmcValueUpdateMonitor {
    public static final String UPDATE_MESSAGE = "Dissolver Enhanced had a Default EMC Values update from GitHub. Restart to apply the changes.";
    public static final URI DEFAULT_VALUES_URI = URI.create("https://raw.githubusercontent.com/Exohayvan/Dissolver-Enhanced/refs/heads/main/emc-values/defaults.yaml");

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);
    private static final long CHECK_INTERVAL_MINUTES = 5;

    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static final AtomicBoolean UPDATE_AVAILABLE = new AtomicBoolean(false);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .connectTimeout(REQUEST_TIMEOUT)
        .build();

    private DefaultEmcValueUpdateMonitor() {
    }

    public static void start(Path localDefaultValuesFile, Consumer<String> infoLogger, BiConsumer<String, Exception> warningLogger) {
        Objects.requireNonNull(localDefaultValuesFile, "localDefaultValuesFile");
        Objects.requireNonNull(infoLogger, "infoLogger");
        Objects.requireNonNull(warningLogger, "warningLogger");

        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "dissolver-default-emc-values-updater");
            thread.setDaemon(true);
            return thread;
        });

        executor.scheduleWithFixedDelay(
            () -> checkForUpdate(localDefaultValuesFile, infoLogger, warningLogger),
            0,
            CHECK_INTERVAL_MINUTES,
            TimeUnit.MINUTES
        );
    }

    public static boolean hasUpdateAvailable() {
        return UPDATE_AVAILABLE.get();
    }

    public static void checkForUpdate(Path localDefaultValuesFile, Consumer<String> infoLogger, BiConsumer<String, Exception> warningLogger) {
        try {
            String remoteYaml = fetchRemoteYaml();
            String desiredContent = DefaultEmcValues.defaultFileContent(remoteYaml);
            String currentContent = Files.exists(localDefaultValuesFile)
                ? Files.readString(localDefaultValuesFile, StandardCharsets.UTF_8)
                : "";

            if (currentContent.equals(desiredContent)) {
                return;
            }

            Files.createDirectories(localDefaultValuesFile.getParent());
            Files.writeString(localDefaultValuesFile, desiredContent, StandardCharsets.UTF_8);
            UPDATE_AVAILABLE.set(true);
            infoLogger.accept(UPDATE_MESSAGE);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            warningLogger.accept("Interrupted while checking GitHub for Dissolver Enhanced default EMC value updates.", e);
        } catch (IOException e) {
            warningLogger.accept("Unable to check GitHub for Dissolver Enhanced default EMC value updates.", e);
        }
    }

    private static String fetchRemoteYaml() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(DEFAULT_VALUES_URI)
            .timeout(REQUEST_TIMEOUT)
            .GET()
            .build();
        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("GitHub returned HTTP " + response.statusCode() + " for " + DEFAULT_VALUES_URI);
        }

        return response.body();
    }
}
