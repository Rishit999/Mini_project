package com.evchargeflow.engine1.repository;

import com.evchargeflow.engine1.entity.Charger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChargerRepository extends JpaRepository<Charger, String> {
    List<Charger> findByStationStationIdOrderByChargerIdAsc(String stationId);
}
