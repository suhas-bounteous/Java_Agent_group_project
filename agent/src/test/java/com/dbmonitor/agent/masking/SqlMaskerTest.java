package com.dbmonitor.agent.masking;

import com.dbmonitor.agent.config.MaskingLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlMaskerTest {

    // =========================================================================
    // DISABLED and METADATA_ONLY levels
    // =========================================================================

    @Test
    void mask_returnsNull_whenLevelIsDisabled() {
        String result = SqlMasker.mask("SELECT * FROM users WHERE id = 1", MaskingLevel.DISABLED);
        assertNull(result, "DISABLED level must always return null");
    }

    @Test
    void mask_returnsNull_whenLevelIsMetadataOnly() {
        String result = SqlMasker.mask("SELECT * FROM users WHERE id = 1", MaskingLevel.METADATA_ONLY);
        assertNull(result, "METADATA_ONLY level must always return null — no SQL text is captured");
    }

    /**
     * Regardless of the masking level, a {@code null} SQL input must produce
     * a {@code null} output without throwing.
     */
    @ParameterizedTest
    @EnumSource(MaskingLevel.class)
    void mask_handlesNullSql(MaskingLevel level) {
        assertNull(SqlMasker.mask(null, level),
                "null sql must return null for every MaskingLevel");
    }

    // =========================================================================
    // FULL masking — string literals
    // =========================================================================

    @Test
    void mask_replacesStringLiterals() {
        String input    = "SELECT * FROM users WHERE name = 'alice'";
        String expected = "SELECT * FROM users WHERE name = ?";
        assertEquals(expected, SqlMasker.mask(input, MaskingLevel.FULL));
    }

    @ParameterizedTest
    @CsvSource({
        // input SQL,                                                expected output
        "SELECT 1 WHERE x = 'hello',                               SELECT ? WHERE x = ?",
        "INSERT INTO t VALUES ('foo', 'bar'),                       INSERT INTO t VALUES (?, ?)",
        "UPDATE t SET col = 'new value' WHERE col = 'old value',   UPDATE t SET col = ? WHERE col = ?",
        "SELECT * FROM t WHERE s = '',                             SELECT * FROM t WHERE s = ?"
    })
    void mask_replacesStringLiterals_parameterised(String input, String expected) {
        assertEquals(expected.trim(), SqlMasker.mask(input.trim(), MaskingLevel.FULL));
    }

    // =========================================================================
    // FULL masking — numeric literals
    // =========================================================================

    @Test
    void mask_replacesNumericLiterals() {
        String input    = "SELECT * FROM orders WHERE id = 42";
        String expected = "SELECT * FROM orders WHERE id = ?";
        assertEquals(expected, SqlMasker.mask(input, MaskingLevel.FULL));
    }

    @Test
    void mask_replacesDecimalLiterals() {
        String input    = "SELECT * FROM products WHERE price > 9.99";
        String expected = "SELECT * FROM products WHERE price > ?";
        assertEquals(expected, SqlMasker.mask(input, MaskingLevel.FULL));
    }

    @ParameterizedTest
    @CsvSource({
        // input SQL,                                               expected output
        "SELECT * FROM t WHERE n = 0,                              SELECT * FROM t WHERE n = ?",
        "SELECT * FROM t WHERE n = 100,                            SELECT * FROM t WHERE n = ?",
        "SELECT * FROM t WHERE v = 3.14,                           SELECT * FROM t WHERE v = ?",
        "SELECT * FROM t WHERE a = 1 AND b = 2,                    SELECT * FROM t WHERE a = ? AND b = ?",
        "SELECT * FROM t LIMIT 10 OFFSET 20,                       SELECT * FROM t LIMIT ? OFFSET ?"
    })
    void mask_replacesNumericLiterals_parameterised(String input, String expected) {
        assertEquals(expected.trim(), SqlMasker.mask(input.trim(), MaskingLevel.FULL));
    }

    // =========================================================================
    // FULL masking — IN clauses
    // =========================================================================

    @Test
    void mask_handlesInClause() {
        String input  = "SELECT * FROM t WHERE id IN (1, 2, 3)";
        String result = SqlMasker.mask(input, MaskingLevel.FULL);
        assertNotNull(result);
        assertTrue(result.contains("IN (?)"),
                "IN clause with multiple values must be collapsed to 'IN (?)', got: " + result);
    }

    @ParameterizedTest
    @CsvSource({
        // input SQL,                                                     expected output
        "SELECT * FROM t WHERE id IN (1),                                SELECT * FROM t WHERE id IN (?)",
        "SELECT * FROM t WHERE id IN (1, 2, 3),                          SELECT * FROM t WHERE id IN (?)",
        "SELECT * FROM t WHERE code IN ('A', 'B', 'C'),                  SELECT * FROM t WHERE code IN (?)",
        "SELECT * FROM t WHERE id IN (10,20,30),                         SELECT * FROM t WHERE id IN (?)",
        "SELECT * FROM t WHERE a IN (1,2) AND b IN (3,4,5),              SELECT * FROM t WHERE a IN (?) AND b IN (?)"
    })
    void mask_handlesInClause_parameterised(String input, String expected) {
        assertEquals(expected.trim(), SqlMasker.mask(input.trim(), MaskingLevel.FULL));
    }

    @ParameterizedTest
    @CsvSource({
        // lower-case 'in' must also be handled
        "select * from t where id in (1, 2, 3),  select * from t where id in (?)",
        "SELECT * FROM t WHERE id In (1, 2),     SELECT * FROM t WHERE id In (?)"
    })
    void mask_handlesInClause_caseInsensitive(String input, String expected) {
        // The masker normalises the IN keyword to upper-case in PATTERN_IN_CLAUSE
        // replacement; verify the replacement fires regardless of input case.
        String result = SqlMasker.mask(input.trim(), MaskingLevel.FULL);
        assertNotNull(result);
        assertTrue(result.toUpperCase().contains("IN (?)"),
                "Case-insensitive IN clause must be collapsed, got: " + result);
    }

    // =========================================================================
    // FULL masking — whitespace normalisation
    // =========================================================================

    @Test
    void mask_normalisesWhitespace() {
        String input    = "SELECT  *   FROM   users  WHERE   id  =  1";
        String expected = "SELECT * FROM users WHERE id = ?";
        assertEquals(expected, SqlMasker.mask(input, MaskingLevel.FULL));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "SELECT\t*\tFROM\tt",
        "SELECT\n*\nFROM\nt",
        "SELECT\r\n*\r\nFROM\r\nt",
        "SELECT   *   FROM   t"
    })
    void mask_normalisesVariousWhitespaceChars(String input) {
        String result = SqlMasker.mask(input, MaskingLevel.FULL);
        assertNotNull(result);
        assertFalse(result.contains("  "),
                "Result must not contain consecutive spaces, got: '" + result + "'");
    }

    // =========================================================================
    // FULL masking — edge cases
    // =========================================================================

    @Test
    void mask_handlesEmptyString() {
        String result = SqlMasker.mask("", MaskingLevel.FULL);
        // Empty input produces an empty (or blank) trimmed result.
        assertNotNull(result);
        assertEquals("", result, "Empty SQL must produce an empty masked string");
    }

    @Test
    void mask_handlesBlankString() {
        String result = SqlMasker.mask("   ", MaskingLevel.FULL);
        assertNotNull(result);
        assertEquals("", result, "All-whitespace SQL must produce an empty trimmed string");
    }

    @Test
    void mask_preservesSqlKeywordsAndIdentifiers() {
        // Table and column names that contain digits must NOT be mangled.
        String input    = "SELECT col1, col2 FROM table1 WHERE table1.id = 99";
        String result   = SqlMasker.mask(input, MaskingLevel.FULL);
        assertNotNull(result);
        assertTrue(result.contains("col1"),  "col1 identifier must be preserved");
        assertTrue(result.contains("col2"),  "col2 identifier must be preserved");
        assertTrue(result.contains("table1"), "table1 identifier must be preserved");
        assertFalse(result.contains("99"),   "literal 99 must have been replaced");
    }

    @Test
    void mask_mixedLiterals() {
        // Query with both string and numeric literals on the same line.
        String input    = "SELECT * FROM orders WHERE customer = 'bob' AND total > 100.50 AND status = 1";
        String result   = SqlMasker.mask(input, MaskingLevel.FULL);
        assertNotNull(result);
        assertFalse(result.contains("bob"),    "String literal 'bob' must be masked");
        assertFalse(result.contains("100.50"), "Decimal literal 100.50 must be masked");
        assertFalse(result.contains("1"),
                "Lone numeric literal at end of query must be masked (note: also checks no digit leaks)");
    }

    // =========================================================================
    // extractFingerprint
    // =========================================================================

    @Test
    void fingerprint_lowercasesAndNormalises() {
        String masked      = "SELECT * FROM users WHERE id = ?";
        String fingerprint = SqlMasker.extractFingerprint(masked);
        assertEquals("select * from users where id = ?", fingerprint,
                "Fingerprint must be fully lowercased");
    }

    @Test
    void fingerprint_normalisesWhitespace() {
        String maskedWithExtraSpaces = "SELECT  *  FROM  users  WHERE  id  =  ?";
        String fingerprint           = SqlMasker.extractFingerprint(maskedWithExtraSpaces);
        assertEquals("select * from users where id = ?", fingerprint);
    }

    @Test
    void fingerprint_trimsLeadingAndTrailingWhitespace() {
        String fingerprint = SqlMasker.extractFingerprint("  SELECT * FROM t WHERE id = ?  ");
        assertEquals("select * from t where id = ?", fingerprint);
    }

    @Test
    void fingerprint_returnsNull_whenInputIsNull() {
        assertNull(SqlMasker.extractFingerprint(null),
                "Null maskedSql must produce a null fingerprint");
    }

    @Test
    void fingerprint_twoIdenticalQueriesProduceSameFingerprint() {
        String sql = "SELECT * FROM users WHERE id = 1";

        String masked1      = SqlMasker.mask(sql, MaskingLevel.FULL);
        String masked2      = SqlMasker.mask(sql, MaskingLevel.FULL);
        String fingerprint1 = SqlMasker.extractFingerprint(masked1);
        String fingerprint2 = SqlMasker.extractFingerprint(masked2);

        assertEquals(fingerprint1, fingerprint2,
                "Masking the same SQL twice must yield the same fingerprint");
    }

    @Test
    void fingerprint_queriesWithDifferentValuesProduceSameFingerprint() {
        String query1 = "SELECT * FROM users WHERE id = 1";
        String query2 = "SELECT * FROM users WHERE id = 9999";
        String query3 = "SELECT * FROM users WHERE id = 42";

        String fp1 = SqlMasker.extractFingerprint(SqlMasker.mask(query1, MaskingLevel.FULL));
        String fp2 = SqlMasker.extractFingerprint(SqlMasker.mask(query2, MaskingLevel.FULL));
        String fp3 = SqlMasker.extractFingerprint(SqlMasker.mask(query3, MaskingLevel.FULL));

        assertEquals(fp1, fp2,
                "Queries differing only in bound values must produce the same fingerprint");
        assertEquals(fp1, fp3,
                "Queries differing only in bound values must produce the same fingerprint");
    }

    @Test
    void fingerprint_queriesWithDifferentStructuresProduceDifferentFingerprints() {
        String q1 = "SELECT * FROM users WHERE id = 1";
        String q2 = "SELECT * FROM orders WHERE id = 1";

        String fp1 = SqlMasker.extractFingerprint(SqlMasker.mask(q1, MaskingLevel.FULL));
        String fp2 = SqlMasker.extractFingerprint(SqlMasker.mask(q2, MaskingLevel.FULL));

        assertNotEquals(fp1, fp2,
                "Queries targeting different tables must produce different fingerprints");
    }

    // =========================================================================
    // Combined mask → fingerprint pipeline
    // =========================================================================

    @ParameterizedTest
    @CsvSource({
        // raw SQL,                                                expected fingerprint
        "SELECT * FROM t WHERE id = 1,                           select * from t where id = ?",
        "SELECT * FROM t WHERE name = 'alice',                   select * from t where name = ?",
        "SELECT * FROM t WHERE id IN (1, 2, 3),                  select * from t where id in (?)",
        "SELECT * FROM t WHERE v = 3.14,                         select * from t where v = ?",
        "SELECT   *   FROM   t   WHERE   id   =   7,             select * from t where id = ?"
    })
    void pipeline_mask_then_fingerprint(String rawSql, String expectedFingerprint) {
        String masked      = SqlMasker.mask(rawSql.trim(), MaskingLevel.FULL);
        String fingerprint = SqlMasker.extractFingerprint(masked);
        assertEquals(expectedFingerprint.trim(), fingerprint);
    }
}
