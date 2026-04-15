package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.MaskingLevel;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for {@link InstrumentationHelper}.
 *
 * <p>No Mockito, no ByteBuddy, no agent attachment — every test calls the
 * static helper methods directly and asserts the returned {@link DbEvent}
 * values.
 */
class InstrumentationHelperTest {

    private static final String SQL      = "SELECT * FROM users WHERE id = 42";
    private static final long   DURATION = 55L;
    private static final String DB_URL   = "jdbc:h2:mem:testdb";

    // =========================================================================
    // buildQueryEvent — success path
    // =========================================================================

    @Test
    void buildQueryEvent_success_returnsSuccessEvent() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        assertNotNull(event);
        assertTrue(event.success(), "Event must be marked as successful");
        assertNull(event.errorMessage(), "Successful event must have no error message");
    }

    @Test
    void buildQueryEvent_success_setsCorrectEventType() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        assertEquals(EventType.QUERY_EXECUTE, event.eventType(),
                "Successful query event must have QUERY_EXECUTE type");
    }

    @Test
    void buildQueryEvent_success_setsDuration() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        assertEquals(DURATION, event.durationMs());
    }

    @Test
    void buildQueryEvent_success_setsDbUrl() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        assertEquals(DB_URL, event.dbUrl());
    }

    // =========================================================================
    // buildQueryEvent — failure path
    // =========================================================================

    @Test
    void buildQueryEvent_failure_returnsFailureEvent() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, false, "Table not found", MaskingLevel.FULL, DB_URL);

        assertNotNull(event);
        assertFalse(event.success(), "Event must be marked as failed");
    }

    @Test
    void buildQueryEvent_failure_setsCorrectEventType() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, false, "ORA-00942", MaskingLevel.FULL, DB_URL);

        assertEquals(EventType.QUERY_ERROR, event.eventType(),
                "Failed query event must have QUERY_ERROR type");
    }

    @Test
    void buildQueryEvent_failure_setsErrorMessage() {
        String errorMsg = "Table 'users' doesn't exist";
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, false, errorMsg, MaskingLevel.FULL, DB_URL);

        assertEquals(errorMsg, event.errorMessage());
    }

    // =========================================================================
    // buildQueryEvent — SQL masking
    // =========================================================================

    @Test
    void buildQueryEvent_masksSQL_whenLevelIsFull() {
        String rawSql    = "SELECT * FROM users WHERE name = 'alice' AND id = 99";
        DbEvent event    = InstrumentationHelper.buildQueryEvent(
                rawSql, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        String maskedSql = event.sql();
        assertNotNull(maskedSql, "FULL masking must still produce a SQL string");
        assertFalse(maskedSql.contains("alice"),
                "String literal 'alice' must have been replaced by ?");
        assertFalse(maskedSql.contains("99"),
                "Numeric literal 99 must have been replaced by ?");
    }

    @Test
    void buildQueryEvent_masksSQL_numericOnly() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                "SELECT * FROM orders WHERE id = 42", DURATION, true, null,
                MaskingLevel.FULL, DB_URL);

        assertEquals("SELECT * FROM orders WHERE id = ?", event.sql());
    }

    @Test
    void buildQueryEvent_returnsNullSql_whenLevelIsMetadataOnly() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.METADATA_ONLY, DB_URL);

        assertNull(event.sql(),
                "METADATA_ONLY masking must store null for the SQL field");
    }

    @Test
    void buildQueryEvent_returnsNullSql_whenLevelIsDisabled() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                SQL, DURATION, true, null, MaskingLevel.DISABLED, DB_URL);

        assertNull(event.sql(),
                "DISABLED masking must store null for the SQL field");
    }

    @Test
    void buildQueryEvent_handlesNullSql() {
        DbEvent event = InstrumentationHelper.buildQueryEvent(
                null, DURATION, true, null, MaskingLevel.FULL, DB_URL);

        assertNotNull(event, "buildQueryEvent must not throw for null sql");
        assertNull(event.sql(), "Null input SQL must yield null masked SQL");
    }

    // =========================================================================
    // buildConnectionEvent
    // =========================================================================

    @Test
    void buildConnectionEvent_setsCorrectEventType() {
        DbEvent event = InstrumentationHelper.buildConnectionEvent(
                EventType.CONNECTION_OPEN, 12L, DB_URL);

        assertEquals(EventType.CONNECTION_OPEN, event.eventType());
    }

    @Test
    void buildConnectionEvent_connectionClose_setsCorrectEventType() {
        DbEvent event = InstrumentationHelper.buildConnectionEvent(
                EventType.CONNECTION_CLOSE, 5L, DB_URL);

        assertEquals(EventType.CONNECTION_CLOSE, event.eventType());
    }

    @Test
    void buildConnectionEvent_hasSqlNull() {
        DbEvent event = InstrumentationHelper.buildConnectionEvent(
                EventType.CONNECTION_OPEN, 12L, DB_URL);

        assertNull(event.sql(), "Connection events must carry no SQL text");
    }

    @Test
    void buildConnectionEvent_setsDbUrl() {
        DbEvent event = InstrumentationHelper.buildConnectionEvent(
                EventType.CONNECTION_OPEN, 12L, DB_URL);

        assertEquals(DB_URL, event.dbUrl());
    }

    @Test
    void buildConnectionEvent_isSuccessful() {
        DbEvent event = InstrumentationHelper.buildConnectionEvent(
                EventType.CONNECTION_OPEN, 12L, DB_URL);

        assertTrue(event.success());
    }

    // =========================================================================
    // buildTransactionEvent
    // =========================================================================

    @Test
    void buildTransactionEvent_commitType() {
        DbEvent event = InstrumentationHelper.buildTransactionEvent(
                EventType.TRANSACTION_COMMIT, 3L);

        assertEquals(EventType.TRANSACTION_COMMIT, event.eventType());
    }

    @Test
    void buildTransactionEvent_rollbackType() {
        DbEvent event = InstrumentationHelper.buildTransactionEvent(
                EventType.TRANSACTION_ROLLBACK, 2L);

        assertEquals(EventType.TRANSACTION_ROLLBACK, event.eventType());
    }

    @Test
    void buildTransactionEvent_dbUrlIsNa() {
        DbEvent event = InstrumentationHelper.buildTransactionEvent(
                EventType.TRANSACTION_COMMIT, 3L);

        assertEquals("n/a", event.dbUrl(),
                "Transaction events must carry 'n/a' as the DB URL");
    }

    @Test
    void buildTransactionEvent_hasSqlNull() {
        DbEvent event = InstrumentationHelper.buildTransactionEvent(
                EventType.TRANSACTION_COMMIT, 3L);

        assertNull(event.sql(), "Transaction events must carry no SQL text");
    }

    @Test
    void buildTransactionEvent_setsDuration() {
        DbEvent event = InstrumentationHelper.buildTransactionEvent(
                EventType.TRANSACTION_ROLLBACK, 8L);

        assertEquals(8L, event.durationMs());
    }

    // =========================================================================
    // isSlowQuery
    // =========================================================================

    @Test
    void isSlowQuery_returnsTrueWhenAboveThreshold() {
        assertTrue(InstrumentationHelper.isSlowQuery(1001L, 1000L),
                "Duration 1001 ms must exceed threshold 1000 ms");
    }

    @Test
    void isSlowQuery_returnsFalseWhenBelowThreshold() {
        assertFalse(InstrumentationHelper.isSlowQuery(999L, 1000L),
                "Duration 999 ms must not exceed threshold 1000 ms");
    }

    @Test
    void isSlowQuery_returnsTrueAtExactThreshold() {
        assertTrue(InstrumentationHelper.isSlowQuery(1000L, 1000L),
                "Duration equal to threshold must be considered slow");
    }

    @ParameterizedTest
    @CsvSource({
        "0,   0,   true",     // zero duration, zero threshold → slow
        "100, 100, true",     // equal → slow
        "50,  100, false",    // below → not slow
        "200, 100, true",     // above → slow
        "0,   1,   false"     // zero duration, positive threshold → not slow
    })
    void isSlowQuery_parameterised(long durationMs, long thresholdMs, boolean expected) {
        assertEquals(expected, InstrumentationHelper.isSlowQuery(durationMs, thresholdMs));
    }

    // =========================================================================
    // sanitizeDbUrl
    // =========================================================================

    @Test
    void sanitizeDbUrl_removesCredentials() {
        String input    = "jdbc:mysql://user:password@localhost:3306/db";
        String expected = "jdbc:mysql://localhost:3306/db";

        assertEquals(expected, InstrumentationHelper.sanitizeDbUrl(input));
    }

    @Test
    void sanitizeDbUrl_removesUsernameOnly() {
        String input    = "jdbc:mysql://alice@localhost:3306/mydb";
        String expected = "jdbc:mysql://localhost:3306/mydb";

        assertEquals(expected, InstrumentationHelper.sanitizeDbUrl(input));
    }

    @Test
    void sanitizeDbUrl_returnsUnchanged_whenNoCredentials() {
        String input = "jdbc:postgresql://localhost:5432/mydb";
        assertEquals(input, InstrumentationHelper.sanitizeDbUrl(input),
                "URL without credentials must be returned unchanged");
    }

    @Test
    void sanitizeDbUrl_returnsUnchanged_forH2InMemory() {
        String input = "jdbc:h2:mem:testdb";
        assertEquals(input, InstrumentationHelper.sanitizeDbUrl(input));
    }

    @Test
    void sanitizeDbUrl_returnsUnknown_whenNull() {
        assertEquals("unknown", InstrumentationHelper.sanitizeDbUrl(null),
                "Null URL must map to the sentinel string 'unknown'");
    }

    @ParameterizedTest
    @CsvSource({
        "jdbc:mysql://root:s3cr3t@db.prod.example.com:3306/orders, jdbc:mysql://db.prod.example.com:3306/orders",
        "jdbc:oracle:thin:scott/tiger@localhost:1521:xe,            jdbc:oracle:thin://localhost:1521:xe",
        "jdbc:postgresql://admin:pass@10.0.0.1:5432/analytics,     jdbc:postgresql://10.0.0.1:5432/analytics"
    })
    void sanitizeDbUrl_removesCredentials_parameterised(String input, String expected) {
        assertEquals(expected.trim(), InstrumentationHelper.sanitizeDbUrl(input.trim()));
    }
}
