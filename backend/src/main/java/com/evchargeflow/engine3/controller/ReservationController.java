package com.evchargeflow.engine3.controller;

import com.evchargeflow.engine1.entity.Reservation;
import com.evchargeflow.engine1.repository.ReservationRepository;
import com.evchargeflow.engine2.reservation.ReservationManager;
import com.evchargeflow.engine3.api.ApiDtos.CreateReservationRequest;
import com.evchargeflow.engine3.api.ApiDtos.ReservationDto;
import com.evchargeflow.engine3.integration.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationManager reservationManager;
    private final ReservationRepository reservationRepo;
    private final DtoMapper mapper;
    private final Clock clock;

    public ReservationController(
            ReservationManager reservationManager,
            ReservationRepository reservationRepo,
            DtoMapper mapper,
            Clock clock) {
        this.reservationManager = reservationManager;
        this.reservationRepo = reservationRepo;
        this.mapper = mapper;
        this.clock = clock;
    }

    @GetMapping
    public List<ReservationDto> listReservations(@RequestParam(required = false) String stationId) {
        List<Reservation> all = reservationRepo.findAllByOrderByStartTimeAsc();
        if (stationId != null && !stationId.isBlank()) {
            all = all.stream()
                    .filter(r -> r.getStation().getStationId().equals(stationId))
                    .toList();
        }
        return all.stream().map(mapper::toReservationDto).toList();
    }

    @GetMapping("/{reservationId}")
    public ReservationDto getReservation(@PathVariable String reservationId) {
        Reservation res = reservationRepo.findById(reservationId)
                .orElseThrow(() -> new com.evchargeflow.common.exception.ResourceNotFoundException(
                        "RES_NOT_FOUND", "Reservation not found: " + reservationId));
        return mapper.toReservationDto(res);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDto createReservation(@Valid @RequestBody CreateReservationRequest body) {
        Reservation saved = reservationManager.createReservation(
                body.vehicleId(),
                body.stationId(),
                body.chargerId(),
                body.startTime(),
                body.endTime(),
                body.notes()
        );
        return mapper.toReservationDto(saved);
    }

    @PostMapping("/{reservationId}/cancel")
    public ReservationDto cancelReservation(@PathVariable String reservationId) {
        return mapper.toReservationDto(reservationManager.cancelReservation(reservationId));
    }

    @PostMapping("/process")
    public Map<String, Integer> processReservations() {
        List<Reservation> updated = reservationManager.processReservations(clock.instant());
        return Map.of("updatedCount", updated.size());
    }
}
