package com.example.JavaAgentBackend.controller;

import com.example.JavaAgentBackend.entity.DbConnectionEventEntity;
import com.example.JavaAgentBackend.model.DbConnectionEvent;
import com.example.JavaAgentBackend.repository.DbConnectionEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/events")
public class EventController {
    @Autowired
   DbConnectionEventRepository repository;

    @PostMapping
    public void receive(@RequestBody List<DbConnectionEvent> events) {
        System.out.println("Receiving data");
        List<DbConnectionEventEntity> entities = events.stream().map(e -> {
            DbConnectionEventEntity entity = new DbConnectionEventEntity();
            entity.setApplicationName(e.getApplicationName());
            entity.setHostName(e.getHostName());
            entity.setJvmId(e.getJvmId());
            entity.setDatabaseType(e.getDatabaseType());
            entity.setOperationType(e.getOperationType());
            entity.setTimestamp(e.getTimestamp());
            entity.setDurationNs(e.getDurationNs());
            entity.setSuccess(e.isSuccess());
            entity.setMetadata(e.getMetadata());
            return entity;
        }).toList();
        repository.saveAll(entities);
    }
}
