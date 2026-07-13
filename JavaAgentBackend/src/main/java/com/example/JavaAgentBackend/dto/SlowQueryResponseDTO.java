package com.example.JavaAgentBackend.dto;

public class SlowQueryResponseDTO {

    private Long   id;
    private String applicationName;
    private String query;
    private double durationMs;
    private long   timestamp;
    private boolean success;
    private String databaseType;
    private String operationType;

    public Long   getId()              { return id; }
    public void   setId(Long v)        { id = v; }
    public String getApplicationName() { return applicationName; }
    public void   setApplicationName(String v){ applicationName = v; }
    public String getQuery()           { return query; }
    public void   setQuery(String v)   { query = v; }
    public double getDurationMs()      { return durationMs; }
    public void   setDurationMs(double v){ durationMs = v; }
    public long   getTimestamp()       { return timestamp; }
    public void   setTimestamp(long v) { timestamp = v; }
    public boolean isSuccess()         { return success; }
    public void   setSuccess(boolean v){ success = v; }
    public String getDatabaseType()    { return databaseType; }
    public void   setDatabaseType(String v){ databaseType = v; }
    public String getOperationType()   { return operationType; }
    public void   setOperationType(String v){ operationType = v; }
}
