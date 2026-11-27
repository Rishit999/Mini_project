package com.evchargeflow.engine1.repository;

import com.evchargeflow.common.enums.StationStatus;
import com.evchargeflow.engine1.entity.ChargingStation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargingStationRepository extends JpaRepository<ChargingStation, String> {
    List<ChargingStation> findByStatus(StationStatus status);
}
