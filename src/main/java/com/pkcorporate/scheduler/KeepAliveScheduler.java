package com.pkcorporate.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Self-ping scheduler to prevent Render free-tier from sleeping.
 *
 * Render spins down free services after ~15 minutes of inactivity.
 * This scheduler pings the server's own /ping endpoint every 10 minutes
 * to simulate activity, keeping the instance warm.
 *
 * Skips pinging between 12:00 AM and 7:00 AM IST to conserve the
 * 750 free-tier hours per month (only one service = ~720h needed).
 */
@Component
public class KeepAliveScheduler {

    private static final Logger log = LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${RENDER_EXTERNAL_URL:}")
    private String renderExternalUrl;

    @Value("${server.port:9090}")
    private int serverPort;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Runs every 10 minutes (600,000 ms).
     * Skips during night hours (00:00–07:00 IST) to save Render hours.
     */
    @Scheduled(fixedRate = 600_000, initialDelay = 60_000)
    public void keepAlive() {
        // Skip during night hours (12 AM - 7 AM IST)
        LocalTime now = LocalTime.now(ZoneId.of("Asia/Kolkata"));
        if (now.isBefore(LocalTime.of(7, 0))) {
            log.debug("[KeepAlive] Skipping — night hours ({} IST)", now);
            return;
        }

        String url = buildPingUrl();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(15))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[KeepAlive] Pinged {} — HTTP {}", url, response.statusCode());
        } catch (Exception e) {
            log.warn("[KeepAlive] Failed to ping {}: {}", url, e.getMessage());
        }
    }

    /**
     * Builds the ping URL.
     * Uses RENDER_EXTERNAL_URL (set automatically by Render) if available,
     * otherwise falls back to localhost.
     */
    private String buildPingUrl() {
        if (renderExternalUrl != null && !renderExternalUrl.isBlank()) {
            // Render sets this env var automatically on deployed services
            return renderExternalUrl + "/api/ping";
        }
        return "http://localhost:" + serverPort + "/api/ping";
    }
}
