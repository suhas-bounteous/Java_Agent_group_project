package com.dbmonitor.collector.repository;

import com.dbmonitor.collector.entity.ConnectionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConnectionEventRepository extends JpaRepository<ConnectionEventEntity, Long> {
}
