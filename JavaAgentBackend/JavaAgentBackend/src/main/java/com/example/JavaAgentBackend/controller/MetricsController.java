package com.example.JavaAgentBackend.controller;

import com.example.JavaAgentBackend.dto.MetricsDTO;
import com.example.JavaAgentBackend.dto.MetricsSummaryDTO;
import com.example.JavaAgentBackend.entity.DbMetricsEntity;
import com.example.JavaAgentBackend.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    @Autowired
    private MetricsService metricsService;

    @PostMapping
    public void receive(@RequestBody MetricsDTO metrics) {
        if (metrics == null) return;
        System.out.println("Metrics received");
        metricsService.saveMetrics(metrics);
    }

    @GetMapping("/latest")
    public ResponseEntity<DbMetricsEntity> getLatest() {
        return ResponseEntity.ok(metricsService.getLatestMetrics());
    }

    @GetMapping("/history")
    public ResponseEntity<List<DbMetricsEntity>> getHistory(
            @RequestParam(defaultValue = "60") int limit) {
        return ResponseEntity.ok(metricsService.getRecentMetrics(limit));
    }

    @GetMapping("/summary")
    public ResponseEntity<MetricsSummaryDTO> getSummary() {
        return ResponseEntity.ok(metricsService.getSummary());
    }
}
