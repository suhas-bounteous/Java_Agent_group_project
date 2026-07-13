package com.example.JavaAgentBackend.dto;

public class MetricsSummaryDTO {

    private long totalQueries;
    private long totalErrors;
    private double errorRate;
    private double avgLatencyMs;
    private long activeConnections;
    private long slowQueries;
    private long commits;
    private long rollbacks;

    public long getTotalQueries() { return totalQueries; }
    public void setTotalQueries(long v) { this.totalQueries = v; }

    public long getTotalErrors() { return totalErrors; }
    public void setTotalErrors(long v) { this.totalErrors = v; }

    public double getErrorRate() { return errorRate; }
    public void setErrorRate(double v) { this.errorRate = v; }

    public double getAvgLatencyMs() { return avgLatencyMs; }
    public void setAvgLatencyMs(double v) { this.avgLatencyMs = v; }

    public long getActiveConnections() { return activeConnections; }
    public void setActiveConnections(long v) { this.activeConnections = v; }

    public long getSlowQueries() { return slowQueries; }
    public void setSlowQueries(long v) { this.slowQueries = v; }

    public long getCommits() { return commits; }
    public void setCommits(long v) { this.commits = v; }

    public long getRollbacks() { return rollbacks; }
    public void setRollbacks(long v) { this.rollbacks = v; }
}
