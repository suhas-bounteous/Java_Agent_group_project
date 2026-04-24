package com.example.JavaAgentBackend.dto;

import java.util.Map;

public class EventStatsDTO {

    private long totalEvents;
    private long queryCount;
    private long connectionCount;
    private long transactionCount;
    private long errorCount;
    private double avgLatencyMs;
    private Map<String, Long> byApplication;
    private Map<String, Long> byOperationType;

    public long getTotalEvents() { return totalEvents; }
    public void setTotalEvents(long v) { this.totalEvents = v; }

    public long getQueryCount() { return queryCount; }
    public void setQueryCount(long v) { this.queryCount = v; }

    public long getConnectionCount() { return connectionCount; }
    public void setConnectionCount(long v) { this.connectionCount = v; }

    public long getTransactionCount() { return transactionCount; }
    public void setTransactionCount(long v) { this.transactionCount = v; }

    public long getErrorCount() { return errorCount; }
    public void setErrorCount(long v) { this.errorCount = v; }

    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double v) { this.avgLatencyMs = v; }

    public Map<String, Long> getByApplication() { return byApplication; }
    public void setByApplication(Map<String, Long> v) { this.byApplication = v; }

    public Map<String, Long> getByOperationType() { return byOperationType; }
    public void setByOperationType(Map<String, Long> v) { this.byOperationType = v; }
}
