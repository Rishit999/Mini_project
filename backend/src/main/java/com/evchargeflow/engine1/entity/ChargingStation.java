package com.evchargeflow.engine1.entity;

import com.evchargeflow.common.enums.StationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "charging_stations")
public class ChargingStation {

    @Id
    @Column(name = "station_id", length = 32)
    private String stationId;

    @NotBlank
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @NotBlank
    @Column(name = "location", nullable = false, length = 200)
    private String location;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private StationStatus status = StationStatus.OPERATIONAL;

    @DecimalMin("10.0")
    @DecimalMax("5000.0")
    @Column(name = "max_power_capacity_kw", nullable = false)
    private double maxPowerCapacityKw;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public ChargingStation() {
    }

    public ChargingStation(String stationId, String name, String location, StationStatus status, double maxPowerCapacityKw) {
        this.stationId = stationId;
        this.name = name;
        this.location = location;
        this.status = status;
        this.maxPowerCapacityKw = maxPowerCapacityKw;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public StationStatus getStatus() {
        return status;
    }

    public void setStatus(StationStatus status) {
        this.status = status;
    }

    public double getMaxPowerCapacityKw() {
        return maxPowerCapacityKw;
    }

    public void setMaxPowerCapacityKw(double maxPowerCapacityKw) {
        this.maxPowerCapacityKw = maxPowerCapacityKw;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
