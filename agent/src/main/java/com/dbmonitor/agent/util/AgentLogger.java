package com.dbmonitor.agent.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Thin facade around {@link java.util.logging.Logger} that the agent uses
 * internally.
 *
 * <p>Using JUL (java.util.logging) rather than a third-party framework means
 * the agent's own log output will never conflict with the logging framework
 * chosen by the instrumented application, since JUL is always available on
 * the boot classpath.
 *
 * <p>All methods are static so callers do not need to hold a logger instance.
 */
public final class AgentLogger {

    private static final Logger LOGGER = Logger.getLogger("com.dbmonitor.agent");

    private AgentLogger() {
        // utility class — not instantiable
    }

    /**
     * Logs an informational message.
     *
     * @param message the message text
     */
    public static void info(String message) {
        LOGGER.info(message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message text
     */
    public static void warn(String message) {
        LOGGER.warning(message);
    }

    /**
     * Logs a warning message with an associated throwable.
     *
     * @param message the message text
     * @param thrown  the exception to attach to the log record
     */
    public static void warn(String message, Throwable thrown) {
        LOGGER.log(Level.WARNING, message, thrown);
    }

    /**
     * Logs a severe error message with an associated throwable.
     *
     * @param message the message text
     * @param thrown  the exception to attach to the log record
     */
    public static void error(String message, Throwable thrown) {
        LOGGER.log(Level.SEVERE, message, thrown);
    }

    /**
     * Logs a severe error message.
     *
     * @param message the message text
     */
    public static void error(String message) {
        LOGGER.severe(message);
    }

    /**
     * Logs a fine-grained debug message. Output is suppressed unless the
     * {@code com.dbmonitor.agent} logger level is set to {@link Level#FINE}
     * or lower.
     *
     * @param message the message text
     */
    public static void debug(String message) {
        LOGGER.fine(message);
    }
}
