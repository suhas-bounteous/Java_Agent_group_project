package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.model.MetricPayload;

/**
 * A {@link MetricSender} that silently discards every payload it receives.
 *
 * <p>Used as the default sender when the agent is disabled
 * ({@code dbmonitor.enabled=false}) or when a real sender cannot be
 * constructed, avoiding null-checks throughout the rest of the code.
 *
 * <p>The singleton {@link #INSTANCE} is safe to share across threads because
 * this class holds no mutable state.
 */
public final class NoOpMetricSender implements MetricSender {

    /**
     * Shared singleton — use this rather than constructing a new instance.
     */
    public static final NoOpMetricSender INSTANCE = new NoOpMetricSender();

    /**
     * Private constructor; callers must use {@link #INSTANCE}.
     */
    private NoOpMetricSender() {}

    /**
     * Does nothing.  The payload is silently discarded.
     *
     * @param payload ignored
     */
    @Override
    public void send(MetricPayload payload) {
        // intentional no-op
    }
}
