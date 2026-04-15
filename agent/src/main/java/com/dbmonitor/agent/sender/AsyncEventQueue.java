package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.logging.AgentLogger;
import com.dbmonitor.agent.model.AgentMetadata;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.MetricPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe, bounded event queue that accumulates {@link DbEvent}s captured
 * by the JDBC interceptors and flushes them as {@link MetricPayload} batches at
 * a configurable rate.
 *
 * <h3>Design notes</h3>
 * <ul>
 *   <li>All enqueuing is non-blocking: if the queue is at capacity,
 *       {@link #offer} returns {@code false} immediately and the dropped-event
 *       counter is incremented rather than blocking the application thread.</li>
 *   <li>Flushing runs on a single daemon {@link ScheduledExecutorService} thread
 *       at a fixed rate defined by {@code config.getReportingIntervalMs()}.
 *       The drain task is wrapped in a {@code try/catch(Throwable)} so that a
 *       serialisation or network error can never kill the scheduler thread.</li>
 *   <li>{@link #close()} shuts the executor down gracefully (5 s timeout), then
 *       performs a final drain to ensure no events are silently dropped on JVM
 *       shutdown.</li>
 * </ul>
 *
 * <p>Instances must be closed, ideally via try-with-resources or a JVM shutdown
 * hook.
 */
public final class AsyncEventQueue implements AutoCloseable {

    private static final String THREAD_NAME = "dbmonitor-queue-flusher";

    private final MetricSender                  sender;
    private final AgentMetadata                 metadata;
    private final AgentConfig                   config;
    private final LinkedBlockingQueue<DbEvent>  queue;
    private final ScheduledExecutorService      scheduler;
    private final AtomicLong                    droppedEventCount;

    /**
     * Constructs and starts an {@code AsyncEventQueue}.
     *
     * @param sender   the sender to which drain batches are forwarded
     * @param metadata agent identity block attached to every {@link MetricPayload}
     * @param config   validated agent configuration
     */
    public AsyncEventQueue(MetricSender sender, AgentMetadata metadata, AgentConfig config) {
        this.sender            = sender;
        this.metadata          = metadata;
        this.config            = config;
        this.queue             = new LinkedBlockingQueue<>(config.getMaxQueueSize());
        this.droppedEventCount = new AtomicLong(0L);
        this.scheduler         = Executors.newSingleThreadScheduledExecutor(daemonThreadFactory(THREAD_NAME));

        scheduler.scheduleAtFixedRate(
                this::drain,
                config.getReportingIntervalMs(),   // initial delay  — first flush after one full interval
                config.getReportingIntervalMs(),   // period
                TimeUnit.MILLISECONDS
        );

        AgentLogger.info(
                "db-monitor-agent: AsyncEventQueue started"
                + " (capacity=" + config.getMaxQueueSize()
                + ", intervalMs=" + config.getReportingIntervalMs() + ")"
        );
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Offers a {@link DbEvent} to the queue without blocking.
     *
     * <p>If the queue is at capacity the event is silently dropped and the
     * dropped-event counter is incremented.  The intercepted application thread
     * is never parked.
     *
     * @param event the event to enqueue; must not be {@code null}
     * @return {@code true} if the event was accepted; {@code false} if it was
     *         dropped because the queue was full
     */
    public boolean offer(DbEvent event) {
        boolean accepted = queue.offer(event);
        if (!accepted) {
            long total = droppedEventCount.incrementAndGet();
            if (total == 1 || total % 100 == 0) {
                // Rate-limit the dropped-event warning to avoid log spam.
                AgentLogger.warn(
                        "db-monitor-agent: event queue is full — dropped "
                        + total + " event(s) total; consider increasing maxQueueSize"
                );
            }
        }
        return accepted;
    }

    /**
     * Returns the cumulative count of events that have been silently dropped
     * because the queue was at capacity at the time of the {@link #offer} call.
     *
     * @return total dropped-event count since this queue was created
     */
    public long getDroppedEventCount() {
        return droppedEventCount.get();
    }

    // -------------------------------------------------------------------------
    // AutoCloseable
    // -------------------------------------------------------------------------

    /**
     * Shuts the background scheduler down and performs a final synchronous
     * drain so that events already in the queue are not silently discarded on
     * JVM shutdown.
     *
     * <p>Blocks for up to 5 seconds waiting for the scheduler thread to finish
     * its current drain task.  If it does not finish in time, the executor is
     * forcibly shut down and the final drain is still attempted.
     */
    @Override
    public void close() {
        AgentLogger.info("db-monitor-agent: AsyncEventQueue shutting down...");

        scheduler.shutdown();
        try {
            boolean terminated = scheduler.awaitTermination(5, TimeUnit.SECONDS);
            if (!terminated) {
                AgentLogger.warn("db-monitor-agent: scheduler did not stop cleanly within 5 s — forcing shutdown");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Final synchronous drain — sends any events still in the queue after
        // the scheduler thread has stopped.
        List<DbEvent> remaining = new ArrayList<>();
        queue.drainTo(remaining);
        if (!remaining.isEmpty()) {
            try {
                MetricPayload finalPayload = MetricPayload.of(metadata, remaining);
                sender.send(finalPayload);
                AgentLogger.info(
                        "db-monitor-agent: flushed final batch of "
                        + remaining.size() + " event(s) on close"
                );
            } catch (Throwable t) {
                AgentLogger.error(
                        "db-monitor-agent: error flushing final batch on close — "
                        + remaining.size() + " event(s) may have been lost",
                        t
                );
            }
        }

        long dropped = droppedEventCount.get();
        if (dropped > 0) {
            AgentLogger.warn("db-monitor-agent: " + dropped + " event(s) were dropped during this session");
        }

        AgentLogger.info("db-monitor-agent: AsyncEventQueue shut down");
    }

    // -------------------------------------------------------------------------
    // Internal drain
    // -------------------------------------------------------------------------

    /**
     * Drains all currently queued events into a local list and sends one batch.
     * This method is invoked by the scheduler thread at each flush interval.
     *
     * <p>The entire body is wrapped in {@code try/catch(Throwable)} so that no
     * exception (including {@link Error}) can kill the scheduler thread.
     */
    private void drain() {
        try {
            List<DbEvent> batch = new ArrayList<>();
            queue.drainTo(batch);

            if (batch.isEmpty()) {
                return;
            }

            AgentLogger.debug(
                    "db-monitor-agent: draining " + batch.size() + " event(s)"
            );

            MetricPayload payload = MetricPayload.of(metadata, batch);
            sender.send(payload);

        } catch (Throwable t) {
            // Catching Throwable is intentional: an OutOfMemoryError or any
            // other Error subtype must never silently kill the daemon thread.
            AgentLogger.error("db-monitor-agent: unexpected error in drain task", t);
        }
    }

    // -------------------------------------------------------------------------
    // Thread factory helper
    // -------------------------------------------------------------------------

    private static ThreadFactory daemonThreadFactory(String name) {
        return runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        };
    }
}
