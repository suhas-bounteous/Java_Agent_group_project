package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.logging.AgentLogger;

import java.util.concurrent.Callable;

/**
 * Executes a {@link Callable} action with a fixed number of attempts and a
 * configurable inter-attempt sleep.
 *
 * <p>Example — attempt an HTTP call up to three times with a 500 ms pause
 * between each failure:
 * <pre>{@code
 * RetryPolicy policy = new RetryPolicy(3, 500);
 * Response response = policy.execute(() -> httpClient.newCall(request).execute());
 * }</pre>
 *
 * <p>If every attempt throws an exception the last exception is rethrown to
 * the caller unchanged.  A warning is logged before each sleep so that retry
 * storms are visible in the application log.
 *
 * <p>Thread-safety: instances are stateless after construction and may be
 * shared across threads.
 */
public final class RetryPolicy {

    /**
     * Total number of times the action will be attempted (including the very
     * first call).  Clamped to a minimum of 1 so the action is always called
     * at least once regardless of the value supplied by the caller.
     */
    private final int maxAttempts;

    /**
     * Milliseconds to sleep between a failed attempt and the next one.
     * A value of 0 means retries are immediate (useful in tests).
     */
    private final long delayMs;

    /**
     * Constructs a new {@code RetryPolicy}.
     *
     * @param maxAttempts total number of attempts; clamped to a minimum of 1
     * @param delayMs     milliseconds to sleep between attempts; 0 means no sleep
     */
    public RetryPolicy(int maxAttempts, long delayMs) {
        this.maxAttempts = Math.max(1, maxAttempts);
        this.delayMs = delayMs;
    }

    /**
     * Executes {@code action}, retrying on any {@link Exception} up to
     * {@link #maxAttempts} total attempts.
     *
     * <p>Between each failed attempt the thread sleeps for {@link #delayMs}
     * milliseconds and logs a warning via {@link AgentLogger}.  On the final
     * failed attempt no sleep occurs and the exception is rethrown.
     *
     * @param <T>    the type returned by the action
     * @param action the operation to attempt; must not be {@code null}
     * @return the value returned by the first successful invocation of {@code action}
     * @throws Exception the last exception thrown by {@code action} when all
     *                   attempts are exhausted
     */
    public <T> T execute(Callable<T> action) throws Exception {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return action.call();
            } catch (Exception e) {
                lastException = e;

                if (attempt < maxAttempts) {
                    AgentLogger.warn(
                            "db-monitor-agent: attempt " + attempt + "/" + maxAttempts
                            + " failed (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                            + ") — retrying in " + delayMs + " ms"
                    );
                    if (delayMs > 0) {
                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            // Restore the interrupt flag and stop retrying.
                            Thread.currentThread().interrupt();
                            throw lastException;
                        }
                    }
                } else {
                    AgentLogger.warn(
                            "db-monitor-agent: all " + maxAttempts
                            + " attempt(s) exhausted — last error: "
                            + e.getClass().getSimpleName() + ": " + e.getMessage()
                    );
                }
            }
        }

        // lastException is non-null here because we only reach this point when
        // every attempt threw — the loop always runs at least once (maxAttempts >= 1).
        throw lastException;
    }
}
