package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.logging.AgentLogger;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.implementation.bytecode.assign.Assigner;

import java.sql.Statement;

/**
 * Byte Buddy {@link Advice} applied to every {@code execute}, {@code executeQuery},
 * {@code executeUpdate}, and {@code executeBatch} method on any class that
 * implements {@link Statement}.
 *
 * <h3>Byte Buddy constraints honoured</h3>
 * <ul>
 *   <li>All {@code @Advice} methods are {@code public static}.</li>
 *   <li>No instance fields — shared state is accessed via the package-private
 *       {@code static volatile} fields {@link #queue} and {@link #config},
 *       which are written once by {@link AgentTransformer} before the first
 *       instrumented call arrives.</li>
 *   <li>The entire {@link #onExit} body is wrapped in
 *       {@code try/catch(Throwable)} so that a bug in the instrumentation can
 *       never affect the host application.</li>
 * </ul>
 */
public class StatementAdvice {

    // -------------------------------------------------------------------------
    // Static holder — written once by AgentTransformer, read on every call.
    // Package-private so AgentTransformer (same package) can set them directly.
    // -------------------------------------------------------------------------

    /** The event queue to which captured events are offered. */
    static volatile AsyncEventQueue queue;

    /** The agent configuration (masking level, slow-query threshold, etc.). */
    static volatile AgentConfig config;

    // -------------------------------------------------------------------------
    // Advice methods — MUST be public static per Byte Buddy spec
    // -------------------------------------------------------------------------

    /**
     * Called <em>before</em> each intercepted statement method.
     * Records the current high-resolution time so that {@link #onExit} can
     * compute the elapsed duration.
     */
    @Advice.OnMethodEnter
    public static void onEnter() {
        TimingContext.start();
    }

    /**
     * Called <em>after</em> each intercepted statement method, whether it
     * completed normally or threw an exception.
     *
     * @param thrown  the exception thrown by the statement method, or
     *                {@code null} if it completed normally
     * @param thisObj the {@code Statement} instance ({@code this} inside the
     *                instrumented method)
     * @param args    all arguments passed to the instrumented method;
     *                {@code args[0]} is the SQL string for
     *                {@code execute(String)}, {@code executeQuery(String)}, and
     *                {@code executeUpdate(String)}
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Thrown Throwable thrown,
            @Advice.This(typing = Assigner.Typing.DYNAMIC) Object thisObj,
            @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args) {

        try {
            AsyncEventQueue localQueue  = queue;
            AgentConfig     localConfig = config;

            if (localQueue == null || localConfig == null) {
                // Agent not yet fully initialised — discard timing and return.
                TimingContext.clear();
                return;
            }

            long durationMs = TimingContext.elapsedMs();

            // Extract SQL from the first argument if it is a String.
            // executeBatch() has no SQL argument (it replays a pre-prepared batch),
            // so we fall back to "unknown" for that case.
            String sql = "unknown";
            if (args != null && args.length > 0 && args[0] instanceof String) {
                sql = (String) args[0];
            }

            // Try to obtain a sanitised DB URL from the Statement's Connection.
            String dbUrl = "unknown";
            try {
                if (thisObj instanceof Statement) {
                    String rawUrl = ((Statement) thisObj).getConnection()
                            .getMetaData()
                            .getURL();
                    dbUrl = InstrumentationHelper.sanitizeDbUrl(rawUrl);
                }
            } catch (Throwable ignored) {
                // getMetaData() may fail on some drivers — fall back to "unknown".
            }

            boolean success  = (thrown == null);
            String  errorMsg = (thrown != null) ? thrown.getMessage() : null;

            DbEvent event = InstrumentationHelper.buildQueryEvent(
                    sql,
                    durationMs,
                    success,
                    errorMsg,
                    localConfig.getSqlCapture(),
                    dbUrl
            );

            localQueue.offer(event);

        } catch (Throwable ignored) {
            // The instrumentation must NEVER affect the host application.
            // All exceptions are silently swallowed here.
        }
    }
}
