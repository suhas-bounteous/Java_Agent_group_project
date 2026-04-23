package com.dbmonitor.agent.util;

public class QuerySanitizer {

    public static String sanitize(String query) {
        if (query == null) return null;

        // mask string literals
        query = query.replaceAll("'[^']*'", "?");

        // mask numbers
        query = query.replaceAll("\\b\\d+\\b", "?");

        return query;
    }
}