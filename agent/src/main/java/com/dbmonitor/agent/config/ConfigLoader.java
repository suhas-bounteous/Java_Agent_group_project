package com.dbmonitor.agent.config;

import com.dbmonitor.agent.util.AgentLogger;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads {@link AgentConfig} by merging configuration sources in priority order.
 *
 * <h3>Priority (highest → lowest)</h3>
 * <ol>
 *   <li>JVM system properties prefixed with {@code dbmonitor.}
 *       (e.g. {@code -Ddbmonitor.appName=my-svc})</li>
 *   <li>External properties file referenced by the system property
 *       {@code dbmonitor.configFile}</li>
 *   <li>Agent arguments passed to {@code premain}
 *       (format: {@code "key=value,key2=value2"}; keys use short names without
 *       the {@code dbmonitor.} prefix, e.g. {@code appName=my-svc})</li>
 *   <li>Environment variables prefixed with {@code DBMONITOR_}
 *       (e.g. {@code DBMONITOR_APP_NAME} → {@code appName})</li>
 *   <li>Classpath resource {@code agent-default.properties} (lowest / defaults)</li>
 * </ol>
 *
 * <p>The method never throws.  Any exception during loading is logged and a
 * disabled-agent config is returned so the instrumented application can still
 * start normally.
 */
public final class ConfigLoader {

    /** Prefix for all JVM system properties and properties-file keys. */
    private static final String PROP_PREFIX = "dbmonitor.";

    /** System property that points to an optional external config file. */
    private static final String CONFIG_FILE_PROP = "dbmonitor.configFile";

    /** Prefix for environment variable overrides. */
    private static final String ENV_PREFIX = "DBMONITOR_";

    private ConfigLoader() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Loads and returns an {@link AgentConfig} by merging all available
     * configuration sources (see class-level Javadoc for priority order).
     *
     * @param agentArgs raw agent argument string passed to {@code premain};
     *                  may be {@code null} or empty
     * @return a validated {@code AgentConfig}; returns a disabled config if
     *         loading or validation fails catastrophically
     */
    public static AgentConfig load(String agentArgs) {
        try {
            // Lowest priority: classpath defaults.
            Properties merged = loadClasspathDefaults();

            // Layer 2: environment variables.
            overlayEnvVars(merged);

            // Layer 3: agent arguments (premain string).
            if (agentArgs != null && !agentArgs.isBlank()) {
                overlayAgentArgs(merged, agentArgs);
            }

            // Layer 4: external properties file (path from system property).
            String configFilePath = System.getProperty(CONFIG_FILE_PROP);
            if (configFilePath != null && !configFilePath.isBlank()) {
                // Exceptions here propagate to the outer catch → disabled config.
                overlayFileProperties(merged, configFilePath);
            }

            // Layer 5 (highest): JVM system properties.
            overlaySystemProperties(merged);

            AgentConfig config = buildConfig(merged);
            AgentLogger.info("db-monitor-agent configuration loaded: " + config);
            return config;

        } catch (Exception e) {
            AgentLogger.error(
                    "db-monitor-agent: fatal error loading configuration — agent will be disabled. Cause: "
                            + e.getMessage(),
                    e
            );
            return buildDisabledConfig();
        }
    }

    // -------------------------------------------------------------------------
    // Source loaders
    // -------------------------------------------------------------------------

    /**
     * Reads {@code agent-default.properties} from the classpath.
     *
     * @return a {@link Properties} instance populated with defaults
     * @throws IOException if the resource cannot be found or read
     */
    private static Properties loadClasspathDefaults() throws IOException {
        Properties props = new Properties();
        try (InputStream in = ConfigLoader.class.getClassLoader()
                .getResourceAsStream("agent-default.properties")) {
            if (in == null) {
                throw new IOException("agent-default.properties not found on classpath");
            }
            props.load(in);
        }
        return props;
    }

    /**
     * Overlays environment variables that match the {@code DBMONITOR_} prefix
     * onto {@code target}.  Environment variable names are converted to property
     * keys using the mapping table below:
     *
     * <pre>
     * DBMONITOR_ENABLED               → dbmonitor.enabled
     * DBMONITOR_APP_NAME              → dbmonitor.appName
     * DBMONITOR_ENVIRONMENT           → dbmonitor.environment
     * DBMONITOR_COLLECTOR_ENDPOINT    → dbmonitor.collectorEndpoint
     * DBMONITOR_REPORTING_INTERVAL_MS → dbmonitor.reportingIntervalMs
     * DBMONITOR_SLOW_QUERY_THRESHOLD_MS → dbmonitor.slowQueryThresholdMs
     * DBMONITOR_SQL_CAPTURE           → dbmonitor.sqlCapture
     * DBMONITOR_MAX_QUEUE_SIZE        → dbmonitor.maxQueueSize
     * DBMONITOR_RETRY_ATTEMPTS        → dbmonitor.retryAttempts
     * DBMONITOR_RETRY_DELAY_MS        → dbmonitor.retryDelayMs
     * </pre>
     */
    private static void overlayEnvVars(Properties target) {
        setIfPresent(target, PROP_PREFIX + "enabled",
                System.getenv(ENV_PREFIX + "ENABLED"));
        setIfPresent(target, PROP_PREFIX + "appName",
                System.getenv(ENV_PREFIX + "APP_NAME"));
        setIfPresent(target, PROP_PREFIX + "environment",
                System.getenv(ENV_PREFIX + "ENVIRONMENT"));
        setIfPresent(target, PROP_PREFIX + "collectorEndpoint",
                System.getenv(ENV_PREFIX + "COLLECTOR_ENDPOINT"));
        setIfPresent(target, PROP_PREFIX + "reportingIntervalMs",
                System.getenv(ENV_PREFIX + "REPORTING_INTERVAL_MS"));
        setIfPresent(target, PROP_PREFIX + "slowQueryThresholdMs",
                System.getenv(ENV_PREFIX + "SLOW_QUERY_THRESHOLD_MS"));
        setIfPresent(target, PROP_PREFIX + "sqlCapture",
                System.getenv(ENV_PREFIX + "SQL_CAPTURE"));
        setIfPresent(target, PROP_PREFIX + "maxQueueSize",
                System.getenv(ENV_PREFIX + "MAX_QUEUE_SIZE"));
        setIfPresent(target, PROP_PREFIX + "retryAttempts",
                System.getenv(ENV_PREFIX + "RETRY_ATTEMPTS"));
        setIfPresent(target, PROP_PREFIX + "retryDelayMs",
                System.getenv(ENV_PREFIX + "RETRY_DELAY_MS"));
    }

    /**
     * Parses a comma-separated {@code key=value} agent argument string and
     * overlays the values onto {@code target}.  Keys are expected to use the
     * short (un-prefixed) names (e.g. {@code appName}), which are normalised to
     * their fully-qualified equivalents (e.g. {@code dbmonitor.appName}).
     *
     * <p>Malformed tokens (no {@code =} separator, blank key) are logged as
     * warnings and skipped.
     *
     * @param target    the properties map to overlay
     * @param agentArgs raw premain argument string (e.g.
     *                  {@code "appName=my-svc,reportingIntervalMs=1000"})
     */
    private static void overlayAgentArgs(Properties target, String agentArgs) {
        String[] tokens = agentArgs.split(",");
        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty()) {
                continue;
            }
            int eq = token.indexOf('=');
            if (eq <= 0) {
                AgentLogger.warn("db-monitor-agent: ignoring malformed agent argument token: '" + token + "'");
                continue;
            }
            String key   = token.substring(0, eq).trim();
            String value = token.substring(eq + 1).trim();
            if (key.isEmpty()) {
                AgentLogger.warn("db-monitor-agent: ignoring agent argument token with blank key: '" + token + "'");
                continue;
            }
            // Normalise to fully-qualified key if the caller used the short form.
            String qualifiedKey = key.startsWith(PROP_PREFIX) ? key : PROP_PREFIX + key;
            target.setProperty(qualifiedKey, value);
        }
    }

    /**
     * Reads a properties file from the filesystem at {@code filePath} and
     * overlays its entries onto {@code target}.
     *
     * @param target   the properties map to overlay
     * @param filePath absolute or relative filesystem path to the file
     * @throws IOException if the file cannot be opened or parsed
     */
    private static void overlayFileProperties(Properties target, String filePath) throws IOException {
        try (FileInputStream fis = new FileInputStream(filePath)) {
            Properties fileProps = new Properties();
            fileProps.load(fis);
            for (String name : fileProps.stringPropertyNames()) {
                target.setProperty(name, fileProps.getProperty(name));
            }
            AgentLogger.info("db-monitor-agent: loaded external config from: " + filePath);
        }
    }

    /**
     * Overlays JVM system properties that carry the {@code dbmonitor.} prefix
     * onto {@code target}.  This is the highest-priority source, so any earlier
     * layer can be overridden from the command line with {@code -Ddbmonitor.*}.
     */
    private static void overlaySystemProperties(Properties target) {
        for (String name : System.getProperties().stringPropertyNames()) {
            if (name.startsWith(PROP_PREFIX)) {
                target.setProperty(name, System.getProperty(name));
            }
        }
    }

    // -------------------------------------------------------------------------
    // Config assembly
    // -------------------------------------------------------------------------

    /**
     * Converts the merged {@link Properties} map into a validated
     * {@link AgentConfig}.
     *
     * @param props fully-merged properties (defaults + all overlays applied)
     * @return a validated {@code AgentConfig}
     */
    private static AgentConfig buildConfig(Properties props) {
        return AgentConfig.builder()
                .enabled(parseBool(props, PROP_PREFIX + "enabled", true))
                .appName(props.getProperty(PROP_PREFIX + "appName"))
                .environment(props.getProperty(PROP_PREFIX + "environment", "dev"))
                .collectorEndpoint(props.getProperty(PROP_PREFIX + "collectorEndpoint"))
                .reportingIntervalMs(parseLong(props, PROP_PREFIX + "reportingIntervalMs", 5_000L))
                .slowQueryThresholdMs(parseLong(props, PROP_PREFIX + "slowQueryThresholdMs", 1_000L))
                .sqlCapture(MaskingLevel.fromString(props.getProperty(PROP_PREFIX + "sqlCapture", "FULL")))
                .maxQueueSize(parseInt(props, PROP_PREFIX + "maxQueueSize", 1_000))
                .retryAttempts(parseInt(props, PROP_PREFIX + "retryAttempts", 2))
                .retryDelayMs(parseLong(props, PROP_PREFIX + "retryDelayMs", 500L))
                .build();
    }

    /**
     * Returns a minimal disabled config used when loading fails catastrophically.
     * The config bypasses normal validation by setting every field to a safe
     * value and calling the builder directly with {@code enabled = false}.
     */
    private static AgentConfig buildDisabledConfig() {
        return AgentConfig.builder()
                .enabled(false)
                .appName("unknown-app")
                .environment("unknown")
                .collectorEndpoint("http://localhost:8080/api/events")
                .reportingIntervalMs(5_000L)
                .slowQueryThresholdMs(1_000L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(1_000)
                .retryAttempts(0)
                .retryDelayMs(500L)
                .build();
    }

    // -------------------------------------------------------------------------
    // Parsing helpers
    // -------------------------------------------------------------------------

    private static void setIfPresent(Properties target, String key, String value) {
        if (value != null && !value.isBlank()) {
            target.setProperty(key, value);
        }
    }

    private static boolean parseBool(Properties props, String key, boolean defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static long parseLong(Properties props, String key, long defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Long.parseLong(raw.trim());
        } catch (NumberFormatException e) {
            AgentLogger.warn("db-monitor-agent: invalid long value for '" + key + "': '" + raw
                    + "' — using default " + defaultValue);
            return defaultValue;
        }
    }

    private static int parseInt(Properties props, String key, int defaultValue) {
        String raw = props.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            AgentLogger.warn("db-monitor-agent: invalid int value for '" + key + "': '" + raw
                    + "' — using default " + defaultValue);
            return defaultValue;
        }
    }
}
