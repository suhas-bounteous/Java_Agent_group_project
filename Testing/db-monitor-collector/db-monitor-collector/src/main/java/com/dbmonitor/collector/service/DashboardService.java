package com.dbmonitor.collector.service;

import com.dbmonitor.collector.entity.MetricsSnapshotEntity;
import com.dbmonitor.collector.entity.QueryEventEntity;
import com.dbmonitor.collector.repository.MetricsSnapshotRepository;
import com.dbmonitor.collector.repository.QueryEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardService {

    private final MetricsSnapshotRepository metricsRepo;
    private final QueryEventRepository queryRepo;

    public DashboardService(MetricsSnapshotRepository metricsRepo,
                            QueryEventRepository queryRepo) {
        this.metricsRepo = metricsRepo;
        this.queryRepo = queryRepo;
    }

    /** Overall summary across all apps in the last N minutes. */
    public Map<String, Object> summary(int minutes) {
        long since = System.currentTimeMillis() - minutes * 60_000L;
        List<MetricsSnapshotEntity> snaps = metricsRepo.findSince(since);

        long queries = 0, updates = 0, errors = 0, slow = 0;
        long commits = 0, rollbacks = 0, latencyNs = 0;
        long activeConns = 0;

        // latest activeConnections per app (connections are gauge, not counter)
        Map<String, MetricsSnapshotEntity> latestPerApp = new HashMap<>();

        for (MetricsSnapshotEntity s : snaps) {
            queries   += s.getTotalQueries();
            updates   += s.getTotalUpdates();
            errors    += s.getTotalErrors();
            slow      += s.getSlowQueries();
            commits   += s.getCommits();
            rollbacks += s.getRollbacks();
            latencyNs += s.getTotalLatencyNs();

            MetricsSnapshotEntity cur = latestPerApp.get(s.getApplicationName());
            if (cur == null || s.getReceivedAt() > cur.getReceivedAt()) {
                latestPerApp.put(s.getApplicationName(), s);
            }
        }
        for (MetricsSnapshotEntity s : latestPerApp.values()) {
            activeConns += s.getActiveConnections();
        }

        long totalCalls = queries + updates;
        double avgLatencyMs = totalCalls > 0
                ? (latencyNs / 1_000_000.0) / totalCalls
                : 0.0;
        double errorRate = totalCalls > 0
                ? (errors * 100.0) / totalCalls
                : 0.0;

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("windowMinutes", minutes);
        out.put("totalCalls", totalCalls);
        out.put("totalQueries", queries);
        out.put("totalUpdates", updates);
        out.put("totalErrors", errors);
        out.put("errorRatePct", round(errorRate));
        out.put("avgLatencyMs", round(avgLatencyMs));
        out.put("slowQueries", slow);
        out.put("commits", commits);
        out.put("rollbacks", rollbacks);
        activeConns = Math.max(0, activeConns);
        out.put("activeConnections", activeConns);
        out.put("appCount", latestPerApp.size());
        return out;
    }

    /** Per-app breakdown. */
    public List<Map<String, Object>> byApp(int minutes) {
        long since = System.currentTimeMillis() - minutes * 60_000L;
        List<MetricsSnapshotEntity> snaps = metricsRepo.findSince(since);

        Map<String, long[]> agg = new HashMap<>(); // [calls, errors, latencyNs, slow, activeConns]
        Map<String, Long> latestTs = new HashMap<>();

        for (MetricsSnapshotEntity s : snaps) {
            String app = s.getApplicationName();
            long[] a = agg.computeIfAbsent(app, k -> new long[5]);
            a[0] += s.getTotalQueries() + s.getTotalUpdates();
            a[1] += s.getTotalErrors();
            a[2] += s.getTotalLatencyNs();
            a[3] += s.getSlowQueries();

            Long last = latestTs.get(app);
            if (last == null || s.getReceivedAt() > last) {
                latestTs.put(app, s.getReceivedAt());
                a[4] = s.getActiveConnections();
            }
        }

        List<Map<String, Object>> out = new ArrayList<>();
        for (Map.Entry<String, long[]> e : agg.entrySet()) {
            long[] a = e.getValue();
            double avg = a[0] > 0 ? (a[2] / 1_000_000.0) / a[0] : 0;
            double err = a[0] > 0 ? (a[1] * 100.0) / a[0] : 0;

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("applicationName", e.getKey());
            row.put("totalCalls", a[0]);
            row.put("errors", a[1]);
            row.put("errorRatePct", round(err));
            row.put("avgLatencyMs", round(avg));
            row.put("slowQueries", a[3]);
            row.put("activeConnections", a[4]);
            out.add(row);
        }
        out.sort((x, y) -> Long.compare((long) y.get("totalCalls"), (long) x.get("totalCalls")));
        return out;
    }

    /** Time-series for charts. Returns raw snapshots; frontend plots them. */
    public List<Map<String, Object>> timeseries(String app, int minutes) {
        long since = System.currentTimeMillis() - minutes * 60_000L;
        List<MetricsSnapshotEntity> snaps = (app == null || app.isBlank())
                ? metricsRepo.findSince(since)
                : metricsRepo.findByAppSince(app, since);

        List<Map<String, Object>> out = new ArrayList<>();
        for (MetricsSnapshotEntity s : snaps) {
            long calls = s.getTotalQueries() + s.getTotalUpdates();
            double avgMs = calls > 0 ? (s.getTotalLatencyNs() / 1_000_000.0) / calls : 0.0;

            Map<String, Object> p = new LinkedHashMap<>();
            p.put("t", s.getReceivedAt());
            p.put("app", s.getApplicationName());
            p.put("calls", calls);
            p.put("errors", s.getTotalErrors());
            p.put("slow", s.getSlowQueries());
            p.put("activeConnections", s.getActiveConnections());
            p.put("avgLatencyMs", round(avgMs));
            out.add(p);
        }
        return out;
    }

    public List<QueryEventEntity> topSlow(int limit) {
        return queryRepo.findTopSlow(PageRequest.of(0, limit));
    }

    public List<QueryEventEntity> recentQueries(int limit) {
        return queryRepo.findRecent(PageRequest.of(0, limit));
    }

    public List<String> knownApps() {
        Set<String> all = new HashSet<>();
        all.addAll(metricsRepo.findDistinctApps());
        all.addAll(queryRepo.findDistinctApps());
        List<String> sorted = new ArrayList<>(all);
        Collections.sort(sorted);
        return sorted;
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
