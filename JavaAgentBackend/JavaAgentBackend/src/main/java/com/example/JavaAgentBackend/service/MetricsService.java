package com.example.JavaAgentBackend.service;

import com.example.JavaAgentBackend.dto.MetricsDTO;
import com.example.JavaAgentBackend.dto.MetricsSummaryDTO;
import com.example.JavaAgentBackend.entity.DbMetricsEntity;
import com.example.JavaAgentBackend.repository.DbMetricsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class MetricsService {

    @Autowired
    private DbMetricsRepository repository;

    // Called by the agent via POST — saves a new metrics snapshot
    public void saveMetrics(MetricsDTO dto) {
        DbMetricsEntity entity = new DbMetricsEntity();
        entity.setTotalQueries(dto.getTotalQueries());
        entity.setTotalUpdates(dto.getTotalUpdates());
        entity.setTotalErrors(dto.getTotalErrors());
        entity.setTotalLatencyNs(dto.getTotalLatencyNs());
        entity.setCommits(dto.getCommits());
        entity.setRollbacks(dto.getRollbacks());
        entity.setActiveConnections(dto.getActiveConnections());
        entity.setSlowQueries(dto.getSlowQueries());
        entity.setTimestamp(System.currentTimeMillis());
        repository.save(entity);
    }

    // Called by dashboard GET — reads only, no writes
    public DbMetricsEntity getLatestMetrics() {
        return repository.findTopByOrderByTimestampDesc()
                .orElse(new DbMetricsEntity());
    }

    // Called by dashboard GET — reads only, no writes
    public List<DbMetricsEntity> getRecentMetrics(int limit) {
        return repository.findAllByOrderByTimestampAsc(PageRequest.of(0, limit)).getContent();
    }

    // Called by dashboard GET — reads only, no writes
    public MetricsSummaryDTO getSummary() {
        Optional<DbMetricsEntity> opt = repository.findTopByOrderByTimestampDesc();
        MetricsSummaryDTO summary = new MetricsSummaryDTO();

        if (opt.isPresent()) {
            DbMetricsEntity m = opt.get();
            long total = m.getTotalQueries() + m.getTotalUpdates();

            summary.setTotalQueries(total);
            summary.setTotalErrors(m.getTotalErrors());
            summary.setErrorRate(total > 0
                    ? Math.round((double) m.getTotalErrors() / total * 10000.0) / 100.0
                    : 0.0);
            summary.setAvgLatencyMs(total > 0
                    ? Math.round((double) m.getTotalLatencyNs() / 1_000_000.0 / total * 100.0) / 100.0
                    : 0.0);
            summary.setActiveConnections(m.getActiveConnections());
            summary.setSlowQueries(m.getSlowQueries());
            summary.setCommits(m.getCommits());
            summary.setRollbacks(m.getRollbacks());
        }

        return summary;
    }
}
