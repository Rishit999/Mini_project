package com.evchargeflow.engine1.repository;

import com.evchargeflow.common.enums.ReservationStatus;
import com.evchargeflow.engine1.entity.Reservation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, String> {
    List<Reservation> findByChargerChargerIdAndStatusIn(String chargerId, Collection<ReservationStatus> statuses);

    List<Reservation> findByStationStationIdAndStatusIn(String stationId, Collection<ReservationStatus> statuses);

    List<Reservation> findAllByOrderByStartTimeAsc();

    boolean existsByVehicleVehicleIdAndStatusIn(String vehicleId, Collection<ReservationStatus> statuses);
}
