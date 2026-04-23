package com.dbmonitor.agent.model;

public class DbTransactionEvent {

    private String applicationName;
    private String hostName;
    private String jvmId;
    private String operationType; // COMMIT / ROLLBACK
    private long timestamp;
    private long durationNs;
    private boolean success;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DbTransactionEvent event = new DbTransactionEvent();

        public Builder applicationName(String val) { event.applicationName = val; return this; }
        public Builder hostName(String val) { event.hostName = val; return this; }
        public Builder jvmId(String val) { event.jvmId = val; return this; }
        public Builder operationType(String val) { event.operationType = val; return this; }
        public Builder timestamp(long val) { event.timestamp = val; return this; }
        public Builder durationNs(long val) { event.durationNs = val; return this; }
        public Builder success(boolean val) { event.success = val; return this; }

        public DbTransactionEvent build() { return event; }
    }

    public String getApplicationName() { return applicationName; }
    public String getHostName() { return hostName; }
    public String getJvmId() { return jvmId; }
    public String getOperationType() { return operationType; }
    public long getTimestamp() { return timestamp; }
    public long getDurationNs() { return durationNs; }
    public boolean isSuccess() { return success; }
}