package com.evchargeflow.engine3.api;

import com.evchargeflow.common.dto.PriorityBreakdown;
import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.ChargerType;
import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.common.enums.UserRole;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;
import java.util.Set;

/**
 * Engine 3: Shared REST API request and response contracts.
 */
public final class ApiDtos {

    private ApiDtos() {
    }

    // --- Requests ---

    public record CreateUserRequest(
            String userId,
            @NotBlank(message = "Full name is required") String fullName,
            @Email(message = "Valid email is required") @NotBlank(message = "Email is required") String email,
            String phone,
            UserRole role
    ) {
    }

    public record StationUpsertRequest(
            String stationId,
            @NotBlank(message = "Station name is required") String name,
            @NotBlank(message = "Location is required") String location,
            @NotNull(message = "Station status is required") StationStatus status,
            @DecimalMin(value = "10.0", message = "Max power must be at least 10 kW")
            @DecimalMax(value = "5000.0", message = "Max power cannot exceed 5000 kW")
            double maxPowerCapacityKw
    ) {
    }

    public record UpdateStationStatusRequest(
            @NotNull(message = "Status is required") StationStatus status
    ) {
    }

    public record CreateChargerRequest(
            String chargerId,
            @NotBlank(message = "Station ID is required") String stationId,
            @NotBlank(message = "Charger name is required") String name,
            @NotNull(message = "Charger type is required") ChargerType chargerType,
            @DecimalMin(value = "3.7", message = "Power rating must be at least 3.7 kW")
            @DecimalMax(value = "350.0", message = "Power rating cannot exceed 350 kW")
            double powerRatingKw,
            @NotEmpty(message = "At least one supported connector is required")
            Set<ConnectorType> supportedConnectors
    ) {
    }

    public record UpdateChargerStatusRequest(
            @NotNull(message = "Operational status is required") ChargerOperationalStatus operationalStatus
    ) {
    }

    public record VehicleUpsertRequest(
            String vehicleId,
            @NotBlank(message = "Registration number is required") String registrationNumber,
            String ownerUserId,
            @NotBlank(message = "Owner name is required") String ownerName,
            @NotBlank(message = "Vehicle model is required") String model,
            @NotNull(message = "Connector type is required") ConnectorType connectorType,
            @DecimalMin(value = "10.0", message = "Battery capacity must be between 10 and 250 kWh")
            @DecimalMax(value = "250.0", message = "Battery capacity must be between 10 and 250 kWh")
            double batteryCapacityKwh,
            @DecimalMin(value = "0.0", message = "Current battery percentage must be between 0 and 100")
            @DecimalMax(value = "100.0", message = "Current battery percentage must be between 0 and 100")
            double currentBatteryPercent
    ) {
    }

    public record SubmitChargingRequestDto(
            @NotBlank(message = "Vehicle ID is required") String vehicleId,
            @NotBlank(message = "Station ID is required") String stationId,
            @DecimalMin(value = "0.0", message = "Current battery must be 0-100%")
            @DecimalMax(value = "100.0", message = "Current battery must be 0-100%")
            double currentBatteryPercent,
            ConnectorType requiredConnector,
            @Min(value = 5, message = "Expected duration must be at least 5 minutes")
            @Max(value = 480, message = "Expected duration cannot exceed 480 minutes")
            int expectedDurationMinutes,
            Instant desiredDepartureTime,
            Double targetBatteryPercent
    ) {
    }

    public record CreateReservationRequest(
            @NotBlank(message = "Vehicle ID is required") String vehicleId,
            @NotBlank(message = "Station ID is required") String stationId,
            @NotBlank(message = "Charger ID is required") String chargerId,
            @NotNull(message = "Start time is required") Instant startTime,
            @NotNull(message = "End time is required") Instant endTime,
            String notes
    ) {
    }

    public record UpdateSettingsRequest(
            @DecimalMin(value = "10.0", message = "Max power capacity must be at least 10 kW")
            @DecimalMax(value = "5000.0", message = "Max power capacity cannot exceed 5000 kW")
            Double maxPowerCapacityKw,
            @Min(value = 3, message = "Queue refresh interval must be 3-300 sec")
            @Max(value = 300, message = "Queue refresh interval must be 3-300 sec")
            int queueRefreshIntervalSec,
            @Min(value = 5, message = "Grace period must be 5-60 min")
            @Max(value = 60, message = "Grace period must be 5-60 min")
            int reservationGracePeriodMinutes,
            @DecimalMin(value = "0.50", message = "Efficiency factor must be 0.50-1.00")
            @DecimalMax(value = "1.00", message = "Efficiency factor must be 0.50-1.00")
            double chargingEfficiencyFactor,
            @Min(value = 10, message = "Default duration must be 10-480 min")
            @Max(value = 480, message = "Default duration must be 10-480 min")
            int defaultChargingDurationMinutes,
            boolean autoAllocationEnabled,
            @DecimalMin(value = "0.5", message = "Simulation speed must be 0.5-20.0")
            @DecimalMax(value = "20.0", message = "Simulation speed must be 0.5-20.0")
            double simulationSpeedMultiplier
    ) {
    }

    // --- Responses ---

    public record UserDto(
            String userId,
            String fullName,
            String email,
            String phone,
            UserRole role,
            Instant createdAt
    ) {
    }

    public record StationSummaryDto(
            String stationId,
            String name,
            String location,
            StationStatus status,
            double maxPowerCapacityKw,
            double currentAllocatedPowerKw,
            double powerUtilizationPercent,
            int totalChargers,
            int availableChargers,
            int occupiedChargers,
            int reservedChargers,
            int maintenanceChargers,
            int queuedVehicles
    ) {
    }

    public record ChargerDto(
            String chargerId,
            String stationId,
            String stationName,
            String name,
            ChargerType chargerType,
            double powerRatingKw,
            Set<ConnectorType> supportedConnectors,
            ChargerOperationalStatus operationalStatus,
            ChargerAvailabilityStatus availabilityStatus,
            String currentVehicleId,
            String currentRequestId
    ) {
    }

    public record StationDetailDto(
            StationSummaryDto station,
            List<ChargerDto> chargers,
            List<QueueItemDto> queue,
            StationConfigDto configuration
    ) {
    }

    public record VehicleDto(
            String vehicleId,
            String registrationNumber,
            String ownerUserId,
            String ownerName,
            String model,
            ConnectorType connectorType,
            double batteryCapacityKwh,
            double currentBatteryPercent
    ) {
    }

    public record QueueItemDto(
            Integer queuePosition,
            String requestId,
            String vehicleId,
            String registrationNumber,
            String vehicleModel,
            String ownerName,
            String stationId,
            String stationName,
            double currentBatteryPercent,
            Double targetBatteryPercent,
            ConnectorType requiredConnector,
            int expectedDurationMinutes,
            Instant arrivalTime,
            Instant desiredDepartureTime,
            int priorityScore,
            PriorityBreakdown priorityBreakdown,
            double estimatedWaitMinutes,
            boolean waitTimeGuaranteed,
            RequestStatus status,
            String assignedChargerId,
            String statusReason
    ) {
    }

    public record ReservationDto(
            String reservationId,
            String vehicleId,
            String registrationNumber,
            String vehicleModel,
            String stationId,
            String stationName,
            String chargerId,
            String chargerName,
            Instant startTime,
            Instant endTime,
            ReservationStatus status,
            String notes,
            Instant createdAt
    ) {
    }

    public record SessionDto(
            String sessionId,
            String requestId,
            String reservationId,
            String vehicleId,
            String registrationNumber,
            String vehicleModel,
            String stationId,
            String stationName,
            String chargerId,
            String chargerName,
            Instant startTime,
            Instant expectedEndTime,
            Instant actualEndTime,
            double allocatedPowerKw,
            double energyConsumedKwh,
            long durationMinutes,
            SessionStatus status,
            boolean simulated
    ) {
    }

    public record StationConfigDto(
            String stationId,
            String stationName,
            double maxPowerCapacityKw,
            int queueRefreshIntervalSec,
            int reservationGracePeriodMinutes,
            double chargingEfficiencyFactor,
            int defaultChargingDurationMinutes,
            boolean autoAllocationEnabled,
            double simulationSpeedMultiplier,
            Instant updatedAt
    ) {
    }

    public record DashboardMetricsDto(
            int totalStations,
            int operationalStations,
            int totalChargers,
            int availableChargers,
            int occupiedChargers,
            int reservedChargers,
            int maintenanceChargers,
            int vehiclesWaiting,
            int activeSessions,
            int totalReservations,
            int activeOrConfirmedReservations,
            double totalMaxPowerKw,
            double totalAllocatedPowerKw,
            double powerUtilizationPercent,
            double averageEstimatedWaitMinutes
    ) {
    }

    public record PriorityBucketDto(
            String range,
            int count
    ) {
    }

    public record DashboardResponseDto(
            Instant timestamp,
            DashboardMetricsDto metrics,
            List<StationSummaryDto> stations,
            List<QueueItemDto> queuePreview,
            List<SessionDto> activeSessions,
            List<PriorityBucketDto> priorityDistribution
    ) {
    }

    public record AnalyticsResponseDto(
            Instant generatedAt,
            DashboardMetricsDto overview,
            List<StationSummaryDto> stationUtilization,
            List<PriorityBucketDto> priorityDistribution,
            int totalRequests,
            int queuedRequests,
            int assignedRequests,
            int completedRequests,
            int cancelledRequests,
            int totalSessions,
            int completedSessions,
            int stoppedSessions,
            double totalSimulatedEnergyKwh,
            double averageSessionDurationMinutes,
            double averageQueueWaitMinutes,
            List<ChargerUsageDto> chargerUsage
    ) {
    }

    public record ChargerUsageDto(
            String chargerId,
            String stationName,
            String chargerName,
            double powerRatingKw,
            int sessionCount,
            double totalSimulatedEnergyKwh,
            ChargerAvailabilityStatus currentAvailability
    ) {
    }
}
