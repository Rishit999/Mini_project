package com.evchargeflow.engine3.controller;

import com.evchargeflow.common.dto.AllocationResult;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import com.evchargeflow.engine3.api.ApiDtos.QueueItemDto;
import com.evchargeflow.engine3.api.ApiDtos.SubmitChargingRequestDto;
import com.evchargeflow.engine3.integration.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class QueueController {

    private final StationOrchestrator orchestrator;
    private final DtoMapper mapper;
    private final Clock clock;

    public QueueController(StationOrchestrator orchestrator, DtoMapper mapper, Clock clock) {
        this.orchestrator = orchestrator;
        this.mapper = mapper;
        this.clock = clock;
    }

    @GetMapping("/queue")
    public List<QueueItemDto> getQueue(@RequestParam(required = false) String stationId) {
        List<VehicleRequest> queue = orchestrator.getLiveQueue(stationId);
        List<QueueItemDto> result = new ArrayList<>();
        for (int i = 0; i < queue.size(); i++) {
            result.add(mapper.toQueueItemDto(queue.get(i), i + 1));
        }
        return result;
    }

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public AllocationResult submitRequest(@Valid @RequestBody SubmitChargingRequestDto body) {
        return orchestrator.submitChargingRequest(
                body.vehicleId(),
                body.stationId(),
                body.currentBatteryPercent(),
                body.requiredConnector(),
                body.expectedDurationMinutes(),
                body.desiredDepartureTime(),
                body.targetBatteryPercent()
        );
    }

    @PostMapping("/requests/{requestId}/cancel")
    public QueueItemDto cancelRequest(@PathVariable String requestId) {
        VehicleRequest cancelled = orchestrator.cancelRequest(requestId);
        return mapper.toQueueItemDto(cancelled, null);
    }

    @DeleteMapping("/requests/{requestId}")
    public Map<String, String> deleteRequest(@PathVariable String requestId) {
        orchestrator.cancelRequest(requestId);
        return Map.of("message", "Request " + requestId + " cancelled successfully.");
    }

    @PostMapping("/allocate")
    public List<AllocationResult> runAllocation(@RequestParam(required = false) String stationId) {
        if (stationId != null && !stationId.isBlank()) {
            return orchestrator.runAllocationCycleForStation(stationId, clock.instant());
        }
        return orchestrator.runAllStationsAllocation();
    }
}
