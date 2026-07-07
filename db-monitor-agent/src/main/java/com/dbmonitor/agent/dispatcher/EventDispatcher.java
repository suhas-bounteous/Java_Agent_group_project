package com.dbmonitor.agent.dispatcher;

import com.dbmonitor.agent.metrics.MetricsCollector;
import com.dbmonitor.agent.metrics.MetricsSnapshot;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventDispatcher {

    private static final int BATCH_SIZE =
            Integer.parseInt(System.getProperty("db.batch.size", "50"));

    // FIX: was 30000ms — dashboard showed zeros for 30s after every restart
    private static final long METRICS_INTERVAL_MS =
            Long.parseLong(System.getProperty("db.metrics.interval.ms", "5000"));

    // FIX: backend URL now configurable via JVM arg -Ddb.backend.url=...
    private static final String BACKEND_URL =
            System.getProperty("db.backend.url", "http://localhost:8081");

    private static final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        startWorker();
    }

    public static void publish(Object event) {
        queue.offer(event);
    }

    private static void startWorker() {
        Thread worker = new Thread(() -> {
            long lastMetricsSent = System.currentTimeMillis();

            while (true) {
                try {
                    List<Object> batch = new ArrayList<>();
                    queue.drainTo(batch, BATCH_SIZE);
                    if (!batch.isEmpty()) {
                        sendBatch(batch);
                    }

                    long now = System.currentTimeMillis();
                    if (now - lastMetricsSent >= METRICS_INTERVAL_MS) {
                        // FIX: always send snapshot (even when idle) so activeConnections
                        // stays accurate on the dashboard rather than going stale
                        MetricsSnapshot snapshot = MetricsCollector.snapshotAndReset();
                        sendMetrics(snapshot);
                        lastMetricsSent = now;
                    }

                    Thread.sleep(1000);

                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.out.println("[db-monitor] Worker error: " + e.getMessage());
                }
            }
        });

        worker.setDaemon(true);
        worker.setName("db-monitor-dispatcher");
        worker.start();
    }

    private static void sendBatch(List<Object> events) {
        try {
            String json = mapper.writeValueAsString(events);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL + "/events"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("[db-monitor] Event batch failed: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[db-monitor] Failed to send events: " + e.getMessage());
        }
    }

    private static void sendMetrics(MetricsSnapshot metrics) {
        try {
            String json = mapper.writeValueAsString(metrics);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BACKEND_URL + "/metrics"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.out.println("[db-monitor] Metrics send failed: " + response.statusCode());
            }
        } catch (Exception e) {
            System.out.println("[db-monitor] Failed to send metrics: " + e.getMessage());
        }
    }
}





//package com.dbmonitor.agent.dispatcher;
//
//import com.dbmonitor.agent.metrics.MetricsCollector;
//import com.dbmonitor.agent.metrics.MetricsSnapshot;
//import com.fasterxml.jackson.databind.ObjectMapper;
//
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.LinkedBlockingQueue;
//
//public class EventDispatcher {
//
//    private static final int BATCH_SIZE =
//            Integer.parseInt(System.getProperty("db.batch.size", "50"));
//
//    // How often (ms) to flush metrics to backend — default 30 seconds
//    private static final long METRICS_INTERVAL_MS =
//            Long.parseLong(System.getProperty("db.metrics.interval.ms", "30000"));
//
//    private static final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();
//    private static final HttpClient client = HttpClient.newHttpClient();
//    private static final ObjectMapper mapper = new ObjectMapper();
//
//    static {
//        startWorker();
//    }
//
//    public static void publish(Object event) {
//        queue.offer(event);
//    }
//
//    private static void startWorker() {
//        Thread worker = new Thread(() -> {
//            long lastMetricsSent = System.currentTimeMillis();
//
//            while (true) {
//                try {
//                    // 1. Send any queued events (only if there are real events)
//                    List<Object> batch = new ArrayList<>();
//                    queue.drainTo(batch, BATCH_SIZE);
//                    if (!batch.isEmpty()) {
//                        sendBatch(batch);
//                    }
//
//                    // 2. Send metrics only if interval elapsed AND real activity happened
//                    long now = System.currentTimeMillis();
//                    if (now - lastMetricsSent >= METRICS_INTERVAL_MS) {
//                        if (MetricsCollector.hasActivity()) {
//                            MetricsSnapshot snapshot = MetricsCollector.snapshotAndReset();
//                            sendMetrics(snapshot);
//                        }
//                        lastMetricsSent = now;
//                    }
//
//                    Thread.sleep(1000);
//
//                } catch (InterruptedException ie) {
//                    Thread.currentThread().interrupt();
//                    break;
//                } catch (Exception e) {
//                    System.out.println("[db-monitor] Worker error: " + e.getMessage());
//                }
//            }
//        });
//
//        worker.setDaemon(true);
//        worker.setName("db-monitor-dispatcher");
//        worker.start();
//    }
//
//    private static void sendBatch(List<Object> events) {
//        try {
//            String json = mapper.writeValueAsString(events);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("http://localhost:8081/events"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//
//            HttpResponse<String> response =
//                    client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() != 200) {
//                System.out.println("[db-monitor] Event batch failed: " + response.statusCode());
//            }
//
//        } catch (Exception e) {
//            System.out.println("[db-monitor] Failed to send events: " + e.getMessage());
//        }
//    }
//
//    private static void sendMetrics(MetricsSnapshot metrics) {
//        try {
//            String json = mapper.writeValueAsString(metrics);
//
//            HttpRequest request = HttpRequest.newBuilder()
//                    .uri(URI.create("http://localhost:8081/metrics"))
//                    .header("Content-Type", "application/json")
//                    .POST(HttpRequest.BodyPublishers.ofString(json))
//                    .build();
//
//            HttpResponse<String> response =
//                    client.send(request, HttpResponse.BodyHandlers.ofString());
//
//            if (response.statusCode() != 200) {
//                System.out.println("[db-monitor] Metrics send failed: " + response.statusCode());
//            }
//
//        } catch (Exception e) {
//            System.out.println("[db-monitor] Failed to send metrics: " + e.getMessage());
//        }
//    }
//}
