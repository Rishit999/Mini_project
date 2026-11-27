package com.evchargeflow.engine1.entity;

import com.evchargeflow.common.enums.ConnectorType;
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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @Column(name = "vehicle_id", length = 32)
    private String vehicleId;

    @NotBlank
    @Column(name = "registration_number", nullable = false, unique = true, length = 32)
    private String registrationNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id")
    private UserAccount owner;

    @NotBlank
    @Column(name = "owner_name", nullable = false, length = 100)
    private String ownerName;

    @NotBlank
    @Column(name = "model", nullable = false, length = 100)
    private String model;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 24)
    private ConnectorType connectorType;

    @DecimalMin("10.0")
    @DecimalMax("250.0")
    @Column(name = "battery_capacity_kwh", nullable = false)
    private double batteryCapacityKwh;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Column(name = "current_battery_percent", nullable = false)
    private double currentBatteryPercent;

    public Vehicle() {
    }

    public Vehicle(
            String vehicleId,
            String registrationNumber,
            UserAccount owner,
            String ownerName,
            String model,
            ConnectorType connectorType,
            double batteryCapacityKwh,
            double currentBatteryPercent) {
        this.vehicleId = vehicleId;
        this.registrationNumber = registrationNumber;
        this.owner = owner;
        this.ownerName = ownerName;
        this.model = model;
        this.connectorType = connectorType;
        this.batteryCapacityKwh = batteryCapacityKwh;
        this.currentBatteryPercent = currentBatteryPercent;
    }

    public String getVehicleId() {
        return vehicleId;
    }

    public void setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public UserAccount getOwner() {
        return owner;
    }

    public void setOwner(UserAccount owner) {
        this.owner = owner;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ConnectorType getConnectorType() {
        return connectorType;
    }

    public void setConnectorType(ConnectorType connectorType) {
        this.connectorType = connectorType;
    }

    public double getBatteryCapacityKwh() {
        return batteryCapacityKwh;
    }

    public void setBatteryCapacityKwh(double batteryCapacityKwh) {
        this.batteryCapacityKwh = batteryCapacityKwh;
    }

    public double getCurrentBatteryPercent() {
        return currentBatteryPercent;
    }

    public void setCurrentBatteryPercent(double currentBatteryPercent) {
        this.currentBatteryPercent = currentBatteryPercent;
    }
}
