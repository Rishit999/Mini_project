package com.evchargeflow.engine3.controller;

import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/wait-time")
public class WaitTimeController {

    private final StationOrchestrator orchestrator;

    public WaitTimeController(StationOrchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @GetMapping("/estimate")
    public Map<String, Object> estimateWaitTime(
            @RequestParam String stationId,
            @RequestParam ConnectorType connectorType,
            @RequestParam double currentBatteryPercent,
            @RequestParam(defaultValue = "45") int expectedDurationMinutes,
            @RequestParam(required = false) String desiredDepartureTime) {

        Instant departure = null;
        if (desiredDepartureTime != null && !desiredDepartureTime.isBlank()) {
            departure = Instant.parse(desiredDepartureTime);
        }

        double waitMinutes = orchestrator.estimateProspectiveWait(
                stationId, connectorType, currentBatteryPercent,
                expectedDurationMinutes, departure);

        return Map.of(
                "stationId", stationId,
                "connectorType", connectorType,
                "currentBatteryPercent", currentBatteryPercent,
                "expectedDurationMinutes", expectedDurationMinutes,
                "estimatedWaitMinutes", waitMinutes,
                "isGuaranteed", false,
                "note", "Estimated wait is a simulation projection, not a guarantee."
        );
    }
}
