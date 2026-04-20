package com.dbmonitor.collector.controller;

import com.dbmonitor.collector.entity.QueryEventEntity;
import com.dbmonitor.collector.service.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /** Overall summary for the last N minutes (default 5). */
    @GetMapping("/summary")
    public Map<String, Object> summary(@RequestParam(defaultValue = "5") int minutes) {
        return service.summary(minutes);
    }

    /** Per-application breakdown. */
    @GetMapping("/apps")
    public List<Map<String, Object>> apps(@RequestParam(defaultValue = "5") int minutes) {
        return service.byApp(minutes);
    }

    /** Time-series data for charts. Optional app filter. */
    @GetMapping("/timeseries")
    public List<Map<String, Object>> timeseries(
            @RequestParam(required = false) String app,
            @RequestParam(defaultValue = "15") int minutes) {
        return service.timeseries(app, minutes);
    }

    /** Top slow queries. */
    @GetMapping("/slow-queries")
    public List<QueryEventEntity> slowQueries(@RequestParam(defaultValue = "20") int limit) {
        return service.topSlow(limit);
    }

    /** Most recent queries. */
    @GetMapping("/recent-queries")
    public List<QueryEventEntity> recentQueries(@RequestParam(defaultValue = "50") int limit) {
        return service.recentQueries(limit);
    }

    /** List of known application names (for dashboard filters). */
    @GetMapping("/known-apps")
    public List<String> knownApps() {
        return service.knownApps();
    }
}
