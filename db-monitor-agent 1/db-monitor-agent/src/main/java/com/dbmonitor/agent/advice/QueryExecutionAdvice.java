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
    @Advice.OnMethodEnter
    static long enter(@Advice.AllArguments Object[] args) {

        if (!AgentConfig.ENABLE_QUERY || !AgentConfig.ENABLE_TRANSACTION) {
            return 0L;   // skip instrumentation
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

        if (!AgentConfig.ENABLE_TRANSACTION || !AgentConfig.ENABLE_QUERY) return;

        long duration = System.nanoTime() - start;
        boolean isSlow = duration > SLOW_QUERY_THRESHOLD_NS;

        String rawQuery = QueryContext.get();
        String query = null;

        switch (AgentConfig.QUERY_CAPTURE_MODE) {
            case "NONE":
                query = null;
                break;
            case "METADATA":
                query = "QUERY_EXECUTED"; // or table name later
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
                .databaseType(ConnectionContext.getDbType()) // can improve later
                .operationType(opType)
                .timestamp(System.currentTimeMillis())
                .durationNs(duration)
                .success(error == null)
                .query(query)
                .slow(isSlow)
                .build();

        EventDispatcher.publish(event);
        ConnectionContext.clear();
        QueryContext.clear();
    }

    private static String resolveOperation(String method) {
        if ("executeQuery".equals(method)) return "QUERY";
        if ("executeUpdate".equals(method)) return "UPDATE";
        if ("executeBatch".equals(method)) return "BATCH";   // ✅ NEW
        return "OTHER";
    }
}