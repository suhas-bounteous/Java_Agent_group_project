package com.dbmonitor.agent.instrumentation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link TimingContext}.
 *
 * <p>Because {@code TimingContext} uses a {@code ThreadLocal}, each test must
 * clean up after itself.  The {@link #cleanUp()} method in {@code @AfterEach}
 * ensures the ThreadLocal is always cleared between tests even if a test fails
 * mid-way.
 */
class TimingContextTest {

    @AfterEach
    void cleanUp() {
        // Guarantee no timing state leaks between test methods.
        TimingContext.clear();
    }

    // -------------------------------------------------------------------------
    // start_and_elapsedMs_returnsPositiveValue
    // -------------------------------------------------------------------------

    @Test
    void start_and_elapsedMs_returnsPositiveValue() throws InterruptedException {
        TimingContext.start();
        // A small sleep gives the timer something to measure.
        Thread.sleep(10);
        long elapsed = TimingContext.elapsedMs();

        assertTrue(elapsed >= 0,
                "elapsedMs() must return a non-negative value after start(); got: " + elapsed);
    }

    @Test
    void start_and_elapsedMs_reflectsActualElapsedTime() throws InterruptedException {
        TimingContext.start();
        Thread.sleep(50);
        long elapsed = TimingContext.elapsedMs();

        // Allow generous upper bound for heavily-loaded CI environments.
        assertTrue(elapsed >= 40 && elapsed < 2_000,
                "Expected elapsed ~50 ms but got: " + elapsed + " ms");
    }

    // -------------------------------------------------------------------------
    // elapsedMs_clearsThreadLocal
    // -------------------------------------------------------------------------

    @Test
    void elapsedMs_clearsThreadLocal() {
        TimingContext.start();

        // First call: returns the elapsed time and clears the ThreadLocal.
        long first = TimingContext.elapsedMs();
        assertTrue(first >= 0,
                "First elapsedMs() call must return a non-negative value");

        // Second call without a new start(): ThreadLocal is now empty, so the
        // implementation returns -1 as the sentinel "not started" value.
        long second = TimingContext.elapsedMs();
        assertEquals(-1L, second,
                "Second elapsedMs() without an intervening start() must return -1 "
                + "(ThreadLocal was cleared by the first call)");
    }

    @Test
    void elapsedMs_returnsMinusOne_whenStartWasNeverCalled() {
        // Fresh thread — start() has never been called.
        long elapsed = TimingContext.elapsedMs();
        assertEquals(-1L, elapsed,
                "elapsedMs() must return -1 when start() was never called on this thread");
    }

    // -------------------------------------------------------------------------
    // clear_removesValue
    // -------------------------------------------------------------------------

    @Test
    void clear_removesValue() {
        TimingContext.start();
        TimingContext.clear();

        // After clear(), elapsedMs() must behave as if start() was never called.
        long elapsed = TimingContext.elapsedMs();
        assertEquals(-1L, elapsed,
                "elapsedMs() must return -1 after clear() has been called");
    }

    @Test
    void clear_isIdempotent() {
        // Calling clear() without a prior start() must not throw.
        assertDoesNotThrow(() -> {
            TimingContext.clear();
            TimingContext.clear();
        });
    }

    // -------------------------------------------------------------------------
    // multiThread_timingsAreIndependent
    // -------------------------------------------------------------------------

    /**
     * Verifies that each thread holds its own independent timing value in the
     * {@code ThreadLocal}.  If the implementation accidentally used a shared
     * static field instead of a {@code ThreadLocal}, the shorter-sleeping thread
     * would see elapsed time contaminated by the longer-sleeping thread's start
     * time.
     */
    @Test
    void multiThread_timingsAreIndependent() throws InterruptedException {
        int          threadCount = 2;
        long[]       sleepMs     = { 100L, 20L };   // thread 0 sleeps longer
        AtomicLong[] elapsed     = { new AtomicLong(-1), new AtomicLong(-1) };
        CountDownLatch latch     = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            Thread t = new Thread(() -> {
                TimingContext.start();
                try {
                    Thread.sleep(sleepMs[idx]);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                elapsed[idx].set(TimingContext.elapsedMs());
                latch.countDown();
            });
            t.setDaemon(true);
            t.start();
        }

        boolean finished = latch.await(5, TimeUnit.SECONDS);
        assertTrue(finished, "Both threads must finish within 5 s");

        // Thread 0 slept ~100 ms; thread 1 slept ~20 ms.
        // Both values must be non-negative (ThreadLocal was set correctly).
        assertTrue(elapsed[0].get() >= 0,
                "Thread 0 elapsed must be non-negative, got: " + elapsed[0].get());
        assertTrue(elapsed[1].get() >= 0,
                "Thread 1 elapsed must be non-negative, got: " + elapsed[1].get());

        // The long-sleeping thread must show significantly more elapsed time.
        // We use a conservative lower bound (50 ms) to absorb scheduler jitter.
        assertTrue(elapsed[0].get() >= 50,
                "Thread 0 (100 ms sleep) elapsed must be >= 50 ms, got: " + elapsed[0].get());

        // Thread 1 slept much less, so its elapsed must be strictly less than
        // thread 0's elapsed (if timings were shared, both would be ~100 ms).
        assertTrue(elapsed[1].get() < elapsed[0].get(),
                "Thread 1 elapsed (" + elapsed[1].get()
                + " ms) must be less than thread 0 elapsed ("
                + elapsed[0].get() + " ms) — timings must be thread-local");
    }

    @Test
    void multiThread_eachThreadCanStartAndClearIndependently() throws InterruptedException {
        CountDownLatch ready   = new CountDownLatch(2);
        CountDownLatch proceed = new CountDownLatch(1);
        AtomicLong     t1Result = new AtomicLong(-99);
        AtomicLong     t2Result = new AtomicLong(-99);

        // Thread 1: start, wait, then clear — should see -1 from elapsedMs.
        Thread t1 = new Thread(() -> {
            TimingContext.start();
            ready.countDown();
            try { proceed.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            TimingContext.clear();
            t1Result.set(TimingContext.elapsedMs());
        });

        // Thread 2: start, wait, then measure — should see a valid elapsed value.
        Thread t2 = new Thread(() -> {
            TimingContext.start();
            ready.countDown();
            try { proceed.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            t2Result.set(TimingContext.elapsedMs());
        });

        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        ready.await(5, TimeUnit.SECONDS);
        proceed.countDown();
        t1.join(3_000);
        t2.join(3_000);

        assertEquals(-1L, t1Result.get(),
                "Thread 1 cleared its timing; elapsedMs() must return -1");
        assertTrue(t2Result.get() >= 0,
                "Thread 2 did not clear; elapsedMs() must return a non-negative value");
    }
}
