package com.dbmonitor.agent;

import com.dbmonitor.agent.transformer.ConnectionTransformer;
import com.dbmonitor.agent.transformer.*;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.lang.instrument.Instrumentation;

import static net.bytebuddy.matcher.ElementMatchers.*;

public class DbMonitorAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("[DB-AGENTv2] Starting DB monitor agent");

        new AgentBuilder.Default()

                // FIX: was nameStartsWith("java") which blocked all java.sql.* interception
                .ignore(nameStartsWith("net.bytebuddy")
                        .or(nameStartsWith("java.lang"))
                        .or(nameStartsWith("java.util"))
                        .or(nameStartsWith("java.io"))
                        .or(nameStartsWith("java.nio"))
                        .or(nameStartsWith("java.net"))
                        .or(nameStartsWith("java.security"))
                        .or(nameStartsWith("sun"))
                        .or(nameStartsWith("jdk")))

                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)

                // DataSource + Connection: getConnection, close, prepareStatement, commit, rollback
                .type(hasSuperType(named("javax.sql.DataSource"))
                        .or(hasSuperType(named("java.sql.Connection")))
                        .and(not(isInterface())))
                .transform(new ConnectionTransformer())

                // Statement only: execute* methods — NO close() here
                .type((hasSuperType(named("java.sql.Statement"))
                        .or(hasSuperType(named("java.sql.PreparedStatement"))))
                        .and(not(isInterface())))
                .transform(new StatementTransformer())

                .installOn(inst);
    }
}


//package com.dbmonitor.agent;
//
//import com.dbmonitor.agent.transformer.ConnectionTransformer;
//import net.bytebuddy.agent.builder.AgentBuilder;
//
//import java.lang.instrument.Instrumentation;
//
//import static net.bytebuddy.matcher.ElementMatchers.*;
//
//public class DbMonitorAgent {
//
//    public static void premain(String agentArgs, Instrumentation inst) {
//        System.out.println("[DB-AGENTv2] Starting DB monitor agent");
//
//        new AgentBuilder.Default()
//
//                .ignore(nameStartsWith("net.bytebuddy")
//                        .or(nameStartsWith("java"))
//                        .or(nameStartsWith("sun")))
//
//                .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
//
//                .type(
//                        hasSuperType(named("java.sql.Statement"))
//                                .or(hasSuperType(named("java.sql.PreparedStatement")))
//                                .or(hasSuperType(named("java.sql.Connection")))
//                                .or(hasSuperType(named("javax.sql.DataSource")))
//                )
//
//                .transform((builder, type, classLoader, module, pd) -> {
//                    System.out.println("TRANSFORMING: " + type.getName());
//                    return new ConnectionTransformer()
//                            .transform(builder, type, classLoader, module, pd);
//                })
//
//                .installOn(inst);
//    }
//}