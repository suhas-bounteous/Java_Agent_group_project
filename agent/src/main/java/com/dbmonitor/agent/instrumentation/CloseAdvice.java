package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.sql.Connection;

/**
 * Byte Buddy {@link Advice} applied to {@code Connection.close()}.
 *
 * <p>Emits a {@link EventType#CONNECTION_CLOSE} event after each call to
 * {@code close()}.  The DB URL is extracted from the {@code Connection}
 * instance's metadata in a best-effort manner — if the connection is already
 * invalidated when the exit advice runs, the URL falls back to
 * {@code "unknown"}.
 *
 * <h3>Byte Buddy constraints honoured</h3>
 * <ul>
 *   <li>All {@code @Advice} methods are {@code public static}.</li>
 *   <li>Shared state lives in {@code static volatile} fields set by
 *       {@link AgentTransformer}.</li>
 *   <li>The entire {@link #onExit} body is wrapped in
 *       {@code try/catch(Throwable)}.</li>
 * </ul>
 */
public class CloseAdvice {

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
     * Called <em>before</em> {@code Connection.close()}.
     * Records the start time for the close operation.
     */
    @Advice.OnMethodEnter
    public static void onEnter() {
        TimingContext.start();
    }

    /**
     * Called <em>after</em> {@code Connection.close()}, whether it completed
     * normally or threw.
     *
     * <p>Note: {@code close()} returns {@code void}, so there is no
     * {@code @Advice.Return} parameter.  The connection reference is obtained
     * via {@code @Advice.This}.  Because {@code close()} may have already
     * invalidated the connection by the time this method runs, the URL lookup
     * is wrapped in a dedicated {@code try/catch}.
     *
     * @param thisObj the {@link Connection} being closed ({@code this} inside
     *                the instrumented method)
     * @param thrown  the exception thrown by {@code close()}, or {@code null}
     *                if it completed normally
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object thisObj,
            @Advice.Thrown Throwable thrown) {

        try {
            AsyncEventQueue localQueue  = queue;
            AgentConfig     localConfig = config;

            if (localQueue == null || localConfig == null) {
                TimingContext.clear();
                return;
            }

            long durationMs = TimingContext.elapsedMs();

            // Best-effort URL extraction.  getMetaData() may throw if the
            // underlying connection is already gone — that is expected and fine.
            String dbUrl = "unknown";
            try {
                if (thisObj instanceof Connection) {
                    String rawUrl = ((Connection) thisObj).getMetaData().getURL();
                    dbUrl = InstrumentationHelper.sanitizeDbUrl(rawUrl);
                }
            } catch (Throwable ignored) {
                // Connection already closed or driver does not support getMetaData
                // after close — fall back to "unknown".
            }

            DbEvent event = InstrumentationHelper.buildConnectionEvent(
                    EventType.CONNECTION_CLOSE, durationMs, dbUrl);
            localQueue.offer(event);

        } catch (Throwable ignored) {
            // Instrumentation must never affect the host application.
        }
    }
}
