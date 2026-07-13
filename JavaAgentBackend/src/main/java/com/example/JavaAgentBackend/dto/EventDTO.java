 package com.example.JavaAgentBackend.dto;

    public class EventDTO {

        private String applicationName;
        private String hostName;
        private String jvmId;
        private String databaseType;
        private String operationType;
        private long timestamp;
        private long durationNs;
        private boolean success;

        private String metadata;
        private String query;
        private Boolean slow;


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

        public String getMetadata() { return metadata; }
        public void setMetadata(String metadata) { this.metadata = metadata; }

        public String getQuery() { return query; }
        public void setQuery(String query) { this.query = query; }

        public Boolean getSlow() { return slow; }
        public void setSlow(Boolean slow) { this.slow = slow; }
    }

