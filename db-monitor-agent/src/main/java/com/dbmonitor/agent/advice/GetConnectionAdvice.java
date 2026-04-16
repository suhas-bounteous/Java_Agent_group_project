package com.dbmonitor.agent.advice;

import com.dbmonitor.agent.dispatcher.EventDispatcher;
import com.dbmonitor.agent.model.DbConnectionEvent;
import com.dbmonitor.agent.util.HostUtil;
import com.dbmonitor.agent.util.JvmUtil;
import net.bytebuddy.asm.Advice;

import java.sql.Connection;

public class GetConnectionAdvice {

    @Advice.OnMethodEnter
    static long enter() {
        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void exit(@Advice.Enter long start,
                     @Advice.Return Object conn,
                     @Advice.Thrown Throwable error) {

        long duration = System.nanoTime() - start;

        String dbType = "UNKNOWN";
        if (conn instanceof Connection) {
            try {
                dbType = ((Connection) conn)
                        .getMetaData()
                        .getDatabaseProductName();
            } catch (Exception ignored) {
            }
        }

        DbConnectionEvent event = DbConnectionEvent.builder()
                .applicationName(System.getProperty("app.name", "unknown-app"))
                .hostName(HostUtil.getHostName())
                .jvmId(JvmUtil.getJvmId())
                .databaseType(dbType)
                .operationType("CONNECTION_OPEN")
                .timestamp(System.currentTimeMillis())
                .durationNs(duration)
                .success(error == null)
                .metadata("connection acquired")
                .build();

        EventDispatcher.publish(event);
    }
}