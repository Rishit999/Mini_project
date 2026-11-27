package com.evchargeflow.engine2;

import com.evchargeflow.common.enums.*;
import com.evchargeflow.engine1.entity.*;
import com.evchargeflow.engine2.allocation.ChargerAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for ChargerAllocator (Engine 2 — Logic & Rule Engine)
 *
 * Tests findFeasibleCharger() and hasReservationConflict() directly.
 * Test Cases: CA-001 through CA-012
 */
@DisplayName("TC-E2-03: ChargerAllocator Unit Tests")
class ChargerAllocatorTest {

    private ChargerAllocator allocator;
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        allocator = new ChargerAllocator();
    }

    private ChargingStation makeStation(double maxPower) {
        ChargingStation s = new ChargingStation();
        s.setStationId("ST-001");
        s.setName("Test Station");
        s.setLocation("Test Location");
        s.setStatus(StationStatus.OPERATIONAL);
        s.setMaxPowerCapacityKw(maxPower);
        return s;
    }

    private Charger makeCharger(String id, ConnectorType connector, double powerKw,
                                ChargerOperationalStatus opStatus,
                                ChargerAvailabilityStatus avStatus) {
        Charger c = new Charger();
        c.setChargerId(id);
        Set<ConnectorType> connectors = new LinkedHashSet<>();
        connectors.add(connector);
        c.setSupportedConnectors(connectors);
        c.setPowerRatingKw(powerKw);
        c.setOperationalStatus(opStatus);
        c.setAvailabilityStatus(avStatus);
        return c;
    }

    private VehicleRequest makeRequest(ConnectorType connector, int expectedDuration) {
        VehicleRequest r = new VehicleRequest();
        r.setRequestId("REQ-TEST");
        r.setRequiredConnector(connector);
        r.setExpectedDurationMinutes(expectedDuration);
        r.setCurrentBatteryPercent(30.0);
        r.setArrivalTime(NOW);
        return r;
    }

    private Reservation makeReservation(Charger charger, Instant start, Instant end, ReservationStatus status) {
        Reservation res = new Reservation();
        res.setCharger(charger);
        res.setStartTime(start);
        res.setEndTime(end);
        res.setStatus(status);
        return res;
    }

    @Test
    @DisplayName("CA-001: Compatible OPERATIONAL+AVAILABLE charger → selected")
    void testAllocation_compatibleCharger() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNotNull();
        assertThat(result.getChargerId()).isEqualTo("C1");
    }

    @Test
    @DisplayName("CA-002: Incompatible connector → null")
    void testAllocation_incompatibleConnector() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.CCS, 50,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-003: OUT_OF_SERVICE charger → not selected")
    void testAllocation_outOfServiceCharger() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OUT_OF_SERVICE, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-004: MAINTENANCE charger → not selected")
    void testAllocation_maintenanceCharger() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.MAINTENANCE, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-005: Power capacity exceeded (90+22>100) → null")
    void testAllocation_powerCapacityExceeded() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 90.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-006: OCCUPIED charger → not selected")
    void testAllocation_occupiedCharger() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.OCCUPIED);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-007: Null station → null (guard)")
    void testAllocation_nullStation() {
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, null, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-008: OFFLINE station → null returned")
    void testAllocation_offlineStation() {
        ChargingStation station = makeStation(100);
        station.setStatus(StationStatus.OFFLINE);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-009: Active reservation conflict → charger skipped")
    void testAllocation_reservationConflict() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Reservation res = makeReservation(charger, NOW.minusSeconds(600), NOW.plusSeconds(1800), ReservationStatus.ACTIVE);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(res), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-010: Empty charger list → null")
    void testAllocation_emptyChargers() {
        ChargingStation station = makeStation(100);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(), List.of(), 0.0, NOW);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("CA-011: Multiple compatible chargers → lexicographically first selected")
    void testAllocation_multipleChargers_firstByIdSelected() {
        ChargingStation station = makeStation(200);
        Charger cB = makeCharger("C-B", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        Charger cA = makeCharger("C-A", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(cB, cA), List.of(), 0.0, NOW);

        assertThat(result).isNotNull();
        assertThat(result.getChargerId()).isEqualTo("C-A");
    }

    @Test
    @DisplayName("CA-012: Past completed reservation → no conflict, charger selected")
    void testAllocation_pastReservation_noConflict() {
        ChargingStation station = makeStation(100);
        Charger charger = makeCharger("C1", ConnectorType.TYPE2, 22,
            ChargerOperationalStatus.OPERATIONAL, ChargerAvailabilityStatus.AVAILABLE);
        VehicleRequest req = makeRequest(ConnectorType.TYPE2, 60);

        Reservation pastRes = makeReservation(charger,
            NOW.minusSeconds(7200), NOW.minusSeconds(3600), ReservationStatus.COMPLETED);

        Charger result = allocator.findFeasibleCharger(req, station, List.of(charger), List.of(pastRes), 0.0, NOW);

        assertThat(result).isNotNull();
    }
}
