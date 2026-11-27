package com.evchargeflow.engine1.repository;

import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.engine1.entity.VehicleRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface VehicleRequestRepository extends JpaRepository<VehicleRequest, String> {
    List<VehicleRequest> findByStatusIn(Collection<RequestStatus> statuses);

    List<VehicleRequest> findByStationStationIdAndStatusIn(String stationId, Collection<RequestStatus> statuses);

    boolean existsByVehicleVehicleIdAndStatusIn(String vehicleId, Collection<RequestStatus> statuses);

    boolean existsByVehicleVehicleId(String vehicleId);
}
