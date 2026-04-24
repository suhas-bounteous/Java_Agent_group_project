package com.example.JavaAgentBackend.dto;

public class QueryEventDTO extends EventDTO {
    private String query;
    private Boolean slow;

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public Boolean getSlow() { return slow; }
    public void setSlow(Boolean slow) { this.slow = slow; }
}
