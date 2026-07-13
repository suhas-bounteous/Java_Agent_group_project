package com.example.JavaAgentBackend.service;

import com.example.JavaAgentBackend.dto.MetricsDTO;
import com.example.JavaAgentBackend.dto.MetricsSummaryDTO;
import com.example.JavaAgentBackend.entity.DbMetricsEntity;
import com.example.JavaAgentBackend.repository.DbConnectionEventRepository;
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

    // FIX: injected to compute live activeConnections from event log
    @Autowired
    private DbConnectionEventRepository connectionRepo;

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

    public DbMetricsEntity getLatestMetrics() {
        return repository.findTopByOrderByTimestampDesc()
                .orElse(new DbMetricsEntity());
    }

    // FIX: was findAllByOrderByTimestampAsc — returned oldest rows instead of recent ones
    public List<DbMetricsEntity> getRecentMetrics(int limit) {
        return repository.findAllByOrderByTimestampDesc(PageRequest.of(0, limit)).getContent();
    }

    // In MetricsService.java, replace the getSummary() method body with this:

    public MetricsSummaryDTO getSummary() {
        MetricsSummaryDTO summary = new MetricsSummaryDTO();

        // Use findAll() with stream sum — safe and always works
        List<DbMetricsEntity> all = repository.findAll();

        if (all.isEmpty()) {
            return summary; // returns zeros — frontend shows "No metrics data yet"
        }

        long totalQueries = all.stream().mapToLong(e -> e.getTotalQueries()).sum();
        long totalUpdates = all.stream().mapToLong(e -> e.getTotalUpdates()).sum();
        long totalErrors  = all.stream().mapToLong(e -> e.getTotalErrors()).sum();
        long totalLatency = all.stream().mapToLong(e -> e.getTotalLatencyNs()).sum();
        long commits      = all.stream().mapToLong(e -> e.getCommits()).sum();
        long rollbacks    = all.stream().mapToLong(e -> e.getRollbacks()).sum();
        long slowQueries  = all.stream().mapToLong(e -> e.getSlowQueries()).sum();
        long total        = totalQueries + totalUpdates;

        summary.setTotalQueries(total);
        summary.setTotalErrors(totalErrors);
        summary.setErrorRate(total > 0
                ? Math.round((double) totalErrors / total * 10000.0) / 100.0 : 0.0);
        summary.setAvgLatencyMs(total > 0
                ? Math.round((double) totalLatency / 1_000_000.0 / total * 100.0) / 100.0 : 0.0);
        summary.setSlowQueries(slowQueries);
        summary.setCommits(commits);
        summary.setRollbacks(rollbacks);

        long opens  = connectionRepo.countByOperationType("CONNECTION_OPEN");
        long closes = connectionRepo.countByOperationType("CONNECTION_CLOSE");
        summary.setActiveConnections(Math.max(0, opens - closes));

        return summary;
    }
}





