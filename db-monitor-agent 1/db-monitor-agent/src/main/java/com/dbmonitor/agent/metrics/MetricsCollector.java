package com.dbmonitor.agent.metrics;

import com.dbmonitor.agent.config.AgentConfig;

import java.util.concurrent.atomic.AtomicLong;

public class MetricsCollector {

    private static final AtomicLong totalQueries = new AtomicLong();
    private static final AtomicLong totalUpdates = new AtomicLong();
    private static final AtomicLong totalErrors = new AtomicLong();
    private static final AtomicLong totalLatencyNs = new AtomicLong();

    private static final AtomicLong commits = new AtomicLong();
    private static final AtomicLong rollbacks = new AtomicLong();
    private static final AtomicLong activeConnections = new AtomicLong();
    private static final AtomicLong slowQueries = new AtomicLong();

    // ---- Recording methods ----

    public static void recordQuery(long durationNs, boolean success, String type) {

        if ("QUERY".equals(type)) {
            totalQueries.incrementAndGet();
        } else if ("UPDATE".equals(type)) {
            totalUpdates.incrementAndGet();
        } else if ("BATCH".equals(type)) {
            totalUpdates.incrementAndGet(); // treat batch as update
        }

        totalLatencyNs.addAndGet(durationNs);

        if (!success) {
            totalErrors.incrementAndGet();
        }

        if (durationNs > AgentConfig.SLOW_QUERY_THRESHOLD_NS) {
            slowQueries.incrementAndGet();
        }
    }

    public static void recordTransaction(String type, boolean success) {
        if ("COMMIT".equals(type)) {
            commits.incrementAndGet();
        } else if ("ROLLBACK".equals(type)) {
            rollbacks.incrementAndGet();
        }

        if (!success) {
            totalErrors.incrementAndGet();
        }
    }

    // ---- Snapshot + Reset ----

    public static MetricsSnapshot snapshotAndReset() {

        MetricsSnapshot snapshot = new MetricsSnapshot();

        snapshot.setTotalQueries(totalQueries.getAndSet(0));
        snapshot.setTotalUpdates(totalUpdates.getAndSet(0));
        snapshot.setTotalErrors(totalErrors.getAndSet(0));
        snapshot.setTotalLatencyNs(totalLatencyNs.getAndSet(0));
        snapshot.setCommits(commits.getAndSet(0));
        snapshot.setRollbacks(rollbacks.getAndSet(0));
        snapshot.setActiveConnections(activeConnections.get());
        snapshot.setSlowQueries(slowQueries.getAndSet(0));


        return snapshot;
    }
    public static void connectionOpened() {
        activeConnections.incrementAndGet();
    }

    public static void connectionClosed() {
        activeConnections.decrementAndGet();
    }
}