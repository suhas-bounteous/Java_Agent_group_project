package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.model.MetricPayload;

/**
 * Strategy interface for forwarding a {@link MetricPayload} batch to a
 * downstream collector.
 *
 * <p>Implementations must be thread-safe: {@link AsyncEventQueue} may call
 * {@link #send} from a single background scheduler thread, but callers are
 * free to share one instance across threads.
 *
 * <p>Implementations must not propagate exceptions — any transmission error
 * should be handled internally (logged, retried, etc.) so that a transient
 * collector outage never crashes the daemon thread that drains the queue.
 */
public interface MetricSender {

    /**
     * Forwards the supplied payload to the configured destination.
     *
     * @param payload the batch of events to send; never {@code null}
     */
    void send(MetricPayload payload);
}
