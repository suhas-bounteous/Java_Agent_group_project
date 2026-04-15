package com.dbmonitor.agent.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link ConfigLoader}.
 *
 * <p>System properties that the tests set are saved before each test and
 * restored afterwards so that they cannot bleed between test methods or
 * affect other test classes that run in the same JVM.
 */
class ConfigLoaderTest {

    /** Property names touched by these tests — saved/restored around each test. */
    private static final String[] MONITORED_PROPS = {
            "dbmonitor.enabled",
            "dbmonitor.appName",
            "dbmonitor.environment",
            "dbmonitor.collectorEndpoint",
            "dbmonitor.reportingIntervalMs",
            "dbmonitor.slowQueryThresholdMs",
            "dbmonitor.sqlCapture",
            "dbmonitor.maxQueueSize",
            "dbmonitor.retryAttempts",
            "dbmonitor.retryDelayMs",
            "dbmonitor.configFile"
    };

    private final Map<String, String> savedProps = new HashMap<>();

    @BeforeEach
    void saveAndClearSystemProperties() {
        for (String key : MONITORED_PROPS) {
            String existing = System.getProperty(key);
            if (existing != null) {
                savedProps.put(key, existing);
            }
            System.clearProperty(key);
        }
    }

    @AfterEach
    void restoreSystemProperties() {
        // First clear everything we may have set during the test.
        for (String key : MONITORED_PROPS) {
            System.clearProperty(key);
        }
        // Then restore original values.
        for (Map.Entry<String, String> entry : savedProps.entrySet()) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
        savedProps.clear();
    }

    // -------------------------------------------------------------------------
    // Defaults
    // -------------------------------------------------------------------------

    @Test
    void load_usesDefaultsWhenNothingSet() {
        // All MONITORED_PROPS have been cleared; agent-default.properties on the
        // classpath supplies the fallback values.
        AgentConfig config = ConfigLoader.load(null);

        assertNotNull(config);
        assertTrue(config.isEnabled(),
                "Default config must have enabled=true");
        assertEquals("unknown-app", config.getAppName(),
                "Default appName must match agent-default.properties");
        assertEquals("dev", config.getEnvironment(),
                "Default environment must match agent-default.properties");
        assertEquals("http://localhost:8080/api/events", config.getCollectorEndpoint(),
                "Default collectorEndpoint must match agent-default.properties");
        assertEquals(5_000L, config.getReportingIntervalMs(),
                "Default reportingIntervalMs must match agent-default.properties");
        assertEquals(1_000L, config.getSlowQueryThresholdMs(),
                "Default slowQueryThresholdMs must match agent-default.properties");
        assertEquals(MaskingLevel.FULL, config.getSqlCapture(),
                "Default sqlCapture must be FULL");
        assertEquals(1_000, config.getMaxQueueSize(),
                "Default maxQueueSize must match agent-default.properties");
        assertEquals(2, config.getRetryAttempts(),
                "Default retryAttempts must match agent-default.properties");
        assertEquals(500L, config.getRetryDelayMs(),
                "Default retryDelayMs must match agent-default.properties");
    }

    // -------------------------------------------------------------------------
    // System property override
    // -------------------------------------------------------------------------

    @Test
    void load_systemPropertyOverridesDefault() {
        System.setProperty("dbmonitor.appName", "overridden-by-sysprop");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals("overridden-by-sysprop", config.getAppName(),
                "System property dbmonitor.appName must override agent-default.properties");
    }

    @Test
    void load_systemProperty_reportingIntervalMs_overridesDefault() {
        System.setProperty("dbmonitor.reportingIntervalMs", "2000");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals(2_000L, config.getReportingIntervalMs());
    }

    @Test
    void load_systemProperty_collectorEndpoint_overridesDefault() {
        System.setProperty("dbmonitor.collectorEndpoint", "http://custom-host:9090/api/events");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals("http://custom-host:9090/api/events", config.getCollectorEndpoint());
    }

    // -------------------------------------------------------------------------
    // Agent argument parsing
    // -------------------------------------------------------------------------

    @Test
    void load_agentArgsAreParsed() {
        // Keys use the short (un-prefixed) form as documented in ConfigLoader.
        AgentConfig config = ConfigLoader.load("appName=test-app,reportingIntervalMs=1000");

        assertEquals("test-app", config.getAppName(),
                "agentArgs appName must be applied");
        assertEquals(1_000L, config.getReportingIntervalMs(),
                "agentArgs reportingIntervalMs must be applied");
    }

    @Test
    void load_agentArgs_withSpacesAroundDelimiters() {
        AgentConfig config = ConfigLoader.load(" appName = spaced-app , environment = staging ");

        assertEquals("spaced-app", config.getAppName(),
                "Whitespace around key/value in agentArgs must be trimmed");
        assertEquals("staging", config.getEnvironment());
    }

    @Test
    void load_agentArgs_systemPropertyTakesPrecedenceOverAgentArgs() {
        // System property is higher priority than agentArgs.
        System.setProperty("dbmonitor.appName", "sysprop-wins");

        AgentConfig config = ConfigLoader.load("appName=agent-arg-loses");

        assertEquals("sysprop-wins", config.getAppName(),
                "System property must win over agentArgs when both are set");
    }

    @Test
    void load_agentArgs_emptyStringDoesNotCrash() {
        AgentConfig config = ConfigLoader.load("");
        assertNotNull(config);
    }

    @Test
    void load_agentArgs_nullDoesNotCrash() {
        AgentConfig config = ConfigLoader.load(null);
        assertNotNull(config);
    }

    // -------------------------------------------------------------------------
    // Disabled config returned on catastrophic failure
    // -------------------------------------------------------------------------

    @Test
    void load_returnsDisabledConfig_whenLoadFails() {
        // Point to a non-existent file.  The FileInputStream constructor throws
        // FileNotFoundException, which propagates to the outer catch in ConfigLoader
        // and triggers the disabled-config fallback path.
        System.setProperty("dbmonitor.configFile",
                "/nonexistent/path/that/cannot/exist/config-12345.properties");

        AgentConfig config = ConfigLoader.load(null);

        assertNotNull(config, "load() must never return null");
        assertFalse(config.isEnabled(),
                "Config returned after a catastrophic load failure must have enabled=false");
    }

    @Test
    void load_disabledConfig_hasValidFieldsForStartup() {
        // Even the fallback disabled config must pass ConfigValidator so the
        // rest of the agent bootstrap code can safely read every field.
        System.setProperty("dbmonitor.configFile",
                "/nonexistent/catastrophic-path/config.properties");

        AgentConfig config = ConfigLoader.load(null);

        assertNotNull(config.getAppName());
        assertNotNull(config.getCollectorEndpoint());
        assertNotNull(config.getSqlCapture());
        assertTrue(config.getReportingIntervalMs() >= 100,
                "Even the disabled config must carry a safe reportingIntervalMs");
        assertTrue(config.getMaxQueueSize() >= 1,
                "Even the disabled config must carry a safe maxQueueSize");
    }

    // -------------------------------------------------------------------------
    // MaskingLevel parsing
    // -------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"full", "FULL", "Full", "fUlL"})
    void load_maskingLevel_parsedCaseInsensitively(String rawValue) {
        System.setProperty("dbmonitor.sqlCapture", rawValue);

        AgentConfig config = ConfigLoader.load(null);

        assertEquals(MaskingLevel.FULL, config.getSqlCapture(),
                "sqlCapture '" + rawValue + "' must resolve to MaskingLevel.FULL");
    }

    @Test
    void load_maskingLevel_disabled() {
        System.setProperty("dbmonitor.sqlCapture", "DISABLED");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals(MaskingLevel.DISABLED, config.getSqlCapture());
    }

    @Test
    void load_maskingLevel_metadataOnly() {
        System.setProperty("dbmonitor.sqlCapture", "metadata_only");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals(MaskingLevel.METADATA_ONLY, config.getSqlCapture());
    }

    @Test
    void load_maskingLevel_unknownValueDefaultsToFull() {
        System.setProperty("dbmonitor.sqlCapture", "UNRECOGNISED_VALUE");

        AgentConfig config = ConfigLoader.load(null);

        assertEquals(MaskingLevel.FULL, config.getSqlCapture(),
                "Unrecognised sqlCapture value must fall back to FULL");
    }
}
