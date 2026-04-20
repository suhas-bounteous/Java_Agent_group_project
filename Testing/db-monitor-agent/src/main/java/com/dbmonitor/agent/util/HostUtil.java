package com.dbmonitor.agent.util;

import java.net.InetAddress;

public class HostUtil {
    public static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown-host";
        }
    }
}