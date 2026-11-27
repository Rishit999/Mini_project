package com.evchargeflow.engine3.controller;

import com.evchargeflow.engine1.entity.ChargingSession;
import com.evchargeflow.engine1.repository.ChargingSessionRepository;
import com.evchargeflow.engine2.scheduling.StationOrchestrator;
import com.evchargeflow.engine3.api.ApiDtos.SessionDto;
import com.evchargeflow.engine3.integration.DtoMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final StationOrchestrator orchestrator;
    private final ChargingSessionRepository sessionRepo;
    private final DtoMapper mapper;

    public SessionController(
            StationOrchestrator orchestrator,
            ChargingSessionRepository sessionRepo,
            DtoMapper mapper) {
        this.orchestrator = orchestrator;
        this.sessionRepo = sessionRepo;
        this.mapper = mapper;
    }

    @GetMapping
    public List<SessionDto> listSessions(@RequestParam(required = false) String stationId) {
        List<ChargingSession> all = sessionRepo.findAllByOrderByStartTimeDesc();
        if (stationId != null && !stationId.isBlank()) {
            all = all.stream()
                    .filter(s -> s.getStation().getStationId().equals(stationId))
                    .toList();
        }
        return all.stream().map(mapper::toSessionDto).toList();
    }

    @GetMapping("/{sessionId}")
    public SessionDto getSession(@PathVariable String sessionId) {
        ChargingSession session = sessionRepo.findById(sessionId)
                .orElseThrow(() -> new com.evchargeflow.common.exception.ResourceNotFoundException(
                        "SES_NOT_FOUND", "Session not found: " + sessionId));
        return mapper.toSessionDto(session);
    }

    @PostMapping("/start/{requestId}")
    public SessionDto startSession(@PathVariable String requestId) {
        return mapper.toSessionDto(orchestrator.startChargingSession(requestId));
    }

    @PostMapping("/start-reservation/{reservationId}")
    public SessionDto startFromReservation(
            @PathVariable String reservationId,
            @RequestParam(defaultValue = "0") int expectedDurationMinutes) {
        return mapper.toSessionDto(
                orchestrator.startSessionFromReservation(reservationId, expectedDurationMinutes));
    }

    @PostMapping("/{sessionId}/complete")
    public SessionDto completeSession(@PathVariable String sessionId) {
        return mapper.toSessionDto(orchestrator.completeOrStopSession(sessionId, true));
    }

    @PostMapping("/{sessionId}/stop")
    public SessionDto stopSession(@PathVariable String sessionId) {
        return mapper.toSessionDto(orchestrator.completeOrStopSession(sessionId, false));
    }
}
