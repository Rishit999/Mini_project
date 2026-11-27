package com.evchargeflow.engine3.controller;

import com.evchargeflow.engine1.entity.Charger;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.StationConfiguration;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.ChargerRepository;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import com.evchargeflow.engine3.api.ApiDtos.ChargerDto;
import com.evchargeflow.engine3.api.ApiDtos.CreateChargerRequest;
import com.evchargeflow.engine3.api.ApiDtos.QueueItemDto;
import com.evchargeflow.engine3.api.ApiDtos.StationConfigDto;
import com.evchargeflow.engine3.api.ApiDtos.StationDetailDto;
import com.evchargeflow.engine3.api.ApiDtos.StationSummaryDto;
import com.evchargeflow.engine3.api.ApiDtos.StationUpsertRequest;
import com.evchargeflow.engine3.api.ApiDtos.UpdateChargerStatusRequest;
import com.evchargeflow.engine3.api.ApiDtos.UpdateSettingsRequest;
import com.evchargeflow.engine3.api.ApiDtos.UpdateStationStatusRequest;
import com.evchargeflow.engine3.integration.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class StationController {

    private final DataIntegrityService dataIntegrityService;
    private final StationOrchestrator orchestrator;
    private final ChargerRepository chargerRepo;
    private final DtoMapper mapper;

    public StationController(
            DataIntegrityService dataIntegrityService,
            StationOrchestrator orchestrator,
            ChargerRepository chargerRepo,
            DtoMapper mapper) {
        this.dataIntegrityService = dataIntegrityService;
        this.orchestrator = orchestrator;
        this.chargerRepo = chargerRepo;
        this.mapper = mapper;
    }

    @GetMapping("/stations")
    public List<StationSummaryDto> listStations() {
        List<ChargingStation> stations = dataIntegrityService.getAllStations();
        List<VehicleRequest> liveQueue = orchestrator.getLiveQueue(null);
        List<StationSummaryDto> result = new ArrayList<>();
        for (ChargingStation st : stations) {
            List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(st.getStationId());
            double allocated = dataIntegrityService.getAllocatedPowerKw(st.getStationId());
            int qCount = (int) liveQueue.stream()
                    .filter(r -> r.getStation().getStationId().equals(st.getStationId()))
                    .count();
            result.add(mapper.toStationSummary(st, chargers, allocated, qCount));
        }
        return result;
    }

    @GetMapping("/stations/{stationId}")
    public StationDetailDto getStationDetail(@PathVariable String stationId) {
        ChargingStation station = dataIntegrityService.getStationOrThrow(stationId);
        List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        List<VehicleRequest> queue = orchestrator.getLiveQueue(stationId);
        double allocated = dataIntegrityService.getAllocatedPowerKw(stationId);
        StationConfiguration cfg = dataIntegrityService.getOrCreateConfig(stationId);

        List<ChargerDto> chargerDtos = chargers.stream().map(mapper::toChargerDto).toList();
        List<QueueItemDto> queueDtos = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            queueDtos.add(mapper.toQueueItemDto(queue.get(i), i + 1));
        }

        return new StationDetailDto(
                mapper.toStationSummary(station, chargers, allocated, queue.size()),
                chargerDtos,
                queueDtos,
                mapper.toStationConfigDto(cfg)
        );
    }

    @PostMapping("/stations")
    @ResponseStatus(HttpStatus.CREATED)
    public StationSummaryDto createStation(@Valid @RequestBody StationUpsertRequest body) {
        ChargingStation station = new ChargingStation(
                body.stationId(),
                body.name(),
                body.location(),
                body.status(),
                body.maxPowerCapacityKw()
        );
        ChargingStation saved = dataIntegrityService.createStation(station);
        return mapper.toStationSummary(saved, List.of(), 0.0, 0);
    }

    @PutMapping("/stations/{stationId}")
    public StationSummaryDto updateStation(
            @PathVariable String stationId,
            @Valid @RequestBody StationUpsertRequest body) {
        ChargingStation saved = dataIntegrityService.updateStation(
                stationId, body.name(), body.location(), body.status(), body.maxPowerCapacityKw());
        List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        double allocated = dataIntegrityService.getAllocatedPowerKw(stationId);
        int queued = orchestrator.getLiveQueue(stationId).size();
        return mapper.toStationSummary(saved, chargers, allocated, queued);
    }

    @PatchMapping("/stations/{stationId}/status")
    public StationSummaryDto updateStationStatus(
            @PathVariable String stationId,
            @Valid @RequestBody UpdateStationStatusRequest body) {
        ChargingStation saved = dataIntegrityService.updateStationStatus(stationId, body.status());
        List<Charger> chargers = chargerRepo.findByStationStationIdOrderByChargerIdAsc(stationId);
        double allocated = dataIntegrityService.getAllocatedPowerKw(stationId);
        int queued = orchestrator.getLiveQueue(stationId).size();
        return mapper.toStationSummary(saved, chargers, allocated, queued);
    }

    @GetMapping("/stations/{stationId}/chargers")
    public List<ChargerDto> getStationChargers(@PathVariable String stationId) {
        return dataIntegrityService.getChargersByStation(stationId).stream()
                .map(mapper::toChargerDto)
                .toList();
    }

    @GetMapping("/chargers")
    public List<ChargerDto> listChargers(@RequestParam(required = false) String stationId) {
        List<Charger> chargers = (stationId != null && !stationId.isBlank())
                ? dataIntegrityService.getChargersByStation(stationId)
                : dataIntegrityService.getAllChargers();
        return chargers.stream().map(mapper::toChargerDto).toList();
    }

    @PostMapping("/chargers")
    @ResponseStatus(HttpStatus.CREATED)
    public ChargerDto createCharger(@Valid @RequestBody CreateChargerRequest body) {
        Charger charger = new Charger();
        charger.setChargerId(body.chargerId());
        charger.setName(body.name());
        charger.setChargerType(body.chargerType());
        charger.setPowerRatingKw(body.powerRatingKw());
        charger.setSupportedConnectors(body.supportedConnectors());
        return mapper.toChargerDto(dataIntegrityService.createCharger(charger, body.stationId()));
    }

    @PatchMapping("/chargers/{chargerId}/status")
    public ChargerDto updateChargerStatus(
            @PathVariable String chargerId,
            @Valid @RequestBody UpdateChargerStatusRequest body) {
        return mapper.toChargerDto(
                dataIntegrityService.updateChargerOperationalStatus(chargerId, body.operationalStatus()));
    }

    @GetMapping("/settings")
    public List<StationConfigDto> listAllSettings() {
        return dataIntegrityService.getAllStations().stream()
                .map(st -> dataIntegrityService.getOrCreateConfig(st.getStationId()))
                .map(mapper::toStationConfigDto)
                .toList();
    }

    @GetMapping("/settings/{stationId}")
    public StationConfigDto getStationSettings(@PathVariable String stationId) {
        return mapper.toStationConfigDto(dataIntegrityService.getOrCreateConfig(stationId));
    }

    @PutMapping("/settings/{stationId}")
    public StationConfigDto updateStationSettings(
            @PathVariable String stationId,
            @Valid @RequestBody UpdateSettingsRequest body) {
        StationConfiguration incoming = new StationConfiguration();
        incoming.setQueueRefreshIntervalSec(body.queueRefreshIntervalSec());
        incoming.setReservationGracePeriodMinutes(body.reservationGracePeriodMinutes());
        incoming.setChargingEfficiencyFactor(body.chargingEfficiencyFactor());
        incoming.setDefaultChargingDurationMinutes(body.defaultChargingDurationMinutes());
        incoming.setAutoAllocationEnabled(body.autoAllocationEnabled());
        incoming.setSimulationSpeedMultiplier(body.simulationSpeedMultiplier());
        return mapper.toStationConfigDto(
                dataIntegrityService.updateStationConfig(stationId, incoming, body.maxPowerCapacityKw()));
    }
}
