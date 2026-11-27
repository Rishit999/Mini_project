package com.evchargeflow.engine1.service;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
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
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.UserAccount;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine1.repository.ChargingStationRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.repository.StationConfigurationRepository;
import com.evchargeflow.engine1.repository.UserAccountRepository;
import com.evchargeflow.engine1.repository.VehicleRepository;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine1.validation.IntegrityValidator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Engine 1: Data Management & Integrity Service.
 * Provides validated CRUD and referential integrity across users, stations, chargers, vehicles, and settings.
 */
@Service
public class DataIntegrityService {

    private static final Set<RequestStatus> ACTIVE_REQUEST_STATUSES =
            Set.of(RequestStatus.QUEUED, RequestStatus.ASSIGNED, RequestStatus.CHARGING);
    private static final Set<ReservationStatus> ACTIVE_RESERVATION_STATUSES =
            Set.of(ReservationStatus.CONFIRMED, ReservationStatus.ACTIVE);

    private final UserAccountRepository userRepo;
    private final ChargingStationRepository stationRepo;
    private final StationConfigurationRepository configRepo;
    private final ChargerRepository chargerRepo;
    private final VehicleRepository vehicleRepo;
    private final VehicleRequestRepository requestRepo;
    private final ReservationRepository reservationRepo;
    private final ChargingSessionRepository sessionRepo;
    private final IntegrityValidator validator;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public DataIntegrityService(
            UserAccountRepository userRepo,
            ChargingStationRepository stationRepo,
            StationConfigurationRepository configRepo,
            ChargerRepository chargerRepo,
            VehicleRepository vehicleRepo,
            VehicleRequestRepository requestRepo,
            ReservationRepository reservationRepo,
            ChargingSessionRepository sessionRepo,
            IntegrityValidator validator,
            AuditLogService auditLogService,
            Clock clock) {
        this.userRepo = userRepo;
        this.stationRepo = stationRepo;
        this.configRepo = configRepo;
        this.chargerRepo = chargerRepo;
        this.vehicleRepo = vehicleRepo;
        this.requestRepo = requestRepo;
        this.reservationRepo = reservationRepo;
        this.sessionRepo = sessionRepo;
        this.validator = validator;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    // --- Users ---
    @Transactional(readOnly = true)
    public List<UserAccount> getAllUsers() {
        return userRepo.findAll();
    }

    @Transactional
    public UserAccount createUser(UserAccount user) {
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            user.setUserId("USR-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        userRepo.findByEmailIgnoreCase(user.getEmail()).ifPresent(existing -> {
            throw new ConflictException("DUP_EMAIL", "A user with email " + user.getEmail() + " already exists.");
        });
        UserAccount saved = userRepo.save(user);
        auditLogService.log("USER", "CREATE", "Registered user " + saved.getUserId() + " (" + saved.getFullName() + ")");
        return saved;
    }

    // --- Charging Stations ---
    @Transactional(readOnly = true)
    public List<ChargingStation> getAllStations() {
        return stationRepo.findAll();
    }

    @Transactional(readOnly = true)
    public ChargingStation getStationOrThrow(String stationId) {
        return stationRepo.findById(stationId)
                .orElseThrow(() -> new ResourceNotFoundException("STN_NOT_FOUND", "Station not found: " + stationId));
    }

    @Transactional
    public ChargingStation createStation(ChargingStation station) {
        if (station.getStationId() == null || station.getStationId().isBlank()) {
            station.setStationId("STN-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (stationRepo.existsById(station.getStationId())) {
            throw new ConflictException("DUP_STATION", "Station ID already exists: " + station.getStationId());
        }
        station.setCreatedAt(clock.instant());
        station.setUpdatedAt(clock.instant());
        validator.validateStation(station);
        ChargingStation saved = stationRepo.save(station);
        configRepo.save(new StationConfiguration(saved));
        auditLogService.log("STATION", "CREATE", "Registered station " + saved.getStationId() + " (" + saved.getName() + ")");
        return saved;
    }

    @Transactional
    public ChargingStation updateStation(String stationId, String name, String location, StationStatus status, double maxPowerCapacityKw) {
        ChargingStation station = getStationOrThrow(stationId);
        double allocatedKw = getAllocatedPowerKw(stationId);
        if (maxPowerCapacityKw < allocatedKw) {
            throw new DomainValidationException("VAL_POWER_BELOW_ALLOCATED",
                    String.format("Station max power (%.1f kW) cannot be reduced below currently allocated power (%.1f kW).",
                            maxPowerCapacityKw, allocatedKw));
        }
        station.setName(name);
        station.setLocation(location);
        station.setStatus(status);
        station.setMaxPowerCapacityKw(maxPowerCapacityKw);
        station.setUpdatedAt(clock.instant());
        validator.validateStation(station);
        ChargingStation saved = stationRepo.save(station);
        auditLogService.log("STATION", "UPDATE", "Updated station " + stationId + " (Status=" + status + ", MaxPower=" + maxPowerCapacityKw + " kW)");
        return saved;
    }

    @Transactional
    public ChargingStation updateStationStatus(String stationId, StationStatus status) {
        ChargingStation station = getStationOrThrow(stationId);
        station.setStatus(status);
        station.setUpdatedAt(clock.instant());
        ChargingStation saved = stationRepo.save(station);
        auditLogService.log("STATION", "STATUS", "Station " + stationId + " status changed to " + status);
        return saved;
    }

    // --- Station Configuration / Settings ---
    @Transactional
    public StationConfiguration getOrCreateConfig(String stationId) {
        ChargingStation station = getStationOrThrow(stationId);
        return configRepo.findById(stationId)
                .orElseGet(() -> configRepo.save(new StationConfiguration(station)));
    }

    @Transactional
    public StationConfiguration updateStationConfig(String stationId, StationConfiguration incoming, Double maxPowerCapacityKw) {
        ChargingStation station = getStationOrThrow(stationId);
        StationConfiguration existing = getOrCreateConfig(stationId);

        if (maxPowerCapacityKw != null) {
            double allocated = getAllocatedPowerKw(stationId);
            if (maxPowerCapacityKw < allocated) {
                throw new DomainValidationException("VAL_POWER_BELOW_ALLOCATED",
                        "Max power capacity cannot be less than currently allocated power (" + allocated + " kW).");
            }
            station.setMaxPowerCapacityKw(maxPowerCapacityKw);
            station.setUpdatedAt(clock.instant());
            validator.validateStation(station);
            stationRepo.save(station);
        }

        existing.setQueueRefreshIntervalSec(incoming.getQueueRefreshIntervalSec());
        existing.setReservationGracePeriodMinutes(incoming.getReservationGracePeriodMinutes());
        existing.setChargingEfficiencyFactor(incoming.getChargingEfficiencyFactor());
        existing.setDefaultChargingDurationMinutes(incoming.getDefaultChargingDurationMinutes());
        existing.setAutoAllocationEnabled(incoming.isAutoAllocationEnabled());
        existing.setSimulationSpeedMultiplier(incoming.getSimulationSpeedMultiplier());
        existing.setUpdatedAt(clock.instant());
        validator.validateStationConfig(existing);

        StationConfiguration saved = configRepo.save(existing);
        auditLogService.log("SETTINGS", "UPDATE", "Updated configuration for station " + stationId);
        return saved;
    }

    // --- Chargers ---
    @Transactional(readOnly = true)
    public List<Charger> getAllChargers() {
        return chargerRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Charger> getChargersByStation(String stationId) {
        getStationOrThrow(stationId);
        return chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
    }

    @Transactional(readOnly = true)
    public Charger getChargerOrThrow(String chargerId) {
        return chargerRepo.findById(chargerId)
                .orElseThrow(() -> new ResourceNotFoundException("CHG_NOT_FOUND", "Charger not found: " + chargerId));
    }

    @Transactional
    public Charger createCharger(Charger charger, String stationId) {
        ChargingStation station = getStationOrThrow(stationId);
        if (charger.getChargerId() == null || charger.getChargerId().isBlank()) {
            charger.setChargerId("CHG-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (chargerRepo.existsById(charger.getChargerId())) {
            throw new ConflictException("DUP_CHARGER", "Charger ID already exists: " + charger.getChargerId());
        }
        charger.setStation(station);
        charger.setOperationalStatus(ChargerOperationalStatus.OPERATIONAL);
        charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
        validator.validateCharger(charger);
        Charger saved = chargerRepo.save(charger);
        auditLogService.log("CHARGER", "CREATE", "Added charger " + saved.getChargerId() + " to " + stationId);
        return saved;
    }

    @Transactional
    public Charger updateChargerOperationalStatus(String chargerId, ChargerOperationalStatus status) {
        Charger charger = getChargerOrThrow(chargerId);
        validator.validateChargerOperationalChange(charger, status);
        charger.setOperationalStatus(status);
        if (status == ChargerOperationalStatus.OPERATIONAL) {
            if (charger.getCurrentVehicleId() == null) {
                charger.setAvailabilityStatus(ChargerAvailabilityStatus.AVAILABLE);
            }
        } else {
            charger.setAvailabilityStatus(ChargerAvailabilityStatus.UNAVAILABLE);
        }
        Charger saved = chargerRepo.save(charger);
        auditLogService.log("CHARGER", "STATUS", "Charger " + chargerId + " operational status -> " + status);
        return saved;
    }

    // --- Vehicles ---
    @Transactional(readOnly = true)
    public List<Vehicle> getAllVehicles() {
        return vehicleRepo.findAll();
    }

    @Transactional(readOnly = true)
    public Vehicle getVehicleOrThrow(String vehicleId) {
        return vehicleRepo.findById(vehicleId)
                .orElseThrow(() -> new ResourceNotFoundException("VEH_NOT_FOUND", "Vehicle not found: " + vehicleId));
    }

    @Transactional
    public Vehicle createVehicle(Vehicle vehicle, String ownerUserId) {
        if (vehicle.getVehicleId() == null || vehicle.getVehicleId().isBlank()) {
            vehicle.setVehicleId("VEH-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase());
        }
        if (vehicleRepo.existsById(vehicle.getVehicleId())) {
            throw new ConflictException("DUP_VEHICLE", "Vehicle ID already exists: " + vehicle.getVehicleId());
        }
        vehicleRepo.findByRegistrationNumberIgnoreCase(vehicle.getRegistrationNumber()).ifPresent(existing -> {
            throw new ConflictException("DUP_REG_NO",
                    "Vehicle with registration number " + vehicle.getRegistrationNumber() + " already exists.");
        });
        if (ownerUserId != null && !ownerUserId.isBlank()) {
            UserAccount owner = userRepo.findById(ownerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("USR_NOT_FOUND", "Owner user not found: " + ownerUserId));
            vehicle.setOwner(owner);
            if (vehicle.getOwnerName() == null || vehicle.getOwnerName().isBlank()) {
                vehicle.setOwnerName(owner.getFullName());
            }
        }
        validator.validateVehicle(vehicle);
        Vehicle saved = vehicleRepo.save(vehicle);
        auditLogService.log("VEHICLE", "CREATE", "Registered vehicle " + saved.getVehicleId() + " (" + saved.getRegistrationNumber() + ")");
        return saved;
    }

    @Transactional
    public Vehicle updateVehicle(String vehicleId, Vehicle incoming, String ownerUserId) {
        Vehicle existing = getVehicleOrThrow(vehicleId);
        vehicleRepo.findByRegistrationNumberIgnoreCase(incoming.getRegistrationNumber()).ifPresent(other -> {
            if (!other.getVehicleId().equals(vehicleId)) {
                throw new ConflictException("DUP_REG_NO",
                        "Another vehicle already uses registration number " + incoming.getRegistrationNumber());
            }
        });
        if (ownerUserId != null && !ownerUserId.isBlank()) {
            UserAccount owner = userRepo.findById(ownerUserId)
                    .orElseThrow(() -> new ResourceNotFoundException("USR_NOT_FOUND", "Owner user not found: " + ownerUserId));
            existing.setOwner(owner);
        }
        existing.setRegistrationNumber(incoming.getRegistrationNumber());
        existing.setOwnerName(incoming.getOwnerName());
        existing.setModel(incoming.getModel());
        existing.setConnectorType(incoming.getConnectorType());
        existing.setBatteryCapacityKwh(incoming.getBatteryCapacityKwh());
        existing.setCurrentBatteryPercent(incoming.getCurrentBatteryPercent());
        validator.validateVehicle(existing);
        Vehicle saved = vehicleRepo.save(existing);
        auditLogService.log("VEHICLE", "UPDATE", "Updated vehicle " + vehicleId);
        return saved;
    }

    @Transactional
    public void deleteVehicle(String vehicleId) {
        Vehicle vehicle = getVehicleOrThrow(vehicleId);
        if (requestRepo.existsByVehicleVehicleIdAndStatusIn(vehicleId, ACTIVE_REQUEST_STATUSES)) {
            throw new ConflictException("VEH_IN_USE",
                    "Cannot delete vehicle " + vehicleId + " while it has an active or queued charging request.");
        }
        if (sessionRepo.existsByVehicleVehicleIdAndStatus(vehicleId, SessionStatus.ACTIVE)) {
            throw new ConflictException("VEH_IN_SESSION",
                    "Cannot delete vehicle " + vehicleId + " while it has an active charging session.");
        }
        if (reservationRepo.existsByVehicleVehicleIdAndStatusIn(vehicleId, ACTIVE_RESERVATION_STATUSES)) {
            throw new ConflictException("VEH_RESERVED",
                    "Cannot delete vehicle " + vehicleId + " while it has a confirmed or active reservation.");
        }
        if (requestRepo.existsByVehicleVehicleId(vehicleId)) {
            throw new ConflictException("VEH_HISTORY_EXISTS",
                    "Cannot delete vehicle " + vehicleId + " because historical charging records reference it.");
        }
        vehicleRepo.delete(vehicle);
        auditLogService.log("VEHICLE", "DELETE", "Deleted vehicle " + vehicleId);
    }

    // --- Power Calculations ---
    @Transactional(readOnly = true)
    public double getAllocatedPowerKw(String stationId) {
        List<ChargingSession> activeSessions = sessionRepo.findByStationStationIdAndStatus(stationId, SessionStatus.ACTIVE);
        double activeSessionPower = activeSessions.stream()
                .mapToDouble(ChargingSession::getAllocatedPowerKw)
                .sum();

        // Also account for chargers held in ASSIGNED state (waiting for operator to press Start Charging)
        List<Charger> stationChargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        double assignedNotStartedPower = stationChargers.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.OCCUPIED)
                .filter(c -> activeSessions.stream().noneMatch(s -> s.getCharger().getChargerId().equals(c.getChargerId())))
                .mapToDouble(Charger::getPowerRatingKw)
                .sum();

        return Math.round((activeSessionPower + assignedNotStartedPower) * 100.0) / 100.0;
    }
}
