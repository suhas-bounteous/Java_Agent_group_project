package com.dbmonitor.collector.controller;

import com.dbmonitor.collector.dto.IncomingEvent;
import com.dbmonitor.collector.dto.IncomingMetrics;
import com.dbmonitor.collector.service.IngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Matches exactly what the agent's EventDispatcher posts to:
 *   POST http://<collector>:8081/events   (array of mixed events)
 *   POST http://<collector>:8081/metrics  (MetricsSnapshot)
 */
@RestController
public class IngestController {

    private final IngestService ingestService;

    public IngestController(IngestService ingestService) {
        this.ingestService = ingestService;
    }

    @PostMapping("/events")
    public ResponseEntity<Map<String, Object>> events(@RequestBody List<IncomingEvent> events) {
        int count = ingestService.ingestEvents(events);
        return ResponseEntity.ok(Map.of("received", count));
    }

    @PostMapping("/metrics")
    public ResponseEntity<Map<String, Object>> metrics(
            @RequestBody IncomingMetrics metrics,
            @RequestHeader(value = "X-App-Name", required = false) String appName,
            @RequestHeader(value = "X-Host-Name", required = false) String hostName) {
        ingestService.ingestMetrics(metrics, appName, hostName);
        return ResponseEntity.ok(Map.of("ok", true));
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP");
    }
}
