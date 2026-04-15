package com.dbmonitor.agent.logging;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Canonical logger for the DB Monitor Agent.
 *
 * <p>All methods are static and will <em>never</em> throw under any circumstances —
 * every code path is wrapped in {@code catch (Throwable)} so that a logging
 * failure cannot destabilise the instrumented application.
 *
 * <p>Uses {@link java.util.logging.Logger} (JUL) rather than a third-party
 * framework so that the agent's own log output cannot conflict with whatever
 * logging implementation the host application has on its classpath.
 *
 * <p>Debug output is gated behind the JVM system property
 * {@code dbmonitor.debug=true} and is evaluated on every call so that the
 * property can be toggled at runtime via JMX or a management agent.
 *
 * <p>Every message is prefixed with {@code [DBMonitor] } to make agent output
 * easy to identify in mixed application logs.
 */
public final class AgentLogger {

    private static final String PREFIX = "[DBMonitor] ";
    private static final String DEBUG_PROP = "dbmonitor.debug";

    /**
     * Logger instance obtained once at class initialisation time.
     * Using a named logger (rather than {@code Logger.getAnonymousLogger()})
     * allows operators to configure the output level via
     * {@code java.util.logging} configuration files.
     */
    private static final Logger LOGGER;

    static {
        Logger l;
        try {
            l = Logger.getLogger("com.dbmonitor.agent");
        } catch (Throwable t) {
            // In the pathological case where JUL itself is broken, fall back to
            // an anonymous logger so the rest of the agent can still start.
            l = Logger.getAnonymousLogger();
        }
        LOGGER = l;
    }

    private AgentLogger() {
        // utility class — not instantiable
    }

    // -------------------------------------------------------------------------
    // Public logging methods
    // -------------------------------------------------------------------------

    /**
     * Logs an informational message at {@link Level#INFO}.
     *
     * @param message the message text; {@code null} is tolerated
     */
    public static void info(String message) {
        try {
            LOGGER.info(PREFIX + message);
        } catch (Throwable ignored) {
            // Swallowed intentionally — logging must never crash the agent.
        }
    }

    /**
     * Logs a warning at {@link Level#WARNING}.
     *
     * @param message the message text; {@code null} is tolerated
     */
    public static void warn(String message) {
        try {
            LOGGER.warning(PREFIX + message);
        } catch (Throwable ignored) {
            // Swallowed intentionally.
        }
    }

    /**
     * Logs a severe error at {@link Level#SEVERE} with an associated
     * {@link Throwable}.
     *
     * @param message the message text; {@code null} is tolerated
     * @param cause   the throwable to attach; {@code null} is tolerated
     */
    public static void error(String message, Throwable cause) {
        try {
            LOGGER.log(Level.SEVERE, PREFIX + message, cause);
        } catch (Throwable ignored) {
            // Swallowed intentionally.
        }
    }

    /**
     * Logs a severe error at {@link Level#SEVERE} without an associated cause.
     *
     * @param message the message text; {@code null} is tolerated
     */
    public static void error(String message) {
        try {
            LOGGER.severe(PREFIX + message);
        } catch (Throwable ignored) {
            // Swallowed intentionally.
        }
    }

    /**
     * Logs a debug message at {@link Level#FINE}, but only when the JVM system
     * property {@code dbmonitor.debug} is set to the literal string
     * {@code "true"} (case-sensitive).
     *
     * <p>The property check is performed on every call so that debug logging
     * can be enabled or disabled at runtime without restarting the JVM.
     *
     * @param message the message text; {@code null} is tolerated
     */
    public static void debug(String message) {
        try {
            if ("true".equals(System.getProperty(DEBUG_PROP))) {
                LOGGER.fine(PREFIX + message);
            }
        } catch (Throwable ignored) {
            // Swallowed intentionally.
        }
    }
}
