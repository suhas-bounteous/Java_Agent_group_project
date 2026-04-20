package com.dbmonitor.agent.util;

import java.lang.management.ManagementFactory;

public class JvmUtil {
    public static String getJvmId() {
        return ManagementFactory.getRuntimeMXBean().getName();
    }
}