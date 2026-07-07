package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbMetricsEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface DbMetricsRepository extends JpaRepository<DbMetricsEntity, Long> {
    Optional<DbMetricsEntity> findTopByOrderByTimestampDesc();
    // FIX: renamed from findAllByOrderByTimestampAsc — history was returning oldest rows first
    Page<DbMetricsEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT SUM(e.totalQueries), SUM(e.totalUpdates), SUM(e.totalErrors), " +
            "SUM(e.totalLatencyNs), SUM(e.commits), SUM(e.rollbacks), SUM(e.slowQueries) " +
            "FROM DbMetricsEntity e")
    Object[] getAggregatedMetrics();
}