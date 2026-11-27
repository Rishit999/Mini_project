package com.evchargeflow.engine1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp = Instant.now();

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "action", nullable = false, length = 64)
    private String action;

    @Column(name = "details", nullable = false, length = 512)
    private String details;

    public AuditLog() {
    }

    public AuditLog(Instant timestamp, String category, String action, String details) {
        this.timestamp = timestamp;
        this.category = category;
        this.action = action;
        this.details = details;
    }

    public Long getId() {
        return id;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getCategory() {
        return category;
    }

    public String getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }
}
