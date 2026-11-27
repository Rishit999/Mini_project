package com.evchargeflow.engine2.scheduling;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.VehicleRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Engine 2: Practical, explainable waiting-time estimation simulating compatible charger
 * timelines, current session completions, upcoming reservations, and station power capacity.
 * Note: Estimated waiting times are advisory projections, not guarantees.
 */
@Component
public class WaitTimeEstimator {

    public void updateQueueWaitEstimates(
            ChargingStation station,
            List<VehicleRequest> orderedStationQueue,
            List<Charger> stationChargers,
            List<ChargingSession> activeSessions,
            List<Reservation> stationReservations,
            double currentlyAllocatedPowerKw,
            Instant now) {

        if (station.getStatus() != StationStatus.OPERATIONAL) {
            orderedStationQueue.forEach(req -> req.setEstimatedWaitMinutes(-1.0));
            return;
        }

        List<ChargerTimeline> timelines = buildTimelines(
                stationChargers, activeSessions, stationReservations, now);

        // If power capacity is saturated right now, find earliest active session finish to free power
        Instant earliestPowerRelease = activeSessions.stream()
                .map(ChargingSession::getExpectedEndTime)
                .filter(end -> end.isAfter(now))
                .min(Comparator.naturalOrder())
                .orElse(now);

        for (VehicleRequest req : orderedStationQueue) {
            double estimate = estimateSingleRequest(
                    req.getRequiredConnector(),
                    req.getExpectedDurationMinutes(),
                    station.getMaxPowerCapacityKw(),
                    currentlyAllocatedPowerKw,
                    earliestPowerRelease,
                    timelines,
                    stationReservations,
                    now,
                    true);
            req.setEstimatedWaitMinutes(estimate);
        }
    }

    public double estimateProspectiveWait(
            ChargingStation station,
            ConnectorType connectorType,
            int expectedDurationMinutes,
            int prospectivePriorityScore,
            List<VehicleRequest> orderedStationQueue,
            List<Charger> stationChargers,
            List<ChargingSession> activeSessions,
            List<Reservation> stationReservations,
            double currentlyAllocatedPowerKw,
            Instant now) {

        if (station.getStatus() != StationStatus.OPERATIONAL) {
            return -1.0;
        }

        List<ChargerTimeline> timelines = buildTimelines(
                stationChargers, activeSessions, stationReservations, now);

        Instant earliestPowerRelease = activeSessions.stream()
                .map(ChargingSession::getExpectedEndTime)
                .filter(end -> end.isAfter(now))
                .min(Comparator.naturalOrder())
                .orElse(now);

        for (VehicleRequest queued : orderedStationQueue) {
            if (queued.getPriorityScore() < prospectivePriorityScore) {
                break;
            }
            estimateSingleRequest(
                    queued.getRequiredConnector(),
                    queued.getExpectedDurationMinutes(),
                    station.getMaxPowerCapacityKw(),
                    currentlyAllocatedPowerKw,
                    earliestPowerRelease,
                    timelines,
                    stationReservations,
                    now,
                    true);
        }

        return estimateSingleRequest(
                connectorType,
                expectedDurationMinutes,
                station.getMaxPowerCapacityKw(),
                currentlyAllocatedPowerKw,
                earliestPowerRelease,
                timelines,
                stationReservations,
                now,
                false);
    }

    private double estimateSingleRequest(
            ConnectorType connector,
            int durationMinutes,
            double stationMaxPowerKw,
            double currentlyAllocatedPowerKw,
            Instant earliestPowerRelease,
            List<ChargerTimeline> timelines,
            List<Reservation> reservations,
            Instant now,
            boolean consumeSlot) {

        List<ChargerTimeline> compatible = timelines.stream()
                .filter(t -> t.charger.supports(connector))
                .filter(t -> t.charger.getPowerRatingKw() <= stationMaxPowerKw + 1e-6)
                .toList();

        if (compatible.isEmpty()) {
            return -1.0;
        }

        ChargerTimeline bestTimeline = null;
        Instant bestStart = null;

        for (ChargerTimeline timeline : compatible) {
            Instant candidateStart = timeline.freeAt;
            if (candidateStart.equals(now)
                    && currentlyAllocatedPowerKw + timeline.charger.getPowerRatingKw() > stationMaxPowerKw + 1e-6
                    && earliestPowerRelease.isAfter(candidateStart)) {
                candidateStart = earliestPowerRelease;
            }
            candidateStart = shiftPastConflictingReservations(
                    timeline.charger.getChargerId(), candidateStart, durationMinutes, reservations);

            if (bestStart == null
                    || candidateStart.isBefore(bestStart)
                    || (candidateStart.equals(bestStart)
                    && timeline.charger.getChargerId().compareTo(bestTimeline.charger.getChargerId()) < 0)) {
                bestStart = candidateStart;
                bestTimeline = timeline;
            }
        }

        if (bestStart == null) {
            return -1.0;
        }

        if (consumeSlot && bestTimeline != null) {
            bestTimeline.freeAt = bestStart.plus(durationMinutes, ChronoUnit.MINUTES);
        }

        double waitMinutes = Math.max(0.0, Duration.between(now, bestStart).toSeconds() / 60.0);
        return Math.round(waitMinutes * 10.0) / 10.0;
    }

    private Instant shiftPastConflictingReservations(
            String chargerId,
            Instant start,
            int durationMinutes,
            List<Reservation> reservations) {
        Instant candidate = start;
        boolean shifted = true;
        while (shifted) {
            shifted = false;
            Instant end = candidate.plus(durationMinutes, ChronoUnit.MINUTES);
            for (Reservation res : reservations) {
                if (!res.getCharger().getChargerId().equals(chargerId)) {
                    continue;
                }
                if (res.getStatus() != ReservationStatus.CONFIRMED && res.getStatus() != ReservationStatus.ACTIVE) {
                    continue;
                }
                if (res.overlaps(candidate, end)) {
                    candidate = res.getEndTime();
                    shifted = true;
                    break;
                }
            }
        }
        return candidate;
    }

    private List<ChargerTimeline> buildTimelines(
            List<Charger> chargers,
            List<ChargingSession> activeSessions,
            List<Reservation> reservations,
            Instant now) {
        List<ChargerTimeline> timelines = new ArrayList<>();
        for (Charger charger : chargers) {
            if (charger.getOperationalStatus() != ChargerOperationalStatus.OPERATIONAL) {
                continue;
            }
            Instant freeAt = now;
            if (charger.getAvailabilityStatus() == ChargerAvailabilityStatus.OCCUPIED) {
                Instant sessionEnd = activeSessions.stream()
                        .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                        .filter(s -> s.getCharger().getChargerId().equals(charger.getChargerId()))
                        .map(ChargingSession::getExpectedEndTime)
                        .findFirst()
                        .orElse(now.plus(30, ChronoUnit.MINUTES));
                freeAt = sessionEnd.isAfter(now) ? sessionEnd : now.plus(5, ChronoUnit.MINUTES);
            } else if (charger.getAvailabilityStatus() == ChargerAvailabilityStatus.RESERVED) {
                Instant resEnd = reservations.stream()
                        .filter(r -> r.getStatus() == ReservationStatus.ACTIVE)
                        .filter(r -> r.getCharger().getChargerId().equals(charger.getChargerId()))
                        .map(Reservation::getEndTime)
                        .findFirst()
                        .orElse(now.plus(30, ChronoUnit.MINUTES));
                freeAt = resEnd.isAfter(now) ? resEnd : now.plus(5, ChronoUnit.MINUTES);
            }
            timelines.add(new ChargerTimeline(charger, freeAt));
        }
        return timelines;
    }

    private static class ChargerTimeline {
        final Charger charger;
        Instant freeAt;

        ChargerTimeline(Charger charger, Instant freeAt) {
            this.charger = charger;
            this.freeAt = freeAt;
        }
    }
}
