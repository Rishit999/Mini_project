package com.evchargeflow.engine3.controller;

import com.evchargeflow.engine1.entity.AuditLog;
import com.evchargeflow.engine1.service.AuditLogService;
import com.evchargeflow.engine3.api.ApiDtos.AnalyticsResponseDto;
import com.evchargeflow.engine3.api.ApiDtos.DashboardResponseDto;
import com.evchargeflow.engine3.integration.DashboardAnalyticsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardAnalyticsService dashboardAnalyticsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public DashboardController(
            DashboardAnalyticsService dashboardAnalyticsService,
            AuditLogService auditLogService,
            Clock clock) {
        this.dashboardAnalyticsService = dashboardAnalyticsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("status", "UP", "service", "EVChargeFlow", "timestamp", clock.instant());
    }

    @GetMapping("/dashboard")
    public DashboardResponseDto getDashboard() {
        return dashboardAnalyticsService.getDashboard();
    }

    @GetMapping("/analytics")
    public AnalyticsResponseDto getAnalytics() {
        return dashboardAnalyticsService.getAnalytics();
    }

    @GetMapping("/audit")
    public List<AuditLog> getAuditLogs() {
        return auditLogService.getRecentLogs();
    }
}
