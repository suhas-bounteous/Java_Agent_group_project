package com.dbmonitor.collector.repository;

import com.dbmonitor.collector.entity.TransactionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionEventRepository extends JpaRepository<TransactionEventEntity, Long> {
}
