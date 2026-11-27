package com.evchargeflow.engine1.repository;

import com.evchargeflow.engine1.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findTop50ByOrderByTimestampDescIdDesc();
}
