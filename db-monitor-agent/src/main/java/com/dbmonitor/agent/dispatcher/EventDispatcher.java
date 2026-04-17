package com.dbmonitor.agent.dispatcher;

import com.dbmonitor.agent.model.DbConnectionEvent;

public class EventDispatcher {

    public static void publish(DbConnectionEvent event) {
        System.out.println("[DB EVENT] " + event);
    }
}