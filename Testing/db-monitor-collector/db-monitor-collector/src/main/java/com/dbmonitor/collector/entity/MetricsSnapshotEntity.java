package com.dbmonitor.collector.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "metrics_snapshot", indexes = {
        @Index(name = "idx_metrics_app_time", columnList = "applicationName,receivedAt"),
        @Index(name = "idx_metrics_time", columnList = "receivedAt")
})
public class MetricsSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // tagged via X-App-Name header so we know which JVM sent this
    private String applicationName;
    private String hostName;

    private long totalQueries;
    private long totalUpdates;
    private long totalErrors;
    private long totalLatencyNs;
    private long commits;
    private long rollbacks;
    private long activeConnections;
    private long slowQueries;

    private long receivedAt; // epoch millis when collector got it

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String v) { this.applicationName = v; }
    public String getHostName() { return hostName; }
    public void setHostName(String v) { this.hostName = v; }
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
    public long getReceivedAt() { return receivedAt; }
    public void setReceivedAt(long v) { this.receivedAt = v; }
}
