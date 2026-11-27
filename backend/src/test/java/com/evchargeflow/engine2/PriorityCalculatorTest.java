package com.evchargeflow.engine2;

import com.evchargeflow.common.dto.PriorityBreakdown;
import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine2.priority.PriorityCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for PriorityCalculator (Engine 2 — Logic & Rule Engine)
 *
 * Test Cases: PC-001 through PC-020
 */
@DisplayName("TC-E2-01: PriorityCalculator Unit Tests")
class PriorityCalculatorTest {

    private PriorityCalculator calculator;
    // Fixed "now" reference point
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        calculator = new PriorityCalculator();
    }

    // Helper to build a minimal VehicleRequest for calculate()
    private VehicleRequest request(double batteryPct, Instant departure, Instant arrival) {
        VehicleRequest r = new VehicleRequest();
        r.setRequestId("TEST-REQ");
        r.setRequiredConnector(ConnectorType.TYPE2);
        r.setCurrentBatteryPercent(batteryPct);
        r.setDesiredDepartureTime(departure);
        r.setArrivalTime(arrival);
        return r;
    }

    // ─── Battery Score Tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("PC-001: Battery 0–10% → score 50")
    void testBattery_0to10_returns50() {
        // Input: batteryLevel=5% Expected: 50 (critical battery)
        assertThat(calculator.calculateBatteryScore(5)).isEqualTo(50);
    }

    @Test
    @DisplayName("PC-002: Battery exactly 10% → score 50")
    void testBattery_exactly10_returns50() {
        assertThat(calculator.calculateBatteryScore(10)).isEqualTo(50);
    }

    @Test
    @DisplayName("PC-003: Battery 11–20% → score 40")
    void testBattery_11to20_returns40() {
        assertThat(calculator.calculateBatteryScore(15)).isEqualTo(40);
        assertThat(calculator.calculateBatteryScore(20)).isEqualTo(40);
    }

    @Test
    @DisplayName("PC-004: Battery 21–30% → score 30")
    void testBattery_21to30_returns30() {
        assertThat(calculator.calculateBatteryScore(25)).isEqualTo(30);
        assertThat(calculator.calculateBatteryScore(30)).isEqualTo(30);
    }

    @Test
    @DisplayName("PC-005: Battery 31–40% → score 20")
    void testBattery_31to40_returns20() {
        assertThat(calculator.calculateBatteryScore(35)).isEqualTo(20);
        assertThat(calculator.calculateBatteryScore(40)).isEqualTo(20);
    }

    @Test
    @DisplayName("PC-006: Battery 41–60% → score 10")
    void testBattery_41to60_returns10() {
        assertThat(calculator.calculateBatteryScore(50)).isEqualTo(10);
        assertThat(calculator.calculateBatteryScore(60)).isEqualTo(10);
    }

    @Test
    @DisplayName("PC-007: Battery above 60% → score 0")
    void testBattery_above60_returns0() {
        assertThat(calculator.calculateBatteryScore(61)).isEqualTo(0);
        assertThat(calculator.calculateBatteryScore(100)).isEqualTo(0);
    }

    // ─── Departure Score Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("PC-008: Departure null → score 0")
    void testDeparture_null_returns0() {
        assertThat(calculator.calculateDepartureScore(null, NOW)).isEqualTo(0);
    }

    @Test
    @DisplayName("PC-009: Departure ≤30 min → score 30")
    void testDeparture_within30min_returns30() {
        // departure in 20 min
        Instant departure = NOW.plusSeconds(20 * 60);
        assertThat(calculator.calculateDepartureScore(departure, NOW)).isEqualTo(30);
    }

    @Test
    @DisplayName("PC-010: Departure 31–60 min → score 25")
    void testDeparture_31to60min_returns25() {
        Instant departure = NOW.plusSeconds(45 * 60);
        assertThat(calculator.calculateDepartureScore(departure, NOW)).isEqualTo(25);
    }

    @Test
    @DisplayName("PC-011: Departure 61–120 min → score 15")
    void testDeparture_61to120min_returns15() {
        Instant departure = NOW.plusSeconds(90 * 60);
        assertThat(calculator.calculateDepartureScore(departure, NOW)).isEqualTo(15);
    }

    @Test
    @DisplayName("PC-012: Departure 121–240 min → score 5")
    void testDeparture_121to240min_returns5() {
        Instant departure = NOW.plusSeconds(180 * 60);
        assertThat(calculator.calculateDepartureScore(departure, NOW)).isEqualTo(5);
    }

    @Test
    @DisplayName("PC-013: Departure >240 min → score 0")
    void testDeparture_above240min_returns0() {
        Instant departure = NOW.plusSeconds(300 * 60);
        assertThat(calculator.calculateDepartureScore(departure, NOW)).isEqualTo(0);
    }

    // ─── Waiting Time Score Tests ──────────────────────────────────────────────

    @Test
    @DisplayName("PC-014: Wait <15 min → score 0")
    void testWait_under15min_returns0() {
        // arrived 10 min ago
        Instant arrival = NOW.minusSeconds(10 * 60);
        assertThat(calculator.calculateWaitingTimeScore(arrival, NOW)).isEqualTo(0);
    }

    @Test
    @DisplayName("PC-015: Wait 15–29 min → score 5")
    void testWait_15to29min_returns5() {
        Instant arrival = NOW.minusSeconds(20 * 60);
        assertThat(calculator.calculateWaitingTimeScore(arrival, NOW)).isEqualTo(5);
    }

    @Test
    @DisplayName("PC-016: Wait 30–44 min → score 10")
    void testWait_30to44min_returns10() {
        Instant arrival = NOW.minusSeconds(35 * 60);
        assertThat(calculator.calculateWaitingTimeScore(arrival, NOW)).isEqualTo(10);
    }

    @Test
    @DisplayName("PC-017: Wait 45–59 min → score 15")
    void testWait_45to59min_returns15() {
        Instant arrival = NOW.minusSeconds(50 * 60);
        assertThat(calculator.calculateWaitingTimeScore(arrival, NOW)).isEqualTo(15);
    }

    @Test
    @DisplayName("PC-018: Wait ≥60 min → score 20")
    void testWait_60plusMin_returns20() {
        Instant arrival = NOW.minusSeconds(90 * 60);
        assertThat(calculator.calculateWaitingTimeScore(arrival, NOW)).isEqualTo(20);
    }

    // ─── Total Priority Score (using calculate(VehicleRequest, Instant)) ──────

    @Test
    @DisplayName("PC-019: Maximum score = 100 (battery 5%, departure ≤30 min, wait ≥60 min)")
    void testMaxScore_returns100() {
        // Battery 5% → 50, departure in 20 min → 30, arrived 90 min ago → 20
        Instant departure = NOW.plusSeconds(20 * 60);
        Instant arrival   = NOW.minusSeconds(90 * 60);
        VehicleRequest req = request(5, departure, arrival);
        PriorityBreakdown breakdown = calculator.calculate(req, NOW);

        assertThat(breakdown.batteryScore()).isEqualTo(50);
        assertThat(breakdown.departureScore()).isEqualTo(30);
        assertThat(breakdown.waitingTimeScore()).isEqualTo(20);
        assertThat(breakdown.totalScore()).isEqualTo(100);
    }

    @Test
    @DisplayName("PC-020: Minimum score = 0 (battery 100%, no departure, wait <15 min)")
    void testMinScore_returns0() {
        Instant arrival = NOW.minusSeconds(5 * 60); // 5 min ago
        VehicleRequest req = request(100, null, arrival);
        PriorityBreakdown breakdown = calculator.calculate(req, NOW);

        assertThat(breakdown.batteryScore()).isEqualTo(0);
        assertThat(breakdown.departureScore()).isEqualTo(0);
        assertThat(breakdown.waitingTimeScore()).isEqualTo(0);
        assertThat(breakdown.totalScore()).isEqualTo(0);
    }
}
