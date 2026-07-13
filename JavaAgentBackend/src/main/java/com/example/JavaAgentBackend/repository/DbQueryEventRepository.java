package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbQueryEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DbQueryEventRepository extends JpaRepository<DbQueryEventEntity, Long> {

    Page<DbQueryEventEntity> findAllByOrderByTimestampDesc(Pageable pageable);

    Page<DbQueryEventEntity> findByApplicationNameOrderByTimestampDesc(String applicationName, Pageable pageable);

    Page<DbQueryEventEntity> findByOperationTypeOrderByTimestampDesc(String operationType, Pageable pageable);

    Page<DbQueryEventEntity> findByApplicationNameAndOperationTypeOrderByTimestampDesc(
            String applicationName, String operationType, Pageable pageable);

    Page<DbQueryEventEntity> findBySlowTrueOrderByDurationNsDesc(Pageable pageable);

    long countBySuccessFalse();

    long countByApplicationName(String applicationName);

    long countByOperationType(String operationType);

    @Query("SELECT DISTINCT e.applicationName FROM DbQueryEventEntity e WHERE e.applicationName IS NOT NULL")
    List<String> findDistinctApplicationNames();
}
