package com.dbmonitor.agent.advice;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.context.ConnectionContext;
import com.dbmonitor.agent.dispatcher.EventDispatcher;
import com.dbmonitor.agent.metrics.MetricsCollector;
import com.dbmonitor.agent.model.DbConnectionEvent;
import com.dbmonitor.agent.util.HostUtil;
import com.dbmonitor.agent.util.JvmUtil;
import net.bytebuddy.asm.Advice;

public class CloseConnectionAdvice {

    @Advice.OnMethodEnter
    static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void exit(@Advice.Enter long start,
                     @Advice.Thrown Throwable error) {

        long duration = System.nanoTime() - start;

        if (error == null) {
            MetricsCollector.connectionClosed();
        }

        // FIXED: use AgentConfig.APP_NAME (consistent with rest of agent)
        //        and use the DB type captured by GetConnectionAdvice
        DbConnectionEvent event = DbConnectionEvent.builder()
                .applicationName(AgentConfig.APP_NAME)
                .hostName(HostUtil.getHostName())
                .jvmId(JvmUtil.getJvmId())
                .databaseType(ConnectionContext.getDbType())
                .operationType("CONNECTION_CLOSE")
                .timestamp(System.currentTimeMillis())
                .durationNs(duration)
                .success(error == null)
                .metadata("connection released")
                .build();

        EventDispatcher.publish(event);

        // NEW: clear the per-thread DB type now that the connection is released
        ConnectionContext.clear();
    }
}
