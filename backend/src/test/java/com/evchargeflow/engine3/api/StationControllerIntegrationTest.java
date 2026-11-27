package com.evchargeflow.engine3.api;

import com.evchargeflow.EVChargeFlowApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Integration tests for Station, Charger, Vehicle, Dashboard REST APIs (Engine 3)
 *
 * Test Cases: SI-001 through SI-010
 */
@SpringBootTest(classes = EVChargeFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TC-E3-01: StationController Integration Tests")
class StationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("SI-001: GET /api/health → 200 OK with status UP")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    @DisplayName("SI-002: GET /api/stations → 200 OK, returns list")
    void testGetStations_returnsList() throws Exception {
        mockMvc.perform(get("/api/stations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("SI-003: GET /api/chargers → 200 OK, returns list")
    void testGetChargers_returnsList() throws Exception {
        mockMvc.perform(get("/api/chargers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("SI-004: POST /api/stations with valid data → 201 Created")
    void testCreateStation_valid() throws Exception {
        String body = """
                {
                  "stationId": "STN-INT-1",
                  "name": "Test Station Alpha",
                  "location": "123 Main St",
                  "status": "OPERATIONAL",
                  "maxPowerCapacityKw": 150.0
                }
                """;
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Test Station Alpha"));
    }

    @Test
    @DisplayName("SI-005: POST /api/stations with blank name → 400 Bad Request")
    void testCreateStation_blankName_returns400() throws Exception {
        String body = """
                {
                  "name": "",
                  "location": "123 Main St",
                  "status": "OPERATIONAL",
                  "maxPowerCapacityKw": 100.0
                }
                """;
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("SI-006: GET /api/stations/{id} for non-existent → 404")
    void testGetStation_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/stations/NONEXISTENT-ID"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SI-007: GET /api/vehicles → 200 OK, returns list")
    void testGetVehicles_returnsList() throws Exception {
        mockMvc.perform(get("/api/vehicles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("SI-008: GET /api/dashboard → 200 OK with metrics")
    void testGetDashboard_returnsMetrics() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metrics.totalStations").exists())
                .andExpect(jsonPath("$.metrics.totalChargers").exists())
                .andExpect(jsonPath("$.metrics.powerUtilizationPercent").exists());
    }

    @Test
    @DisplayName("SI-009: GET /api/queue → 200 OK, returns list")
    void testGetQueue_returnsList() throws Exception {
        mockMvc.perform(get("/api/queue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("SI-010: GET /api/reservations → 200 OK, returns list")
    void testGetReservations_returnsList() throws Exception {
        mockMvc.perform(get("/api/reservations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
