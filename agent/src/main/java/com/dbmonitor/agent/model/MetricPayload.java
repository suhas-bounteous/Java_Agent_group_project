package com.dbmonitor.agent.model;

import java.util.List;

/**
 * Batch payload sent to the remote collector on every flush cycle.
 *
 * <p>The event list is defensively copied with {@link List#copyOf} so that
 * mutations to the original list after construction do not affect the payload
 * in-flight.
 *
 * @param metadata               agent identity block, constant for the JVM's
 *                               lifetime
 * @param events                 immutable snapshot of the events captured
 *                               since the last flush
 * @param batchTimestampEpochMs  epoch-millisecond timestamp recorded when this
 *                               batch was assembled
 */
public record MetricPayload(
        AgentMetadata metadata,
        List<DbEvent> events,
        long batchTimestampEpochMs
) {

    /**
     * Compact canonical constructor — ensures the stored event list is always
     * immutable regardless of the {@link List} implementation passed by callers.
     */
    public MetricPayload {
        events = List.copyOf(events);
    }

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Assembles a {@link MetricPayload} from the given metadata and event list,
     * stamping the current wall-clock time as the batch timestamp.
     *
     * <p>The supplied {@code events} list is defensively copied; callers may
     * safely clear or reuse the list after this method returns.
     *
     * @param metadata agent identity block
     * @param events   events collected since the last flush (may be empty,
     *                 must not be {@code null})
     * @return a new, immutable {@code MetricPayload}
     */
    public static MetricPayload of(AgentMetadata metadata, List<DbEvent> events) {
        return new MetricPayload(metadata, events, System.currentTimeMillis());
    }
}
