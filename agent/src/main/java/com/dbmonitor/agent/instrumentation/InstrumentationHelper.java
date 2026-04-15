package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.MaskingLevel;
import com.dbmonitor.agent.masking.SqlMasker;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;

/**
 * Pure-function helper that contains all business logic for converting raw
 * interception data into {@link DbEvent} instances.
 *
 * <p>Keeping this logic out of the {@code @Advice} classes has two benefits:
 * <ol>
 *   <li>The helper is fully testable with plain JUnit 5 — no ByteBuddy class
 *       loading, no agent attachment required.</li>
 *   <li>The {@code @Advice} methods remain minimal, reducing the size of inlined
 *       bytecode injected into every instrumented class.</li>
 * </ol>
 *
 * <p>All methods are {@code public static}; this class is never instantiated.
 */
public final class InstrumentationHelper {

    /**
     * Regex that matches the credential block in a JDBC URL of the form
     * {@code jdbc:scheme://user:password@host:port/db}.
     * The pattern captures everything between {@code //} and the last {@code @}
     * (inclusive) so that the replacement retains only {@code //}.
     */
    private static final String CREDENTIAL_PATTERN = "//[^@]*@";

    private InstrumentationHelper() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Event builders
    // -------------------------------------------------------------------------

    /**
     * Builds a {@link DbEvent} for a statement execution (any of
     * {@code execute}, {@code executeQuery}, {@code executeUpdate},
     * {@code executeBatch}).
     *
     * @param sql          raw SQL string from the JDBC call; may be {@code null}
     * @param durationMs   wall-clock execution time in milliseconds
     * @param success      {@code true} if no exception was thrown
     * @param errorMsg     exception message when {@code success} is {@code false};
     *                     ignored otherwise
     * @param maskingLevel controls how much of the SQL text is captured
     * @param dbUrl        sanitised JDBC URL of the target database
     * @return a fully-populated {@code DbEvent}
     */
    public static DbEvent buildQueryEvent(
            String sql,
            long durationMs,
            boolean success,
            String errorMsg,
            MaskingLevel maskingLevel,
            String dbUrl) {

        String maskedSql = SqlMasker.mask(sql, maskingLevel);

        if (success) {
            return DbEvent.success(EventType.QUERY_EXECUTE, maskedSql, durationMs, dbUrl);
        } else {
            return DbEvent.failure(EventType.QUERY_ERROR, maskedSql, durationMs, errorMsg, dbUrl);
        }
    }

    /**
     * Builds a {@link DbEvent} for a connection lifecycle operation
     * ({@link EventType#CONNECTION_OPEN} or {@link EventType#CONNECTION_CLOSE}).
     *
     * @param type       {@code CONNECTION_OPEN} or {@code CONNECTION_CLOSE}
     * @param durationMs wall-clock time for the operation in milliseconds
     * @param dbUrl      sanitised JDBC URL of the connection
     * @return a fully-populated {@code DbEvent} with {@code sql = null}
     */
    public static DbEvent buildConnectionEvent(EventType type, long durationMs, String dbUrl) {
        return DbEvent.success(type, null, durationMs, dbUrl);
    }

    /**
     * Builds a {@link DbEvent} for a transaction boundary operation
     * ({@link EventType#TRANSACTION_COMMIT} or
     * {@link EventType#TRANSACTION_ROLLBACK}).
     *
     * <p>Transaction events carry no SQL text and use {@code "n/a"} as the
     * database URL because the URL is not readily available at the commit/rollback
     * call site without additional overhead.
     *
     * @param type       {@code TRANSACTION_COMMIT} or {@code TRANSACTION_ROLLBACK}
     * @param durationMs wall-clock time for the commit or rollback in milliseconds
     * @return a fully-populated {@code DbEvent} with {@code sql = null}
     *         and {@code dbUrl = "n/a"}
     */
    public static DbEvent buildTransactionEvent(EventType type, long durationMs) {
        return DbEvent.success(type, null, durationMs, "n/a");
    }

    // -------------------------------------------------------------------------
    // Threshold check
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the query duration meets or exceeds the configured
     * slow-query threshold.
     *
     * <p>A threshold of {@code 0} means every query is considered slow (useful
     * for debugging).
     *
     * @param durationMs  observed wall-clock execution time in milliseconds
     * @param thresholdMs configured slow-query threshold in milliseconds
     * @return {@code true} if {@code durationMs >= thresholdMs}
     */
    public static boolean isSlowQuery(long durationMs, long thresholdMs) {
        return durationMs >= thresholdMs;
    }

    // -------------------------------------------------------------------------
    // URL sanitisation
    // -------------------------------------------------------------------------

    /**
     * Strips user credentials from a JDBC URL so that the URL can be safely
     * stored in telemetry without leaking secrets.
     *
     * <p>Handles URLs of the form
     * {@code jdbc:scheme://user:password@host:port/db} or
     * {@code jdbc:scheme://user@host:port/db} by removing everything between
     * {@code //} and the last {@code @} (inclusive).  URLs that contain no
     * {@code @} character are returned unchanged.
     *
     * <p>Examples:
     * <pre>{@code
     *   sanitizeDbUrl("jdbc:mysql://alice:s3cr3t@db.example.com:3306/mydb")
     *     → "jdbc:mysql://db.example.com:3306/mydb"
     *
     *   sanitizeDbUrl("jdbc:h2:mem:testdb")
     *     → "jdbc:h2:mem:testdb"   (no credentials present)
     *
     *   sanitizeDbUrl(null) → "unknown"
     * }</pre>
     *
     * @param rawUrl the raw JDBC URL; may be {@code null}
     * @return sanitised URL, or {@code "unknown"} if {@code rawUrl} is
     *         {@code null}
     */
    public static String sanitizeDbUrl(String rawUrl) {
        if (rawUrl == null) {
            return "unknown";
        }
        // Only apply the credential-stripping replacement when an '@' is present;
        // this avoids touching URLs that already have no credentials.
        if (rawUrl.contains("@")) {
            return rawUrl.replaceAll(CREDENTIAL_PATTERN, "//");
        }
        return rawUrl;
    }
}
