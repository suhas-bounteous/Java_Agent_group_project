package com.dbmonitor.agent.sender;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link RetryPolicy}.
 *
 * <p>All tests use {@code delayMs = 0} to keep the test suite fast while still
 * exercising the retry loop logic.
 */
@ExtendWith(MockitoExtension.class)
class RetryPolicyTest {

    // -------------------------------------------------------------------------
    // execute_returnsValueOnFirstSuccess
    // -------------------------------------------------------------------------

    @Test
    void execute_returnsValueOnFirstSuccess() throws Exception {
        RetryPolicy policy = new RetryPolicy(3, 0);

        String result = policy.execute(() -> "expected-value");

        assertEquals("expected-value", result,
                "execute() must return the value produced by the action on the first successful call");
    }

    @Test
    void execute_returnsNullOnFirstSuccess_whenActionReturnsNull() throws Exception {
        RetryPolicy policy = new RetryPolicy(3, 0);

        String result = policy.execute(() -> null);

        assertNull(result, "execute() must propagate a null return value from the action");
    }

    // -------------------------------------------------------------------------
    // execute_retriesOnFailureAndSucceedsSecondAttempt
    // -------------------------------------------------------------------------

    @Test
    void execute_retriesOnFailureAndSucceedsSecondAttempt() throws Exception {
        RetryPolicy policy = new RetryPolicy(3, 0);
        AtomicInteger attemptCount = new AtomicInteger(0);

        String result = policy.execute(() -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 2) {
                throw new IOException("transient network error on attempt " + attempt);
            }
            return "recovered";
        });

        assertEquals("recovered", result,
                "execute() must return the value from the first successful attempt");
        assertEquals(2, attemptCount.get(),
                "The action must have been called exactly twice: once failing, once succeeding");
    }

    @Test
    void execute_retriesOnFailureAndSucceedsThirdAttempt() throws Exception {
        RetryPolicy policy = new RetryPolicy(3, 0);
        AtomicInteger attemptCount = new AtomicInteger(0);

        String result = policy.execute(() -> {
            int attempt = attemptCount.incrementAndGet();
            if (attempt < 3) {
                throw new RuntimeException("fail on attempt " + attempt);
            }
            return "success-on-third";
        });

        assertEquals("success-on-third", result);
        assertEquals(3, attemptCount.get());
    }

    // -------------------------------------------------------------------------
    // execute_throwsAfterAllAttemptsExhausted
    // -------------------------------------------------------------------------

    @Test
    void execute_throwsAfterAllAttemptsExhausted() {
        RetryPolicy policy = new RetryPolicy(3, 0);
        IOException sentinel = new IOException("persistent failure");

        Exception thrown = assertThrows(IOException.class,
                () -> policy.execute(() -> { throw sentinel; }));

        assertSame(sentinel, thrown,
                "execute() must rethrow the exact last exception without wrapping");
    }

    @Test
    void execute_throwsLastException_notFirstException() {
        RetryPolicy policy = new RetryPolicy(3, 0);
        AtomicInteger count = new AtomicInteger(0);

        RuntimeException lastException = assertThrows(RuntimeException.class,
                () -> policy.execute(() -> {
                    int n = count.incrementAndGet();
                    throw new RuntimeException("error on attempt " + n);
                }));

        // The message must reflect the LAST attempt, not the first.
        assertTrue(lastException.getMessage().contains("3"),
                "The rethrown exception must be from the last (3rd) attempt, got: "
                + lastException.getMessage());
    }

    // -------------------------------------------------------------------------
    // execute_callsActionExactlyMaxAttemptsTimes
    // -------------------------------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void execute_callsActionExactlyMaxAttemptsTimes() throws Exception {
        int maxAttempts = 4;
        RetryPolicy policy = new RetryPolicy(maxAttempts, 0);

        Callable<Void> mockCallable = mock(Callable.class);
        when(mockCallable.call()).thenThrow(new RuntimeException("always fails"));

        assertThrows(RuntimeException.class, () -> policy.execute(mockCallable));

        verify(mockCallable, times(maxAttempts)).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_callsActionExactlyOnce_whenMaxAttemptsIsOne() throws Exception {
        RetryPolicy policy = new RetryPolicy(1, 0);

        Callable<Void> mockCallable = mock(Callable.class);
        when(mockCallable.call()).thenThrow(new RuntimeException("fail"));

        assertThrows(RuntimeException.class, () -> policy.execute(mockCallable));

        verify(mockCallable, times(1)).call();
    }

    @Test
    @SuppressWarnings("unchecked")
    void execute_callsActionOnce_whenSuccessful_regardlessOfMaxAttempts() throws Exception {
        RetryPolicy policy = new RetryPolicy(5, 0);

        Callable<String> mockCallable = mock(Callable.class);
        when(mockCallable.call()).thenReturn("ok");

        policy.execute(mockCallable);

        verify(mockCallable, times(1)).call();
    }

    // -------------------------------------------------------------------------
    // Edge cases
    // -------------------------------------------------------------------------

    @Test
    void constructor_clampsMaxAttemptsToOne_whenZeroSupplied() throws Exception {
        // maxAttempts=0 must not mean "never attempt"; the action must run once.
        RetryPolicy policy = new RetryPolicy(0, 0);

        String result = policy.execute(() -> "still-runs");

        assertEquals("still-runs", result,
                "Policy with maxAttempts=0 must still execute the action once");
    }

    @Test
    void constructor_clampsMaxAttemptsToOne_whenNegativeSupplied() throws Exception {
        RetryPolicy policy = new RetryPolicy(-5, 0);

        String result = policy.execute(() -> "still-runs");

        assertEquals("still-runs", result);
    }

    @Test
    void execute_propagatesCheckedExceptions() {
        RetryPolicy policy = new RetryPolicy(1, 0);

        assertThrows(IOException.class,
                () -> policy.execute(() -> { throw new IOException("checked"); }));
    }
}
