package com.dbmonitor.collector.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "transaction_event", indexes = {
        @Index(name = "idx_tx_app_time", columnList = "applicationName,timestamp"),
        @Index(name = "idx_tx_time", columnList = "timestamp")
})
public class TransactionEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String applicationName;
    private String hostName;
    private String jvmId;
    private String operationType; // COMMIT / ROLLBACK
    private long timestamp;
    private long durationNs;
    private boolean success;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationName() { return applicationName; }
    public void setApplicationName(String v) { this.applicationName = v; }
    public String getHostName() { return hostName; }
    public void setHostName(String v) { this.hostName = v; }
    public String getJvmId() { return jvmId; }
    public void setJvmId(String v) { this.jvmId = v; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String v) { this.operationType = v; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long v) { this.timestamp = v; }
    public long getDurationNs() { return durationNs; }
    public void setDurationNs(long v) { this.durationNs = v; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
}
