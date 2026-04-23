package com.dbmonitor.agent.model;

public class DbQueryEvent {

    private String applicationName;
    private String hostName;
    private String jvmId;
    private String databaseType;
    private String operationType; // QUERY / UPDATE
    private long timestamp;
    private long durationNs;
    private boolean success;
    private String query;
    private boolean slow;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DbQueryEvent event = new DbQueryEvent();

        public Builder applicationName(String val) { event.applicationName = val; return this; }
        public Builder hostName(String val) { event.hostName = val; return this; }
        public Builder jvmId(String val) { event.jvmId = val; return this; }
        public Builder databaseType(String val) { event.databaseType = val; return this; }
        public Builder operationType(String val) { event.operationType = val; return this; }
        public Builder timestamp(long val) { event.timestamp = val; return this; }
        public Builder durationNs(long val) { event.durationNs = val; return this; }
        public Builder success(boolean val) { event.success = val; return this; }
        public Builder query(String val) { event.query = val; return this; }
        public Builder slow(boolean val) { event.slow = val; return this; }

        public DbQueryEvent build() { return event; }
    }

    // getters (important for Jackson)
    public String getApplicationName() { return applicationName; }
    public String getHostName() { return hostName; }
    public String getJvmId() { return jvmId; }
    public String getDatabaseType() { return databaseType; }
    public String getOperationType() { return operationType; }
    public long getTimestamp() { return timestamp; }
    public long getDurationNs() { return durationNs; }
    public boolean isSuccess() { return success; }
    public String getQuery() { return query; }
    public boolean isSlow() {return slow;}
}