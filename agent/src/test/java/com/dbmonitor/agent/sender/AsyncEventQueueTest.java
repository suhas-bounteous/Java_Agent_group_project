package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.config.MaskingLevel;
import com.dbmonitor.agent.model.AgentMetadata;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import com.dbmonitor.agent.model.MetricPayload;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AsyncEventQueue} using a mock {@link MetricSender}.
 *
 * <h3>Timing conventions</h3>
 * <ul>
 *   <li>Tests that need the drain to fire quickly use a {@code reportingIntervalMs}
 *       of {@code 150 ms}.</li>
 *   <li>Tests that want the drain to <em>not</em> fire during the test use
 *       {@code 30_000 ms} (30 s).</li>
 *   <li>{@link CountDownLatch} with a 3-second timeout guards all timing-sensitive
 *       assertions so the suite never hangs.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class AsyncEventQueueTest {

    private static final int    SMALL_QUEUE_SIZE = 3;
    private static final long   FAST_INTERVAL_MS = 150L;
    private static final long   SLOW_INTERVAL_MS = 30_000L;

    @Mock
    private MetricSender mockSender;

    private AgentMetadata   metadata;
    private AsyncEventQueue queue;

    @BeforeEach
    void setUp() {
        metadata = AgentMetadata.create("queue-test", "test");
    }

    @AfterEach
    void tearDown() throws Exception {
        if (queue != null) {
            queue.close();
        }
    }

    // -------------------------------------------------------------------------
    // offer_returnsTrue_whenQueueHasCapacity
    // -------------------------------------------------------------------------

    @Test
    void offer_returnsTrue_whenQueueHasCapacity() {
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, 10));

        assertTrue(queue.offer(makeEvent()),
                "offer() must return true when the queue has free capacity");
    }

    @Test
    void offer_returnsTrue_upToMaxQueueSize() {
        int capacity = 5;
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, capacity));

        for (int i = 0; i < capacity; i++) {
            assertTrue(queue.offer(makeEvent()),
                    "offer() must return true for event " + (i + 1) + " of " + capacity);
        }
    }

    // -------------------------------------------------------------------------
    // offer_returnsFalse_whenQueueFull
    // -------------------------------------------------------------------------

    @Test
    void offer_returnsFalse_whenQueueFull() {
        queue = new AsyncEventQueue(mockSender, metadata,
                buildConfig(SLOW_INTERVAL_MS, SMALL_QUEUE_SIZE));

        // Fill the queue to capacity.
        for (int i = 0; i < SMALL_QUEUE_SIZE; i++) {
            queue.offer(makeEvent());
        }

        // One more offer must fail.
        assertFalse(queue.offer(makeEvent()),
                "offer() must return false when the queue is at capacity");
    }

    // -------------------------------------------------------------------------
    // droppedEventCount_incrementsOnOverflow
    // -------------------------------------------------------------------------

    @Test
    void droppedEventCount_incrementsOnOverflow() {
        queue = new AsyncEventQueue(mockSender, metadata,
                buildConfig(SLOW_INTERVAL_MS, SMALL_QUEUE_SIZE));

        // Fill to capacity.
        for (int i = 0; i < SMALL_QUEUE_SIZE; i++) {
            queue.offer(makeEvent());
        }

        assertEquals(0L, queue.getDroppedEventCount(),
                "No events should have been dropped while the queue had capacity");

        // Three overflow offers.
        queue.offer(makeEvent());
        queue.offer(makeEvent());
        queue.offer(makeEvent());

        assertEquals(3L, queue.getDroppedEventCount(),
                "Each offer() against a full queue must increment the dropped counter");
    }

    @Test
    void droppedEventCount_isZero_initially() {
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, 10));
        assertEquals(0L, queue.getDroppedEventCount());
    }

    // -------------------------------------------------------------------------
    // drainedEvents_areSentAsBatch
    // -------------------------------------------------------------------------

    @Test
    void drainedEvents_areSentAsBatch() throws InterruptedException {
        CountDownLatch sendLatch = new CountDownLatch(1);

        // Release the latch the moment sender.send() is called.
        doAnswer(invocation -> {
            sendLatch.countDown();
            return null;
        }).when(mockSender).send(any(MetricPayload.class));

        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(FAST_INTERVAL_MS, 100));

        queue.offer(makeEvent());
        queue.offer(makeEvent());
        queue.offer(makeEvent());

        boolean drained = sendLatch.await(3, TimeUnit.SECONDS);
        assertTrue(drained, "sender.send() must be called within 3 s of enqueuing events");

        verify(mockSender, atLeastOnce()).send(any(MetricPayload.class));
    }

    @Test
    void drainedEvents_payloadContainsAllEnqueuedEvents() throws InterruptedException {
        AtomicReference<MetricPayload> capturedPayload = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(invocation -> {
            capturedPayload.set(invocation.getArgument(0));
            latch.countDown();
            return null;
        }).when(mockSender).send(any(MetricPayload.class));

        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(FAST_INTERVAL_MS, 100));

        int eventCount = 5;
        for (int i = 0; i < eventCount; i++) {
            queue.offer(makeEvent());
        }

        assertTrue(latch.await(3, TimeUnit.SECONDS), "Drain must complete within 3 s");

        MetricPayload payload = capturedPayload.get();
        assertNotNull(payload);
        assertEquals(eventCount, payload.events().size(),
                "The drained payload must contain all " + eventCount + " enqueued events");
    }

    // -------------------------------------------------------------------------
    // close_sendsRemainingEvents
    // -------------------------------------------------------------------------

    @Test
    void close_sendsRemainingEvents() throws Exception {
        // Use a very long interval so the scheduled drain never fires during the test.
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, 100));

        queue.offer(makeEvent());
        queue.offer(makeEvent());

        // close() must perform a final synchronous drain before returning.
        queue.close();
        queue = null;  // prevent @AfterEach from calling close() again

        verify(mockSender, times(1)).send(any(MetricPayload.class));
    }

    @Test
    void close_finalBatch_containsRemainingEvents() throws Exception {
        AtomicReference<MetricPayload> capturedPayload = new AtomicReference<>();
        doAnswer(inv -> { capturedPayload.set(inv.getArgument(0)); return null; })
                .when(mockSender).send(any(MetricPayload.class));

        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, 100));

        queue.offer(makeEvent());
        queue.offer(makeEvent());
        queue.offer(makeEvent());

        queue.close();
        queue = null;

        assertNotNull(capturedPayload.get(), "Final batch must have been sent on close()");
        assertEquals(3, capturedPayload.get().events().size(),
                "Final batch must contain all 3 events queued before close()");
    }

    @Test
    void close_doesNotThrow_whenQueueIsEmpty() {
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(SLOW_INTERVAL_MS, 10));

        assertDoesNotThrow(() -> {
            queue.close();
            queue = null;
        });
    }

    // -------------------------------------------------------------------------
    // emptyQueue_doesNotCallSender
    // -------------------------------------------------------------------------

    @Test
    void emptyQueue_doesNotCallSender() throws InterruptedException {
        // Use a fast interval so we go through at least two drain cycles.
        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(FAST_INTERVAL_MS, 100));

        // Wait for at least 2 full drain cycles.
        Thread.sleep(FAST_INTERVAL_MS * 3);

        // close() will perform a final drain — still nothing to send.
        queue.close();
        queue = null;

        verify(mockSender, never()).send(any(MetricPayload.class));
    }

    // -------------------------------------------------------------------------
    // Metadata propagation
    // -------------------------------------------------------------------------

    @Test
    void drainedPayload_containsCorrectMetadata() throws InterruptedException {
        AtomicReference<MetricPayload> capturedPayload = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        doAnswer(inv -> {
            capturedPayload.set(inv.getArgument(0));
            latch.countDown();
            return null;
        }).when(mockSender).send(any(MetricPayload.class));

        queue = new AsyncEventQueue(mockSender, metadata, buildConfig(FAST_INTERVAL_MS, 100));
        queue.offer(makeEvent());

        assertTrue(latch.await(3, TimeUnit.SECONDS));
        assertSame(metadata, capturedPayload.get().metadata(),
                "The MetricPayload sent by the drain must carry the same AgentMetadata instance");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static DbEvent makeEvent() {
        return DbEvent.success(
                EventType.QUERY_EXECUTE,
                "SELECT 1",
                10L,
                "jdbc:h2:mem:test"
        );
    }

    /**
     * Builds a validated {@link AgentConfig} with the supplied interval and queue size.
     * All other fields use safe, non-interfering defaults.
     */
    private static AgentConfig buildConfig(long reportingIntervalMs, int maxQueueSize) {
        return AgentConfig.builder()
                .appName("queue-test-app")
                .environment("test")
                .collectorEndpoint("http://localhost:8080/api/events")
                .reportingIntervalMs(reportingIntervalMs)
                .slowQueryThresholdMs(1_000L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(maxQueueSize)
                .retryAttempts(1)
                .retryDelayMs(0L)
                .build();
    }
}
