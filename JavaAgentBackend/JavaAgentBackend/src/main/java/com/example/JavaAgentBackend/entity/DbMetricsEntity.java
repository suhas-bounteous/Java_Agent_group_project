package com.example.JavaAgentBackend.entity;

import jakarta.persistence.*;

@Entity
public class DbMetricsEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long totalQueries;
    private long totalUpdates;
    private long totalErrors;
    private long totalLatencyNs;
    private long commits;
    private long rollbacks;

    private long timestamp;
    private long activeConnections;
    private long slowQueries;

    // getters/setters
    public Long getId() { return id; }

    public long getTotalQueries() { return totalQueries; }
    public void setTotalQueries(long totalQueries) { this.totalQueries = totalQueries; }

    public long getTotalUpdates() { return totalUpdates; }
    public void setTotalUpdates(long totalUpdates) { this.totalUpdates = totalUpdates; }

    public long getTotalErrors() { return totalErrors; }
    public void setTotalErrors(long totalErrors) { this.totalErrors = totalErrors; }

    public long getTotalLatencyNs() { return totalLatencyNs; }
    public void setTotalLatencyNs(long totalLatencyNs) { this.totalLatencyNs = totalLatencyNs; }

    public long getCommits() { return commits; }
    public void setCommits(long commits) { this.commits = commits; }

    public long getRollbacks() { return rollbacks; }
    public void setRollbacks(long rollbacks) { this.rollbacks = rollbacks; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getActiveConnections() {
        return activeConnections;
    }

    public void setActiveConnections(long activeConnections) {
        this.activeConnections = activeConnections;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getSlowQueries() {
        return slowQueries;
    }

    public void setSlowQueries(long slowQueries) {
        this.slowQueries = slowQueries;
    }
}