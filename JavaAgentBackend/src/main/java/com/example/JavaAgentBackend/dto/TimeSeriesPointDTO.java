package com.example.JavaAgentBackend.dto;

public class TimeSeriesPointDTO {

    private long timestamp;
    private double value;

    public TimeSeriesPointDTO(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    public long getTimestamp() { return timestamp; }
    public double getValue()   { return value; }
}
