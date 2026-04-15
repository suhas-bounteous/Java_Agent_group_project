package com.dbmonitor.agent.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class ConfigValidatorTest {

    // -------------------------------------------------------------------------
    // Helper — builds a fully-valid config that can be tweaked per test
    // -------------------------------------------------------------------------

    /**
     * Returns a builder pre-populated with values that pass every constraint
     * so individual tests can modify a single field without triggering
     * unrelated failures.
     */
    private AgentConfig.Builder validBuilder() {
        return AgentConfig.builder()
                .enabled(true)
                .appName("my-service")
                .environment("test")
                .collectorEndpoint("http://localhost:8080/api/events")
                .reportingIntervalMs(100L)
                .slowQueryThresholdMs(0L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(1)
                .retryAttempts(2)
                .retryDelayMs(500L);
    }

    // -------------------------------------------------------------------------
    // Happy-path
    // -------------------------------------------------------------------------

    @Test
    void validate_passes_withValidConfig() {
        // Must not throw.
        AgentConfig config = validBuilder().build();
        assertNotNull(config);
    }

    @Test
    void validate_passes_whenEndpointUsesHttps() {
        AgentConfig config = validBuilder()
                .collectorEndpoint("https://collector.prod.example.com/api/events")
                .build();
        assertNotNull(config);
    }

    @Test
    void validate_passes_whenReportingIntervalIsExactly100ms() {
        AgentConfig config = validBuilder()
                .reportingIntervalMs(100L)
                .build();
        assertEquals(100L, config.getReportingIntervalMs());
    }

    @Test
    void validate_passes_whenSlowQueryThresholdIsZero() {
        // Zero means every query is flagged as slow — intentional for debugging.
        AgentConfig config = validBuilder()
                .slowQueryThresholdMs(0L)
                .build();
        assertEquals(0L, config.getSlowQueryThresholdMs());
    }

    @Test
    void validate_passes_whenMaxQueueSizeIsOne() {
        AgentConfig config = validBuilder()
                .maxQueueSize(1)
                .build();
        assertEquals(1, config.getMaxQueueSize());
    }

    // -------------------------------------------------------------------------
    // appName
    // -------------------------------------------------------------------------

    @Test
    void validate_throws_whenAppNameIsNull() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().appName(null).build()
        );
        assertTrue(ex.getMessage().contains("appName"),
                "Exception message must mention the violating field");
    }

    @Test
    void validate_throws_whenAppNameIsBlank() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().appName("   ").build()
        );
        assertTrue(ex.getMessage().contains("appName"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n"})
    void validate_throws_whenAppNameIsWhitespaceVariants(String whitespace) {
        assertThrows(
                AgentConfigException.class,
                () -> validBuilder().appName(whitespace).build()
        );
    }

    // -------------------------------------------------------------------------
    // collectorEndpoint
    // -------------------------------------------------------------------------

    @Test
    void validate_throws_whenEndpointIsNull() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().collectorEndpoint(null).build()
        );
        assertTrue(ex.getMessage().contains("collectorEndpoint"));
    }

    @Test
    void validate_throws_whenEndpointIsBlank() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().collectorEndpoint("   ").build()
        );
        assertTrue(ex.getMessage().contains("collectorEndpoint"));
    }

    @Test
    void validate_throws_whenEndpointDoesNotStartWithHttp() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().collectorEndpoint("ftp://collector/events").build()
        );
        assertTrue(ex.getMessage().contains("collectorEndpoint"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"grpc://host/path", "tcp://host:1234", "jdbc:h2:mem:test", "/relative/path"})
    void validate_throws_whenEndpointHasNonHttpScheme(String endpoint) {
        assertThrows(
                AgentConfigException.class,
                () -> validBuilder().collectorEndpoint(endpoint).build()
        );
    }

    // -------------------------------------------------------------------------
    // reportingIntervalMs
    // -------------------------------------------------------------------------

    @Test
    void validate_throws_whenReportingIntervalTooLow() {
        // 99 ms is one below the minimum of 100 ms.
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().reportingIntervalMs(99L).build()
        );
        assertTrue(ex.getMessage().contains("reportingIntervalMs"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, 1L, 50L, 99L})
    void validate_throws_whenReportingIntervalBelowMinimum(long interval) {
        assertThrows(
                AgentConfigException.class,
                () -> validBuilder().reportingIntervalMs(interval).build()
        );
    }

    // -------------------------------------------------------------------------
    // slowQueryThresholdMs
    // -------------------------------------------------------------------------

    @Test
    void validate_throws_whenSlowQueryThresholdIsNegative() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().slowQueryThresholdMs(-1L).build()
        );
        assertTrue(ex.getMessage().contains("slowQueryThresholdMs"));
    }

    // -------------------------------------------------------------------------
    // maxQueueSize
    // -------------------------------------------------------------------------

    @Test
    void validate_throws_whenMaxQueueSizeLessThanOne() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> validBuilder().maxQueueSize(0).build()
        );
        assertTrue(ex.getMessage().contains("maxQueueSize"));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void validate_throws_whenMaxQueueSizeIsZeroOrNegative(int size) {
        assertThrows(
                AgentConfigException.class,
                () -> validBuilder().maxQueueSize(size).build()
        );
    }

    // -------------------------------------------------------------------------
    // Multiple violations reported together
    // -------------------------------------------------------------------------

    @Test
    void validate_reportsAllViolationsInSingleException() {
        AgentConfigException ex = assertThrows(
                AgentConfigException.class,
                () -> AgentConfig.builder()
                        .appName(null)
                        .collectorEndpoint(null)
                        .reportingIntervalMs(10L)
                        .slowQueryThresholdMs(0L)
                        .sqlCapture(MaskingLevel.FULL)
                        .maxQueueSize(0)
                        .retryAttempts(2)
                        .retryDelayMs(500L)
                        .build()
        );
        String msg = ex.getMessage();
        assertTrue(msg.contains("appName"),           "Should mention appName violation");
        assertTrue(msg.contains("collectorEndpoint"), "Should mention collectorEndpoint violation");
        assertTrue(msg.contains("reportingIntervalMs"), "Should mention reportingIntervalMs violation");
        assertTrue(msg.contains("maxQueueSize"),      "Should mention maxQueueSize violation");
    }
}
