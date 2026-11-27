package com.evchargeflow.engine2.reservation;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.exception.ConflictException;
import com.evchargeflow.common.exception.ResourceNotFoundException;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.service.AuditLogService;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine1.validation.IntegrityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Engine 2: Manages reservations, slot overlap detection, grace-period expiry, and charger reservation locking.
 */
@Service
public class ReservationManager {

    private static final Set<ReservationStatus> BLOCKING_STATUSES =
            Set.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);

    private final ReservationRepository reservationRepo;
    private final ChargerRepository chargerRepo;
    private final ChargingSessionRepository sessionRepo;
    private final DataIntegrityService dataIntegrityService;
    private final IntegrityValidator validator;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ReservationManager(
            ReservationRepository reservationRepo,
            ChargerRepository chargerRepo,
            ChargingSessionRepository sessionRepo,
            DataIntegrityService dataIntegrityService,
            IntegrityValidator validator,
            AuditLogService auditLogService,
            Clock clock) {
        this.reservationRepo = reservationRepo;
        this.chargerRepo = chargerRepo;
        this.sessionRepo = sessionRepo;
        this.dataIntegrityService = dataIntegrityService;
        this.validator = validator;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional
    public Reservation createReservation(
            String vehicleId,
            String stationId,
            String chargerId,
            Instant startTime,
            Instant endTime,
            String notes) {
        Instant now = clock.instant();
        Vehicle vehicle = dataIntegrityService.getVehicleOrThrow(vehicleId);
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);
        Charger charger = dataIntegrityService.getChargerOrThrow(chargerId);

        Reservation reservation = new Reservation();
        reservation.setReservationId("RES-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        reservation.setVehicle(vehicle);
        reservation.setStation(station);
        reservation.setCharger(charger);
        reservation.setStartTime(startTime);
        reservation.setEndTime(endTime);
        reservation.setStatus(ReservationStatus.CONFIRMED);
        reservation.setNotes(notes);
        reservation.setCreatedAt(now);

        validator.validateReservation(reservation, now);

        // Rule: ExistingStart < RequestedEnd AND RequestedStart < ExistingEnd (adjacent slots allowed)
        List<Reservation> existing = reservationRepo.findByChargerChargerIdAndStatusIn(chargerId, BLOCKING_STATUSES);
        for (Reservation other : existing) {
            if (other.overlaps(startTime, endTime)) {
                throw new ConflictException("RES_CONFLICT",
                        "Charger " + chargerId + " already has an overlapping reservation ("
                                + other.getReservationId() + ") during this time slot.");
            }
        }

        // Prevent creating an immediate reservation that overlaps an ongoing active charging session
        Optional<ChargingSession> activeSession = sessionRepo.findByChargerChargerIdAndStatus(chargerId, SessionStatus.ACTIVE);
        if (activeSession.isPresent() && activeSession.get().getExpectedEndTime().isAfter(startTime)) {
            throw new ConflictException("RES_SESSION_CONFLICT",
                    "Charger " + chargerId + " has an active charging session expected to end at "
                            + activeSession.get().getExpectedEndTime() + ".");
        }

        Reservation saved = reservationRepo.save(reservation);
        auditLogService.log("RESERVATION", "CREATE",
                "Created reservation " + saved.getReservationId() + " for " + vehicleId + " on " + chargerId);
        return saved;
    }

    @Transactional
    public Reservation cancelReservation(String reservationId) {
        Reservation reservation = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException("RES_NOT_FOUND", "Reservation not found: " + reservationId));
        validator.validateReservationTransition(reservation, ReservationStatus.CANCELLED);
        reservation.setStatus(ReservationStatus.CANCELLED);

        Charger charger = reservation.getCharger();
        if (charger.getAvailabilityStatus() == ChargerAvailabilityStatus.RESERVED
                && reservation.getVehicle().getVehicleId().equals(charger.getCurrentVehicleId())) {
            charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
            charger.setCurrentVehicleId(null);
            chargerRepo.save(charger);
        }

        Reservation saved = reservationRepo.save(reservation);
        auditLogService.log("RESERVATION", "CANCEL", "Cancelled reservation " + reservationId);
        return saved;
    }

    @Transactional
    public List<Reservation> processReservations(Instant now) {
        List<Reservation> changed = new ArrayList<>();
        List<Reservation> pending = reservationRepo.findAllByOrderByStartTimeAsc();

        for (Reservation res : pending) {
            if (res.getStatus() == ReservationStatus.CONFIRMED) {
                StationConfiguration cfg = dataIntegrityService.getOrCreateConfig(res.getStation().getStationId());
                Instant graceDeadline = res.getStartTime().plus(cfg.getReservationGracePeriodMinutes(), ChronoUnit.MINUTES);

                if (now.isAfter(graceDeadline) || !now.isBefore(res.getEndTime())) {
                    res.setStatus(ReservationStatus.EXPIRED);
                    reservationRepo.save(res);
                    releaseReservedChargerIfMatching(res);
                    auditLogService.log("RESERVATION", "EXPIRE", "Expired reservation " + res.getReservationId());
                    changed.add(res);
                } else if (!now.isBefore(res.getStartTime()) && now.isBefore(res.getEndTime())) {
                    Charger charger = res.getCharger();
                    if (charger.isOperationalAndAvailable()) {
                        res.setStatus(ReservationStatus.ACTIVE);
                        reservationRepo.save(res);
                        charger.setAvailabilityStatus(ChargerAvailabilityStatus.RESERVED);
                        charger.setCurrentVehicleId(res.getVehicle().getVehicleId());
                        chargerRepo.save(charger);
                        auditLogService.log("RESERVATION", "ACTIVATE",
                                "Activated reservation " + res.getReservationId() + " on " + charger.getChargerId());
                        changed.add(res);
                    }
                }
            } else if (res.getStatus() == ReservationStatus.ACTIVE && !now.isBefore(res.getEndTime())) {
                // Reservation window ended without an ongoing session
                Optional<ChargingSession> activeSession = sessionRepo.findByChargerChargerIdAndStatus(
                        res.getCharger().getChargerId(), SessionStatus.ACTIVE);
                if (activeSession.isEmpty()) {
                    res.setStatus(ReservationStatus.COMPLETED);
                    reservationRepo.save(res);
                    releaseReservedChargerIfMatching(res);
                    changed.add(res);
                }
            }
        }
        return changed;
    }

    private void releaseReservedChargerIfMatching(Reservation res) {
        Charger charger = res.getCharger();
        if (charger.getAvailabilityStatus() == ChargerAvailabilityStatus.RESERVED
                && res.getVehicle().getVehicleId().equals(charger.getCurrentVehicleId())) {
            charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
            charger.setCurrentVehicleId(null);
            chargerRepo.save(charger);
        }
    }
}
