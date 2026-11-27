package com.evchargeflow.engine1;

import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.validation.IntegrityValidator;
import com.evchargeflow.common.exception.DomainValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for IntegrityValidator (Engine 1 — Data Management & Integrity Engine)
 *
 * Test Cases: IV-001 through IV-014
 */
@DisplayName("TC-E1-01: IntegrityValidator Unit Tests")
class IntegrityValidatorTest {

    private IntegrityValidator validator;

    @BeforeEach
    void setUp() {
        validator = new IntegrityValidator();
    }

    private Vehicle makeVehicle(double battery) {
        Vehicle v = new Vehicle();
        v.setVehicleId("V1");
        v.setRegistrationNumber("KA01AB1234");
        v.setOwnerName("Test Owner");
        v.setModel("Test Model");
        v.setBatteryCapacityKwh(60.0);
        v.setCurrentBatteryPercent(battery);
        v.setConnectorType(ConnectorType.TYPE2);
        return v;
    }

    // ─── Battery Validation ────────────────────────────────────────────────────

    @Test
    @DisplayName("IV-001: Battery 0% → valid (edge case)")
    void testBattery_zero_valid() {
        Vehicle v = makeVehicle(0.0);
        assertThatCode(() -> validator.validateVehicle(v)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IV-002: Battery 100% → valid")
    void testBattery_100_valid() {
        Vehicle v = makeVehicle(100.0);
        assertThatCode(() -> validator.validateVehicle(v)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IV-003: Battery -1% → invalid")
    void testBattery_negative_invalid() {
        Vehicle v = makeVehicle(-1.0);
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-004: Battery 101% → invalid")
    void testBattery_over100_invalid() {
        Vehicle v = makeVehicle(101.0);
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    // ─── Vehicle Validation ────────────────────────────────────────────────────

    @Test
    @DisplayName("IV-005: Null registration number → invalid")
    void testVehicle_nullRegistration_invalid() {
        Vehicle v = makeVehicle(50.0);
        v.setRegistrationNumber(null);
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-006: Blank owner name → invalid")
    void testVehicle_blankOwner_invalid() {
        Vehicle v = makeVehicle(50.0);
        v.setOwnerName("   ");
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-007: Null connector type → invalid")
    void testVehicle_nullConnector_invalid() {
        Vehicle v = makeVehicle(50.0);
        v.setConnectorType(null);
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-008: Battery capacity below 10 kWh → invalid")
    void testVehicle_lowBatteryCapacity_invalid() {
        Vehicle v = makeVehicle(50.0);
        v.setBatteryCapacityKwh(5.0); // below 10 kWh minimum
        assertThatThrownBy(() -> validator.validateVehicle(v))
            .isInstanceOf(DomainValidationException.class);
    }

    // ─── Battery Percent Validation (direct) ──────────────────────────────────

    @Test
    @DisplayName("IV-009: validateBatteryPercent 50.0 → valid")
    void testBatteryPercent_50_valid() {
        assertThatCode(() -> validator.validateBatteryPercent(50.0, "Test field"))
            .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IV-010: validateBatteryPercent 105.0 → invalid")
    void testBatteryPercent_over100_invalid() {
        assertThatThrownBy(() -> validator.validateBatteryPercent(105.0, "Test field"))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-011: validateBatteryPercent -5.0 → invalid")
    void testBatteryPercent_negative_invalid() {
        assertThatThrownBy(() -> validator.validateBatteryPercent(-5.0, "Test field"))
            .isInstanceOf(DomainValidationException.class);
    }

    @Test
    @DisplayName("IV-012: validateBatteryPercent NaN → invalid")
    void testBatteryPercent_NaN_invalid() {
        assertThatThrownBy(() -> validator.validateBatteryPercent(Double.NaN, "Test field"))
            .isInstanceOf(DomainValidationException.class);
    }

    // ─── State Transition Validation ──────────────────────────────────────────

    @Test
    @DisplayName("IV-013: Valid transition QUEUED → ASSIGNED")
    void testTransition_queued_to_assigned_valid() {
        // Build a minimal VehicleRequest for the transition test
        com.evchargeflow.engine1.entity.VehicleRequest r = new com.evchargeflow.engine1.entity.VehicleRequest();
        r.setStatus(RequestStatus.QUEUED);
        assertThatCode(() ->
            validator.validateRequestTransition(r, RequestStatus.ASSIGNED)
        ).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("IV-014: Invalid transition COMPLETED → CHARGING")
    void testTransition_completed_to_charging_invalid() {
        com.evchargeflow.engine1.entity.VehicleRequest r = new com.evchargeflow.engine1.entity.VehicleRequest();
        r.setStatus(RequestStatus.COMPLETED);
        assertThatThrownBy(() ->
            validator.validateRequestTransition(r, RequestStatus.CHARGING)
        ).isInstanceOf(DomainValidationException.class);
    }
}
