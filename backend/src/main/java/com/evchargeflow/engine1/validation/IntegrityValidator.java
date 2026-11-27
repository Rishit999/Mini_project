package com.evchargeflow.engine1.validation;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.common.exception.DomainValidationException;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.entity.VehicleRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Engine 1: Centralized domain validation and state-transition integrity checks.
 */
@Component
public class IntegrityValidator {

    public void validateStation(ChargingStation station) {
        if (station == null) {
            throw new DomainValidationException("VAL_NULL_STATION", "Charging station must not be null.");
        }
        if (isBlank(station.getStationId())) {
            throw new DomainValidationException("VAL_STATION_ID", "Station ID is required.");
        }
        if (isBlank(station.getName())) {
            throw new DomainValidationException("VAL_STATION_NAME", "Station name is required.");
        }
        if (isBlank(station.getLocation())) {
            throw new DomainValidationException("VAL_STATION_LOCATION", "Station location is required.");
        }
        if (station.getStatus() == null) {
            throw new DomainValidationException("VAL_STATION_STATUS", "Station status is required.");
        }
        if (station.getMaxPowerCapacityKw() < 10.0 || station.getMaxPowerCapacityKw() > 5000.0) {
            throw new DomainValidationException("VAL_STATION_POWER",
                    "Station maximum power capacity must be between 10 and 5000 kW.");
        }
    }

    public void validateStationConfig(StationConfiguration config) {
        if (config == null) {
            throw new DomainValidationException("VAL_NULL_CONFIG", "Station configuration must not be null.");
        }
        if (config.getQueueRefreshIntervalSec() < 3 || config.getQueueRefreshIntervalSec() > 300) {
            throw new DomainValidationException("VAL_REFRESH_INTERVAL",
                    "Queue refresh interval must be between 3 and 300 seconds.");
        }
        if (config.getReservationGracePeriodMinutes() < 5 || config.getReservationGracePeriodMinutes() > 60) {
            throw new DomainValidationException("VAL_GRACE_PERIOD",
                    "Reservation grace period must be between 5 and 60 minutes.");
        }
        if (config.getChargingEfficiencyFactor() < 0.50 || config.getChargingEfficiencyFactor() > 1.00) {
            throw new DomainValidationException("VAL_EFFICIENCY",
                    "Charging efficiency factor must be between 0.50 and 1.00.");
        }
        if (config.getDefaultChargingDurationMinutes() < 10 || config.getDefaultChargingDurationMinutes() > 480) {
            throw new DomainValidationException("VAL_DEFAULT_DURATION",
                    "Default charging duration must be between 10 and 480 minutes.");
        }
        if (config.getSimulationSpeedMultiplier() < 0.5 || config.getSimulationSpeedMultiplier() > 20.0) {
            throw new DomainValidationException("VAL_SIM_SPEED",
                    "Simulation speed multiplier must be between 0.5 and 20.0.");
        }
    }

    public void validateCharger(Charger charger) {
        if (charger == null) {
            throw new DomainValidationException("VAL_NULL_CHARGER", "Charger must not be null.");
        }
        if (isBlank(charger.getChargerId())) {
            throw new DomainValidationException("VAL_CHARGER_ID", "Charger ID is required.");
        }
        if (charger.getStation() == null) {
            throw new DomainValidationException("REF_STATION_MISSING", "Charger must belong to a valid station.");
        }
        if (isBlank(charger.getName())) {
            throw new DomainValidationException("VAL_CHARGER_NAME", "Charger name is required.");
        }
        if (charger.getChargerType() == null) {
            throw new DomainValidationException("VAL_CHARGER_TYPE", "Charger type is required.");
        }
        if (charger.getPowerRatingKw() < 3.7 || charger.getPowerRatingKw() > 350.0) {
            throw new DomainValidationException("VAL_CHARGER_POWER",
                    "Charger power rating must be between 3.7 and 350 kW.");
        }
        if (charger.getSupportedConnectors() == null || charger.getSupportedConnectors().isEmpty()) {
            throw new DomainValidationException("VAL_CHARGER_CONNECTORS",
                    "Charger must support at least one connector type.");
        }
    }

    public void validateVehicle(Vehicle vehicle) {
        if (vehicle == null) {
            throw new DomainValidationException("VAL_NULL_VEHICLE", "Vehicle must not be null.");
        }
        if (isBlank(vehicle.getVehicleId())) {
            throw new DomainValidationException("VAL_VEHICLE_ID", "Vehicle ID is required.");
        }
        if (isBlank(vehicle.getRegistrationNumber())) {
            throw new DomainValidationException("VAL_REGISTRATION", "Vehicle registration number is required.");
        }
        if (isBlank(vehicle.getOwnerName())) {
            throw new DomainValidationException("VAL_OWNER_NAME", "Owner name is required.");
        }
        if (isBlank(vehicle.getModel())) {
            throw new DomainValidationException("VAL_MODEL", "Vehicle model is required.");
        }
        if (vehicle.getConnectorType() == null) {
            throw new DomainValidationException("VAL_CONNECTOR", "Connector type is required.");
        }
        if (vehicle.getBatteryCapacityKwh() < 10.0 || vehicle.getBatteryCapacityKwh() > 250.0) {
            throw new DomainValidationException("VAL_BATTERY_CAP",
                    "Battery capacity must be between 10 and 250 kWh.");
        }
        validateBatteryPercent(vehicle.getCurrentBatteryPercent(), "Current battery percentage");
    }

    public void validateChargingRequestInput(
            Vehicle vehicle,
            ChargingStation station,
            double currentBatteryPercent,
            Double targetBatteryPercent,
            int expectedDurationMinutes,
            Instant desiredDepartureTime,
            Instant now) {
        if (vehicle == null) {
            throw new DomainValidationException("REF_VEHICLE_MISSING", "Referenced vehicle does not exist.");
        }
        if (station == null) {
            throw new DomainValidationException("REF_STATION_MISSING", "Referenced station does not exist.");
        }
        validateBatteryPercent(currentBatteryPercent, "Current battery percentage");
        if (targetBatteryPercent != null) {
            validateBatteryPercent(targetBatteryPercent, "Target battery percentage");
            if (targetBatteryPercent <= currentBatteryPercent) {
                throw new DomainValidationException("VAL_TARGET_BATTERY",
                        "Target battery percentage must be greater than current battery percentage.");
            }
        }
        if (expectedDurationMinutes < 5 || expectedDurationMinutes > 480) {
            throw new DomainValidationException("VAL_DURATION",
                    "Expected charging duration must be between 5 and 480 minutes.");
        }
        if (desiredDepartureTime != null && !desiredDepartureTime.isAfter(now.minusSeconds(60))) {
            throw new DomainValidationException("VAL_DEPARTURE_TIME",
                    "Desired departure time cannot be in the past.");
        }
    }

    public void validateReservation(Reservation reservation, Instant now) {
        if (reservation == null) {
            throw new DomainValidationException("VAL_NULL_RESERVATION", "Reservation must not be null.");
        }
        Vehicle vehicle = reservation.getVehicle();
        ChargingStation station = reservation.getStation();
        Charger charger = reservation.getCharger();

        if (vehicle == null) {
            throw new DomainValidationException("REF_VEHICLE_MISSING", "Referenced vehicle does not exist.");
        }
        if (station == null) {
            throw new DomainValidationException("REF_STATION_MISSING", "Referenced station does not exist.");
        }
        if (charger == null) {
            throw new DomainValidationException("REF_CHARGER_MISSING", "Referenced charger does not exist.");
        }
        if (!charger.getStation().getStationId().equals(station.getStationId())) {
            throw new DomainValidationException("REF_STATION_CHARGER_MISMATCH",
                    "Selected charger does not belong to the selected station.");
        }
        if (station.getStatus() != StationStatus.OPERATIONAL) {
            throw new DomainValidationException("STATE_STATION_UNAVAILABLE",
                    "Reservations can only be created at operational stations.");
        }
        if (charger.getOperationalStatus() != ChargerOperationalStatus.OPERATIONAL) {
            throw new DomainValidationException("STATE_CHARGER_MAINTENANCE",
                    "Cannot reserve a charger that is not operational.");
        }
        if (!charger.supports(vehicle.getConnectorType())) {
            throw new DomainValidationException("COMPAT_CONNECTOR",
                    "Charger " + charger.getChargerId() + " does not support vehicle connector "
                            + vehicle.getConnectorType() + ".");
        }
        if (reservation.getStartTime() == null || reservation.getEndTime() == null) {
            throw new DomainValidationException("VAL_RES_TIMESTAMPS",
                    "Reservation start and end times are required.");
        }
        if (!reservation.getEndTime().isAfter(reservation.getStartTime())) {
            throw new DomainValidationException("VAL_RES_WINDOW",
                    "Reservation end time must be strictly after start time.");
        }
        if (reservation.getEndTime().isBefore(now)) {
            throw new DomainValidationException("VAL_RES_PAST",
                    "Reservation time slot cannot end in the past.");
        }
        long durationMinutes = java.time.Duration.between(reservation.getStartTime(), reservation.getEndTime()).toMinutes();
        if (durationMinutes < 10 || durationMinutes > 480) {
            throw new DomainValidationException("VAL_RES_DURATION",
                    "Reservation duration must be between 10 and 480 minutes.");
        }
    }

    public void validateRequestTransition(VehicleRequest request, RequestStatus target) {
        RequestStatus current = request.getStatus();
        boolean valid = switch (current) {
            case QUEUED -> target == RequestStatus.ASSIGNED
                    || target == RequestStatus.CHARGING
                    || target == RequestStatus.CANCELLED
                    || target == RequestStatus.REJECTED;
            case ASSIGNED -> target == RequestStatus.CHARGING
                    || target == RequestStatus.QUEUED
                    || target == RequestStatus.CANCELLED;
            case CHARGING -> target == RequestStatus.COMPLETED;
            case COMPLETED, CANCELLED, REJECTED -> false;
        };
        if (!valid) {
            throw new DomainValidationException("STATE_INVALID_REQ",
                    "Invalid request status transition: " + current + " -> " + target);
        }
    }

    public void validateReservationTransition(Reservation reservation, ReservationStatus target) {
        ReservationStatus current = reservation.getStatus();
        boolean valid = switch (current) {
            case CONFIRMED -> target == ReservationStatus.ACTIVE
                    || target == ReservationStatus.EXPIRED
                    || target == ReservationStatus.CANCELLED;
            case ACTIVE -> target == ReservationStatus.COMPLETED || target == ReservationStatus.CANCELLED;
            case COMPLETED, EXPIRED, CANCELLED -> false;
        };
        if (!valid) {
            throw new DomainValidationException("STATE_INVALID_RES",
                    "Invalid reservation status transition: " + current + " -> " + target);
        }
    }

    public void validateChargerOperationalChange(Charger charger, ChargerOperationalStatus target) {
        if (charger.getAvailabilityStatus() == ChargerAvailabilityStatus.OCCUPIED
                && target != ChargerOperationalStatus.OPERATIONAL) {
            throw new DomainValidationException("STATE_CHARGER_BUSY",
                    "Cannot change operational status while charger is actively occupied.");
        }
    }

    public void validateBatteryPercent(double value, String fieldLabel) {
        if (Double.isNaN(value) || value < 0.0 || value > 100.0) {
            throw new DomainValidationException("VAL_BATTERY_PERCENT",
                    fieldLabel + " must be between 0 and 100.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
