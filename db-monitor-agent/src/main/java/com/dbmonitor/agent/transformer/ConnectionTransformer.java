package com.dbmonitor.agent.transformer;

import com.dbmonitor.agent.advice.CloseConnectionAdvice;
import com.dbmonitor.agent.advice.GetConnectionAdvice;
import com.dbmonitor.agent.advice.PrepareStatementAdvice;
import com.dbmonitor.agent.advice.TransactionAdvice;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.utility.JavaModule;

import java.security.ProtectionDomain;

import static net.bytebuddy.matcher.ElementMatchers.named;

// FIX: Only handles DataSource/Connection methods.
// execute*() moved to StatementTransformer to prevent
// Statement.close() / ResultSet.close() decrementing activeConnections.
public class ConnectionTransformer implements AgentBuilder.Transformer {

    @Override
    public DynamicType.Builder<?> transform(
            DynamicType.Builder<?> builder,
            TypeDescription typeDescription,
            ClassLoader classLoader,
            JavaModule module,
            ProtectionDomain protectionDomain) {

        System.out.println("[db-monitor] Transforming Connection/DataSource: " + typeDescription.getName());

        return builder
                .visit(Advice.to(GetConnectionAdvice.class)
                        .on(named("getConnection")))
                .visit(Advice.to(CloseConnectionAdvice.class)
                        .on(named("close")))
                .visit(Advice.to(PrepareStatementAdvice.class)
                        .on(named("prepareStatement")))
                .visit(Advice.to(TransactionAdvice.class)
                        .on(named("commit").or(named("rollback"))));
    }
}

//
//import com.dbmonitor.agent.advice.*;
//import net.bytebuddy.asm.Advice;
//import net.bytebuddy.description.type.TypeDescription;
//import net.bytebuddy.dynamic.DynamicType;
//import net.bytebuddy.utility.JavaModule;
//import net.bytebuddy.agent.builder.AgentBuilder;
//
//import java.security.ProtectionDomain;
//
//import static net.bytebuddy.matcher.ElementMatchers.named;
//
//public class ConnectionTransformer implements AgentBuilder.Transformer {
//
//    @Override
//    public DynamicType.Builder<?> transform(
//            DynamicType.Builder<?> builder,
//            TypeDescription typeDescription,
//            ClassLoader classLoader,
//            JavaModule module,
//            ProtectionDomain protectionDomain) {
//
//        return builder
//                .visit(Advice.to(GetConnectionAdvice.class)
//                        .on(named("getConnection")))
//                .visit(Advice.to(CloseConnectionAdvice.class)
//                        .on(named("close")))
//                // NEW: capture SQL
//                .visit(Advice.to(PrepareStatementAdvice.class)
//                .on(named("prepareStatement")))
//
//                // NEW: execution
//                .visit(Advice.to(QueryExecutionAdvice.class)
//                        .on(named("execute")
//                                .or(named("executeQuery"))
//                                .or(named("executeUpdate"))
//                                .or(named("executeBatch"))))
//                .visit(Advice.to(TransactionAdvice.class)
//                        .on(named("commit").or(named("rollback"))));
//    }
//}