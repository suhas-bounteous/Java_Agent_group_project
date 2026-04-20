package com.dbmonitor.collector.repository;

import com.dbmonitor.collector.entity.MetricsSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MetricsSnapshotRepository extends JpaRepository<MetricsSnapshotEntity, Long> {

    @Query("SELECT m FROM MetricsSnapshotEntity m WHERE m.receivedAt >= :since ORDER BY m.receivedAt ASC")
    List<MetricsSnapshotEntity> findSince(@Param("since") long since);

    @Query("SELECT m FROM MetricsSnapshotEntity m WHERE m.applicationName = :app AND m.receivedAt >= :since ORDER BY m.receivedAt ASC")
    List<MetricsSnapshotEntity> findByAppSince(@Param("app") String app, @Param("since") long since);

    @Query("SELECT DISTINCT m.applicationName FROM MetricsSnapshotEntity m")
    List<String> findDistinctApps();
}
