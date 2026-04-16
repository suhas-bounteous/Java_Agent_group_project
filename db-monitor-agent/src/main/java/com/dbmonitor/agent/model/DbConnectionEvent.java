package com.dbmonitor.agent.model;

public class DbConnectionEvent {
    private String applicationName;
    private String hostName;
    private String jvmId;
    private String databaseType;
    private String operationType;
    private long timestamp;
    private long durationNs;
    private boolean success;
    private String metadata;

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final DbConnectionEvent event = new DbConnectionEvent();

        public Builder applicationName(String val) { event.applicationName = val; return this; }
        public Builder hostName(String val) { event.hostName = val; return this; }
        public Builder jvmId(String val) { event.jvmId = val; return this; }
        public Builder databaseType(String val) { event.databaseType = val; return this; }
        public Builder operationType(String val) { event.operationType = val; return this; }
        public Builder timestamp(long val) { event.timestamp = val; return this; }
        public Builder durationNs(long val) { event.durationNs = val; return this; }
        public Builder success(boolean val) { event.success = val; return this; }
        public Builder metadata(String val) { event.metadata = val; return this; }

        public DbConnectionEvent build() { return event; }
    }

    @Override
    public String toString() {
        return "DbConnectionEvent{" +
                "applicationName='" + applicationName + '\'' +
                ", hostName='" + hostName + '\'' +
                ", jvmId='" + jvmId + '\'' +
                ", databaseType='" + databaseType + '\'' +
                ", operationType='" + operationType + '\'' +
                ", timestamp=" + timestamp +
                ", durationNs=" + durationNs +
                ", success=" + success +
                ", metadata='" + metadata + '\'' +
                '}';
    }
}