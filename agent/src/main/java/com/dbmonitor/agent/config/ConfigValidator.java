package com.dbmonitor.agent.config;

/**
 * Stateless validator for {@link AgentConfig} instances.
 *
 * <p>All validation rules are collected and reported as a single
 * {@link AgentConfigException} so that operators see every problem at once
 * rather than fixing one constraint at a time.
 */
public final class ConfigValidator {

    private ConfigValidator() {
        // utility class — not instantiable
    }

    /**
     * Validates every constraint on the supplied {@link AgentConfig}.
     *
     * <p>Validation rules:
     * <ul>
     *   <li>{@code appName} must not be {@code null} or blank.</li>
     *   <li>{@code collectorEndpoint} must not be {@code null} or blank, and must
     *       start with {@code "http"} (covers both {@code http://} and
     *       {@code https://}).</li>
     *   <li>{@code reportingIntervalMs} must be &ge; 100 to prevent the reporter
     *       thread from becoming a busy-loop.</li>
     *   <li>{@code slowQueryThresholdMs} must be &ge; 0; a value of zero means
     *       every query is considered slow, which is intentional for debugging.</li>
     *   <li>{@code maxQueueSize} must be &ge; 1; a queue of zero would discard
     *       every event immediately.</li>
     * </ul>
     *
     * @param config the configuration to validate; must not be {@code null}
     * @throws AgentConfigException if one or more constraints are violated;
     *                              the message lists every violation
     */
    public static void validate(AgentConfig config) {
        if (config == null) {
            throw new AgentConfigException("AgentConfig must not be null");
        }

        StringBuilder violations = new StringBuilder();

        if (config.getAppName() == null || config.getAppName().isBlank()) {
            violations.append("[appName must not be null or blank] ");
        }

        if (config.getCollectorEndpoint() == null || config.getCollectorEndpoint().isBlank()) {
            violations.append("[collectorEndpoint must not be null or blank] ");
        } else if (!config.getCollectorEndpoint().startsWith("http")) {
            violations.append("[collectorEndpoint must start with 'http', got: '")
                      .append(config.getCollectorEndpoint())
                      .append("'] ");
        }

        if (config.getReportingIntervalMs() < 100) {
            violations.append("[reportingIntervalMs must be >= 100, got: ")
                      .append(config.getReportingIntervalMs())
                      .append("] ");
        }

        if (config.getSlowQueryThresholdMs() < 0) {
            violations.append("[slowQueryThresholdMs must be >= 0, got: ")
                      .append(config.getSlowQueryThresholdMs())
                      .append("] ");
        }

        if (config.getMaxQueueSize() < 1) {
            violations.append("[maxQueueSize must be >= 1, got: ")
                      .append(config.getMaxQueueSize())
                      .append("] ");
        }

        if (!violations.isEmpty()) {
            throw new AgentConfigException("Invalid agent configuration: " + violations.toString().trim());
        }
    }
}
