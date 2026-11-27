package com.evchargeflow.engine3.integration;

import com.evchargeflow.common.dto.PriorityBreakdown;
import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.UserAccount;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine3.api.ApiDtos.ChargerDto;
import com.evchargeflow.engine3.api.ApiDtos.QueueItemDto;
import com.evchargeflow.engine3.api.ApiDtos.ReservationDto;
import com.evchargeflow.engine3.api.ApiDtos.SessionDto;
import com.evchargeflow.engine3.api.ApiDtos.StationConfigDto;
import com.evchargeflow.engine3.api.ApiDtos.StationSummaryDto;
import com.evchargeflow.engine3.api.ApiDtos.UserDto;
import com.evchargeflow.engine3.api.ApiDtos.VehicleDto;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Engine 3: Maps Engine 1 domain entities into clean API response DTOs.
 */
@Component
public class DtoMapper {

    private final Clock clock;

    public DtoMapper(Clock clock) {
        this.clock = clock;
    }

    public UserDto toUserDto(UserAccount user) {
        return new UserDto(
                user.getUserId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getCreatedAt()
        );
    }

    public StationSummaryDto toStationSummary(
            ChargingStation station,
            List<Charger> chargers,
            double allocatedPowerKw,
            int queuedCount) {

        int available = (int) chargers.stream()
                .filter(c -> c.getOperationalStatus() == ChargerOperationalStatus.OPERATIONAL
                        && c.getAvailabilityStatus() == ChargerAvailabilityStatus.AVAILABLE)
                .count();
        int occupied = (int) chargers.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.OCCUPIED)
                .count();
        int reserved = (int) chargers.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.RESERVED)
                .count();
        int maintenance = (int) chargers.stream()
                .filter(c -> c.getOperationalStatus() != ChargerOperationalStatus.OPERATIONAL)
                .count();

        double utilPct = station.getMaxPowerCapacityKw() > 0
                ? Math.min(100.0, Math.round((allocatedPowerKw / station.getMaxPowerCapacityKw()) * 1000.0) / 10.0)
                : 0.0;

        return new StationSummaryDto(
                station.getStationId(),
                station.getName(),
                station.getLocation(),
                station.getStatus(),
                station.getMaxPowerCapacityKw(),
                allocatedPowerKw,
                utilPct,
                chargers.size(),
                available,
                occupied,
                reserved,
                maintenance,
                queuedCount
        );
    }

    public ChargerDto toChargerDto(Charger charger) {
        return new ChargerDto(
                charger.getChargerId(),
                charger.getStation().getStationId(),
                charger.getStation().getName(),
                charger.getName(),
                charger.getChargerType(),
                charger.getPowerRatingKw(),
                charger.getSupportedConnectors(),
                charger.getOperationalStatus(),
                charger.getAvailabilityStatus(),
                charger.getCurrentVehicleId(),
                charger.getCurrentRequestId()
        );
    }

    public VehicleDto toVehicleDto(Vehicle vehicle) {
        return new VehicleDto(
                vehicle.getVehicleId(),
                vehicle.getRegistrationNumber(),
                vehicle.getOwner() != null ? vehicle.getOwner().getUserId() : null,
                vehicle.getOwnerName(),
                vehicle.getModel(),
                vehicle.getConnectorType(),
                vehicle.getBatteryCapacityKwh(),
                vehicle.getCurrentBatteryPercent()
        );
    }

    public QueueItemDto toQueueItemDto(VehicleRequest req, Integer queuePosition) {
        PriorityBreakdown breakdown = new PriorityBreakdown(
                req.getBatteryScore(),
                req.getDepartureScore(),
                req.getWaitingTimeScore(),
                req.getPriorityScore()
        );
        return new QueueItemDto(
                req.getStatus() == RequestStatus.QUEUED ? queuePosition : null,
                req.getRequestId(),
                req.getVehicle().getVehicleId(),
                req.getVehicle().getRegistrationNumber(),
                req.getVehicle().getModel(),
                req.getVehicle().getOwnerName(),
                req.getStation().getStationId(),
                req.getStation().getName(),
                req.getCurrentBatteryPercent(),
                req.getTargetBatteryPercent(),
                req.getRequiredConnector(),
                req.getExpectedDurationMinutes(),
                req.getArrivalTime(),
                req.getDesiredDepartureTime(),
                req.getPriorityScore(),
                breakdown,
                req.getEstimatedWaitMinutes(),
                false,
                req.getStatus(),
                req.getAssignedChargerId(),
                req.getStatusReason()
        );
    }

    public ReservationDto toReservationDto(Reservation res) {
        return new ReservationDto(
                res.getReservationId(),
                res.getVehicle().getVehicleId(),
                res.getVehicle().getRegistrationNumber(),
                res.getVehicle().getModel(),
                res.getStation().getStationId(),
                res.getStation().getName(),
                res.getCharger().getChargerId(),
                res.getCharger().getName(),
                res.getStartTime(),
                res.getEndTime(),
                res.getStatus(),
                res.getNotes(),
                res.getCreatedAt()
        );
    }

    public SessionDto toSessionDto(ChargingSession session) {
        Instant endRef = session.getActualEndTime() != null ? session.getActualEndTime() : clock.instant();
        long durationMinutes = Math.max(0, Duration.between(session.getStartTime(), endRef).toMinutes());

        double liveSimulatedEnergy = session.getEnergyConsumedKwh();
        if (session.getStatus() == SessionStatus.ACTIVE) {
            double elapsedHours = Math.max(0.0, Duration.between(session.getStartTime(), endRef).toSeconds() / 3600.0);
            liveSimulatedEnergy = Math.round((session.getAllocatedPowerKw() * 0.90 * elapsedHours) * 100.0) / 100.0;
        }

        return new SessionDto(
                session.getSessionId(),
                session.getRequest() != null ? session.getRequest().getRequestId() : null,
                session.getReservation() != null ? session.getReservation().getReservationId() : null,
                session.getVehicle().getVehicleId(),
                session.getVehicle().getRegistrationNumber(),
                session.getVehicle().getModel(),
                session.getStation().getStationId(),
                session.getStation().getName(),
                session.getCharger().getChargerId(),
                session.getCharger().getName(),
                session.getStartTime(),
                session.getExpectedEndTime(),
                session.getActualEndTime(),
                session.getAllocatedPowerKw(),
                liveSimulatedEnergy,
                durationMinutes,
                session.getStatus(),
                session.isSimulated()
        );
    }

    public StationConfigDto toStationConfigDto(StationConfiguration cfg) {
        return new StationConfigDto(
                cfg.getStationId(),
                cfg.getStation().getName(),
                cfg.getStation().getMaxPowerCapacityKw(),
                cfg.getQueueRefreshIntervalSec(),
                cfg.getReservationGracePeriodMinutes(),
                cfg.getChargingEfficiencyFactor(),
                cfg.getDefaultChargingDurationMinutes(),
                cfg.isAutoAllocationEnabled(),
                cfg.getSimulationSpeedMultiplier(),
                cfg.getUpdatedAt()
        );
    }
}
