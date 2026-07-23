package com.pkcorporate.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.stream.Collectors;




@Slf4j
@Configuration
public class DatabaseFailFastConfig {

    @Value("${server.port:}")
    private String serverPort;

    @Value("${DB_URL:}")
    private String dbUrl;

    @Value("${DB_USERNAME:}")
    private String dbUsername;

    @Value("${DB_PASSWORD:}")
    private String dbPassword;

    // Spring profile(s)
    private final Environment environment;

    // Supabase pooler selection (optional):
    // direct | session_pooler | transaction_pooler
    // Default: direct
    @Value("${SUPABASE_CONNECTION_TYPE:direct}")
    private String supabaseConnectionType;

    // Optional tenant routing values used by Supabase when required.
    // If your chosen endpoint requires them, set them in Render.
    @Value("${SUPABASE_EXTERNAL_ID:}")
    private String supabaseExternalId;

    @Value("${SUPABASE_SNI_HOSTNAME:}")
    private String supabaseSniHostname;

    public DatabaseFailFastConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void verifyDatabaseConnectionBeforeHibernate() {
        // Step 4: Print required startup diagnostics (mask secrets)
        String[] activeProfiles = environment.getActiveProfiles();
        String activeProfilesStr = (activeProfiles == null || activeProfiles.length == 0)
                ? "<none>"
                : Arrays.stream(activeProfiles).collect(Collectors.joining(","));

        // Fail-fast with clear logs rather than letting Hibernate fail later.
        try {
            // Step 1: Verify port safety (Render port safety guard)
            if ("6543".equals(serverPort)) {
                throw new IllegalStateException("Invalid configuration: server.port must be ${PORT:9090} on Render; it must never be 6543.");
            }

            // Resolve DB_URL (may include supabase tenant routing params)
            String resolvedUrl = resolveJdbcUrl();

            // Validate JDBC URL
            if (resolvedUrl == null || resolvedUrl.isBlank()) {
                throw new IllegalArgumentException("DB_URL is empty. Set Render env var DB_URL to a full JDBC URL.");
            }
            if (!resolvedUrl.startsWith("jdbc:postgresql://")) {
                throw new IllegalArgumentException(
                        "DB_URL must start with jdbc:postgresql:// but got: " + maskJdbcUrl(resolvedUrl));
            }

            if (dbUsername == null || dbUsername.isBlank()) {
                throw new IllegalArgumentException("DB_USERNAME is empty. Set Render env var DB_USERNAME.");
            }
            if (dbPassword == null || dbPassword.isBlank()) {
                throw new IllegalArgumentException("DB_PASSWORD is empty. Set Render env var DB_PASSWORD.");
            }

            // Parse host/port/dbname from JDBC URL for logging (no secrets)
            ParsedJdbcInfo parsed = parseJdbcUrl(resolvedUrl);

            log.info(
                    "🚀 Starting Spring Boot diagnostics: server.port={}, spring.profiles.active={}, DB.host={}, DB.port={}, DB.database={}, DB.username={}, DB_URL(format)={}, connectionType={}, tenant(external_id_present={}, sni_hostname_present={})",
                    serverPort,
                    activeProfilesStr,
                    parsed.host == null ? "" : parsed.host,
                    parsed.port == null ? "" : String.valueOf(parsed.port),
                    parsed.database == null ? "" : parsed.database,
                    dbUsername,
                    maskJdbcUrl(resolvedUrl),
                    supabaseConnectionType,
                    !isBlank(supabaseExternalId),
                    !isBlank(supabaseSniHostname)
            );

            // Try a real connection.
            try (Connection conn = DriverManager.getConnection(resolvedUrl, dbUsername, dbPassword)) {
                boolean valid = conn.isValid(5);
                if (!valid) {
                    throw new SQLException("DB connection isValid(5) returned false.");
                }

                log.info("✅ DB connectivity check passed. Hibernate should be able to create the EntityManagerFactory.");
            }
        } catch (Exception e) {
            String resolvedUrl = null;
            try {
                resolvedUrl = resolveJdbcUrl();
            } catch (Exception ignored) {
                // ignore
            }

            String resolvedNoPassword = resolvedUrl == null ? null : maskJdbcUrlPreservingStructure(resolvedUrl);

            // Log complete exception chain (root cause) without secrets.
            log.error("❌ DB connectivity check failed before Hibernate startup: {}", e.toString());
            logExceptionChain(e);

            if (resolvedNoPassword != null) {
                log.error("Supabase/JDBC exact URL used (password/user masked) => {}", resolvedNoPassword);
            }

            String full = (e.toString() + " " + (e.getMessage() == null ? "" : e.getMessage())).toLowerCase();
            if (full.contains("enoidentifier") || full.contains("external_id") || full.contains("sni_hostname")) {
                log.error(
                        "Supabase rejected the connection because tenant routing identifiers are missing or the selected endpoint/pooler doesn't match the JDBC URL. Fix by setting DB_URL to the exact Supabase JDBC URL for Direct/Session Pooler/Transaction Pooler, and ensure required tenant params are present (external_id and/or sni_hostname).");
            }

            // Re-throw to stop Spring Boot startup early.
            throw (e instanceof RuntimeException re) ? (RuntimeException) e : new RuntimeException(e);
        }
    }

    private record ParsedJdbcInfo(String host, Integer port, String database) {}

    private ParsedJdbcInfo parseJdbcUrl(String jdbcUrl) {
        try {
            // Example Supabase JDBC format:
            // jdbc:postgresql://aws-xyz.pooler.supabase.com:6543/postgres?sslmode=...&...&external_id=...
            String working = jdbcUrl;
            String withoutPrefix = working.substring("jdbc:postgresql://".length());

            int qIdx = withoutPrefix.indexOf('?');
            String hostPortDb = qIdx >= 0 ? withoutPrefix.substring(0, qIdx) : withoutPrefix;

            // hostPortDb => host:port/dbname (or host/dbname)
            String hostPortPart;
            String dbPart;
            int slashIdx = hostPortDb.indexOf('/');
            if (slashIdx >= 0) {
                hostPortPart = hostPortDb.substring(0, slashIdx);
                dbPart = hostPortDb.substring(slashIdx + 1);
            } else {
                hostPortPart = hostPortDb;
                dbPart = null;
            }

            String host;
            Integer port;
            int colonIdx = hostPortPart.lastIndexOf(':');
            if (colonIdx >= 0) {
                host = hostPortPart.substring(0, colonIdx);
                String portStr = hostPortPart.substring(colonIdx + 1);
                port = portStr.isBlank() ? null : Integer.parseInt(portStr);
            } else {
                host = hostPortPart;
                port = null;
            }

            String db = (dbPart == null || dbPart.isBlank()) ? null : dbPart;
            return new ParsedJdbcInfo(host, port, db);
        } catch (Exception parseEx) {
            return new ParsedJdbcInfo(null, null, null);
        }
    }

    private void logExceptionChain(Throwable t) {
        Throwable curr = t;
        int depth = 0;
        while (curr != null && depth < 25) {
            log.error("↳ cause[{}]: {}", depth, curr.toString());
            if (curr.getMessage() != null) {
                log.error("   ↳ message[{}]: {}", depth, curr.getMessage());
            }
            curr = curr.getCause();
            depth++;
        }

        // Root cause marker
        Throwable root = getRootCause(t);
        if (root != null && root != t) {
            log.error("🧩 rootCause: {}", root.toString());
            if (root.getMessage() != null) {
                log.error("   ↳ rootCause message: {}", root.getMessage());
            }
        }
    }

    private Throwable getRootCause(Throwable t) {
        Throwable curr = t;
        Throwable prev = null;
        while (curr != null) {
            prev = curr;
            curr = curr.getCause();
        }
        return prev;
    }


    private String resolveJdbcUrl() {
        // If DB_URL is already a JDBC URL, we will optionally append Supabase tenant routing params if needed.
        // If DB_URL is not JDBC, we fail fast in verifyDatabaseConnectionBeforeHibernate().
        String url = dbUrl;
        if (url == null) return null;

        // Normalize type: direct/session_pooler/transaction_pooler
        String type = supabaseConnectionType == null ? "direct" : supabaseConnectionType.trim().toLowerCase();

        // We do not attempt to guess Supabase host/port here (that depends on your Render secrets).
        // Instead, we ensure required tenant routing params are present if your endpoint requires them.
        // If your Render DB_URL already encodes the right host/port, this will be enough.
        // If you need different host/port selection, set DB_URL to the right endpoint.

        // Determine whether we should append routing params
        boolean hasExternal = !isBlank(supabaseExternalId);
        boolean hasSni = !isBlank(supabaseSniHostname);

        if (!hasExternal && !hasSni) {
            // No tenant routing params; return URL as-is.
            // This will surface the exact Supabase error you reported, but earlier.
            return url;
        }

        StringBuilder sb = new StringBuilder(url);
        char joiner = url.contains("?") ? '&' : '?';

        if (hasExternal) {
            sb.append(joiner).append("external_id=").append(encodeQueryParam(supabaseExternalId));
            joiner = '&';
        }
        if (hasSni) {
            sb.append(joiner).append("sni_hostname=").append(encodeQueryParam(supabaseSniHostname));
        }

        String resolved = sb.toString();

        // Type is informational only for now; correct endpoint selection must be done via DB_URL.
        log.info("Supabase connection type requested={}, resolved JDBC URL with tenant params if present.", type);
        return resolved;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String encodeQueryParam(String s) {
        // Keep it simple to avoid adding dependencies. JDBC URL query param values should be URL-safe.
        // This is sufficient for typical Supabase identifiers.
        return s.replace(" ", "%20").replace("@", "%40").replace(":", "%3A").replace("/", "%2F");
    }

    private static String maskJdbcUrlPreservingStructure(String jdbcUrl) {
        // Step 7 helper: return JDBC URL with password/user masked, structure preserved.
        if (jdbcUrl == null) return null;
        String masked = jdbcUrl;
        masked = masked.replaceAll("(?i)(password=)([^&]*)", "$1***");
        masked = masked.replaceAll("(?i)(user=)([^&]*)", "$1***");
        // Some Supabase URLs may include creds via other params; keep this conservative.
        return masked;
    }

    // Existing helper used for Step 4 logs.
    private static String maskJdbcUrl(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        return jdbcUrl
                .replaceAll("(?i)(password=)([^&]*)", "$1***")
                .replaceAll("(?i)(user=)([^&]*)", "$1***");
    }

}

