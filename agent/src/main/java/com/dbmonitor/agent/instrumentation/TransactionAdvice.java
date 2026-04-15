package com.dbmonitor.agent.instrumentation;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import net.bytebuddy.asm.Advice;

/**
 * Byte Buddy {@link Advice} applied to {@code Connection.commit()} and
 * {@code Connection.rollback()}.
 *
 * <p>Emits a {@link EventType#TRANSACTION_COMMIT} or
 * {@link EventType#TRANSACTION_ROLLBACK} event for each call.  The
 * {@link Advice.Origin @Advice.Origin} annotation injects the method name at
 * bytecode-weave time so the same advice class handles both methods without
 * branching on the class name.
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
public class TransactionAdvice {

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
     * Called <em>before</em> {@code commit()} or {@code rollback()}.
     * Records the start time for the transaction boundary operation.
     */
    @Advice.OnMethodEnter
    public static void onEnter() {
        TimingContext.start();
    }

    /**
     * Called <em>after</em> {@code commit()} or {@code rollback()}, whether it
     * completed normally or threw.
     *
     * @param methodName the simple name of the intercepted method — {@code "commit"}
     *                   or {@code "rollback"} — injected by Byte Buddy via
     *                   {@code @Advice.Origin("#m")}
     * @param thrown     the exception thrown by the transaction method, or
     *                   {@code null} if it completed normally
     */
    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
            @Advice.Origin("#m") String methodName,
            @Advice.Thrown Throwable thrown) {

        try {
            AsyncEventQueue localQueue  = queue;
            AgentConfig     localConfig = config;

            if (localQueue == null || localConfig == null) {
                TimingContext.clear();
                return;
            }

            long durationMs = TimingContext.elapsedMs();

            // Determine the event type from the intercepted method name.
            // Byte Buddy guarantees that methodName is either "commit" or "rollback"
            // because the advice is only applied to namedOneOf("commit", "rollback").
            EventType type = "commit".equals(methodName)
                    ? EventType.TRANSACTION_COMMIT
                    : EventType.TRANSACTION_ROLLBACK;

            DbEvent event = InstrumentationHelper.buildTransactionEvent(type, durationMs);
            localQueue.offer(event);

        } catch (Throwable ignored) {
            // Instrumentation must never affect the host application.
        }
    }
}
