package com.dbmonitor.agent.dispatcher;

import com.dbmonitor.agent.model.DbConnectionEvent;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EventDispatcher {

    private static final BlockingQueue<DbConnectionEvent> queue = new LinkedBlockingQueue<>();
    private static final HttpClient client = HttpClient.newHttpClient();
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        startWorker();
    }
    public static void publish(DbConnectionEvent event) {

        queue.offer(event);
    }


    private static void startWorker() {
        Thread worker = new Thread(() -> {
            while (true) {
                try {
                    List<DbConnectionEvent> batch = new ArrayList<>();
                    queue.drainTo(batch, 50);

                    if (!batch.isEmpty()) {
                        sendBatch(batch);
                    }

                    Thread.sleep(1000);
                } catch (Exception e) {
                    System.out.println("Worker error: " + e.getMessage());
                }
            }
        });

        worker.setDaemon(true);
        worker.start();
    }

    private static void sendBatch(List<DbConnectionEvent> events) {
        System.out.println("Sending events to backend...");
        try {
            String json = mapper.writeValueAsString(events);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8081/events"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());

        } catch (Exception e) {
            System.out.println("Failed to send events: " + e.getMessage());
        }
    }
}