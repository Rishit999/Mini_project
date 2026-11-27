package com.evchargeflow.engine1.repository;

import com.evchargeflow.engine1.entity.StationConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StationConfigurationRepository extends JpaRepository<StationConfiguration, String> {
}
