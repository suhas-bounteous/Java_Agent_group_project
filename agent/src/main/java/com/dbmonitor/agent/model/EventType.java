package com.dbmonitor.agent.model;

/**
 * Classifies the database interaction that produced a {@link DbEvent}.
 *
 * <p>Each constant carries a human-readable {@code displayName} suitable for
 * log messages, dashboards, and serialised payloads sent to the collector.
 */
public enum EventType {

    /** A SELECT (or any read-only) statement executed via {@code Statement.executeQuery}. */
    QUERY_EXECUTE("query.execute"),

    /** A DML/DDL statement executed via {@code Statement.executeUpdate} or
     *  {@code PreparedStatement.executeUpdate}. */
    UPDATE_EXECUTE("update.execute"),

    /** One or more statements dispatched via {@code Statement.executeBatch} /
     *  {@code PreparedStatement.executeBatch}. */
    BATCH_EXECUTE("batch.execute"),

    /** A new physical or logical connection was obtained from the driver or pool. */
    CONNECTION_OPEN("connection.open"),

    /** A connection was returned to the pool or physically closed. */
    CONNECTION_CLOSE("connection.close"),

    /** {@code Connection.commit()} was called successfully. */
    TRANSACTION_COMMIT("transaction.commit"),

    /** {@code Connection.rollback()} was called. */
    TRANSACTION_ROLLBACK("transaction.rollback"),

    /** Any SQL execution that terminated with a {@link java.sql.SQLException}. */
    QUERY_ERROR("query.error");

    // -------------------------------------------------------------------------

    private final String displayName;

    EventType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the dot-separated display name used in metrics labels and JSON output.
     *
     * @return non-null, non-empty display name
     */
    public String getDisplayName() {
        return displayName;
    }
}
