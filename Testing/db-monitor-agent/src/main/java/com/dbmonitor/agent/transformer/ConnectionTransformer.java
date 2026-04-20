package com.dbmonitor.agent.transformer;

import com.dbmonitor.agent.advice.*;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;

public class ConnectionTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassLoader classLoader,
            JavaModule module,
            ProtectionDomain protectionDomain) {

        return builder
                .visit(Advice.to(GetConnectionAdvice.class)
                        .on(named("getConnection")))
                .visit(Advice.to(CloseConnectionAdvice.class)
                        .on(named("close")))
                // NEW: capture SQL
                .visit(Advice.to(PrepareStatementAdvice.class)
                .on(named("prepareStatement")))

                // NEW: execution
                .visit(Advice.to(QueryExecutionAdvice.class)
                        .on(named("execute")
                                .or(named("executeQuery"))
                                .or(named("executeUpdate"))
                                .or(named("executeBatch"))))
                .visit(Advice.to(TransactionAdvice.class)
                        .on(named("commit").or(named("rollback"))));
    }
}