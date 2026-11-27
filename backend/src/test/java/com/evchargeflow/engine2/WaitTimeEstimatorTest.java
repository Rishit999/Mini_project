package com.evchargeflow.engine2;

import com.evchargeflow.common.enums.*;
import com.evchargeflow.engine1.entity.*;
import com.evchargeflow.engine2.scheduling.WaitTimeEstimator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for WaitTimeEstimator (Engine 2 — Logic & Rule Engine)
 *
 * Test Cases: WT-001 through WT-005
 */
@DisplayName("TC-E2-07: WaitTimeEstimator Unit Tests")
class WaitTimeEstimatorTest {

    private WaitTimeEstimator estimator;
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        estimator = new WaitTimeEstimator();
    }

    @Test
    @DisplayName("WT-001: Available compatible charger → estimated wait = 0.0 min")
    void testProspectiveWait_availableCharger_returnsZero() {
        ChargingStation station = new ChargingStation("STN-01", "Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0);
        Charger c1 = new Charger("CHG-01", station, "Bay 1", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS));

        double wait = estimator.estimateProspectiveWait(
                station, ConnectorType.CCS, 30, 50,
                List.of(), List.of(c1), List.of(), List.of(), 0.0, NOW);

        assertThat(wait).isEqualTo(0.0);
    }

    @Test
    @DisplayName("WT-002: Occupied charger with active session → wait equals remaining session duration")
    void testProspectiveWait_occupiedCharger_usesSessionExpectedEnd() {
        ChargingStation station = new ChargingStation("STN-01", "Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0);
        Charger c1 = new Charger("CHG-01", station, "Bay 1", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS));
        c1.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);

        ChargingSession activeSession = new ChargingSession();
        activeSession.setCharger(c1);
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setExpectedEndTime(NOW.plus(25, ChronoUnit.MINUTES));

        double wait = estimator.estimateProspectiveWait(
                station, ConnectorType.CCS, 30, 50,
                List.of(), List.of(c1), List.of(activeSession), List.of(), 60.0, NOW);

        assertThat(wait).isEqualTo(25.0);
    }

    @Test
    @DisplayName("WT-003: Queued requests ahead with higher priority add cumulative wait time")
    void testUpdateQueueWaitEstimates_ordersTimelines() {
        ChargingStation station = new ChargingStation("STN-01", "Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0);
        Charger c1 = new Charger("CHG-01", station, "Bay 1", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS));
        c1.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);

        ChargingSession activeSession = new ChargingSession();
        activeSession.setCharger(c1);
        activeSession.setStatus(SessionStatus.ACTIVE);
        activeSession.setExpectedEndTime(NOW.plus(15, ChronoUnit.MINUTES));

        VehicleRequest req1 = new VehicleRequest();
        req1.setRequestId("REQ-1");
        req1.setRequiredConnector(ConnectorType.CCS);
        req1.setExpectedDurationMinutes(20);
        req1.setPriorityScore(80);

        VehicleRequest req2 = new VehicleRequest();
        req2.setRequestId("REQ-2");
        req2.setRequiredConnector(ConnectorType.CCS);
        req2.setExpectedDurationMinutes(30);
        req2.setPriorityScore(40);

        estimator.updateQueueWaitEstimates(
                station, List.of(req1, req2), List.of(c1), List.of(activeSession), List.of(), 60.0, NOW);

        assertThat(req1.getEstimatedWaitMinutes()).isEqualTo(15.0);
        assertThat(req2.getEstimatedWaitMinutes()).isEqualTo(35.0); // 15 + 20
    }

    @Test
    @DisplayName("WT-004: No compatible operational charger → returns -1.0")
    void testProspectiveWait_noCompatibleCharger_returnsMinusOne() {
        ChargingStation station = new ChargingStation("STN-01", "Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0);
        Charger c1 = new Charger("CHG-01", station, "Bay 1", ChargerType.STANDARD_AC, 22.0, Set.of(ConnectorType.TYPE2));

        double wait = estimator.estimateProspectiveWait(
                station, ConnectorType.CHADEMO, 30, 50,
                List.of(), List.of(c1), List.of(), List.of(), 0.0, NOW);

        assertThat(wait).isEqualTo(-1.0);
    }

    @Test
    @DisplayName("WT-005: Confirmed reservation shifts candidate start past reserved window")
    void testProspectiveWait_shiftsPastReservation() {
        ChargingStation station = new ChargingStation("STN-01", "Hub", "MG Rd", StationStatus.OPERATIONAL, 200.0);
        Charger c1 = new Charger("CHG-01", station, "Bay 1", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS));

        Reservation upcoming = new Reservation();
        upcoming.setCharger(c1);
        upcoming.setStatus(ReservationStatus.CONFIRMED);
        upcoming.setStartTime(NOW.plus(10, ChronoUnit.MINUTES));
        upcoming.setEndTime(NOW.plus(40, ChronoUnit.MINUTES));

        // 30-min request starting NOW (0..30) overlaps reservation (10..40), shifts to 40
        double wait = estimator.estimateProspectiveWait(
                station, ConnectorType.CCS, 30, 50,
                List.of(), List.of(c1), List.of(), List.of(upcoming), 0.0, NOW);

        assertThat(wait).isEqualTo(40.0);
    }
}
