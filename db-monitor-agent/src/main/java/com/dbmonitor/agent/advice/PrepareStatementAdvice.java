package com.dbmonitor.agent.advice;

import com.dbmonitor.agent.context.QueryContext;
import net.bytebuddy.asm.Advice;

public class PrepareStatementAdvice {

    @Advice.OnMethodEnter
    static void enter(@Advice.Argument(0) String sql) {
        System.out.println(">>> PREPARE SQL: " + sql);
        QueryContext.set(sql);
    }
}