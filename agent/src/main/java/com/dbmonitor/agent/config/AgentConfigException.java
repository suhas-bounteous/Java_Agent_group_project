package com.dbmonitor.agent.config;

/**
 * Thrown when an {@link AgentConfig} fails validation or cannot be constructed
 * due to invalid configuration values.
 *
 * <p>This is an unchecked exception so that it propagates naturally through the
 * agent bootstrap path without forcing callers to declare checked exceptions.
 */
public class AgentConfigException extends RuntimeException {

    /**
     * Constructs an {@code AgentConfigException} with the supplied detail message.
     *
     * @param message human-readable description of the configuration problem
     */
    public AgentConfigException(String message) {
        super(message);
    }

    /**
     * Constructs an {@code AgentConfigException} with a detail message and the
     * underlying cause.
     *
     * @param message human-readable description of the configuration problem
     * @param cause   the exception that triggered this error
     */
    public AgentConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
