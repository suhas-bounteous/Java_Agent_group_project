package com.dbmonitor.agent;

import com.dbmonitor.agent.transformer.ConnectionTransformer;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class DbMonitorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[DB-AGENT] Starting DB monitor agent");

        new AgentBuilder.Default()
                .type(hasSuperType(named("java.sql.Statement"))
                        .or(hasSuperType(named("java.sql.Connection")))
                        .or(hasSuperType(named("javax.sql.DataSource"))))
                .transform(new ConnectionTransformer())
                .installOn(inst);
    }
}