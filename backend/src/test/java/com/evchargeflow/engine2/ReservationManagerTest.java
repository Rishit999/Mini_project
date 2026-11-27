package com.evchargeflow.engine2;

import com.evchargeflow.EVChargeFlowApplication;
import com.evchargeflow.common.enums.*;
import com.evchargeflow.common.exception.ConflictException;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingStationRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.repository.VehicleRepository;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine2.reservation.ReservationManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Integration-backed service tests for ReservationManager (Engine 2 — Logic &
 * Rule Engine)
 *
 * Test Cases: RMS-001 through RMS-005
 */
@SpringBootTest(classes = EVChargeFlowApplication.class)
@ActiveProfiles("test")
@Transactional
@DisplayName("TC-E2-06: ReservationManager Service Tests")
class ReservationManagerTest {

        @Autowired
        private ReservationManager reservationManager;
        @Autowired
        private DataIntegrityService dataIntegrityService;
        @Autowired
        private ReservationRepository reservationRepo;
        @Autowired
        private ChargingStationRepository stationRepo;
        @Autowired
        private ChargerRepository chargerRepo;
        @Autowired
        private VehicleRepository vehicleRepo;
        @Autowired
        private Clock clock;

        private String stationId;
        private String chargerId;
        private String vehicleId;

        @BeforeEach
        void setUp() {
                ChargingStation station = dataIntegrityService.createStation(
                                new ChargingStation("STN-RM-1", "RM Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0));
                Charger charger = new Charger("CHG-RM-1", station, "Bay 1", ChargerType.FAST_DC, 60.0,
                                Set.of(ConnectorType.CCS));
                dataIntegrityService.createCharger(charger, station.getStationId());
                Vehicle vehicle = dataIntegrityService.createVehicle(
                                new Vehicle("VEH-RM-1", "KA01RM01", null, "Driver", "Ioniq 5", ConnectorType.CCS, 72.0,
                                                30.0),
                                null);

                stationId = station.getStationId();
                chargerId = charger.getChargerId();
                vehicleId = vehicle.getVehicleId();
        }

        @Test
        @DisplayName("RMS-001: Valid reservation created when slot is free")
        void testCreateReservation_success() {
                Instant now = clock.instant();
                Reservation created = reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(30, ChronoUnit.MINUTES),
                                now.plus(90, ChronoUnit.MINUTES),
                                "Test booking");

                assertThat(created.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
                assertThat(created.getCharger().getChargerId()).isEqualTo(chargerId);
        }

        @Test
        @DisplayName("RMS-002: Overlapping reservation throws ConflictException")
        void testCreateReservation_overlapConflict() {
                Instant now = clock.instant();
                reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(30, ChronoUnit.MINUTES),
                                now.plus(90, ChronoUnit.MINUTES),
                                "First slot");

                assertThatThrownBy(() -> reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(60, ChronoUnit.MINUTES),
                                now.plus(120, ChronoUnit.MINUTES),
                                "Overlapping slot"))
                                .isInstanceOf(ConflictException.class);
        }

        @Test
        @DisplayName("RMS-003: Adjacent reservation slot (end == start) succeeds")
        void testCreateReservation_adjacentSlotAllowed() {
                Instant now = clock.instant();
                reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(30, ChronoUnit.MINUTES),
                                now.plus(90, ChronoUnit.MINUTES),
                                "Slot 1");

                Reservation adjacent = reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(90, ChronoUnit.MINUTES),
                                now.plus(150, ChronoUnit.MINUTES),
                                "Back-to-back slot");

                assertThat(adjacent.getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
        }

        @Test
        @DisplayName("RMS-004: Grace period expiry transitions CONFIRMED -> EXPIRED")
        void testProcessReservations_gracePeriodExpiry() {
                Instant now = clock.instant();
                Reservation created = reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(30, ChronoUnit.MINUTES),
                                now.plus(90, ChronoUnit.MINUTES),
                                "Pending reservation");

                // Simulate time well past start + grace period (15 min default)
                Instant futureCheck = now.plus(60, ChronoUnit.MINUTES);
                List<Reservation> updated = reservationManager.processReservations(futureCheck);

                Reservation refreshed = reservationRepo.findById(created.getReservationId()).orElseThrow();
                assertThat(updated).extracting(Reservation::getReservationId).contains(created.getReservationId());
                assertThat(refreshed.getStatus()).isEqualTo(ReservationStatus.EXPIRED);
        }

        @Test
        @DisplayName("RMS-005: Cancelling active reservation releases RESERVED charger")
        void testCancelReservation_releasesCharger() {
                Instant now = clock.instant();
                Reservation created = reservationManager.createReservation(
                                vehicleId, stationId, chargerId,
                                now.plus(1, ChronoUnit.MINUTES),
                                now.plus(61, ChronoUnit.MINUTES),
                                "Soon active");

                // Activate at start time
                reservationManager.processReservations(now.plus(2, ChronoUnit.MINUTES));
                Charger busyCharger = chargerRepo.findById(chargerId).orElseThrow();
                assertThat(busyCharger.getAvailabilityStatus()).isEqualTo(ChargerAvailabilityStatus.RESERVED);

                Reservation cancelled = reservationManager.cancelReservation(created.getReservationId());
                assertThat(cancelled.getStatus()).isEqualTo(ReservationStatus.CANCELLED);

                Charger released = chargerRepo.findById(chargerId).orElseThrow();
                assertThat(released.getAvailabilityStatus()).isEqualTo(ChargerAvailabilityStatus.AVAILABLE);
                assertThat(released.getCurrentVehicleId()).isNull();
        }
}
