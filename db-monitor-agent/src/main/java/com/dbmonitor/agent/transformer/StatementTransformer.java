package com.dbmonitor.agent.transformer;

import com.dbmonitor.agent.advice.QueryExecutionAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;

// NEW FILE: Handles only Statement/PreparedStatement execute* interception.
// Kept separate from ConnectionTransformer so close() is never applied here.
public class StatementTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassLoader classLoader,
            JavaModule module,
            ProtectionDomain protectionDomain) {

        System.out.println("[db-monitor] Transforming Statement: " + typeDescription.getName());

        return builder
                .visit(Advice.to(QueryExecutionAdvice.class)
                        .on(named("execute")
                                .or(named("executeQuery"))
                                .or(named("executeUpdate"))
                                .or(named("executeBatch"))));
    }
}