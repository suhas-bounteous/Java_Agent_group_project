package com.example.JavaAgentBackend.dto;

import java.util.List;

public class DashboardSummaryResponseDTO {

    // ---- Scalar summary (derived from latest + recent snapshots) ----
    private long   totalQueries;
    private long   totalUpdates;
    private long   totalErrors;
    private long   activeConnections;
    private double avgLatencyMs;
    private double errorRate;        // 0–100 percentage
    private long   slowQueryCount;
    private long   commitCount;
    private long   rollbackCount;
    private double throughput;       // queries per minute

    // ---- Trend arrays (60 recent metric snapshots) ----
    private List<TimeSeriesPointDTO> queryTrend;
    private List<TimeSeriesPointDTO> latencyTrend;
    private List<TimeSeriesPointDTO> connectionTrend;
    private List<TimeSeriesPointDTO> errorTrend;

    // ---- Getters & setters ----
    public long   getTotalQueries()     { return totalQueries; }
    public void   setTotalQueries(long v)    { totalQueries = v; }
    public long   getTotalUpdates()     { return totalUpdates; }
    public void   setTotalUpdates(long v)    { totalUpdates = v; }
    public long   getTotalErrors()      { return totalErrors; }
    public void   setTotalErrors(long v)     { totalErrors = v; }
    public long   getActiveConnections(){ return activeConnections; }
    public void   setActiveConnections(long v){ activeConnections = v; }
    public double getAvgLatencyMs()     { return avgLatencyMs; }
    public void   setAvgLatencyMs(double v)  { avgLatencyMs = v; }
    public double getErrorRate()        { return errorRate; }
    public void   setErrorRate(double v)     { errorRate = v; }
    public long   getSlowQueryCount()   { return slowQueryCount; }
    public void   setSlowQueryCount(long v)  { slowQueryCount = v; }
    public long   getCommitCount()      { return commitCount; }
    public void   setCommitCount(long v)     { commitCount = v; }
    public long   getRollbackCount()    { return rollbackCount; }
    public void   setRollbackCount(long v)   { rollbackCount = v; }
    public double getThroughput()       { return throughput; }
    public void   setThroughput(double v)    { throughput = v; }

    public List<TimeSeriesPointDTO> getQueryTrend()      { return queryTrend; }
    public void setQueryTrend(List<TimeSeriesPointDTO> v){ queryTrend = v; }
    public List<TimeSeriesPointDTO> getLatencyTrend()    { return latencyTrend; }
    public void setLatencyTrend(List<TimeSeriesPointDTO> v){ latencyTrend = v; }
    public List<TimeSeriesPointDTO> getConnectionTrend() { return connectionTrend; }
    public void setConnectionTrend(List<TimeSeriesPointDTO> v){ connectionTrend = v; }
    public List<TimeSeriesPointDTO> getErrorTrend()      { return errorTrend; }
    public void setErrorTrend(List<TimeSeriesPointDTO> v){ errorTrend = v; }
}
