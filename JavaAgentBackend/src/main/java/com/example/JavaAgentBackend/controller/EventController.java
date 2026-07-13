package com.example.JavaAgentBackend.controller;

import com.example.JavaAgentBackend.dto.EventDTO;
import com.example.JavaAgentBackend.dto.EventStatsDTO;
import com.example.JavaAgentBackend.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    public void receive(@RequestBody List<EventDTO> events) {
        if (events == null || events.isEmpty()) return;
        System.out.println("Received events: " + events.size());
        eventService.processEvents(events);
    }

    @GetMapping
    public ResponseEntity<List<EventDTO>> getEvents(
            @RequestParam(defaultValue = "100") int limit,
            @RequestParam(required = false) String appName,
            @RequestParam(required = false) String operationType) {
        return ResponseEntity.ok(eventService.getRecentEvents(limit, appName, operationType));
    }

    @GetMapping("/slow-queries")
    public ResponseEntity<List<EventDTO>> getSlowQueries(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(eventService.getSlowQueries(limit));
    }

    @GetMapping("/applications")
    public ResponseEntity<List<String>> getApplicationNames() {
        return ResponseEntity.ok(eventService.getApplicationNames());
    }

    @GetMapping("/stats")
    public ResponseEntity<EventStatsDTO> getStats() {
        return ResponseEntity.ok(eventService.getStats());
    }
}
