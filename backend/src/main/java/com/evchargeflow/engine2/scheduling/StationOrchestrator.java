package com.evchargeflow.engine2.scheduling;

import com.evchargeflow.common.dto.AllocationResult;
import com.evchargeflow.common.dto.PriorityBreakdown;
import com.evchargeflow.common.enums.AllocationOutcome;
import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.common.exception.ConflictException;
import com.evchargeflow.common.exception.DomainValidationException;
import com.evchargeflow.common.exception.ResourceNotFoundException;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.repository.VehicleRepository;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine1.service.AuditLogService;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine1.validation.IntegrityValidator;
import com.evchargeflow.engine2.allocation.ChargerAllocator;
import com.evchargeflow.engine2.priority.PriorityCalculator;
import com.evchargeflow.engine2.queue.QueueManager;
import com.evchargeflow.engine2.reservation.ReservationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Engine 2: Central orchestration facade connecting PriorityCalculator, QueueManager,
 * ChargerAllocator, ReservationManager, WaitTimeEstimator, and ChargingSession lifecycle.
 */
@Service
public class StationOrchestrator {

    private static final Set<RequestStatus> ACTIVE_REQUEST_STATUSES =
            Set.of(RequestStatus.QUEUED, RequestStatus.ASSIGNED, RequestStatus.CHARGING);

    private final DataIntegrityService dataIntegrityService;
    private final IntegrityValidator validator;
    private final AuditLogService auditLogService;
    private final PriorityCalculator priorityCalculator;
    private final QueueManager queueManager;
    private final ChargerAllocator chargerAllocator;
    private final ReservationManager reservationManager;
    private final WaitTimeEstimator waitTimeEstimator;
    private final VehicleRequestRepository requestRepo;
    private final ChargerRepository chargerRepo;
    private final ReservationRepository reservationRepo;
    private final ChargingSessionRepository sessionRepo;
    private final VehicleRepository vehicleRepo;
    private final Clock clock;

    public StationOrchestrator(
            DataIntegrityService dataIntegrityService,
            IntegrityValidator validator,
            AuditLogService auditLogService,
            PriorityCalculator priorityCalculator,
            QueueManager queueManager,
            ChargerAllocator chargerAllocator,
            ReservationManager reservationManager,
            WaitTimeEstimator waitTimeEstimator,
            VehicleRequestRepository requestRepo,
            ChargerRepository chargerRepo,
            ReservationRepository reservationRepo,
            ChargingSessionRepository sessionRepo,
            VehicleRepository vehicleRepo,
            Clock clock) {
        this.dataIntegrityService = dataIntegrityService;
        this.validator = validator;
        this.auditLogService = auditLogService;
        this.priorityCalculator = priorityCalculator;
        this.queueManager = queueManager;
        this.chargerAllocator = chargerAllocator;
        this.reservationManager = reservationManager;
        this.waitTimeEstimator = waitTimeEstimator;
        this.requestRepo = requestRepo;
        this.chargerRepo = chargerRepo;
        this.reservationRepo = reservationRepo;
        this.sessionRepo = sessionRepo;
        this.vehicleRepo = vehicleRepo;
        this.clock = clock;
    }

    /**
     * Submits a charging request, calculates priority, validates constraints,
     * attempts immediate allocation if feasible, or preserves it in the priority queue.
     */
    @Transactional
    public synchronized AllocationResult submitChargingRequest(
            String vehicleId,
            String stationId,
            double currentBatteryPercent,
            com.evchargeflow.common.enums.ConnectorType requiredConnector,
            int expectedDurationMinutes,
            Instant desiredDepartureTime,
            Double targetBatteryPercent) {

        Instant now = clock.instant();
        Vehicle vehicle = dataIntegrityService.getVehicleOrThrow(vehicleId);
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);

        validator.validateChargingRequestInput(
                vehicle, station, currentBatteryPercent, targetBatteryPercent,
                expectedDurationMinutes, desiredDepartureTime, now);

        if (requestRepo.existsByVehicleVehicleIdAndStatusIn(vehicleId, ACTIVE_REQUEST_STATUSES)) {
            throw new ConflictException("DUP_ACTIVE_REQUEST",
                    "Vehicle " + vehicleId + " already has an active or queued charging request.");
        }

        // Update vehicle battery reading at request arrival
        vehicle.setCurrentBatteryPercent(currentBatteryPercent);
        vehicleRepo.save(vehicle);

        String requestId = "REQ-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        VehicleRequest request = new VehicleRequest();
        request.setRequestId(requestId);
        request.setVehicle(vehicle);
        request.setStation(station);
        request.setCurrentBatteryPercent(currentBatteryPercent);
        request.setTargetBatteryPercent(targetBatteryPercent);
        request.setRequiredConnector(requiredConnector != null ? requiredConnector : vehicle.getConnectorType());
        request.setExpectedDurationMinutes(expectedDurationMinutes);
        request.setDesiredDepartureTime(desiredDepartureTime);
        request.setArrivalTime(now);

        PriorityBreakdown breakdown = priorityCalculator.calculate(request, now);
        request.setBatteryScore(breakdown.batteryScore());
        request.setDepartureScore(breakdown.departureScore());
        request.setWaitingTimeScore(breakdown.waitingTimeScore());
        request.setPriorityScore(breakdown.totalScore());

        List<Charger> stationChargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        if (station.getStatus() != StationStatus.OPERATIONAL) {
            request.setStatus(RequestStatus.REJECTED);
            request.setEstimatedWaitMinutes(-1.0);
            request.setStatusReason("Station " + station.getName() + " is currently " + station.getStatus() + ".");
            requestRepo.save(request);
            auditLogService.log("REQUEST", "REJECTED", requestId + ": " + request.getStatusReason());
            return new AllocationResult(
                    AllocationOutcome.REJECTED, requestId, stationId,
                    null, null, null, -1.0, breakdown, request.getStatusReason(), null);
        }

        if (!chargerAllocator.hasAnyCompatibleOperationalCharger(request, stationChargers, station.getMaxPowerCapacityKw())) {
            request.setStatus(RequestStatus.REJECTED);
            request.setEstimatedWaitMinutes(-1.0);
            request.setStatusReason("Station has no operational charger supporting "
                    + request.getRequiredConnector() + " within station power capacity.");
            requestRepo.save(request);
            auditLogService.log("REQUEST", "REJECTED", requestId + ": " + request.getStatusReason());
            return new AllocationResult(
                    AllocationOutcome.REJECTED, requestId, stationId,
                    null, null, null, -1.0, breakdown, request.getStatusReason(), null);
        }

        // Enqueue request and run allocation cycle if auto-allocation is enabled
        queueManager.enqueue(request, now);
        auditLogService.log("QUEUE", "ENQUEUE",
                "Queued " + requestId + " for vehicle " + vehicleId + " (Score=" + breakdown.totalScore() + ")");

        StationConfiguration config = dataIntegrityService.getOrCreateConfig(stationId);
        if (config.isAutoAllocationEnabled()) {
            runAllocationCycleForStation(stationId, now);
        } else {
            refreshStationWaitEstimates(stationId, now);
        }

        VehicleRequest updated = requestRepo.findById(requestId).orElse(request);
        if (updated.getStatus() == RequestStatus.ASSIGNED || updated.getStatus() == RequestStatus.CHARGING) {
            Charger assigned = updated.getAssignedChargerId() != null
                    ? dataIntegrityService.getChargerOrThrow(updated.getAssignedChargerId())
                    : null;
            return new AllocationResult(
                    AllocationOutcome.ASSIGNED,
                    updated.getRequestId(),
                    stationId,
                    updated.getAssignedChargerId(),
                    assigned != null ? assigned.getName() : null,
                    null,
                    0.0,
                    breakdown,
                    updated.getStatusReason(),
                    null);
        }

        Integer pos = getStationQueuePosition(stationId, updated.getRequestId(), now);
        return new AllocationResult(
                AllocationOutcome.WAITING,
                updated.getRequestId(),
                stationId,
                null,
                null,
                pos,
                updated.getEstimatedWaitMinutes(),
                breakdown,
                updated.getStatusReason(),
                null);
    }

    @Transactional
    public synchronized List<AllocationResult> runAllocationCycleForStation(String stationId, Instant now) {
        reservationManager.processReservations(now);
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);
        List<VehicleRequest> orderedQueue = queueManager.getOrderedQueueForStation(stationId, now);
        List<Charger> stationChargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        List<Reservation> reservations = reservationRepo.findByStationStationIdAndStatusIn(
                stationId, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE));

        double allocatedPower = dataIntegrityService.getAllocatedPowerKw(stationId);
        List<AllocationResult> results = new ArrayList<>();

        for (VehicleRequest req : orderedQueue) {
            Charger feasible = chargerAllocator.findFeasibleCharger(
                    req, station, stationChargers, reservations, allocatedPower, now);

            if (feasible != null) {
                queueManager.removeById(req.getRequestId());
                validator.validateRequestTransition(req, RequestStatus.ASSIGNED);
                req.setStatus(RequestStatus.ASSIGNED);
                req.setAssignedChargerId(feasible.getChargerId());
                req.setEstimatedWaitMinutes(0.0);
                req.setStatusReason("Assigned to " + feasible.getName() + " (" + feasible.getPowerRatingKw() + " kW). Ready to start session.");
                requestRepo.save(req);

                feasible.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
                feasible.setCurrentVehicleId(req.getVehicle().getVehicleId());
                feasible.setCurrentRequestId(req.getRequestId());
                chargerRepo.save(feasible);

                allocatedPower += feasible.getPowerRatingKw();

                PriorityBreakdown breakdown = new PriorityBreakdown(
                        req.getBatteryScore(), req.getDepartureScore(), req.getWaitingTimeScore(), req.getPriorityScore());

                auditLogService.log("ALLOCATION", "ASSIGN",
                        "Assigned " + req.getRequestId() + " (" + req.getVehicle().getVehicleId() + ") -> "
                                + feasible.getChargerId() + " [Power: " + allocatedPower + "/" + station.getMaxPowerCapacityKw() + " kW]");

                results.add(new AllocationResult(
                        AllocationOutcome.ASSIGNED,
                        req.getRequestId(),
                        stationId,
                        feasible.getChargerId(),
                        feasible.getName(),
                        null,
                        0.0,
                        breakdown,
                        req.getStatusReason(),
                        null));
            } else {
                String waitReason = chargerAllocator.explainWaitReason(
                        req, station, stationChargers, reservations, allocatedPower, now);
                req.setStatusReason(waitReason);
                requestRepo.save(req);
            }
        }

        refreshStationWaitEstimates(stationId, now);
        return results;
    }

    @Transactional
    public synchronized List<AllocationResult> runAllStationsAllocation() {
        Instant now = clock.instant();
        List<AllocationResult> all = new ArrayList<>();
        for (ChargingStation station : dataIntegrityService.getAllStations()) {
            if (station.getStatus() == StationStatus.OPERATIONAL) {
                all.addAll(runAllocationCycleForStation(station.getStationId(), now));
            }
        }
        return all;
    }

    @Transactional
    public synchronized ChargingSession startChargingSession(String requestId) {
        Instant now = clock.instant();
        VehicleRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("REQ_NOT_FOUND", "Request not found: " + requestId));

        if (request.getStatus() != RequestStatus.ASSIGNED) {
            throw new DomainValidationException("STATE_NOT_ASSIGNED",
                    "Only ASSIGNED requests can start a charging session. Current status: " + request.getStatus());
        }
        if (request.getAssignedChargerId() == null) {
            throw new DomainValidationException("CHG_NOT_ASSIGNED", "Request has no assigned charger.");
        }

        Charger charger = dataIntegrityService.getChargerOrThrow(request.getAssignedChargerId());
        if (sessionRepo.findByChargerChargerIdAndStatus(charger.getChargerId(), SessionStatus.ACTIVE).isPresent()) {
            throw new ConflictException("SES_ALREADY_ACTIVE", "Charger already has an active session.");
        }

        validator.validateRequestTransition(request, RequestStatus.CHARGING);
        request.setStatus(RequestStatus.CHARGING);
        request.setStatusReason("Charging session active (simulated) on " + charger.getChargerId() + ".");
        requestRepo.save(request);

        charger.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
        charger.setCurrentVehicleId(request.getVehicle().getVehicleId());
        charger.setCurrentRequestId(request.getRequestId());
        chargerRepo.save(charger);

        ChargingSession session = new ChargingSession();
        session.setSessionId("SES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        session.setRequest(request);
        session.setVehicle(request.getVehicle());
        session.setStation(request.getStation());
        session.setCharger(charger);
        session.setStartTime(now);
        session.setExpectedEndTime(now.plus(request.getExpectedDurationMinutes(), ChronoUnit.MINUTES));
        session.setAllocatedPowerKw(charger.getPowerRatingKw());
        session.setEnergyConsumedKwh(0.0);
        session.setStatus(SessionStatus.ACTIVE);
        session.setSimulated(true);

        ChargingSession saved = sessionRepo.save(session);
        auditLogService.log("SESSION", "START",
                "Started simulated session " + saved.getSessionId() + " on " + charger.getChargerId());
        return saved;
    }

    @Transactional
    public synchronized ChargingSession startSessionFromReservation(String reservationId, int expectedDurationMinutes) {
        Instant now = clock.instant();
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RES_NOT_FOUND", "Reservation not found: " + reservationId));

        if (reservation.getStatus() != ReservationStatus.CONFIRMED && reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new DomainValidationException("RES_NOT_ELIGIBLE",
                    "Reservation must be CONFIRMED or ACTIVE to start a session.");
        }

        Charger charger = reservation.getCharger();
        if (sessionRepo.findByChargerChargerIdAndStatus(charger.getChargerId(), SessionStatus.ACTIVE).isPresent()) {
            throw new ConflictException("SES_ALREADY_ACTIVE", "Charger already has an active charging session.");
        }

        ChargingStation station = reservation.getStation();
        double currentPower = dataIntegrityService.getAllocatedPowerKw(station.getStationId());
        if (currentPower + charger.getPowerRatingKw() > station.getMaxPowerCapacityKw() + 1e-6) {
            throw new ConflictException("POWER_CAPACITY_EXCEEDED",
                    "Starting session would exceed station max power capacity.");
        }

        reservation.setStatus(ReservationStatus.ACTIVE);
        reservationRepo.save(reservation);

        charger.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
        charger.setCurrentVehicleId(reservation.getVehicle().getVehicleId());
        charger.setCurrentRequestId(null);
        chargerRepo.save(charger);

        ChargingSession session = new ChargingSession();
        session.setSessionId("SES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        session.setReservation(reservation);
        session.setVehicle(reservation.getVehicle());
        session.setStation(station);
        session.setCharger(charger);
        session.setStartTime(now);
        int mins = expectedDurationMinutes > 0 ? expectedDurationMinutes
                : (int) Math.max(15, Duration.between(now, reservation.getEndTime()).toMinutes());
        session.setExpectedEndTime(now.plus(mins, ChronoUnit.MINUTES));
        session.setAllocatedPowerKw(charger.getPowerRatingKw());
        session.setEnergyConsumedKwh(0.0);
        session.setStatus(SessionStatus.ACTIVE);
        session.setSimulated(true);

        ChargingSession saved = sessionRepo.save(session);
        auditLogService.log("SESSION", "START_RESERVATION",
                "Started simulated session " + saved.getSessionId() + " from reservation " + reservationId);
        return saved;
    }

    @Transactional
    public synchronized ChargingSession completeOrStopSession(String sessionId, boolean completed) {
        Instant now = clock.instant();
        ChargingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("SES_NOT_FOUND", "Session not found: " + sessionId));

        if (session.getStatus() != SessionStatus.ACTIVE) {
            throw new DomainValidationException("SES_NOT_ACTIVE", "Session " + sessionId + " is already " + session.getStatus());
        }

        StationConfiguration config = dataIntegrityService.getOrCreateConfig(session.getStation().getStationId());
        double elapsedMinutes = Math.max(1.0, Duration.between(session.getStartTime(), now).toSeconds() / 60.0);
        double simulatedMinutes = completed
                ? Math.max(elapsedMinutes, Duration.between(session.getStartTime(), session.getExpectedEndTime()).toMinutes())
                : elapsedMinutes;
        double energyKwh = (session.getAllocatedPowerKw() * config.getChargingEfficiencyFactor()) * (simulatedMinutes / 60.0);

        Vehicle vehicle = session.getVehicle();
        double addedPercent = (energyKwh / Math.max(1.0, vehicle.getBatteryCapacityKwh())) * 100.0;
        double newBattery = Math.min(100.0, Math.round((vehicle.getCurrentBatteryPercent() + addedPercent) * 10.0) / 10.0);
        vehicle.setCurrentBatteryPercent(newBattery);
        vehicleRepo.save(vehicle);

        session.setActualEndTime(now);
        session.setEnergyConsumedKwh(Math.round(energyKwh * 100.0) / 100.0);
        session.setStatus(completed ? SessionStatus.COMPLETED : SessionStatus.STOPPED);
        ChargingSession savedSession = sessionRepo.save(session);

        if (session.getRequest() != null) {
            VehicleRequest req = session.getRequest();
            if (req.getStatus() == RequestStatus.CHARGING || req.getStatus() == RequestStatus.ASSIGNED) {
                req.setStatus(RequestStatus.COMPLETED);
                req.setStatusReason((completed ? "Completed" : "Stopped") + " simulated session " + sessionId + ".");
                requestRepo.save(req);
            }
        }
        if (session.getReservation() != null) {
            Reservation res = session.getReservation();
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setStatus(ReservationStatus.COMPLETED);
                reservationRepo.save(res);
            }
        }

        Charger charger = session.getCharger();
        charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
        charger.setCurrentVehicleId(null);
        charger.setCurrentRequestId(null);
        chargerRepo.save(charger);

        auditLogService.log("SESSION", completed ? "COMPLETE" : "STOP",
                (completed ? "Completed" : "Stopped") + " session " + sessionId + " (" + savedSession.getEnergyConsumedKwh() + " kWh simulated)");

        // Re-evaluate queue and allocate next feasible request
        if (config.isAutoAllocationEnabled()) {
            runAllocationCycleForStation(session.getStation().getStationId(), now);
        } else {
            refreshStationWaitEstimates(session.getStation().getStationId(), now);
        }

        return savedSession;
    }

    @Transactional
    public synchronized VehicleRequest cancelRequest(String requestId) {
        Instant now = clock.instant();
        VehicleRequest request = requestRepo.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("REQ_NOT_FOUND", "Request not found: " + requestId));

        if (request.getStatus() == RequestStatus.CHARGING) {
            throw new ConflictException("REQ_CHARGING",
                    "Cannot cancel request while charging session is active. Stop the session instead.");
        }

        validator.validateRequestTransition(request, RequestStatus.CANCELLED);

        if (request.getStatus() == RequestStatus.ASSIGNED && request.getAssignedChargerId() != null) {
            Charger charger = dataIntegrityService.getChargerOrThrow(request.getAssignedChargerId());
            if (requestId.equals(charger.getCurrentRequestId())) {
                charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
                charger.setCurrentVehicleId(null);
                charger.setCurrentRequestId(null);
                chargerRepo.save(charger);
            }
        }

        queueManager.removeById(requestId);
        request.setStatus(RequestStatus.CANCELLED);
        request.setStatusReason("Cancelled by operator/user.");
        VehicleRequest saved = requestRepo.save(request);

        auditLogService.log("QUEUE", "CANCEL", "Cancelled request " + requestId);
        runAllocationCycleForStation(request.getStation().getStationId(), now);
        return saved;
    }

    @Transactional
    public synchronized List<VehicleRequest> getLiveQueue(String stationId) {
        Instant now = clock.instant();
        if (stationId == null || stationId.isBlank()) {
            List<VehicleRequest> all = queueManager.getOrderedQueue(now);
            for (ChargingStation st : dataIntegrityService.getAllStations()) {
                refreshStationWaitEstimates(st.getStationId(), now);
            }
            return queueManager.getOrderedQueue(now);
        }
        refreshStationWaitEstimates(stationId, now);
        return queueManager.getOrderedQueueForStation(stationId, now);
    }

    @Transactional
    public synchronized void refreshStationWaitEstimates(String stationId, Instant now) {
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);
        List<VehicleRequest> orderedStationQueue = queueManager.getOrderedQueueForStation(stationId, now);
        List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        List<ChargingSession> activeSessions = sessionRepo.findByStationStationIdAndStatus(stationId, SessionStatus.ACTIVE);
        List<Reservation> reservations = reservationRepo.findByStationStationIdAndStatusIn(
                stationId, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE));
        double allocatedPower = dataIntegrityService.getAllocatedPowerKw(stationId);

        waitTimeEstimator.updateQueueWaitEstimates(
                station, orderedStationQueue, chargers, activeSessions, reservations, allocatedPower, now);

        for (VehicleRequest req : orderedStationQueue) {
            requestRepo.save(req);
        }
    }

    @Transactional
    public double estimateProspectiveWait(
            String stationId,
            com.evchargeflow.common.enums.ConnectorType connectorType,
            double currentBatteryPercent,
            int expectedDurationMinutes,
            Instant desiredDepartureTime) {
        Instant now = clock.instant();
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);

        VehicleRequest probe = new VehicleRequest();
        probe.setCurrentBatteryPercent(currentBatteryPercent);
        probe.setDesiredDepartureTime(desiredDepartureTime);
        probe.setArrivalTime(now);
        PriorityBreakdown breakdown = priorityCalculator.calculate(probe, now);

        List<VehicleRequest> ordered = queueManager.getOrderedQueueForStation(stationId, now);
        List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        List<ChargingSession> activeSessions = sessionRepo.findByStationStationIdAndStatus(stationId, SessionStatus.ACTIVE);
        List<Reservation> reservations = reservationRepo.findByStationStationIdAndStatusIn(
                stationId, Set.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE));
        double allocatedPower = dataIntegrityService.getAllocatedPowerKw(stationId);

        return waitTimeEstimator.estimateProspectiveWait(
                station, connectorType, expectedDurationMinutes, breakdown.totalScore(),
                ordered, chargers, activeSessions, reservations, allocatedPower, now);
    }

    private Integer getStationQueuePosition(String stationId, String requestId, Instant now) {
        List<VehicleRequest> list = queueManager.getOrderedQueueForStation(stationId, now);
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getRequestId().equals(requestId)) {
                return i + 1;
            }
        }
        return null;
    }
}
