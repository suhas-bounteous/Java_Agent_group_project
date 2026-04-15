package com.dbmonitor.agent.masking;

import com.dbmonitor.agent.config.MaskingLevel;

import java.util.regex.Pattern;

/**
 * Masks sensitive literal values out of SQL strings before they are stored or
 * forwarded to the collector.
 *
 * <h3>Masking pipeline (applied in order for {@link MaskingLevel#FULL})</h3>
 * <ol>
 *   <li>Single-quoted string literals → {@code ?}</li>
 *   <li>IN-clause value lists → {@code IN (?)}</li>
 *   <li>Standalone integer and decimal numeric literals → {@code ?}</li>
 *   <li>Runs of whitespace → single space</li>
 *   <li>Leading/trailing whitespace trimmed</li>
 * </ol>
 *
 * <p>The ordering is intentional: string literals are removed first so that
 * digit sequences inside quoted strings are never matched by the numeric
 * pattern; IN-clause lists are collapsed before individual numbers so that the
 * single-{@code ?} form is established before generic numeric replacement runs.
 */
public final class SqlMasker {

    // -------------------------------------------------------------------------
    // Pre-compiled patterns (static final = compiled once at class load time)
    // -------------------------------------------------------------------------

    /**
     * Matches a single-quoted SQL string literal.
     * The {@code [^']*} inner group intentionally does not handle escaped
     * single-quotes (e.g. {@code ''}) — for the purposes of masking, any
     * content between outer quotes is considered a literal.
     */
    private static final Pattern PATTERN_STRING_LITERAL =
            Pattern.compile("'[^']*'");

    /**
     * Matches an SQL IN clause whose parenthesised list has not yet been masked.
     * The {@code [^)]+} body handles any mix of values (numbers, quoted strings
     * that have already been replaced with {@code ?}, commas, and spaces).
     * Case-insensitive so that {@code in}, {@code IN}, and {@code In} all match.
     */
    private static final Pattern PATTERN_IN_CLAUSE =
            Pattern.compile("\\bIN\\s*\\([^)]+\\)", Pattern.CASE_INSENSITIVE);

    /**
     * Matches a standalone integer or decimal numeric literal delimited by
     * word boundaries.  The word-boundary anchors prevent false matches inside
     * SQL identifiers such as {@code table1} or {@code col_42}.
     *
     * <p>The capturing group {@code (\\.\\d+)?} is non-capturing for replacement
     * purposes — {@code replaceAll} replaces the whole match regardless of groups.
     */
    private static final Pattern PATTERN_NUMERIC_LITERAL =
            Pattern.compile("\\b\\d+(\\.\\d+)?\\b");

    /**
     * Matches any run of one or more whitespace characters (spaces, tabs,
     * newlines, carriage returns) for normalisation.
     */
    private static final Pattern PATTERN_WHITESPACE =
            Pattern.compile("\\s+");

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    private SqlMasker() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Masks sensitive values in {@code sql} according to the supplied
     * {@link MaskingLevel}.
     *
     * <table border="1">
     *   <tr><th>Level</th><th>Return value</th></tr>
     *   <tr><td>{@code null} sql (any level)</td><td>{@code null}</td></tr>
     *   <tr><td>{@link MaskingLevel#DISABLED}</td><td>{@code null}</td></tr>
     *   <tr><td>{@link MaskingLevel#METADATA_ONLY}</td><td>{@code null}</td></tr>
     *   <tr><td>{@link MaskingLevel#FULL}</td><td>masked SQL string</td></tr>
     * </table>
     *
     * @param sql   the raw SQL string; may be {@code null}
     * @param level the masking strategy to apply; must not be {@code null}
     * @return the masked SQL, or {@code null} as documented above
     */
    public static String mask(String sql, MaskingLevel level) {
        if (sql == null) {
            return null;
        }
        if (level == MaskingLevel.DISABLED || level == MaskingLevel.METADATA_ONLY) {
            return null;
        }

        // FULL masking pipeline ---------------------------------------------------

        // Step 1: erase string literals so their content cannot be matched as
        //         numbers in step 3.
        String result = PATTERN_STRING_LITERAL.matcher(sql).replaceAll("?");

        // Step 2: collapse IN-clause value lists to a single placeholder.
        //         Must run before step 3 so that individual numbers inside
        //         IN (...) are handled in bulk rather than left as separate '?'.
        result = PATTERN_IN_CLAUSE.matcher(result).replaceAll("IN (?)");

        // Step 3: replace standalone integer and decimal literals.
        result = PATTERN_NUMERIC_LITERAL.matcher(result).replaceAll("?");

        // Step 4: normalise whitespace runs to a single space.
        result = PATTERN_WHITESPACE.matcher(result).replaceAll(" ");

        // Step 5: trim leading/trailing whitespace.
        return result.trim();
    }

    /**
     * Derives a stable "query fingerprint" from a masked SQL string.
     *
     * <p>The fingerprint is used to group distinct executions of logically
     * identical queries (same shape, different bound values) for aggregation and
     * slow-query reporting.  Two queries with different literal values but the
     * same structure will produce the same fingerprint after masking.
     *
     * <p>Transformation applied:
     * <ol>
     *   <li>Lowercase the entire string.</li>
     *   <li>Normalise whitespace runs to a single space.</li>
     *   <li>Trim leading/trailing whitespace.</li>
     * </ol>
     *
     * @param maskedSql the output of {@link #mask} for a {@link MaskingLevel#FULL}
     *                  call; may be {@code null}
     * @return the normalised fingerprint string, or {@code null} if
     *         {@code maskedSql} is {@code null}
     */
    public static String extractFingerprint(String maskedSql) {
        if (maskedSql == null) {
            return null;
        }
        return PATTERN_WHITESPACE
                .matcher(maskedSql.toLowerCase())
                .replaceAll(" ")
                .trim();
    }
}
