package com.dbmonitor.agent.agent;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.instrumentation.AgentTransformer;
import com.dbmonitor.agent.logging.AgentLogger;
import com.dbmonitor.agent.model.AgentMetadata;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import com.dbmonitor.agent.sender.HttpMetricSender;
import com.dbmonitor.agent.sender.MetricSender;

import java.lang.instrument.Instrumentation;

/**
 * Constructs and wires together the full agent pipeline and holds the singleton
 * references for the lifetime of the JVM.
 *
 * <h3>Component graph</h3>
 * <pre>
 *   AgentMetadata (identity)
 *       │
 *       ├─ HttpMetricSender   (HTTP transport → collector)
 *       │         │
 *       └─ AsyncEventQueue ◄──── JDBC interceptors offer() events here
 *                 │
 *       AgentTransformer  (ByteBuddy wiring, sets static fields on Advice classes)
 * </pre>
 *
 * <h3>Thread safety</h3>
 * <p>Both static fields are {@code volatile}.  {@link #initialize} is called
 * exactly once at agent startup from the JVM agent thread; {@link #shutdown}
 * is called from the shutdown-hook thread.  The {@code volatile} keyword
 * ensures the hook thread sees the values written by the startup thread without
 * requiring additional synchronisation.
 *
 * <h3>Idempotent shutdown</h3>
 * <p>{@link #shutdown} nulls out the static fields before closing the queue so
 * that repeated calls (e.g. from a test harness) are safe and produce no
 * double-close or NPE.
 */
public final class AgentBootstrap {

    /**
     * The single event queue that all ByteBuddy advice classes write captured
     * {@link com.dbmonitor.agent.model.DbEvent}s into.
     */
    private static volatile AsyncEventQueue eventQueue;

    /**
     * The ByteBuddy agent transformer — held here to prevent the reference from
     * being garbage-collected (the transformer registers itself with the JVM
     * instrumentation, but we keep our own reference for cleanliness).
     */
    private static volatile AgentTransformer transformer;

    private AgentBootstrap() {
        // Not instantiable — all methods are static.
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Builds and starts the complete agent pipeline.
     *
     * <ol>
     *   <li>Creates an {@link AgentMetadata} snapshot for this JVM instance.</li>
     *   <li>Creates an {@link HttpMetricSender} backed by the collector endpoint
     *       in {@code config}.</li>
     *   <li>Creates an {@link AsyncEventQueue} that periodically flushes events
     *       to the sender.</li>
     *   <li>Creates an {@link AgentTransformer} and calls {@code install()} to
     *       register the ByteBuddy class-file transformer with the JVM.</li>
     *   <li>Stores the queue and transformer in the static fields so that
     *       {@link #shutdown()} can clean them up later.</li>
     * </ol>
     *
     * @param instrumentation the JVM {@link Instrumentation} handle from
     *                        {@code premain} or {@code agentmain}
     * @param config          validated agent configuration
     */
    public static void initialize(Instrumentation instrumentation, AgentConfig config) {
        AgentLogger.info("db-monitor-agent: initialising pipeline for app '"
                + config.getAppName() + "' in environment '" + config.getEnvironment() + "'");

        // Step 1 — stable identity block attached to every metric payload.
        AgentMetadata metadata = AgentMetadata.create(
                config.getAppName(), config.getEnvironment());

        // Step 2 — HTTP transport to the collector.
        MetricSender sender = new HttpMetricSender(config);

        // Step 3 — bounded, async event queue with background flush thread.
        AsyncEventQueue queue = new AsyncEventQueue(sender, metadata, config);

        // Step 4 — ByteBuddy instrumentation; also sets static fields on all
        //           Advice classes so intercepted calls have valid references.
        AgentTransformer agentTransformer = new AgentTransformer(instrumentation, config, queue);
        agentTransformer.install();

        // Step 5 — persist references for the shutdown path.
        eventQueue  = queue;
        transformer = agentTransformer;

        AgentLogger.info("db-monitor-agent: pipeline initialised"
                + " — agentId=" + metadata.agentId()
                + ", collectorEndpoint=" + config.getCollectorEndpoint()
                + ", reportingIntervalMs=" + config.getReportingIntervalMs());
    }

    /**
     * Shuts the agent down gracefully.
     *
     * <p>Nulls out the static fields before closing the queue so that:
     * <ul>
     *   <li>Any intercepted JDBC call that arrives during shutdown sees
     *       {@code null} in the queue reference and silently drops the event
     *       rather than trying to enqueue into a closing structure.</li>
     *   <li>Repeated calls to {@code shutdown()} are safe (idempotent).</li>
     * </ul>
     *
     * <p>Called from the JVM shutdown hook registered in
     * {@link AgentMain#premain} and from test teardown.
     */
    public static void shutdown() {
        AgentLogger.info("db-monitor-agent: shutdown initiated");

        // Capture and null out the references atomically-enough for our needs:
        // the shutdown-hook thread is the only writer at this point.
        AsyncEventQueue localQueue = eventQueue;
        eventQueue  = null;
        transformer = null;

        if (localQueue != null) {
            try {
                // close() flushes any buffered events to the collector before
                // stopping the background scheduler thread.
                localQueue.close();
            } catch (Exception e) {
                AgentLogger.error(
                        "db-monitor-agent: error while closing event queue during shutdown", e);
            }
        }

        AgentLogger.info("db-monitor-agent: shutdown complete");
    }
}
