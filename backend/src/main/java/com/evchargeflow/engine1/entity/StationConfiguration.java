package com.evchargeflow.engine1.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Persistable;

import java.time.Instant;

@Entity
@Table(name = "station_configurations")
public class StationConfiguration implements Persistable<String> {

    @Id
    @Column(name = "station_id", length = 32)
    private String stationId;

    @OneToOne(fetch = FetchType.EAGER)
    @MapsId
    @JoinColumn(name = "station_id")
    private ChargingStation station;

    @Transient
    private boolean newEntity = true;

    @Min(3)
    @Max(300)
    @Column(name = "queue_refresh_interval_sec", nullable = false)
    private int queueRefreshIntervalSec = 10;

    @Min(5)
    @Max(60)
    @Column(name = "reservation_grace_period_min", nullable = false)
    private int reservationGracePeriodMinutes = 15;

    @DecimalMin("0.50")
    @DecimalMax("1.00")
    @Column(name = "charging_efficiency_factor", nullable = false)
    private double chargingEfficiencyFactor = 0.90;

    @Min(10)
    @Max(480)
    @Column(name = "default_charging_duration_min", nullable = false)
    private int defaultChargingDurationMinutes = 45;

    @Column(name = "auto_allocation_enabled", nullable = false)
    private boolean autoAllocationEnabled = true;

    @DecimalMin("0.5")
    @DecimalMax("20.0")
    @Column(name = "simulation_speed_multiplier", nullable = false)
    private double simulationSpeedMultiplier = 1.0;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public StationConfiguration() {
    }

    public StationConfiguration(ChargingStation station) {
        this.station = station;
        this.stationId = station.getStationId();
        this.updatedAt = Instant.now();
        this.newEntity = true;
    }

    @Override
    public String getId() {
        return stationId;
    }

    @Override
    public boolean isNew() {
        return newEntity;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.newEntity = false;
    }

    public String getStationId() {
        return stationId;
    }

    public void setStationId(String stationId) {
        this.stationId = stationId;
    }

    public ChargingStation getStation() {
        return station;
    }

    public void setStation(ChargingStation station) {
        this.station = station;
    }

    public int getQueueRefreshIntervalSec() {
        return queueRefreshIntervalSec;
    }

    public void setQueueRefreshIntervalSec(int queueRefreshIntervalSec) {
        this.queueRefreshIntervalSec = queueRefreshIntervalSec;
    }

    public int getReservationGracePeriodMinutes() {
        return reservationGracePeriodMinutes;
    }

    public void setReservationGracePeriodMinutes(int reservationGracePeriodMinutes) {
        this.reservationGracePeriodMinutes = reservationGracePeriodMinutes;
    }

    public double getChargingEfficiencyFactor() {
        return chargingEfficiencyFactor;
    }

    public void setChargingEfficiencyFactor(double chargingEfficiencyFactor) {
        this.chargingEfficiencyFactor = chargingEfficiencyFactor;
    }

    public int getDefaultChargingDurationMinutes() {
        return defaultChargingDurationMinutes;
    }

    public void setDefaultChargingDurationMinutes(int defaultChargingDurationMinutes) {
        this.defaultChargingDurationMinutes = defaultChargingDurationMinutes;
    }

    public boolean isAutoAllocationEnabled() {
        return autoAllocationEnabled;
    }

    public void setAutoAllocationEnabled(boolean autoAllocationEnabled) {
        this.autoAllocationEnabled = autoAllocationEnabled;
    }

    public double getSimulationSpeedMultiplier() {
        return simulationSpeedMultiplier;
    }

    public void setSimulationSpeedMultiplier(double simulationSpeedMultiplier) {
        this.simulationSpeedMultiplier = simulationSpeedMultiplier;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
