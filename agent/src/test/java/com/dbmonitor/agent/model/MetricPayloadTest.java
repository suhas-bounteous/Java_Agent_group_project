package com.dbmonitor.agent.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MetricPayloadTest {

    private AgentMetadata metadata;
    private DbEvent       event;

    @BeforeEach
    void setUp() {
        metadata = AgentMetadata.create("test-app", "test");
        event    = DbEvent.success(EventType.QUERY_EXECUTE, "SELECT 1", 10L, "jdbc:h2:mem:test");
    }

    // -------------------------------------------------------------------------
    // of() factory
    // -------------------------------------------------------------------------

    @Test
    void of_wrapsMetadataAndEvents() {
        List<DbEvent> events  = List.of(event);
        MetricPayload payload = MetricPayload.of(metadata, events);

        assertSame(metadata, payload.metadata(), "metadata reference must be the same object");
        assertEquals(1, payload.events().size(), "events list must contain the single supplied event");
        assertEquals(event, payload.events().get(0), "the event in the payload must equal the supplied event");
    }

    @Test
    void of_eventListIsImmutable() {
        // Build a mutable list, hand it to the factory, then modify it.
        List<DbEvent> mutableList = new ArrayList<>();
        mutableList.add(event);

        MetricPayload payload = MetricPayload.of(metadata, mutableList);

        // Mutate the original list after payload creation.
        DbEvent extraEvent = DbEvent.success(EventType.UPDATE_EXECUTE, "UPDATE t SET x=1", 5L, "jdbc:h2:mem:test");
        mutableList.add(extraEvent);

        assertEquals(
                1,
                payload.events().size(),
                "Modifying the original list after calling of() must not affect the payload's event list"
        );
    }

    @Test
    void of_payloadEventsListThrowsOnMutation() {
        MetricPayload payload = MetricPayload.of(metadata, List.of(event));

        assertThrows(
                UnsupportedOperationException.class,
                () -> payload.events().add(event),
                "The events list exposed by the payload must be unmodifiable"
        );
    }

    @Test
    void of_batchTimestampIsRecent() {
        long before = System.currentTimeMillis();
        MetricPayload payload = MetricPayload.of(metadata, List.of(event));
        long after = System.currentTimeMillis();

        assertTrue(
                payload.batchTimestampEpochMs() >= before && payload.batchTimestampEpochMs() <= after + 1000,
                "batchTimestampEpochMs must be within 1000 ms of System.currentTimeMillis() at creation time"
        );
    }

    @Test
    void of_acceptsEmptyEventList() {
        MetricPayload payload = MetricPayload.of(metadata, List.of());

        assertNotNull(payload.events(), "events list must not be null even when no events are supplied");
        assertTrue(payload.events().isEmpty(), "events list must be empty when an empty list is supplied");
    }

    @Test
    void of_multipleEventsPreservedInOrder() {
        DbEvent e1 = DbEvent.success(EventType.QUERY_EXECUTE,  "SELECT 1",       10L, "jdbc:h2:mem:test");
        DbEvent e2 = DbEvent.success(EventType.UPDATE_EXECUTE, "UPDATE t SET x=1", 5L, "jdbc:h2:mem:test");
        DbEvent e3 = DbEvent.failure(EventType.QUERY_ERROR,    "SELECT bad",     12L, "ORA-00942", "jdbc:h2:mem:test");

        MetricPayload payload = MetricPayload.of(metadata, List.of(e1, e2, e3));

        assertEquals(3, payload.events().size());
        assertEquals(e1, payload.events().get(0));
        assertEquals(e2, payload.events().get(1));
        assertEquals(e3, payload.events().get(2));
    }
}
