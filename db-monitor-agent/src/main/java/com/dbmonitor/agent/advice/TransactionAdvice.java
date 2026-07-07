package com.dbmonitor.agent.advice;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.dispatcher.EventDispatcher;
import com.dbmonitor.agent.metrics.MetricsCollector;
import com.dbmonitor.agent.model.DbTransactionEvent;
import com.dbmonitor.agent.util.HostUtil;
import com.dbmonitor.agent.util.JvmUtil;
import net.bytebuddy.asm.Advice;

public class TransactionAdvice {

    @Advice.OnMethodEnter
    static long enter() {

        if (!AgentConfig.ENABLE_TRANSACTION) {
            return 0L;
        }

        return System.nanoTime();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    static void exit(@Advice.Enter long start,
                     @Advice.Origin("#m") String method,
                     @Advice.Thrown Throwable error) {

        long duration = System.nanoTime() - start;

        String op = method.equals("commit") ? "COMMIT" : "ROLLBACK";

        MetricsCollector.recordTransaction(op, error == null);

        DbTransactionEvent event = DbTransactionEvent.builder()
                .applicationName(AgentConfig.APP_NAME)
                .hostName(HostUtil.getHostName())
                .jvmId(JvmUtil.getJvmId())
                .operationType(op)
                .timestamp(System.currentTimeMillis())
                .durationNs(duration)
                .success(error == null)
                .build();

        EventDispatcher.publish(event);
    }
}