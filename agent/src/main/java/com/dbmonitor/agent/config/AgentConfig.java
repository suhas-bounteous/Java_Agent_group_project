package com.dbmonitor.agent.config;

/**
 * Immutable configuration snapshot for the DB Monitor Agent.
 *
 * <p>Instances are constructed exclusively via {@link Builder}:
 * <pre>{@code
 * AgentConfig config = AgentConfig.builder()
 *     .enabled(true)
 *     .appName("my-service")
 *     .environment("prod")
 *     .collectorEndpoint("http://collector.internal/api/events")
 *     .reportingIntervalMs(5_000)
 *     .slowQueryThresholdMs(1_000)
 *     .sqlCapture(MaskingLevel.FULL)
 *     .maxQueueSize(1_000)
 *     .retryAttempts(2)
 *     .retryDelayMs(500)
 *     .build();
 * }</pre>
 *
 * <p>{@link Builder#build()} delegates to {@link ConfigValidator#validate} before
 * returning; a misconfigured instance is never handed to the caller.
 */
public final class AgentConfig {

    private final boolean      enabled;
    private final String       appName;
    private final String       environment;
    private final String       collectorEndpoint;
    private final long         reportingIntervalMs;
    private final long         slowQueryThresholdMs;
    private final MaskingLevel sqlCapture;
    private final int          maxQueueSize;
    private final int          retryAttempts;
    private final long         retryDelayMs;

    // -------------------------------------------------------------------------
    // Private constructor — only the builder may instantiate
    // -------------------------------------------------------------------------

    private AgentConfig(Builder builder) {
        this.enabled              = builder.enabled;
        this.appName              = builder.appName;
        this.environment          = builder.environment;
        this.collectorEndpoint    = builder.collectorEndpoint;
        this.reportingIntervalMs  = builder.reportingIntervalMs;
        this.slowQueryThresholdMs = builder.slowQueryThresholdMs;
        this.sqlCapture           = builder.sqlCapture;
        this.maxQueueSize         = builder.maxQueueSize;
        this.retryAttempts        = builder.retryAttempts;
        this.retryDelayMs         = builder.retryDelayMs;
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /** Whether the agent is active; all instrumentation is skipped when false. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Logical name of the monitored application, attached to every event. */
    public String getAppName() {
        return appName;
    }

    /** Deployment environment label (e.g. {@code dev}, {@code staging}, {@code prod}). */
    public String getEnvironment() {
        return environment;
    }

    /** HTTP endpoint of the collector service that receives {@link com.dbmonitor.agent.model.MetricPayload} batches. */
    public String getCollectorEndpoint() {
        return collectorEndpoint;
    }

    /** How often (milliseconds) the background reporter flushes the event queue. */
    public long getReportingIntervalMs() {
        return reportingIntervalMs;
    }

    /**
     * Queries whose wall-clock execution time exceeds this value (milliseconds)
     * are marked as slow in the emitted events.
     */
    public long getSlowQueryThresholdMs() {
        return slowQueryThresholdMs;
    }

    /** Controls how much SQL text is captured per intercepted statement. */
    public MaskingLevel getSqlCapture() {
        return sqlCapture;
    }

    /** Maximum number of {@link com.dbmonitor.agent.model.DbEvent}s held in the in-memory queue. */
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    /** Number of HTTP retry attempts when the collector endpoint is unreachable. */
    public int getRetryAttempts() {
        return retryAttempts;
    }

    /** Base delay (milliseconds) between consecutive HTTP retry attempts. */
    public long getRetryDelayMs() {
        return retryDelayMs;
    }

    // -------------------------------------------------------------------------
    // Builder entry point
    // -------------------------------------------------------------------------

    /**
     * Returns a new {@link Builder} pre-populated with safe sentinel values.
     * Callers must explicitly set at least {@code appName} and
     * {@code collectorEndpoint} before calling {@link Builder#build()}.
     *
     * @return a fresh {@code Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    // -------------------------------------------------------------------------
    // Builder
    // -------------------------------------------------------------------------

    /**
     * Fluent builder for {@link AgentConfig}.
     *
     * <p>Default field values mirror those in {@code agent-default.properties}
     * so that a builder with only {@code appName} and {@code collectorEndpoint}
     * set is still a valid, usable configuration.
     */
    public static final class Builder {

        private boolean      enabled              = true;
        private String       appName              = null;
        private String       environment          = "dev";
        private String       collectorEndpoint    = null;
        private long         reportingIntervalMs  = 5_000L;
        private long         slowQueryThresholdMs = 1_000L;
        private MaskingLevel sqlCapture           = MaskingLevel.FULL;
        private int          maxQueueSize         = 1_000;
        private int          retryAttempts        = 2;
        private long         retryDelayMs         = 500L;

        private Builder() {}

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder appName(String appName) {
            this.appName = appName;
            return this;
        }

        public Builder environment(String environment) {
            this.environment = environment;
            return this;
        }

        public Builder collectorEndpoint(String collectorEndpoint) {
            this.collectorEndpoint = collectorEndpoint;
            return this;
        }

        public Builder reportingIntervalMs(long reportingIntervalMs) {
            this.reportingIntervalMs = reportingIntervalMs;
            return this;
        }

        public Builder slowQueryThresholdMs(long slowQueryThresholdMs) {
            this.slowQueryThresholdMs = slowQueryThresholdMs;
            return this;
        }

        public Builder sqlCapture(MaskingLevel sqlCapture) {
            this.sqlCapture = sqlCapture;
            return this;
        }

        public Builder maxQueueSize(int maxQueueSize) {
            this.maxQueueSize = maxQueueSize;
            return this;
        }

        public Builder retryAttempts(int retryAttempts) {
            this.retryAttempts = retryAttempts;
            return this;
        }

        public Builder retryDelayMs(long retryDelayMs) {
            this.retryDelayMs = retryDelayMs;
            return this;
        }

        /**
         * Constructs a validated {@link AgentConfig}.
         *
         * @return an immutable, validated configuration instance
         * @throws AgentConfigException if any field fails validation
         */
        public AgentConfig build() {
            AgentConfig config = new AgentConfig(this);
            ConfigValidator.validate(config);
            return config;
        }
    }

    // -------------------------------------------------------------------------
    // toString — useful for startup logging
    // -------------------------------------------------------------------------

    @Override
    public String toString() {
        return "AgentConfig{"
                + "enabled=" + enabled
                + ", appName='" + appName + '\''
                + ", environment='" + environment + '\''
                + ", collectorEndpoint='" + collectorEndpoint + '\''
                + ", reportingIntervalMs=" + reportingIntervalMs
                + ", slowQueryThresholdMs=" + slowQueryThresholdMs
                + ", sqlCapture=" + sqlCapture
                + ", maxQueueSize=" + maxQueueSize
                + ", retryAttempts=" + retryAttempts
                + ", retryDelayMs=" + retryDelayMs
                + '}';
    }
}
