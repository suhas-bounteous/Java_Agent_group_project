package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbMetricsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DbMetricsRepository extends JpaRepository<DbMetricsEntity, Long> {
    Optional<DbMetricsEntity> findTopByOrderByTimestampDesc();
    Page<DbMetricsEntity> findAllByOrderByTimestampAsc(Pageable pageable);
}
