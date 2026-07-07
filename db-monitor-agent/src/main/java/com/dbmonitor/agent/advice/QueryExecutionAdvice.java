package com.dbmonitor.agent.advice;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.context.ConnectionContext;
import com.dbmonitor.agent.context.QueryContext;
import com.dbmonitor.agent.dispatcher.EventDispatcher;
import com.dbmonitor.agent.metrics.MetricsCollector;
import com.dbmonitor.agent.model.DbQueryEvent;
import com.dbmonitor.agent.util.HostUtil;
import com.dbmonitor.agent.util.JvmUtil;
import com.dbmonitor.agent.util.QuerySanitizer;
import net.bytebuddy.asm.Advice;

public class QueryExecutionAdvice {

    private static final long SLOW_QUERY_THRESHOLD_NS = 100_000_000; // 100ms

    /**
     * Returns true if the query is an internal/system query that should NOT
     * be counted as a real application operation.
     *
     * This filters out:
     *  - HikariCP pool validation queries  (SELECT 1, /* ping *\/ SELECT 1)
     *  - Hibernate schema/catalog queries  (select current_catalog, show transaction isolation level, etc.)
     *  - Spring Boot health-check queries
     *  - Empty / null queries
     *  - Monitoring backend's own SELECT queries on its own tables
     */
    public static boolean isInternalQuery(String query) {
        if (query == null || query.isEmpty()) return true;

        String q = query.trim().toLowerCase();

        // HikariCP pool keep-alive / validation
        if (q.equals("select 1") || q.equals("select 1;")) return true;
        if (q.contains("/* ping */")) return true;
        if (q.equals("/* isvalid */")) return true;

        // Hibernate dialect / schema introspection queries
        if (q.startsWith("select current_catalog")) return true;
        if (q.startsWith("select current_schema")) return true;
        if (q.startsWith("show transaction isolation level")) return true;
        if (q.startsWith("select version()")) return true;
        if (q.contains("information_schema")) return true;
        if (q.contains("pg_catalog")) return true;
        if (q.contains("pg_class")) return true;
        if (q.contains("pg_namespace")) return true;
        if (q.contains("pg_type")) return true;

        // Hibernate sequence generators
        if (q.startsWith("select nextval")) return true;
        if (q.contains("hibernate_sequence")) return true;

        // Monitoring backend's own tables — these are metrics infrastructure,
        // not real application queries from the sample app.
        // If the agent is correctly attached only to LibApplication this won't
        // be an issue, but we guard here just in case.
        if (q.contains("db_metrics_entity")) return true;
        if (q.contains("db_query_event_entity")) return true;
        if (q.contains("db_connection_event_entity")) return true;
        if (q.contains("db_transaction_event_entity")) return true;

        // UNKNOWN_QUERY means we captured a close/wrapper call with no real SQL
        if (q.equals("unknown_query")) return true;

        return false;
    }

    @Advice.OnMethodEnter
    static long enter(@Advice.AllArguments Object[] args) {

        if (!AgentConfig.ENABLE_QUERY) {
            return 0L;
        }

        if (args != null && args.length > 0 && args[0] instanceof String) {
            QueryContext.set((String) args[0]);
        }

        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void exit(@Advice.Enter long start,
                     @Advice.Origin("#m") String method,
                     @Advice.Thrown Throwable error) {

        if (!AgentConfig.ENABLE_QUERY) return;
        if (start == 0L) return; // was skipped in enter

        String rawQuery = QueryContext.get();

        // Filter out internal / system queries — do NOT count them as operations
        if (isInternalQuery(rawQuery)) {
            QueryContext.clear();
            return;
        }

        long duration = System.nanoTime() - start;
        boolean isSlow = duration > SLOW_QUERY_THRESHOLD_NS;

        String query;
        switch (AgentConfig.QUERY_CAPTURE_MODE) {
            case "NONE":
                query = null;
                break;
            case "METADATA":
                query = "QUERY_EXECUTED";
                break;
            case "FULL":
            default:
                query = QuerySanitizer.sanitize(rawQuery);
        }

        System.out.println("QUERY INTERCEPTED: " + query);

        String opType = resolveOperation(method);

        MetricsCollector.recordQuery(duration, error == null, opType);

        DbQueryEvent event = DbQueryEvent.builder()
                .applicationName(AgentConfig.APP_NAME)
                .hostName(HostUtil.getHostName())
                .jvmId(JvmUtil.getJvmId())
                .databaseType(ConnectionContext.getDbType())
                .operationType(opType)
                .timestamp(System.currentTimeMillis())
                .durationNs(duration)
                .success(error == null)
                .query(query)
                .slow(isSlow)
                .build();

        System.out.println(">>> PUBLISHING QUERY EVENT: " + opType + " | " + query);
        EventDispatcher.publish(event);
        ConnectionContext.clear();
        QueryContext.clear();
    }

    public static String resolveOperation(String method) {
        if (method.contains("executeQuery"))  return "QUERY";
        if (method.contains("executeUpdate")) return "UPDATE";
        if (method.contains("executeBatch"))  return "BATCH";
        if (method.contains("execute"))       return "QUERY"; // Hibernate fallback
        return "OTHER";
    }
}