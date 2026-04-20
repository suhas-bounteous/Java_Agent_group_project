package com.dbmonitor.collector.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Mirror of every possible field the agent can send.
 * Agent sends a mixed JSON array of connection/query/transaction events,
 * so we deserialize all of them into this single flexible DTO and route
 * by operationType in the service layer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IncomingEvent {
    private String applicationName;
    private String hostName;
    private String jvmId;
    private String databaseType;
    private String operationType;
    private Long timestamp;
    private Long durationNs;
    private Boolean success;

    // connection-only
    private String metadata;

    // query-only
    private String query;
    private Boolean slow;

    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String v) { this.applicationName = v; }
    public String getHostName() { return hostName; }
    public void setHostName(String v) { this.hostName = v; }
    public String getJvmId() { return jvmId; }
    public void setJvmId(String v) { this.jvmId = v; }
    public String getDatabaseType() { return databaseType; }
    public void setDatabaseType(String v) { this.databaseType = v; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String v) { this.operationType = v; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long v) { this.timestamp = v; }
    public Long getDurationNs() { return durationNs; }
    public void setDurationNs(Long v) { this.durationNs = v; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean v) { this.success = v; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String v) { this.metadata = v; }
    public String getQuery() { return query; }
    public void setQuery(String v) { this.query = v; }
    public Boolean getSlow() { return slow; }
    public void setSlow(Boolean v) { this.slow = v; }
}
