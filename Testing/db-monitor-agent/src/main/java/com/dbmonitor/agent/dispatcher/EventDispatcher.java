package com.dbmonitor.agent.dispatcher;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.metrics.MetricsCollector;
import com.dbmonitor.agent.metrics.MetricsSnapshot;
import com.dbmonitor.agent.util.HostUtil;
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

    // NEW: configurable collector URL via -Ddb.collector.url=...
    private static final String COLLECTOR_URL =
            System.getProperty("db.collector.url", "http://localhost:8081");

    private static final int BATCH_SIZE =
            Integer.parseInt(System.getProperty("db.batch.size", "50"));

    // Supports multiple event types (Connection, Query, Transaction, etc.)
    private static final BlockingQueue<Object> queue = new LinkedBlockingQueue<>();

    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        startWorker();
    }

    // Publish any event type
    public static void publish(Object event) {
        queue.offer(event);
    }

    private static void startWorker() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    List<Object> batch = new ArrayList<>();
                    queue.drainTo(batch, BATCH_SIZE);

                    if (!batch.isEmpty()) {
                        sendBatch(batch);
                    }

                    MetricsSnapshot snapshot = MetricsCollector.snapshotAndReset();
                    sendMetrics(snapshot);

                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("Worker error: " + e.getMessage());
                }
            }
        });

        worker.setDaemon(true);
        worker.setName("db-monitor-dispatcher");
        worker.start();
    }

    private static void sendBatch(List<Object> events) {
        System.out.println("Sending events to backend... size=" + events.size());

        try {
            String json = mapper.writeValueAsString(events);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COLLECTOR_URL + "/events"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Failed response: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Failed to send events: " + e.getMessage());
        }
    }

    private static void sendMetrics(MetricsSnapshot metrics) {
        try {
            String json = mapper.writeValueAsString(metrics);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(COLLECTOR_URL + "/metrics"))
                    .header("Content-Type", "application/json")
                    // NEW: tag the snapshot so the collector knows which JVM sent it
                    .header("X-App-Name",  AgentConfig.APP_NAME)
                    .header("X-Host-Name", HostUtil.getHostName())
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                System.out.println("Metrics send failed: " + response.body());
            }
        } catch (Exception e) {
            System.out.println("Failed to send metrics: " + e.getMessage());
        }
    }
}
