package com.dbmonitor.collector.repository;

import com.dbmonitor.collector.entity.QueryEventEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QueryEventRepository extends JpaRepository<QueryEventEntity, Long> {

    @Query("SELECT q FROM QueryEventEntity q WHERE q.slow = true ORDER BY q.durationNs DESC")
    List<QueryEventEntity> findTopSlow(Pageable pageable);

    @Query("SELECT q FROM QueryEventEntity q ORDER BY q.timestamp DESC")
    List<QueryEventEntity> findRecent(Pageable pageable);

    @Query("SELECT DISTINCT q.applicationName FROM QueryEventEntity q")
    List<String> findDistinctApps();

    @Query("SELECT COUNT(q) FROM QueryEventEntity q WHERE q.applicationName = :app AND q.timestamp >= :since")
    long countByAppSince(@Param("app") String app, @Param("since") long since);
}
