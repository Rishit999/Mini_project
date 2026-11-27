package com.evchargeflow.engine1.entity;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.ChargerType;
import com.evchargeflow.common.enums.ConnectorType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
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
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "chargers")
public class Charger {

    @Id
    @Column(name = "charger_id", length = 32)
    private String chargerId;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "station_id", nullable = false)
    private ChargingStation station;

    @NotBlank
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "charger_type", nullable = false, length = 24)
    private ChargerType chargerType;

    @DecimalMin("3.7")
    @DecimalMax("350.0")
    @Column(name = "power_rating_kw", nullable = false)
    private double powerRatingKw;

    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "charger_connectors", joinColumns = @JoinColumn(name = "charger_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "connector_type", nullable = false, length = 24)
    private Set<ConnectorType> supportedConnectors = new LinkedHashSet<>();

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "operational_status", nullable = false, length = 24)
    private ChargerOperationalStatus operationalStatus = ChargerOperationalStatus.OPERATIONAL;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "availability_status", nullable = false, length = 24)
    private ChargerAvailabilityStatus availabilityStatus = ChargerAvailabilityStatus.AVAILABLE;

    @Column(name = "current_vehicle_id", length = 32)
    private String currentVehicleId;

    @Column(name = "current_request_id", length = 64)
    private String currentRequestId;

    public Charger() {
    }

    public Charger(
            String chargerId,
            ChargingStation station,
            String name,
            ChargerType chargerType,
            double powerRatingKw,
            Set<ConnectorType> supportedConnectors) {
        this.chargerId = chargerId;
        this.station = station;
        this.name = name;
        this.chargerType = chargerType;
        this.powerRatingKw = powerRatingKw;
        this.supportedConnectors = new LinkedHashSet<>(supportedConnectors);
        this.operationalStatus = ChargerOperationalStatus.OPERATIONAL;
        this.availabilityStatus = ChargerAvailabilityStatus.AVAILABLE;
    }

    public boolean supports(ConnectorType connector) {
        return supportedConnectors != null && supportedConnectors.contains(connector);
    }

    public boolean isOperationalAndAvailable() {
        return operationalStatus == ChargerOperationalStatus.OPERATIONAL
                && availabilityStatus == ChargerAvailabilityStatus.AVAILABLE;
    }

    public String getChargerId() {
        return chargerId;
    }

    public void setChargerId(String chargerId) {
        this.chargerId = chargerId;
    }

    public ChargingStation getStation() {
        return station;
    }

    public void setStation(ChargingStation station) {
        this.station = station;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChargerType getChargerType() {
        return chargerType;
    }

    public void setChargerType(ChargerType chargerType) {
        this.chargerType = chargerType;
    }

    public double getPowerRatingKw() {
        return powerRatingKw;
    }

    public void setPowerRatingKw(double powerRatingKw) {
        this.powerRatingKw = powerRatingKw;
    }

    public Set<ConnectorType> getSupportedConnectors() {
        return supportedConnectors;
    }

    public void setSupportedConnectors(Set<ConnectorType> supportedConnectors) {
        this.supportedConnectors = supportedConnectors == null ? new LinkedHashSet<>() : new LinkedHashSet<>(supportedConnectors);
    }

    public ChargerOperationalStatus getOperationalStatus() {
        return operationalStatus;
    }

    public void setOperationalStatus(ChargerOperationalStatus operationalStatus) {
        this.operationalStatus = operationalStatus;
    }

    public ChargerAvailabilityStatus getAvailabilityStatus() {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(ChargerAvailabilityStatus availabilityStatus) {
        this.availabilityStatus = availabilityStatus;
    }

    public String getCurrentVehicleId() {
        return currentVehicleId;
    }

    public void setCurrentVehicleId(String currentVehicleId) {
        this.currentVehicleId = currentVehicleId;
    }

    public String getCurrentRequestId() {
        return currentRequestId;
    }

    public void setCurrentRequestId(String currentRequestId) {
        this.currentRequestId = currentRequestId;
    }
}
