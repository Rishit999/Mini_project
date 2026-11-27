package com.evchargeflow.engine3.integration;

import com.evchargeflow.common.enums.ChargerAvailabilityStatus;
import com.evchargeflow.common.enums.ChargerOperationalStatus;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import com.evchargeflow.engine3.api.ApiDtos.AnalyticsResponseDto;
import com.evchargeflow.engine3.api.ApiDtos.ChargerUsageDto;
import com.evchargeflow.engine3.api.ApiDtos.DashboardMetricsDto;
import com.evchargeflow.engine3.api.ApiDtos.DashboardResponseDto;
import com.evchargeflow.engine3.api.ApiDtos.PriorityBucketDto;
import com.evchargeflow.engine3.api.ApiDtos.QueueItemDto;
import com.evchargeflow.engine3.api.ApiDtos.SessionDto;
import com.evchargeflow.engine3.api.ApiDtos.StationSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class DashboardAnalyticsService {

    private final DataIntegrityService dataIntegrityService;
    private final StationOrchestrator orchestrator;
    private final ChargerRepository chargerRepo;
    private final VehicleRequestRepository requestRepo;
    private final ReservationRepository reservationRepo;
    private final ChargingSessionRepository sessionRepo;
    private final DtoMapper mapper;
    private final Clock clock;

    public DashboardAnalyticsService(
            DataIntegrityService dataIntegrityService,
            StationOrchestrator orchestrator,
            ChargerRepository chargerRepo,
            VehicleRequestRepository requestRepo,
            ReservationRepository reservationRepo,
            ChargingSessionRepository sessionRepo,
            DtoMapper mapper,
            Clock clock) {
        this.dataIntegrityService = dataIntegrityService;
        this.orchestrator = orchestrator;
        this.chargerRepo = chargerRepo;
        this.requestRepo = requestRepo;
        this.reservationRepo = reservationRepo;
        this.sessionRepo = sessionRepo;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DashboardResponseDto getDashboard() {
        List<VehicleRequest> liveQueue = orchestrator.getLiveQueue(null);
        List<StationSummaryDto> stationSummaries = buildStationSummaries(liveQueue);
        List<Charger> chargers = chargerRepo.findAll();
        List<ChargingSession> activeSessionEntities = sessionRepo.findByStatus(SessionStatus.ACTIVE);
        List<Reservation> reservations = reservationRepo.findAll();

        DashboardMetricsDto metrics = computeMetrics(
                stationSummaries, chargers, liveQueue, activeSessionEntities, reservations);

        List<QueueItemDto> queuePreview = new ArrayList<>();
        for (int i = 0; i < Math.min(liveQueue.size(), 8); i++) {
            queuePreview.add(mapper.toQueueItemDto(liveQueue.get(i), i + 1));
        }

        List<SessionDto> activeSessions = activeSessionEntities.stream()
                .map(mapper::toSessionDto)
                .toList();

        return new DashboardResponseDto(
                clock.instant(),
                metrics,
                stationSummaries,
                queuePreview,
                activeSessions,
                computePriorityDistribution(liveQueue)
        );
    }

    @Transactional
    public AnalyticsResponseDto getAnalytics() {
        List<VehicleRequest> liveQueue = orchestrator.getLiveQueue(null);
        List<StationSummaryDto> stationSummaries = buildStationSummaries(liveQueue);
        List<Charger> chargers = chargerRepo.findAll();
        List<VehicleRequest> allRequests = requestRepo.findAll();
        List<ChargingSession> allSessions = sessionRepo.findAllByOrderByStartTimeDesc();
        List<Reservation> reservations = reservationRepo.findAll();

        List<ChargingSession> activeSessions = allSessions.stream()
                .filter(s -> s.getStatus() == SessionStatus.ACTIVE)
                .toList();

        DashboardMetricsDto overview = computeMetrics(
                stationSummaries, chargers, liveQueue, activeSessions, reservations);

        int completedSessions = (int) allSessions.stream().filter(s -> s.getStatus() == SessionStatus.COMPLETED).count();
        int stoppedSessions = (int) allSessions.stream().filter(s -> s.getStatus() == SessionStatus.STOPPED).count();

        double totalEnergy = allSessions.stream()
                .mapToDouble(s -> mapper.toSessionDto(s).energyConsumedKwh())
                .sum();

        double avgSessionDuration = allSessions.stream()
                .filter(s -> s.getActualEndTime() != null)
                .mapToLong(s -> Math.max(0, Duration.between(s.getStartTime(), s.getActualEndTime()).toMinutes()))
                .average()
                .orElse(0.0);

        List<ChargerUsageDto> chargerUsage = chargers.stream()
                .map(c -> {
                    List<ChargingSession> cSessions = allSessions.stream()
                            .filter(s -> s.getCharger().getChargerId().equals(c.getChargerId()))
                            .toList();
                    double energy = cSessions.stream()
                            .mapToDouble(s -> mapper.toSessionDto(s).energyConsumedKwh())
                            .sum();
                    return new ChargerUsageDto(
                            c.getChargerId(),
                            c.getStation().getName(),
                            c.getName(),
                            c.getPowerRatingKw(),
                            cSessions.size(),
                            Math.round(energy * 100.0) / 100.0,
                            c.getAvailabilityStatus()
                    );
                })
                .toList();

        return new AnalyticsResponseDto(
                clock.instant(),
                overview,
                stationSummaries,
                computePriorityDistribution(liveQueue),
                allRequests.size(),
                (int) allRequests.stream().filter(r -> r.getStatus() == RequestStatus.QUEUED).count(),
                (int) allRequests.stream().filter(r -> r.getStatus() == RequestStatus.ASSIGNED).count(),
                (int) allRequests.stream().filter(r -> r.getStatus() == RequestStatus.COMPLETED).count(),
                (int) allRequests.stream().filter(r -> r.getStatus() == RequestStatus.CANCELLED).count(),
                allSessions.size(),
                completedSessions,
                stoppedSessions,
                Math.round(totalEnergy * 100.0) / 100.0,
                Math.round(avgSessionDuration * 10.0) / 10.0,
                overview.averageEstimatedWaitMinutes(),
                chargerUsage
        );
    }

    public List<StationSummaryDto> buildStationSummaries(List<VehicleRequest> liveQueue) {
        List<ChargingStation> stations = dataIntegrityService.getAllStations();
        List<StationSummaryDto> summaries = new ArrayList<>();
        for (ChargingStation station : stations) {
            List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(station.getStationId());
            double allocatedPower = dataIntegrityService.getAllocatedPowerKw(station.getStationId());
            int queuedCount = (int) liveQueue.stream()
                    .filter(r -> r.getStation().getStationId().equals(station.getStationId()))
                    .count();
            summaries.add(mapper.toStationSummary(station, chargers, allocatedPower, queuedCount));
        }
        return summaries;
    }

    private DashboardMetricsDto computeMetrics(
            List<StationSummaryDto> stationSummaries,
            List<Charger> chargers,
            List<VehicleRequest> liveQueue,
            List<ChargingSession> activeSessions,
            List<Reservation> reservations) {

        int totalStations = stationSummaries.size();
        int operationalStations = (int) stationSummaries.stream()
                .filter(s -> s.status() == StationStatus.OPERATIONAL)
                .count();

        int availableChargers = (int) chargers.stream()
                .filter(c -> c.getOperationalStatus() == ChargerOperationalStatus.OPERATIONAL
                        && c.getAvailabilityStatus() == ChargerAvailabilityStatus.AVAILABLE)
                .count();
        int occupiedChargers = (int) chargers.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.OCCUPIED)
                .count();
        int reservedChargers = (int) chargers.stream()
                .filter(c -> c.getAvailabilityStatus() == ChargerAvailabilityStatus.RESERVED)
                .count();
        int maintenanceChargers = (int) chargers.stream()
                .filter(c -> c.getOperationalStatus() != ChargerOperationalStatus.OPERATIONAL)
                .count();

        int activeRes = (int) reservations.stream()
                .filter(r -> r.getStatus() == ReservationStatus.CONFIRMED || r.getStatus() == ReservationStatus.ACTIVE)
                .count();

        double totalMaxPower = stationSummaries.stream().mapToDouble(StationSummaryDto::maxPowerCapacityKw).sum();
        double totalAllocated = stationSummaries.stream().mapToDouble(StationSummaryDto::currentAllocatedPowerKw).sum();
        double utilPct = totalMaxPower > 0 ? Math.min(100.0, Math.round((totalAllocated / totalMaxPower) * 1000.0) / 10.0) : 0.0;

        double avgWait = liveQueue.stream()
                .mapToDouble(VehicleRequest::getEstimatedWaitMinutes)
                .filter(w -> w >= 0.0)
                .average()
                .orElse(0.0);

        return new DashboardMetricsDto(
                totalStations,
                operationalStations,
                chargers.size(),
                availableChargers,
                occupiedChargers,
                reservedChargers,
                maintenanceChargers,
                liveQueue.size(),
                activeSessions.size(),
                reservations.size(),
                activeRes,
                Math.round(totalMaxPower * 10.0) / 10.0,
                Math.round(totalAllocated * 10.0) / 10.0,
                utilPct,
                Math.round(avgWait * 10.0) / 10.0
        );
    }

    private List<PriorityBucketDto> computePriorityDistribution(List<VehicleRequest> liveQueue) {
        int critical = 0;
        int high = 0;
        int medium = 0;
        int low = 0;
        for (VehicleRequest req : liveQueue) {
            int s = req.getPriorityScore();
            if (s >= 75) {
                critical++;
            } else if (s >= 50) {
                high++;
            } else if (s >= 25) {
                medium++;
            } else {
                low++;
            }
        }
        return List.of(
                new PriorityBucketDto("75-100 (Critical)", critical),
                new PriorityBucketDto("50-74 (High)", high),
                new PriorityBucketDto("25-49 (Medium)", medium),
                new PriorityBucketDto("0-24 (Standard)", low)
        );
    }
}
