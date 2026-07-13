package com.example.JavaAgentBackend.dto;

public class ApplicationStatsDTO {

    private String applicationName;
    private String hostName;
    private String jvmId;
    private String databaseType;
    private long   lastSeen;
    private long   totalQueries;
    private long   errorCount;
    private double avgLatencyMs;
    private String status;

    // Constructor used by JPQL @Query (avgDurationNs is raw nanoseconds)
    public ApplicationStatsDTO(String applicationName, String hostName,
                                String jvmId, String databaseType,
                                long lastSeen, long totalQueries,
                                long errorCount, double avgDurationNs) {
        this.applicationName = applicationName;
        this.hostName        = hostName;
        this.jvmId           = jvmId;
        this.databaseType    = databaseType;
        this.lastSeen        = lastSeen;
        this.totalQueries    = totalQueries;
        this.errorCount      = errorCount;
        this.avgLatencyMs    = avgDurationNs / 1_000_000.0;

        // Derive status from error rate
        double errorRate = totalQueries > 0 ? (double) errorCount / totalQueries : 0;
        this.status = errorRate > 0.1 ? "error" : errorRate > 0.02 ? "warning" : "online";
    }

    public String getApplicationName() { return applicationName; }
    public String getHostName()        { return hostName; }
    public String getJvmId()           { return jvmId; }
    public String getDatabaseType()    { return databaseType; }
    public long   getLastSeen()        { return lastSeen; }
    public long   getTotalQueries()    { return totalQueries; }
    public long   getErrorCount()      { return errorCount; }
    public double getAvgLatencyMs()    { return avgLatencyMs; }
    public String getStatus()          { return status; }
}
