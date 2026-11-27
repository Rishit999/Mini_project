package com.evchargeflow.engine2;

import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine2.allocation.ChargerAllocator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for reservation overlap logic via ChargerAllocator.hasReservationConflict()
 * (Engine 2 — Logic & Rule Engine)
 *
 * Uses: ExistingStart < RequestedEnd AND RequestedStart < ExistingEnd (strict, adjacent OK)
 * Test Cases: RM-001 through RM-012
 */
@DisplayName("TC-E2-04: Reservation Conflict Detection Unit Tests")
class ReservationConflictTest {

    private ChargerAllocator allocator;
    private Charger charger;
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    @BeforeEach
    void setUp() {
        allocator = new ChargerAllocator();
        charger = new Charger();
        charger.setChargerId("C1");
    }

    private Reservation makeRes(Instant start, Instant end, ReservationStatus status) {
        Reservation r = new Reservation();
        r.setCharger(charger);
        r.setStartTime(start);
        r.setEndTime(end);
        r.setStatus(status);
        return r;
    }

    // ─── Overlap Detection Tests ───────────────────────────────────────────────

    @Test
    @DisplayName("RM-001: Exact same slot → conflict detected")
    void testOverlap_exactSameSlot() {
        Instant s = NOW.plusSeconds(3600);
        Instant e = NOW.plusSeconds(7200);
        Reservation ex = makeRes(s, e, ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict("C1", s, e, List.of(ex))).isTrue();
    }

    @Test
    @DisplayName("RM-002: Requested slot completely inside existing → conflict")
    void testOverlap_insideExisting() {
        Reservation ex = makeRes(NOW.plusSeconds(3600), NOW.plusSeconds(7200), ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(4200), NOW.plusSeconds(5400), List.of(ex)
        )).isTrue();
    }

    @Test
    @DisplayName("RM-003: Partial overlap at start → conflict")
    void testOverlap_partialAtStart() {
        Reservation ex = makeRes(NOW.plusSeconds(3600), NOW.plusSeconds(7200), ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(1800), NOW.plusSeconds(5400), List.of(ex)
        )).isTrue();
    }

    @Test
    @DisplayName("RM-004: Partial overlap at end → conflict")
    void testOverlap_partialAtEnd() {
        Reservation ex = makeRes(NOW.plusSeconds(3600), NOW.plusSeconds(7200), ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(5400), NOW.plusSeconds(9000), List.of(ex)
        )).isTrue();
    }

    @Test
    @DisplayName("RM-005: Adjacent slots (back-to-back) → NO conflict")
    void testAdjacentSlots_noConflict() {
        // Existing: 11:00–12:00. Request starts at 12:00 → adjacent, NOT overlapping
        Reservation ex = makeRes(NOW.plusSeconds(3600), NOW.plusSeconds(7200), ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(7200), NOW.plusSeconds(10800), List.of(ex)
        )).isFalse();
    }

    @Test
    @DisplayName("RM-006: Non-overlapping future slot → no conflict")
    void testNonOverlapping_noConflict() {
        Reservation ex = makeRes(NOW.plusSeconds(3600), NOW.plusSeconds(7200), ReservationStatus.CONFIRMED);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(9000), NOW.plusSeconds(12600), List.of(ex)
        )).isFalse();
    }

    @Test
    @DisplayName("RM-007: Different charger → no conflict even with overlapping times")
    void testDifferentCharger_noConflict() {
        Instant s = NOW.plusSeconds(3600);
        Instant e = NOW.plusSeconds(7200);
        Reservation ex = makeRes(s, e, ReservationStatus.CONFIRMED);

        // hasReservationConflict checks charger ID "C2" but reservation is on "C1"
        assertThat(allocator.hasReservationConflict("C2", s, e, List.of(ex))).isFalse();
    }

    @Test
    @DisplayName("RM-008: Cancelled reservation → no conflict")
    void testCancelledReservation_noConflict() {
        Instant s = NOW.plusSeconds(3600);
        Instant e = NOW.plusSeconds(7200);
        Reservation ex = makeRes(s, e, ReservationStatus.CANCELLED);

        assertThat(allocator.hasReservationConflict("C1", s, e, List.of(ex))).isFalse();
    }

    @Test
    @DisplayName("RM-009: Expired reservation → no conflict")
    void testExpiredReservation_noConflict() {
        Instant s = NOW.plusSeconds(3600);
        Instant e = NOW.plusSeconds(7200);
        Reservation ex = makeRes(s, e, ReservationStatus.EXPIRED);

        assertThat(allocator.hasReservationConflict("C1", s, e, List.of(ex))).isFalse();
    }

    @Test
    @DisplayName("RM-010: Empty reservation list → no conflict")
    void testEmptyReservations_noConflict() {
        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(3600), NOW.plusSeconds(7200), List.of()
        )).isFalse();
    }

    @Test
    @DisplayName("RM-011: Existing fully wraps requested slot → conflict")
    void testOverlap_existingWrapsRequested() {
        Reservation ex = makeRes(NOW.plusSeconds(1800), NOW.plusSeconds(14400), ReservationStatus.ACTIVE);

        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(3600), NOW.plusSeconds(7200), List.of(ex)
        )).isTrue();
    }

    @Test
    @DisplayName("RM-012: Multiple reservations, one overlaps → true")
    void testMultipleReservations_oneConflicts() {
        Reservation r1 = makeRes(NOW.plusSeconds(10800), NOW.plusSeconds(14400), ReservationStatus.CONFIRMED);
        Reservation r2 = makeRes(NOW.plusSeconds(3600),  NOW.plusSeconds(7200),  ReservationStatus.CONFIRMED);

        // Request overlaps r2 (11:30–12:30 overlaps 11:00–12:00)
        assertThat(allocator.hasReservationConflict(
            "C1", NOW.plusSeconds(5400), NOW.plusSeconds(9000), List.of(r1, r2)
        )).isTrue();
    }
}
