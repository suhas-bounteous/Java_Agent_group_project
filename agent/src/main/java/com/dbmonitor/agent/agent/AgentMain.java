package com.dbmonitor.agent.agent;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.config.ConfigLoader;
import com.dbmonitor.agent.logging.AgentLogger;

import java.lang.instrument.Instrumentation;

/**
 * JVM agent entry point.
 *
 * <p>The JVM calls {@link #premain} before the application's {@code main}
 * method when the agent is specified on the command line via
 * {@code -javaagent:db-monitor-agent-1.0.0-shaded.jar[=agentArgs]}.
 *
 * <p>The JVM calls {@link #agentmain} when the agent is attached to a running
 * JVM via the Attach API (e.g. from a management tool).  Both entry points
 * delegate to the same initialisation pipeline.
 *
 * <h3>Fault isolation</h3>
 * <p>The entire body of {@link #premain} is wrapped in a
 * {@code catch (Throwable)} block.  A JVM agent that lets an exception escape
 * from {@code premain} will crash the host application at startup — an
 * unacceptable outcome for an observability tool.  Any fatal error is logged
 * and the agent simply remains inactive.
 */
public final class AgentMain {

    private AgentMain() {
        // Not instantiable — all entry points are static.
    }

    /**
     * Static-load agent entry point, called before the application's
     * {@code main} method.
     *
     * @param agentArgs       raw agent argument string (format: {@code key=value,key=value});
     *                        may be {@code null}
     * @param instrumentation the JVM {@link Instrumentation} handle
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) {
        try {
            AgentLogger.info("Agent starting...");

            // Step 1 — load and validate configuration from all sources.
            AgentConfig config = ConfigLoader.load(agentArgs);

            // Step 2 — honour the master kill-switch.
            if (!config.isEnabled()) {
                AgentLogger.info(
                        "Agent is disabled (dbmonitor.enabled=false) — skipping all instrumentation");
                return;
            }

            // Step 3 — initialise the full agent pipeline
            //           (metadata, sender, queue, ByteBuddy transformer).
            AgentBootstrap.initialize(instrumentation, config);

            // Step 4 — register a shutdown hook so the event queue is flushed
            //           before the JVM exits, even on SIGTERM / System.exit().
            Runtime.getRuntime().addShutdownHook(
                    new Thread(AgentBootstrap::shutdown, "dbmonitor-shutdown-hook"));

            // Step 5 — confirm startup.
            AgentLogger.info("Agent started successfully for app: " + config.getAppName());

        } catch (Throwable t) {
            // Catching Throwable is intentional: premain MUST NOT throw under
            // any circumstances.  Log the failure and leave the application
            // running without instrumentation.
            AgentLogger.error(
                    "Fatal error during agent startup — agent will not be active. "
                    + "Cause: " + t.getMessage(),
                    t
            );
        }
    }

    /**
     * Dynamic-attach agent entry point, called when the agent is attached to a
     * running JVM via the Attach API.
     *
     * <p>Delegates unconditionally to {@link #premain} so that the same
     * configuration loading, initialisation, and fault-isolation logic applies
     * regardless of how the agent was loaded.
     *
     * @param agentArgs       raw agent argument string; may be {@code null}
     * @param instrumentation the JVM {@link Instrumentation} handle
     */
    public static void agentmain(String agentArgs, Instrumentation instrumentation) {
        premain(agentArgs, instrumentation);
    }
}
