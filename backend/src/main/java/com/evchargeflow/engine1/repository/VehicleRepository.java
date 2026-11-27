package com.evchargeflow.engine1.repository;

import com.evchargeflow.engine1.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    Optional<Vehicle> findByRegistrationNumberIgnoreCase(String registrationNumber);
}
