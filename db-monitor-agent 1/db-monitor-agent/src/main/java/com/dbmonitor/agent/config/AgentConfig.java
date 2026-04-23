package com.dbmonitor.agent.config;

public class AgentConfig {

    public static final String APP_NAME =
            System.getProperty("app.name",
                    System.getenv().getOrDefault("APP_NAME", "unknown-app"));

    public static final String QUERY_CAPTURE_MODE =
            System.getProperty("db.query.capture", "FULL"); // NONE | METADATA | FULL

    public static final long SLOW_QUERY_THRESHOLD_NS =
            Long.parseLong(System.getProperty("db.slow.threshold.ns", "100000000"));

    public static final boolean ENABLE_QUERY =
            Boolean.parseBoolean(System.getProperty("db.monitor.query", "true"));

    public static final boolean ENABLE_TRANSACTION =
            Boolean.parseBoolean(System.getProperty("db.monitor.tx", "true"));
}