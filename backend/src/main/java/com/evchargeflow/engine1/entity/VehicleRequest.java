package com.evchargeflow.engine1.entity;

import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.RequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "vehicle_requests")
public class VehicleRequest {

    @Id
    @Column(name = "request_id", length = 64)
    private String requestId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "current_battery_percent", nullable = false)
    private double currentBatteryPercent;

    @Column(name = "target_battery_percent")
    private Double targetBatteryPercent;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "required_connector", nullable = false, length = 24)
    private ConnectorType requiredConnector;

    @Min(5)
    @Max(480)
    @Column(name = "expected_duration_minutes", nullable = false)
    private int expectedDurationMinutes;

    @Column(name = "desired_departure_time")
    private Instant desiredDepartureTime;

    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private Instant arrivalTime;

    @Column(name = "battery_score", nullable = false)
    private int batteryScore;

    @Column(name = "departure_score", nullable = false)
    private int departureScore;

    @Column(name = "waiting_time_score", nullable = false)
    private int waitingTimeScore;

    @Column(name = "priority_score", nullable = false)
    private int priorityScore;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private RequestStatus status = RequestStatus.QUEUED;

    @Column(name = "assigned_charger_id", length = 32)
    private String assignedChargerId;

    @Column(name = "estimated_wait_minutes", nullable = false)
    private double estimatedWaitMinutes = 0.0;

    @Column(name = "status_reason", length = 255)
    private String statusReason;

    public VehicleRequest() {
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

    public ChargingStation getStation() {
        return station;
    }

    public void setStation(ChargingStation station) {
        this.station = station;
    }

    public double getCurrentBatteryPercent() {
        return currentBatteryPercent;
    }

    public void setCurrentBatteryPercent(double currentBatteryPercent) {
        this.currentBatteryPercent = currentBatteryPercent;
    }

    public Double getTargetBatteryPercent() {
        return targetBatteryPercent;
    }

    public void setTargetBatteryPercent(Double targetBatteryPercent) {
        this.targetBatteryPercent = targetBatteryPercent;
    }

    public ConnectorType getRequiredConnector() {
        return requiredConnector;
    }

    public void setRequiredConnector(ConnectorType requiredConnector) {
        this.requiredConnector = requiredConnector;
    }

    public int getExpectedDurationMinutes() {
        return expectedDurationMinutes;
    }

    public void setExpectedDurationMinutes(int expectedDurationMinutes) {
        this.expectedDurationMinutes = expectedDurationMinutes;
    }

    public Instant getDesiredDepartureTime() {
        return desiredDepartureTime;
    }

    public void setDesiredDepartureTime(Instant desiredDepartureTime) {
        this.desiredDepartureTime = desiredDepartureTime;
    }

    public Instant getArrivalTime() {
        return arrivalTime;
    }

    public void setArrivalTime(Instant arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public int getBatteryScore() {
        return batteryScore;
    }

    public void setBatteryScore(int batteryScore) {
        this.batteryScore = batteryScore;
    }

    public int getDepartureScore() {
        return departureScore;
    }

    public void setDepartureScore(int departureScore) {
        this.departureScore = departureScore;
    }

    public int getWaitingTimeScore() {
        return waitingTimeScore;
    }

    public void setWaitingTimeScore(int waitingTimeScore) {
        this.waitingTimeScore = waitingTimeScore;
    }

    public int getPriorityScore() {
        return priorityScore;
    }

    public void setPriorityScore(int priorityScore) {
        this.priorityScore = priorityScore;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public String getAssignedChargerId() {
        return assignedChargerId;
    }

    public void setAssignedChargerId(String assignedChargerId) {
        this.assignedChargerId = assignedChargerId;
    }

    public double getEstimatedWaitMinutes() {
        return estimatedWaitMinutes;
    }

    public void setEstimatedWaitMinutes(double estimatedWaitMinutes) {
        this.estimatedWaitMinutes = estimatedWaitMinutes;
    }

    public String getStatusReason() {
        return statusReason;
    }

    public void setStatusReason(String statusReason) {
        this.statusReason = statusReason;
    }
}
