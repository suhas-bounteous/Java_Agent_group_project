package com.dbmonitor.agent;

import com.dbmonitor.agent.transformer.ConnectionTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class DbMonitorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[DB-AGENT] Starting DB monitor agent");

        new AgentBuilder.Default()
                .type(hasSuperType(named("javax.sql.DataSource"))
                        .or(hasSuperType(named("java.sql.Connection"))))
                .transform(new ConnectionTransformer())
                .installOn(inst);
    }
}