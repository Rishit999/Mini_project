package com.evchargeflow.engine1.service;

import com.evchargeflow.engine1.entity.AuditLog;
import com.evchargeflow.engine1.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final Clock clock;

    public AuditLogService(AuditLogRepository auditLogRepository, Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.clock = clock;
    }

    @Transactional
    public void log(String category, String action, String details) {
        String safeDetails = details != null && details.length() > 500 ? details.substring(0, 500) : details;
        auditLogRepository.save(new AuditLog(clock.instant(), category, action, safeDetails == null ? "" : safeDetails));
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getRecentLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDescIdDesc();
    }
}
