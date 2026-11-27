package com.evchargeflow.engine2.allocation;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.VehicleRequest;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

/**
 * Engine 2: Intelligent charger allocation respecting operational status,
 * connector compatibility, station power capacity, and reservation conflicts.
 */
@Component
public class ChargerAllocator {

    public Charger findFeasibleCharger(
            VehicleRequest request,
            ChargingStation station,
            List<Charger> stationChargers,
            List<Reservation> activeAndConfirmedReservations,
            double currentlyAllocatedPowerKw,
            Instant now) {

        if (station == null || station.getStatus() != StationStatus.OPERATIONAL) {
            return null;
        }

        Instant requestedStart = now;
        Instant requestedEnd = now.plus(request.getExpectedDurationMinutes(), ChronoUnit.MINUTES);

        return stationChargers.stream()
                .filter(c -> c.getOperationalStatus() == ChargerOperationalStatus.OPERATIONAL)
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.AVAILABLE)
                .filter(c -> c.supports(request.getRequiredConnector()))
                .filter(c -> currentlyAllocatedPowerKw + c.getPowerRatingKw() <= station.getMaxPowerCapacityKw() + 1e-6)
                .filter(c -> !hasReservationConflict(c.getChargerId(), requestedStart, requestedEnd, activeAndConfirmedReservations))
                .min(Comparator.comparing(Charger::getChargerId))
                .orElse(null);
    }

    public boolean hasAnyCompatibleOperationalCharger(
            VehicleRequest request,
            List<Charger> stationChargers,
            double stationMaxPowerKw) {
        return stationChargers.stream()
                .filter(c -> c.getOperationalStatus() == ChargerOperationalStatus.OPERATIONAL)
                .filter(c -> c.supports(request.getRequiredConnector()))
                .anyMatch(c -> c.getPowerRatingKw() <= stationMaxPowerKw + 1e-6);
    }

    public boolean hasReservationConflict(
            String chargerId,
            Instant requestedStart,
            Instant requestedEnd,
            List<Reservation> reservations) {
        for (Reservation res : reservations) {
            if (!res.getCharger().getChargerId().equals(chargerId)) {
                continue;
            }
            if (res.getStatus() != ReservationStatus.CONFIRMED && res.getStatus() != ReservationStatus.ACTIVE) {
                continue;
            }
            if (res.overlaps(requestedStart, requestedEnd)) {
                return true;
            }
        }
        return false;
    }

    public String explainWaitReason(
            VehicleRequest request,
            ChargingStation station,
            List<Charger> stationChargers,
            List<Reservation> reservations,
            double currentlyAllocatedPowerKw,
            Instant now) {

        List<Charger> compatible = stationChargers.stream()
                .filter(c -> c.getOperationalStatus() == ChargerOperationalStatus.OPERATIONAL)
                .filter(c -> c.supports(request.getRequiredConnector()))
                .toList();

        if (compatible.isEmpty()) {
            return "No operational chargers at station support " + request.getRequiredConnector() + ".";
        }

        List<Charger> available = compatible.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.AVAILABLE)
                .toList();

        if (available.isEmpty()) {
            return "All compatible " + request.getRequiredConnector() + " chargers are currently occupied or reserved.";
        }

        Instant requestedEnd = now.plus(request.getExpectedDurationMinutes(), ChronoUnit.MINUTES);
        boolean allBlockedByPower = available.stream()
                .allMatch(c -> currentlyAllocatedPowerKw + c.getPowerRatingKw() > station.getMaxPowerCapacityKw() + 1e-6);
        if (allBlockedByPower) {
            return String.format("Station power capacity reached (%.1f / %.1f kW currently allocated).",
                    currentlyAllocatedPowerKw, station.getMaxPowerCapacityKw());
        }

        boolean blockedByReservation = available.stream()
                .anyMatch(c -> hasReservationConflict(c.getChargerId(), now, requestedEnd, reservations));
        if (blockedByReservation) {
            return "Available compatible chargers conflict with upcoming confirmed reservations.";
        }

        return "Waiting in priority queue for next feasible charging bay.";
    }
}
