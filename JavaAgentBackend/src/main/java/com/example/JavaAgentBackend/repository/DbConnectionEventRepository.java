package com.example.JavaAgentBackend.repository;

import com.example.JavaAgentBackend.entity.DbConnectionEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbConnectionEventRepository extends JpaRepository<DbConnectionEventEntity,Long> {
}
