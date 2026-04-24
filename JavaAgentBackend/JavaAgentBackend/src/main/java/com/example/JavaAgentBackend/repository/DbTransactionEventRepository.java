package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbTransactionEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbTransactionEventRepository extends JpaRepository<DbTransactionEventEntity, Long> {

    Page<DbTransactionEventEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    long countByOperationType(String operationType);
}
