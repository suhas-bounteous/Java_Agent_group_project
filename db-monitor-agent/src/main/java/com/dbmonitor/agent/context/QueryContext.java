package com.dbmonitor.agent.context;

public class QueryContext {

    private static final ThreadLocal<String> QUERY = new ThreadLocal<>();

    public static void set(String query) {
        QUERY.set(query);
    }

    public static String get() {
        return QUERY.get();
    }

    public static void clear() {
        QUERY.remove();
    }
}