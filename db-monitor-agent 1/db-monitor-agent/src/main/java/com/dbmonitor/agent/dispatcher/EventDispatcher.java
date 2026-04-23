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

                    // Drain up to 50 events
                    queue.drainTo(batch, BATCH_SIZE);

                    if (!batch.isEmpty()) {
                        sendBatch(batch);
                    }

                    // Send aggregated metrics every cycle
                    MetricsSnapshot snapshot = MetricsCollector.snapshotAndReset();
                    sendMetrics(snapshot);

                    // configurable interval (currently 1 second)
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
                    .uri(URI.create("http://localhost:8081/events"))
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
                    .uri(URI.create("http://localhost:8081/metrics"))
                    .header("Content-Type", "application/json")
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