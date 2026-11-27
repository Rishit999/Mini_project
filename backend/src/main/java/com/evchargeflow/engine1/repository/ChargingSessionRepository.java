package com.evchargeflow.engine1.repository;

import com.evchargeflow.common.enums.SessionStatus;
import com.evchargeflow.engine1.entity.ChargingSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChargingSessionRepository extends JpaRepository<ChargingSession, String> {
    List<ChargingSession> findByStatus(SessionStatus status);

    List<ChargingSession> findByStationStationIdAndStatus(String stationId, SessionStatus status);

    Optional<ChargingSession> findByChargerChargerIdAndStatus(String chargerId, SessionStatus status);

    List<ChargingSession> findAllByOrderByStartTimeDesc();

    boolean existsByVehicleVehicleIdAndStatus(String vehicleId, SessionStatus status);
}
