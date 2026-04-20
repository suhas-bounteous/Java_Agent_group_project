package com.dbmonitor.collector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingMetrics {
    private long totalQueries;
    private long totalUpdates;
    private long totalErrors;
    private long totalLatencyNs;
    private long commits;
    private long rollbacks;
    private long activeConnections;
    private long slowQueries;

    public long getTotalQueries() { return totalQueries; }
    public void setTotalQueries(long v) { this.totalQueries = v; }
    public long getTotalUpdates() { return totalUpdates; }
    public void setTotalUpdates(long v) { this.totalUpdates = v; }
    public long getTotalErrors() { return totalErrors; }
    public void setTotalErrors(long v) { this.totalErrors = v; }
    public long getTotalLatencyNs() { return totalLatencyNs; }
    public void setTotalLatencyNs(long v) { this.totalLatencyNs = v; }
    public long getCommits() { return commits; }
    public void setCommits(long v) { this.commits = v; }
    public long getRollbacks() { return rollbacks; }
    public void setRollbacks(long v) { this.rollbacks = v; }
    public long getActiveConnections() { return activeConnections; }
    public void setActiveConnections(long v) { this.activeConnections = v; }
    public long getSlowQueries() { return slowQueries; }
    public void setSlowQueries(long v) { this.slowQueries = v; }
}
