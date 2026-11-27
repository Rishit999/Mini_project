package com.evchargeflow.engine3.controller;

import com.evchargeflow.engine1.entity.Vehicle;
import com.evchargeflow.engine1.service.DataIntegrityService;
import com.evchargeflow.engine3.api.ApiDtos.VehicleDto;
import com.evchargeflow.engine3.api.ApiDtos.VehicleUpsertRequest;
import com.evchargeflow.engine3.integration.DtoMapper;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final DataIntegrityService dataIntegrityService;
    private final DtoMapper mapper;

    public VehicleController(DataIntegrityService dataIntegrityService, DtoMapper mapper) {
        this.dataIntegrityService = dataIntegrityService;
        this.mapper = mapper;
    }

    @GetMapping
    public List<VehicleDto> listVehicles() {
        return dataIntegrityService.getAllVehicles().stream().map(mapper::toVehicleDto).toList();
    }

    @GetMapping("/{vehicleId}")
    public VehicleDto getVehicle(@PathVariable String vehicleId) {
        return mapper.toVehicleDto(dataIntegrityService.getVehicleOrThrow(vehicleId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleDto createVehicle(@Valid @RequestBody VehicleUpsertRequest body) {
        Vehicle vehicle = new Vehicle();
        vehicle.setVehicleId(body.vehicleId());
        vehicle.setRegistrationNumber(body.registrationNumber());
        vehicle.setOwnerName(body.ownerName());
        vehicle.setModel(body.model());
        vehicle.setConnectorType(body.connectorType());
        vehicle.setBatteryCapacityKwh(body.batteryCapacityKwh());
        vehicle.setCurrentBatteryPercent(body.currentBatteryPercent());
        return mapper.toVehicleDto(dataIntegrityService.createVehicle(vehicle, body.ownerUserId()));
    }

    @PutMapping("/{vehicleId}")
    public VehicleDto updateVehicle(
            @PathVariable String vehicleId,
            @Valid @RequestBody VehicleUpsertRequest body) {
        Vehicle incoming = new Vehicle();
        incoming.setRegistrationNumber(body.registrationNumber());
        incoming.setOwnerName(body.ownerName());
        incoming.setModel(body.model());
        incoming.setConnectorType(body.connectorType());
        incoming.setBatteryCapacityKwh(body.batteryCapacityKwh());
        incoming.setCurrentBatteryPercent(body.currentBatteryPercent());
        return mapper.toVehicleDto(dataIntegrityService.updateVehicle(vehicleId, incoming, body.ownerUserId()));
    }

    @DeleteMapping("/{vehicleId}")
    public Map<String, String> deleteVehicle(@PathVariable String vehicleId) {
        dataIntegrityService.deleteVehicle(vehicleId);
        return Map.of("message", "Vehicle " + vehicleId + " deleted successfully.");
    }
}
