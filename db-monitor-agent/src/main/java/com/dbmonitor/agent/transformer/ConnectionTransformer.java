package com.dbmonitor.agent.transformer;

import com.dbmonitor.agent.advice.CloseConnectionAdvice;
import com.dbmonitor.agent.advice.GetConnectionAdvice;
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
                        .on(named("close")));
    }
}