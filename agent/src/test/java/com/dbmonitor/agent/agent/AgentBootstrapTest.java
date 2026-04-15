package com.dbmonitor.agent.agent;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.config.MaskingLevel;
import com.dbmonitor.agent.instrumentation.AgentTransformer;
import com.dbmonitor.agent.sender.AsyncEventQueue;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AgentBootstrap}.
 *
 * <h3>Mocking strategy</h3>
 * <p>{@link Instrumentation} is mocked with Mockito.  Three stubs are required
 * so that ByteBuddy's {@code AgentBuilder} can install without throwing:
 * <ul>
 *   <li>{@code isRetransformClassesSupported()} → {@code true}: ByteBuddy's
 *       {@link net.bytebuddy.agent.builder.AgentBuilder.RedefinitionStrategy#RETRANSFORMATION}
 *       checks this flag before proceeding; a {@code false} return would cause
 *       it to throw {@link IllegalArgumentException}.</li>
 *   <li>{@code isRedefineClassesSupported()} → {@code true}: required by
 *       {@link net.bytebuddy.agent.builder.AgentBuilder.TypeStrategy.Default#REDEFINE}.</li>
 *   <li>{@code getAllLoadedClasses()} → {@code new Class[0]}: prevents a
 *       {@link NullPointerException} inside ByteBuddy when it iterates the
 *       loaded-class list to perform retransformation of already-loaded classes.</li>
 * </ul>
 *
 * <h3>Static-field isolation</h3>
 * <p>{@link AgentBootstrap} holds its {@link AsyncEventQueue} and
 * {@link AgentTransformer} in {@code private static volatile} fields.  Because
 * JUnit does not reload classes between test methods, these fields must be
 * reset to {@code null} in {@code @BeforeEach} via reflection so that each
 * test starts from a clean slate.  {@code @AfterEach} calls
 * {@link AgentBootstrap#shutdown()} to stop any background scheduler thread
 * started by {@link AsyncEventQueue}.
 */
@ExtendWith(MockitoExtension.class)
class AgentBootstrapTest {

    @Mock
    private Instrumentation instrumentation;

    // -------------------------------------------------------------------------
    // Setup / teardown
    // -------------------------------------------------------------------------

    @BeforeEach
    void setUp() throws Exception {
        // Stubs required by ByteBuddy AgentBuilder.
        when(instrumentation.isRetransformClassesSupported()).thenReturn(true);
        when(instrumentation.isRedefineClassesSupported()).thenReturn(true);
        when(instrumentation.getAllLoadedClasses()).thenReturn(new Class[0]);

        // Reset the static singleton state so tests are fully independent.
        resetStaticField("eventQueue");
        resetStaticField("transformer");
    }

    @AfterEach
    void tearDown() {
        // Always shut down after each test to stop any background daemon thread
        // started by AsyncEventQueue.  shutdown() is idempotent — safe to call
        // even when initialize() was never called in a particular test.
        AgentBootstrap.shutdown();
    }

    // -------------------------------------------------------------------------
    // initialize_createsQueueAndTransformer
    // -------------------------------------------------------------------------

    @Test
    void initialize_createsQueueAndTransformer() throws Exception {
        AgentConfig config = buildTestConfig();

        assertDoesNotThrow(() -> AgentBootstrap.initialize(instrumentation, config),
                "initialize() must not throw under any circumstances");

        // Verify that the static fields were populated — use reflection since
        // they are private.
        Object queue       = getStaticField("eventQueue");
        Object xformer     = getStaticField("transformer");

        assertNotNull(queue,
                "eventQueue must be non-null after initialize()");
        assertNotNull(xformer,
                "transformer must be non-null after initialize()");
        assertInstanceOf(AsyncEventQueue.class, queue,
                "eventQueue must be an AsyncEventQueue instance");
        assertInstanceOf(AgentTransformer.class, xformer,
                "transformer must be an AgentTransformer instance");
    }

    @Test
    void initialize_doesNotThrow_whenCalledTwice() {
        // A second call is unusual but must not crash (e.g. agentmain called
        // while premain was already run).
        AgentConfig config = buildTestConfig();

        assertDoesNotThrow(() -> {
            AgentBootstrap.initialize(instrumentation, config);
            AgentBootstrap.initialize(instrumentation, config);
        }, "Calling initialize() twice must not throw");
    }

    // -------------------------------------------------------------------------
    // shutdown_closesQueueGracefully
    // -------------------------------------------------------------------------

    @Test
    void shutdown_closesQueueGracefully() {
        AgentConfig config = buildTestConfig();
        AgentBootstrap.initialize(instrumentation, config);

        // Shutdown must complete without throwing even though the queue has
        // a live background scheduler thread.
        assertDoesNotThrow(() -> AgentBootstrap.shutdown(),
                "shutdown() must not throw when the queue is active");
    }

    @Test
    void shutdown_nullsStaticFields_afterClose() throws Exception {
        AgentConfig config = buildTestConfig();
        AgentBootstrap.initialize(instrumentation, config);

        AgentBootstrap.shutdown();

        assertNull(getStaticField("eventQueue"),
                "eventQueue static field must be null after shutdown()");
        assertNull(getStaticField("transformer"),
                "transformer static field must be null after shutdown()");
    }

    @Test
    void shutdown_isIdempotent() {
        AgentConfig config = buildTestConfig();
        AgentBootstrap.initialize(instrumentation, config);

        // Calling shutdown() twice must not throw (e.g. shutdown hook + manual call).
        assertDoesNotThrow(() -> {
            AgentBootstrap.shutdown();
            AgentBootstrap.shutdown();
        }, "Calling shutdown() twice must be safe");
    }

    // -------------------------------------------------------------------------
    // shutdown_doesNotThrow_whenQueueIsNull
    // -------------------------------------------------------------------------

    @Test
    void shutdown_doesNotThrow_whenQueueIsNull() {
        // Static fields have been reset to null in @BeforeEach.
        // Calling shutdown() without a prior initialize() must be safe.
        assertDoesNotThrow(() -> AgentBootstrap.shutdown(),
                "shutdown() must not throw when called before initialize()");
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    /**
     * Resets a {@code private static} field on {@link AgentBootstrap} to
     * {@code null} so that tests start from a clean state.
     */
    private static void resetStaticField(String fieldName) throws Exception {
        setStaticField(fieldName, null);
    }

    /**
     * Sets the value of a {@code private static} field on {@link AgentBootstrap}.
     */
    private static void setStaticField(String fieldName, Object value) throws Exception {
        Field field = AgentBootstrap.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * Reads the value of a {@code private static} field from {@link AgentBootstrap}.
     */
    private static Object getStaticField(String fieldName) throws Exception {
        Field field = AgentBootstrap.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    // -------------------------------------------------------------------------
    // Config helper
    // -------------------------------------------------------------------------

    /**
     * Returns a validated {@link AgentConfig} suitable for bootstrap tests.
     * Uses a long {@code reportingIntervalMs} so the background scheduler does
     * not fire during the test window, keeping tests fast and deterministic.
     */
    private static AgentConfig buildTestConfig() {
        return AgentConfig.builder()
                .appName("bootstrap-test-app")
                .environment("test")
                .collectorEndpoint("http://localhost:8080/api/events")
                .reportingIntervalMs(30_000L)   // long interval — scheduler must not fire
                .slowQueryThresholdMs(1_000L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(100)
                .retryAttempts(1)
                .retryDelayMs(0L)
                .build();
    }
}
