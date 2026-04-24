package com.example.JavaAgentBackend.entity;

import jakarta.persistence.*;

@Entity
public class DbQueryEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationName;
    private String hostName;
    private String jvmId;
    private String databaseType;
    private String operationType;
    private long timestamp;
    private long durationNs;
    private boolean success;
    private boolean slow;

    @Column(length = 2000)
    private String query;

    // getters + setters
    public Long getId() { return id; }

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String applicationName) { this.applicationName = applicationName; }

    public String getHostName() { return hostName; }
    public void setHostName(String hostName) { this.hostName = hostName; }

    public String getJvmId() { return jvmId; }
    public void setJvmId(String jvmId) { this.jvmId = jvmId; }

    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String databaseType) { this.databaseType = databaseType; }

    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public long getDurationNs() { return durationNs; }
    public void setDurationNs(long durationNs) { this.durationNs = durationNs; }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public boolean isSlow() { return slow; }
    public void setSlow(boolean slow) { this.slow = slow; }
}