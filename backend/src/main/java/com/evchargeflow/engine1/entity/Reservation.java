package com.evchargeflow.engine1.entity;

import com.evchargeflow.common.enums.ReservationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @Column(name = "reservation_id", length = 64)
    private String reservationId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "charger_id", nullable = false)
    private Charger charger;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 24)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(name = "notes", length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Reservation() {
    }

    /**
     * Overlap check rule: ExistingStart < RequestedEnd AND RequestedStart < ExistingEnd.
     * Adjacent slots (e.g. end == other.start) are allowed and return false.
     */
    public boolean overlaps(Instant requestedStart, Instant requestedEnd) {
        return startTime.isBefore(requestedEnd) && requestedStart.isBefore(endTime);
    }

    public String getReservationId() {
        return reservationId;
    }

    public void setReservationId(String reservationId) {
        this.reservationId = reservationId;
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

    public Charger getCharger() {
        return charger;
    }

    public void setCharger(Charger charger) {
        this.charger = charger;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public void setStartTime(Instant startTime) {
        this.startTime = startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public void setEndTime(Instant endTime) {
        this.endTime = endTime;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
