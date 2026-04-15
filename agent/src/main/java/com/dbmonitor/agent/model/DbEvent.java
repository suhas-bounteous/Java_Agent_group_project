package com.dbmonitor.agent.model;

/**
 * Immutable snapshot of a single database interaction captured by the agent's
 * JDBC interceptors.
 *
 * <p>Instances are created exclusively through the two static factory methods
 * {@link #success} and {@link #failure}; direct record construction is
 * intentionally left package-private by design (all callers inside this
 * package may use the canonical constructor, but external callers should
 * prefer the factories for clarity).
 *
 * @param eventType        the category of database operation
 * @param sql              the SQL text — may be {@code null} when capture mode
 *                         is {@code METADATA_ONLY} or {@code DISABLED}
 * @param durationMs       wall-clock execution time in milliseconds
 * @param success          {@code true} if the operation completed without error
 * @param errorMessage     the exception message when {@code success} is
 *                         {@code false}; {@code null} otherwise
 * @param timestampEpochMs epoch-millisecond timestamp recorded at capture time
 * @param dbUrl            sanitised JDBC URL (no credentials)
 */
public record DbEvent(
        EventType eventType,
        String sql,
        long durationMs,
        boolean success,
        String errorMessage,
        long timestampEpochMs,
        String dbUrl
) {

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates a {@link DbEvent} representing a successfully completed operation.
     *
     * @param type       the category of database operation
     * @param sql        the SQL text (may be {@code null})
     * @param durationMs wall-clock execution time in milliseconds
     * @param dbUrl      sanitised JDBC URL
     * @return a new {@code DbEvent} with {@code success = true} and no error message
     */
    public static DbEvent success(EventType type, String sql, long durationMs, String dbUrl) {
        return new DbEvent(
                type,
                sql,
                durationMs,
                true,
                null,
                System.currentTimeMillis(),
                dbUrl
        );
    }

    /**
     * Creates a {@link DbEvent} representing an operation that terminated with
     * an error.
     *
     * @param type         the category of database operation
     * @param sql          the SQL text (may be {@code null})
     * @param durationMs   wall-clock execution time in milliseconds (up to the
     *                     point of failure)
     * @param errorMessage the exception message; {@code null} is normalised to
     *                     an empty string so callers never receive a null error
     *                     field on a failed event
     * @param dbUrl        sanitised JDBC URL
     * @return a new {@code DbEvent} with {@code success = false}
     */
    public static DbEvent failure(
            EventType type,
            String sql,
            long durationMs,
            String errorMessage,
            String dbUrl
    ) {
        return new DbEvent(
                type,
                sql,
                durationMs,
                false,
                errorMessage != null ? errorMessage : "",
                System.currentTimeMillis(),
                dbUrl
        );
    }
}
