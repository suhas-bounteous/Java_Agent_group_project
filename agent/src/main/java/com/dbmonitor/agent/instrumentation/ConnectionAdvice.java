package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.sql.Connection;

/**
 * Byte Buddy {@link Advice} applied to {@code DataSource.getConnection()}.
 *
 * <p>Captures a {@link EventType#CONNECTION_OPEN} event each time a connection
 * is successfully obtained from a data source.  The DB URL is extracted from the
 * freshly-acquired {@link Connection}'s metadata so that each event carries the
 * actual URL used (helpful when a pool manages connections to multiple databases).
 *
 * <h3>Byte Buddy constraints honoured</h3>
 * <ul>
 *   <li>All {@code @Advice} methods are {@code public static}.</li>
 *   <li>Shared state lives in {@code static volatile} fields set by
 *       {@link AgentTransformer} before instrumentation is active.</li>
 *   <li>The entire {@link #onExit} body is wrapped in
 *       {@code try/catch(Throwable)}.</li>
 * </ul>
 */
public class ConnectionAdvice {

    // -------------------------------------------------------------------------
    // Static holder fields — written once by AgentTransformer
    // -------------------------------------------------------------------------

    /** The event queue to which captured events are offered. */
    static volatile AsyncEventQueue queue;

    /** The agent configuration used at capture time. */
    static volatile AgentConfig config;

    // -------------------------------------------------------------------------
    // Advice methods
    // -------------------------------------------------------------------------

    /**
     * Called <em>before</em> {@code DataSource.getConnection()}.
     * Records the start time for the connection-acquisition operation.
     */
    @Advice.OnMethodEnter
    public static void onEnter() {
        TimingContext.start();
    }

    /**
     * Called <em>after</em> {@code DataSource.getConnection()}, whether it
     * returned a connection or threw.
     *
     * @param connection the {@link Connection} returned by the data source, or
     *                   {@code null} if the method threw an exception
     * @param thrown     the exception thrown by {@code getConnection()}, or
     *                   {@code null} if it completed normally
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = true) Object connection,
            @Advice.Thrown Throwable thrown) {

        try {
            AsyncEventQueue localQueue  = queue;
            AgentConfig     localConfig = config;

            if (localQueue == null || localConfig == null) {
                TimingContext.clear();
                return;
            }

            long durationMs = TimingContext.elapsedMs();

            // If getConnection() threw, there is no connection to inspect.
            // We still record the event so slow/failing connection acquisitions
            // are visible in the telemetry.
            if (thrown != null) {
                // No URL available when the method failed — use "unknown".
                DbEvent event = InstrumentationHelper.buildConnectionEvent(
                        EventType.CONNECTION_OPEN, durationMs, "unknown");
                localQueue.offer(event);
                return;
            }

            // Extract the sanitised DB URL from the returned Connection.
            String dbUrl = "unknown";
            try {
                if (connection instanceof Connection) {
                    String rawUrl = ((Connection) connection).getMetaData().getURL();
                    dbUrl = InstrumentationHelper.sanitizeDbUrl(rawUrl);
                }
            } catch (Throwable ignored) {
                // getMetaData() may fail — fall back to "unknown".
            }

            DbEvent event = InstrumentationHelper.buildConnectionEvent(
                    EventType.CONNECTION_OPEN, durationMs, dbUrl);
            localQueue.offer(event);

        } catch (Throwable ignored) {
            // Instrumentation must never affect the host application.
        }
    }
}
