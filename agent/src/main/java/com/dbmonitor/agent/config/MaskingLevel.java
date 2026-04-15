package com.dbmonitor.agent.config;

/**
 * Controls how much of the SQL text is captured and forwarded to the collector.
 *
 * <ul>
 *   <li>{@link #DISABLED} — no SQL text is captured; only timing and error metadata
 *       are recorded.</li>
 *   <li>{@link #METADATA_ONLY} — the SQL statement type and target table/object are
 *       inferred, but the literal query string is not stored.  The {@code sql} field
 *       on {@link com.dbmonitor.agent.model.DbEvent} will be {@code null}.</li>
 *   <li>{@link #FULL} — the complete SQL text is captured as-is.  Sensitive literal
 *       values (e.g. passwords in ad-hoc queries) are the caller's responsibility.</li>
 * </ul>
 */
public enum MaskingLevel {

    /** No SQL text recorded; timing and error metadata only. */
    DISABLED,

    /** Statement shape recorded, literal SQL text omitted. */
    METADATA_ONLY,

    /** Full SQL text captured. */
    FULL;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Case-insensitive lookup by name.  Returns {@link #FULL} for any value that
     * does not match a known constant, including {@code null} and blank strings.
     *
     * <p>Examples:
     * <pre>{@code
     *   MaskingLevel.fromString("full")          // FULL
     *   MaskingLevel.fromString("METADATA_ONLY") // METADATA_ONLY
     *   MaskingLevel.fromString("disabled")      // DISABLED
     *   MaskingLevel.fromString("garbage")       // FULL  (default)
     *   MaskingLevel.fromString(null)            // FULL  (default)
     * }</pre>
     *
     * @param value the raw string from configuration; may be {@code null}
     * @return the matching {@code MaskingLevel}, or {@link #FULL} if unrecognised
     */
    public static MaskingLevel fromString(String value) {
        if (value == null || value.isBlank()) {
            return FULL;
        }
        for (MaskingLevel level : values()) {
            if (level.name().equalsIgnoreCase(value.trim())) {
                return level;
            }
        }
        return FULL;
    }
}
