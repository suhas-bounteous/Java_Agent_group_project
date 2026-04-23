package com.dbmonitor.agent.context;

public class ConnectionContext {

    private static final ThreadLocal<String> DB_TYPE = new ThreadLocal<>();

    public static void setDbType(String type) {
        DB_TYPE.set(type);
    }

    public static String getDbType() {
        return DB_TYPE.get();
    }

    public static void clear() {
        DB_TYPE.remove();
    }
}