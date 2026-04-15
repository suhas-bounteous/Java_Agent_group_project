package com.dbmonitor.agent.model;

import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.util.UUID;

/**
 * Immutable identity block attached to every {@link MetricPayload} sent to the
 * collector.  One instance is created at agent startup and reused for the
 * lifetime of the JVM.
 *
 * @param agentId            randomly generated UUID, unique per JVM launch
 * @param agentVersion       the agent release version string
 * @param appName            logical application name supplied via configuration
 * @param hostName           hostname of the machine running the JVM;
 *                           {@code "unknown"} when the host cannot be resolved
 * @param jvmId              the JVM's runtime name
 *                           ({@code ManagementFactory.getRuntimeMXBean().getName()})
 *                           which typically contains {@code <pid>@<host>}
 * @param environment        deployment environment label (e.g. {@code dev},
 *                           {@code staging}, {@code prod})
 * @param startupTimeEpochMs epoch-millisecond timestamp recorded when the
 *                           metadata was first created
 */
public record AgentMetadata(
        String agentId,
        String agentVersion,
        String appName,
        String hostName,
        String jvmId,
        String environment,
        long startupTimeEpochMs
) {

    /** Immutable agent version baked into every build. */
    public static final String AGENT_VERSION = "1.0.0";

    // -------------------------------------------------------------------------
    // Static factory
    // -------------------------------------------------------------------------

    /**
     * Constructs a fully-populated {@link AgentMetadata} from the runtime
     * environment.
     *
     * <ul>
     *   <li>{@code agentId} — a fresh {@link UUID#randomUUID()} on every call</li>
     *   <li>{@code agentVersion} — the compile-time constant {@value #AGENT_VERSION}</li>
     *   <li>{@code hostName} — resolved via {@link InetAddress#getLocalHost()};
     *       falls back to {@code "unknown"} if the lookup fails</li>
     *   <li>{@code jvmId} — {@code ManagementFactory.getRuntimeMXBean().getName()}</li>
     *   <li>{@code startupTimeEpochMs} — {@code System.currentTimeMillis()} at the
     *       moment this method is invoked</li>
     * </ul>
     *
     * @param appName     logical name of the monitored application
     * @param environment deployment environment label
     * @return a new {@code AgentMetadata} instance
     */
    public static AgentMetadata create(String appName, String environment) {
        String hostName;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            hostName = "unknown";
        }

        return new AgentMetadata(
                UUID.randomUUID().toString(),
                AGENT_VERSION,
                appName,
                hostName,
                ManagementFactory.getRuntimeMXBean().getName(),
                environment,
                System.currentTimeMillis()
        );
    }
}
