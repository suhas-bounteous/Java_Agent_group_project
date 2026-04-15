package com.dbmonitor.agent.instrumentation;

/**
 * Per-thread wall-clock timer for JDBC instrumentation.
 *
 * <p>Stores the start instant in a {@link ThreadLocal} so that the enter advice
 * and the exit advice — which run on the same application thread but in
 * different stack frames — can share timing data without any heap allocation or
 * synchronisation overhead.
 *
 * <p>All methods are static so that they can be called from Byte Buddy
 * {@code @Advice} methods, which must be {@code public static}.
 */
public final class TimingContext {

    /**
     * Stores the {@code System.nanoTime()} value captured at the start of the
     * instrumented method.  {@code null} means no timing is in progress on this
     * thread (either {@link #start()} has not been called yet, or
     * {@link #elapsedMs()} / {@link #clear()} has already consumed the value).
     */
    private static final ThreadLocal<Long> START_NANOS = new ThreadLocal<>();

    private TimingContext() {
        // utility class — not instantiable
    }

    /**
     * Records the current high-resolution time as the start of a new timing
     * window for the calling thread.
     *
     * <p>Any previously stored (unconsumed) start value is silently overwritten.
     */
    public static void start() {
        START_NANOS.set(System.nanoTime());
    }

    /**
     * Computes the elapsed time in milliseconds since the last {@link #start()}
     * call on this thread and then <em>removes</em> the stored start value from
     * the {@code ThreadLocal}.
     *
     * <p>If {@link #start()} was never called or the value has already been
     * consumed (cleared), this method returns {@code -1} rather than throwing or
     * computing a meaningless large number from epoch time.
     *
     * @return elapsed time in milliseconds, or {@code -1} if no timing was in
     *         progress on this thread
     */
    public static long elapsedMs() {
        Long startNanos = START_NANOS.get();
        START_NANOS.remove();
        if (startNanos == null) {
            return -1L;
        }
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    /**
     * Unconditionally removes any stored start value from the {@code ThreadLocal}
     * for the calling thread.
     *
     * <p>Called in the {@code finally}-equivalent advice path when a timing value
     * must be discarded (e.g. an unexpected code path that bypassed
     * {@link #elapsedMs()}).
     */
    public static void clear() {
        START_NANOS.remove();
    }
}
