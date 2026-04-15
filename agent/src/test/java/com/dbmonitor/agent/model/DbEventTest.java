package com.dbmonitor.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DbEventTest {

    private static final EventType TYPE    = EventType.QUERY_EXECUTE;
    private static final String    SQL     = "SELECT 1 FROM dual";
    private static final long      DURATION = 42L;
    private static final String    DB_URL  = "jdbc:h2:mem:testdb";
    private static final String    ERROR   = "Connection refused";

    // -------------------------------------------------------------------------
    // success() factory
    // -------------------------------------------------------------------------

    @Test
    void successFactory_setsSuccessTrue() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertTrue(event.success(), "success() factory must set success=true");
    }

    @Test
    void successFactory_durationIsRecorded() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertEquals(DURATION, event.durationMs(), "durationMs must equal the value passed to the factory");
    }

    @Test
    void successFactory_timestampIsRecent() {
        long before = System.currentTimeMillis();
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        long after = System.currentTimeMillis();

        assertTrue(
                event.timestampEpochMs() >= before && event.timestampEpochMs() <= after + 1000,
                "timestampEpochMs must be within 1000 ms of System.currentTimeMillis() at creation time"
        );
    }

    @Test
    void successFactory_errorMessageIsNull() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertNull(event.errorMessage(), "errorMessage must be null on a successful event");
    }

    @Test
    void successFactory_setsEventType() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertEquals(TYPE, event.eventType());
    }

    @Test
    void successFactory_setsSql() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertEquals(SQL, event.sql());
    }

    @Test
    void successFactory_setsDbUrl() {
        DbEvent event = DbEvent.success(TYPE, SQL, DURATION, DB_URL);
        assertEquals(DB_URL, event.dbUrl());
    }

    @Test
    void successFactory_sqlMayBeNull() {
        DbEvent event = DbEvent.success(TYPE, null, DURATION, DB_URL);
        assertNull(event.sql(), "sql field may be null when capture mode is METADATA_ONLY or DISABLED");
    }

    // -------------------------------------------------------------------------
    // failure() factory
    // -------------------------------------------------------------------------

    @Test
    void failureFactory_setsSuccessFalse() {
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, ERROR, DB_URL);
        assertFalse(event.success(), "failure() factory must set success=false");
    }

    @Test
    void failureFactory_setsErrorMessage() {
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, ERROR, DB_URL);
        assertEquals(ERROR, event.errorMessage(), "errorMessage must equal the value passed to failure()");
    }

    @Test
    void failureFactory_nullErrorMessageNormalisedToEmptyString() {
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, null, DB_URL);
        assertNotNull(event.errorMessage(), "null errorMessage must be normalised to empty string");
        assertEquals("", event.errorMessage());
    }

    @Test
    void failureFactory_durationIsRecorded() {
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, ERROR, DB_URL);
        assertEquals(DURATION, event.durationMs());
    }

    @Test
    void failureFactory_timestampIsRecent() {
        long before = System.currentTimeMillis();
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, ERROR, DB_URL);
        long after = System.currentTimeMillis();

        assertTrue(
                event.timestampEpochMs() >= before && event.timestampEpochMs() <= after + 1000,
                "timestampEpochMs must be within 1000 ms of System.currentTimeMillis() at creation time"
        );
    }

    @Test
    void failureFactory_setsEventType() {
        DbEvent event = DbEvent.failure(TYPE, SQL, DURATION, ERROR, DB_URL);
        assertEquals(TYPE, event.eventType());
    }

    // -------------------------------------------------------------------------
    // Record equality (structural equality from the compiler-generated equals)
    // -------------------------------------------------------------------------

    @Test
    void record_equality() {
        // Two records are equal iff all component values are equal.
        // Because timestampEpochMs is set via System.currentTimeMillis() inside
        // the factory, we cannot use the factories here — use the canonical
        // constructor directly to control the timestamp.
        long fixedTimestamp = 1_700_000_000_000L;

        DbEvent a = new DbEvent(TYPE, SQL, DURATION, true, null, fixedTimestamp, DB_URL);
        DbEvent b = new DbEvent(TYPE, SQL, DURATION, true, null, fixedTimestamp, DB_URL);

        assertEquals(a, b, "Two DbEvent records with identical fields must be equal");
        assertEquals(a.hashCode(), b.hashCode(), "Equal records must have the same hashCode");
    }

    @Test
    void record_inequalityOnDifferentSql() {
        long fixedTimestamp = 1_700_000_000_000L;

        DbEvent a = new DbEvent(TYPE, "SELECT 1", DURATION, true, null, fixedTimestamp, DB_URL);
        DbEvent b = new DbEvent(TYPE, "SELECT 2", DURATION, true, null, fixedTimestamp, DB_URL);

        assertNotEquals(a, b, "Records with different sql fields must not be equal");
    }
}
