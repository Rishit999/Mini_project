package com.evchargeflow.config;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.ChargerType;
import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.common.enums.UserRole;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.UserAccount;
import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine1.repository.ChargingStationRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.repository.StationConfigurationRepository;
import com.evchargeflow.engine1.repository.UserAccountRepository;
import com.evchargeflow.engine1.repository.VehicleRepository;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine1.service.AuditLogService;
import com.evchargeflow.engine2.priority.PriorityCalculator;
import com.evchargeflow.engine2.queue.QueueManager;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

/**
 * Seeds realistic demo data on initial startup when the database is empty.
 */
@Component
@ConditionalOnProperty(name = "evchargeflow.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final UserAccountRepository userRepo;
    private final ChargingStationRepository stationRepo;
    private final StationConfigurationRepository configRepo;
    private final ChargerRepository chargerRepo;
    private final VehicleRepository vehicleRepo;
    private final VehicleRequestRepository requestRepo;
    private final ReservationRepository reservationRepo;
    private final ChargingSessionRepository sessionRepo;
    private final PriorityCalculator priorityCalculator;
    private final QueueManager queueManager;
    private final StationOrchestrator orchestrator;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public DataSeeder(
            UserAccountRepository userRepo,
            ChargingStationRepository stationRepo,
            StationConfigurationRepository configRepo,
            ChargerRepository chargerRepo,
            VehicleRepository vehicleRepo,
            VehicleRequestRepository requestRepo,
            ReservationRepository reservationRepo,
            ChargingSessionRepository sessionRepo,
            PriorityCalculator priorityCalculator,
            QueueManager queueManager,
            StationOrchestrator orchestrator,
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
        this.priorityCalculator = priorityCalculator;
        this.queueManager = queueManager;
        this.orchestrator = orchestrator;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (stationRepo.count() > 0) {
            queueManager.syncFromDatabase(clock.instant());
            return;
        }

        Instant now = clock.instant();

        // 1. Seed Users
        UserAccount u1 = userRepo.save(new UserAccount("USR-001", "Aarav Sharma", "aarav.sharma@evflow.demo", "+91-9876501001", UserRole.DRIVER));
        UserAccount u2 = userRepo.save(new UserAccount("USR-002", "Priya Nair", "priya.nair@evflow.demo", "+91-9876501002", UserRole.DRIVER));
        UserAccount u3 = userRepo.save(new UserAccount("USR-003", "Rohan Verma", "rohan.verma@evflow.demo", "+91-9876501003", UserRole.DRIVER));
        UserAccount u4 = userRepo.save(new UserAccount("USR-004", "Meera Patel", "meera.patel@evflow.demo", "+91-9876501004", UserRole.DRIVER));
        userRepo.save(new UserAccount("USR-ADMIN", "Station Operator", "operator@evflow.demo", "+91-9876500001", UserRole.OPERATOR));

        // 2. Seed Stations & Configurations
        ChargingStation s1 = stationRepo.save(new ChargingStation(
                "STN-01", "Central Tech Park Hub", "MG Road, Bengaluru", StationStatus.OPERATIONAL, 250.0));
        ChargingStation s2 = stationRepo.save(new ChargingStation(
                "STN-02", "Greenway Express Plaza", "Indiranagar 100ft Rd, Bengaluru", StationStatus.OPERATIONAL, 180.0));
        ChargingStation s3 = stationRepo.save(new ChargingStation(
                "STN-03", "Airport Corridor Depot", "Hebbal Ring Road, Bengaluru", StationStatus.MAINTENANCE, 200.0));

        configRepo.save(new StationConfiguration(s1));
        configRepo.save(new StationConfiguration(s2));
        configRepo.save(new StationConfiguration(s3));

        // 3. Seed Chargers
        Charger c1 = chargerRepo.save(new Charger("CHG-01", s1, "Bay 1 - HyperRapid DC", ChargerType.RAPID_DC, 120.0, Set.of(ConnectorType.CCS, ConnectorType.NACS)));
        Charger c2 = chargerRepo.save(new Charger("CHG-02", s1, "Bay 2 - Fast DC", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS, ConnectorType.CHADEMO)));
        Charger c3 = chargerRepo.save(new Charger("CHG-03", s1, "Bay 3 - Fast DC", ChargerType.FAST_DC, 50.0, Set.of(ConnectorType.CCS)));
        Charger c4 = chargerRepo.save(new Charger("CHG-04", s1, "Bay 4 - Standard AC", ChargerType.STANDARD_AC, 22.0, Set.of(ConnectorType.TYPE2)));

        Charger c5 = chargerRepo.save(new Charger("CHG-05", s2, "Bay 1 - Rapid DC", ChargerType.RAPID_DC, 100.0, Set.of(ConnectorType.CCS, ConnectorType.NACS)));
        Charger c6 = chargerRepo.save(new Charger("CHG-06", s2, "Bay 2 - Fast DC", ChargerType.FAST_DC, 60.0, Set.of(ConnectorType.CCS, ConnectorType.CHADEMO)));
        Charger c7 = chargerRepo.save(new Charger("CHG-07", s2, "Bay 3 - Standard AC", ChargerType.STANDARD_AC, 22.0, Set.of(ConnectorType.TYPE2)));

        Charger c8 = chargerRepo.save(new Charger("CHG-08", s3, "Bay 1 - Depot DC", ChargerType.RAPID_DC, 120.0, Set.of(ConnectorType.CCS)));
        c8.setOperationalStatus(ChargerOperationalStatus.MAINTENANCE);
        c8.setAvailabilityStatus(ChargerAvailabilityStatus.UNAVAILABLE);
        chargerRepo.save(c8);
        chargerRepo.save(new Charger("CHG-09", s3, "Bay 2 - Depot AC", ChargerType.STANDARD_AC, 22.0, Set.of(ConnectorType.TYPE2)));

        // 4. Seed Vehicles
        Vehicle v1 = vehicleRepo.save(new Vehicle("VEH-101", "KA-01-EV-1024", u1, u1.getFullName(), "Hyundai Ioniq 5", ConnectorType.CCS, 72.6, 14.0));
        Vehicle v2 = vehicleRepo.save(new Vehicle("VEH-102", "KA-02-EV-2048", u2, u2.getFullName(), "Tata Nexon EV Max", ConnectorType.CCS, 40.5, 19.0));
        Vehicle v3 = vehicleRepo.save(new Vehicle("VEH-103", "KA-03-EV-3096", u3, u3.getFullName(), "MG ZS EV", ConnectorType.CCS, 50.3, 9.0));
        Vehicle v4 = vehicleRepo.save(new Vehicle("VEH-104", "KA-04-EV-4120", u4, u4.getFullName(), "BYD Atto 3", ConnectorType.CCS, 60.5, 28.0));
        Vehicle v5 = vehicleRepo.save(new Vehicle("VEH-105", "KA-05-EV-5500", u1, u1.getFullName(), "Mahindra XUV400", ConnectorType.TYPE2, 39.4, 34.0));
        Vehicle v6 = vehicleRepo.save(new Vehicle("VEH-106", "KA-05-EV-6611", u2, u2.getFullName(), "Tesla Model 3", ConnectorType.NACS, 75.0, 22.0));
        Vehicle v7 = vehicleRepo.save(new Vehicle("VEH-107", "KA-01-EV-7722", u3, u3.getFullName(), "Nissan Leaf", ConnectorType.CHADEMO, 40.0, 48.0));
        Vehicle v8 = vehicleRepo.save(new Vehicle("VEH-108", "KA-02-EV-8833", u4, u4.getFullName(), "Kia EV6", ConnectorType.CCS, 77.4, 65.0));

        // 5. Active charging sessions on STN-01 consuming 180/250 kW (leaving 70 kW available, while CHG-03 is 50 kW or occupied)
        VehicleRequest reqActive1 = createRequest("REQ-1001", v1, s1, 14.0, 85.0, ConnectorType.CCS, 35,
                now.minus(20, ChronoUnit.MINUTES), now.plus(35, ChronoUnit.MINUTES), RequestStatus.CHARGING, c1.getChargerId(),
                "Charging session active (simulated) on CHG-01.");
        c1.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
        c1.setCurrentVehicleId(v1.getVehicleId());
        c1.setCurrentRequestId(reqActive1.getRequestId());
        chargerRepo.save(c1);

        ChargingSession ses1 = new ChargingSession();
        ses1.setSessionId("SES-1001");
        ses1.setRequest(reqActive1);
        ses1.setVehicle(v1);
        ses1.setStation(s1);
        ses1.setCharger(c1);
        ses1.setStartTime(now.minus(18, ChronoUnit.MINUTES));
        ses1.setExpectedEndTime(now.plus(17, ChronoUnit.MINUTES));
        ses1.setAllocatedPowerKw(c1.getPowerRatingKw());
        ses1.setEnergyConsumedKwh(32.4);
        ses1.setStatus(SessionStatus.ACTIVE);
        ses1.setSimulated(true);
        sessionRepo.save(ses1);

        VehicleRequest reqActive2 = createRequest("REQ-1002", v2, s1, 19.0, 80.0, ConnectorType.CCS, 40,
                now.minus(12, ChronoUnit.MINUTES), now.plus(50, ChronoUnit.MINUTES), RequestStatus.CHARGING, c2.getChargerId(),
                "Charging session active (simulated) on CHG-02.");
        c2.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
        c2.setCurrentVehicleId(v2.getVehicleId());
        c2.setCurrentRequestId(reqActive2.getRequestId());
        chargerRepo.save(c2);

        ChargingSession ses2 = new ChargingSession();
        ses2.setSessionId("SES-1002");
        ses2.setRequest(reqActive2);
        ses2.setVehicle(v2);
        ses2.setStation(s1);
        ses2.setCharger(c2);
        ses2.setStartTime(now.minus(10, ChronoUnit.MINUTES));
        ses2.setExpectedEndTime(now.plus(30, ChronoUnit.MINUTES));
        ses2.setAllocatedPowerKw(c2.getPowerRatingKw());
        ses2.setEnergyConsumedKwh(9.0);
        ses2.setStatus(SessionStatus.ACTIVE);
        ses2.setSimulated(true);
        sessionRepo.save(ses2);

        // Occupy CHG-03 as well so CCS vehicles queue cleanly at STN-01
        VehicleRequest reqActive3 = createRequest("REQ-1003", v6, s1, 22.0, 80.0, ConnectorType.CCS, 30,
                now.minus(5, ChronoUnit.MINUTES), now.plus(45, ChronoUnit.MINUTES), RequestStatus.CHARGING, c3.getChargerId(),
                "Charging session active (simulated) on CHG-03.");
        c3.setAvailabilityStatus(ChargerAvailabilityStatus.OCCUPIED);
        c3.setCurrentVehicleId(v6.getVehicleId());
        c3.setCurrentRequestId(reqActive3.getRequestId());
        chargerRepo.save(c3);

        ChargingSession ses3 = new ChargingSession();
        ses3.setSessionId("SES-1003");
        ses3.setRequest(reqActive3);
        ses3.setVehicle(v6);
        ses3.setStation(s1);
        ses3.setCharger(c3);
        ses3.setStartTime(now.minus(5, ChronoUnit.MINUTES));
        ses3.setExpectedEndTime(now.plus(25, ChronoUnit.MINUTES));
        ses3.setAllocatedPowerKw(c3.getPowerRatingKw());
        ses3.setEnergyConsumedKwh(3.75);
        ses3.setStatus(SessionStatus.ACTIVE);
        ses3.setSimulated(true);
        sessionRepo.save(ses3);

        // Historical completed session for analytics
        ChargingSession sesCompleted = new ChargingSession();
        sesCompleted.setSessionId("SES-0999");
        sesCompleted.setVehicle(v8);
        sesCompleted.setStation(s2);
        sesCompleted.setCharger(c5);
        sesCompleted.setStartTime(now.minus(120, ChronoUnit.MINUTES));
        sesCompleted.setExpectedEndTime(now.minus(85, ChronoUnit.MINUTES));
        sesCompleted.setActualEndTime(now.minus(85, ChronoUnit.MINUTES));
        sesCompleted.setAllocatedPowerKw(100.0);
        sesCompleted.setEnergyConsumedKwh(52.5);
        sesCompleted.setStatus(SessionStatus.COMPLETED);
        sesCompleted.setSimulated(true);
        sessionRepo.save(sesCompleted);

        // 6. Queued requests on STN-01 (CCS chargers busy -> queued with varied priority scores)
        createRequest("REQ-1004", v3, s1, 9.0, 85.0, ConnectorType.CCS, 40,
                now.minus(35, ChronoUnit.MINUTES), now.plus(25, ChronoUnit.MINUTES), RequestStatus.QUEUED, null,
                "All compatible CCS chargers are currently occupied.");
        createRequest("REQ-1005", v4, s1, 28.0, 80.0, ConnectorType.CCS, 30,
                now.minus(18, ChronoUnit.MINUTES), now.plus(95, ChronoUnit.MINUTES), RequestStatus.QUEUED, null,
                "All compatible CCS chargers are currently occupied.");
        createRequest("REQ-1006", v7, s2, 48.0, 90.0, ConnectorType.CHADEMO, 45,
                now.minus(8, ChronoUnit.MINUTES), now.plus(180, ChronoUnit.MINUTES), RequestStatus.QUEUED, null,
                "Queued for allocation at Greenway Express Plaza.");

        // 7. Reservations
        Reservation res1 = new Reservation();
        res1.setReservationId("RES-1001");
        res1.setVehicle(v5);
        res1.setStation(s1);
        res1.setCharger(c4);
        res1.setStartTime(now.plus(30, ChronoUnit.MINUTES));
        res1.setEndTime(now.plus(90, ChronoUnit.MINUTES));
        res1.setStatus(ReservationStatus.CONFIRMED);
        res1.setNotes("Evening scheduled AC top-up");
        res1.setCreatedAt(now.minus(40, ChronoUnit.MINUTES));
        reservationRepo.save(res1);

        Reservation res2 = new Reservation();
        res2.setReservationId("RES-1002");
        res2.setVehicle(v8);
        res2.setStation(s2);
        res2.setCharger(c5);
        res2.setStartTime(now.plus(60, ChronoUnit.MINUTES));
        res2.setEndTime(now.plus(105, ChronoUnit.MINUTES));
        res2.setStatus(ReservationStatus.CONFIRMED);
        res2.setNotes("Pre-booked rapid DC bay");
        res2.setCreatedAt(now.minus(25, ChronoUnit.MINUTES));
        reservationRepo.save(res2);

        queueManager.syncFromDatabase(now);
        orchestrator.refreshStationWaitEstimates("STN-01", now);
        orchestrator.refreshStationWaitEstimates("STN-02", now);

        auditLogService.log("SYSTEM", "SEED", "Initialized EVChargeFlow demo stations, chargers, vehicles, active sessions, queue, and reservations.");
    }

    private VehicleRequest createRequest(
            String id,
            Vehicle vehicle,
            ChargingStation station,
            double batteryPct,
            Double targetPct,
            ConnectorType connector,
            int durationMin,
            Instant arrival,
            Instant departure,
            RequestStatus status,
            String chargerId,
            String reason) {
        VehicleRequest req = new VehicleRequest();
        req.setRequestId(id);
        req.setVehicle(vehicle);
        req.setStation(station);
        req.setCurrentBatteryPercent(batteryPct);
        req.setTargetBatteryPercent(targetPct);
        req.setRequiredConnector(connector);
        req.setExpectedDurationMinutes(durationMin);
        req.setArrivalTime(arrival);
        req.setDesiredDepartureTime(departure);
        req.setStatus(status);
        req.setAssignedChargerId(chargerId);
        req.setStatusReason(reason);
        priorityCalculator.applyToRequest(req, clock.instant());
        return requestRepo.save(req);
    }
}
