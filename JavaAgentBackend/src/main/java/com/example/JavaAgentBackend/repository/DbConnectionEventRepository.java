package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbConnectionEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbConnectionEventRepository extends JpaRepository<DbConnectionEventEntity, Long> {

    Page<DbConnectionEventEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    long countByOperationType(String operationType);

    @Query("SELECT DISTINCT e.applicationName FROM DbConnectionEventEntity e WHERE e.applicationName IS NOT NULL")
    List<String> findDistinctApplicationNames();
}
