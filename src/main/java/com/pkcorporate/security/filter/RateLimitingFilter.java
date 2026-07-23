package com.pkcorporate.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Step 5c — Fix LIMIT_MAP memory leak + ObjectMapper allocation.
 * <p>
 * Original issue: {@code LIMIT_MAP} was a static {@code ConcurrentHashMap} that never evicted.
 * Every unique IP added a permanent entry — on a long-running Render free-tier instance
 * that grows without bound until OOM.
 * <p>
 * Fix: replaced with a Caffeine cache that automatically evicts buckets 30 minutes after
 * last access. A server handling 100 unique IPs/day will never accumulate more than a few
 * hundred entries in memory.
 * <p>
 * Also: {@code new ObjectMapper()} was being called inside the rejection path on every
 * rate-limited request. ObjectMapper construction is expensive (type cache, module scanning).
 * Hoisted to a shared static instance.
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    /**
     * Bounded, auto-expiring rate-limit map.
     * expireAfterAccess(30, MINUTES) means an IP bucket is discarded after 30 minutes
     * of inactivity — no unbounded growth, no explicit eviction thread needed.
     */
    private static final Cache<String, TokenBucket> LIMIT_MAP = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterAccess(30, TimeUnit.MINUTES)
            .build();

    /** Shared instance — ObjectMapper is thread-safe once constructed. */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static class TokenBucket {
        private final long capacity;
        private final long refillPeriodMs;
        private final long refillAmount;
        private double tokens;
        private long lastRefillTime;

        public TokenBucket(long capacity, long refillPeriodMs, long refillAmount) {
            this.capacity = capacity;
            this.refillPeriodMs = refillPeriodMs;
            this.refillAmount = refillAmount;
            this.tokens = capacity;
            this.lastRefillTime = System.currentTimeMillis();
        }

        public synchronized boolean tryConsume() {
            refill();
            if (tokens >= 1.0) {
                tokens -= 1.0;
                return true;
            }
            return false;
        }

        public synchronized long getSecondsToWait() {
            if (tokens >= 1.0) return 0;
            long now = System.currentTimeMillis();
            long nextRefillTime = lastRefillTime + refillPeriodMs;
            long waitMs = nextRefillTime - now;
            return Math.max(1, waitMs / 1000);
        }

        private void refill() {
            long now = System.currentTimeMillis();
            long timePassed = now - lastRefillTime;
            if (timePassed >= refillPeriodMs) {
                double refills = (double) timePassed / refillPeriodMs;
                tokens = Math.min(capacity, tokens + refills * refillAmount);
                lastRefillTime = now;
            }
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String ip = getClientIP(request);
        String user = request.getHeader("X-User-ID");

        // Bypass rate limiting for local development and local testing
        if (ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1") || ip.equalsIgnoreCase("localhost")) {
            filterChain.doFilter(request, response);
            return;
        }

        String limitKey;
        long capacity;
        long periodMs;

        if (path.contains("/auth/")) {
            // Auth endpoints: 5 requests per 15 minutes per IP
            limitKey = "auth:" + ip;
            capacity = 5;
            periodMs = TimeUnit.MINUTES.toMillis(15);
        } else if (path.contains("/upload") || path.contains("/upload-logo") || path.contains("/upload-references")) {
            // File upload endpoints: 5 requests per minute per IP
            limitKey = "upload:" + ip;
            capacity = 5;
            periodMs = TimeUnit.MINUTES.toMillis(1);
        } else if (path.contains("/ai/")) {
            // AI / LLM proxy endpoints: 10 requests per minute per user (IP fallback)
            limitKey = "ai:" + (user != null ? user : ip);
            capacity = 10;
            periodMs = TimeUnit.MINUTES.toMillis(1);
        } else {
            // General API routes: 60 requests per minute per IP
            limitKey = "general:" + ip;
            capacity = 60;
            periodMs = TimeUnit.MINUTES.toMillis(1);
        }

        final long cap = capacity;
        final long period = periodMs;
        TokenBucket bucket = LIMIT_MAP.get(limitKey, k -> new TokenBucket(cap, period, cap));

        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            long waitSeconds = bucket.getSecondsToWait();
            response.setHeader("Retry-After", String.valueOf(waitSeconds));

            Map<String, Object> errorDetails = Map.of(
                "success", false,
                "status", HttpStatus.TOO_MANY_REQUESTS.value(),
                "error", "Too Many Requests",
                "message", "Too many requests. Please wait " + waitSeconds + " seconds before trying again."
            );

            // Use shared ObjectMapper instance — not new ObjectMapper() per rejection
            OBJECT_MAPPER.writeValue(response.getOutputStream(), errorDetails);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isBlank()) {
            return request.getRemoteAddr();
        }
        // Use the LAST IP in the chain — the one appended by the trusted reverse proxy.
        // The first IP is client-supplied and can be easily spoofed.
        String[] ips = xfHeader.split(",");
        if (ips.length == 0) {
            return request.getRemoteAddr();
        }
        String clientIP = ips[ips.length - 1].trim();
        if (clientIP.isBlank()) {
            return request.getRemoteAddr();
        }
        return clientIP;
    }
}
