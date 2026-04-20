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

    @Advice.OnMethodEnter
    static long enter(@Advice.AllArguments Object[] args) {

        // FIXED: only depends on ENABLE_QUERY (was incorrectly also requiring ENABLE_TRANSACTION)
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

        // FIXED: only depends on ENABLE_QUERY
        if (!AgentConfig.ENABLE_QUERY) return;

        long duration = System.nanoTime() - start;

        // FIXED: use configurable threshold from AgentConfig instead of hardcoded 100ms
        boolean isSlow = duration > AgentConfig.SLOW_QUERY_THRESHOLD_NS;

        String rawQuery = QueryContext.get();
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

        EventDispatcher.publish(event);

        // FIXED: removed ConnectionContext.clear() here - it was wiping the DB type
        // after the first query. DB type should persist until the connection is closed.
        // The clear is now done in CloseConnectionAdvice.
        QueryContext.clear();
    }

     public static String resolveOperation(String method) {
        if ("executeQuery".equals(method)) return "QUERY";
        if ("executeUpdate".equals(method)) return "UPDATE";
        if ("executeBatch".equals(method)) return "BATCH";
        return "OTHER";
    }
}
