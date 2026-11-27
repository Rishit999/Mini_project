package com.evchargeflow.engine3.api;

import com.evchargeflow.EVChargeFlowApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Integration tests for Queue & Charging Request REST API (Engine 3)
 *
 * Test Cases: QI-001 through QI-004
 */
@SpringBootTest(classes = EVChargeFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("TC-E3-02: QueueController Integration Tests")
class QueueControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("QI-001: Invalid battery (>100%) on request submission → 400 Bad Request")
    void testSubmitRequest_invalidBattery_returns400() throws Exception {
        String body = """
                {
                  "vehicleId": "VEH-X",
                  "stationId": "STN-X",
                  "currentBatteryPercent": 120.0,
                  "requiredConnector": "CCS",
                  "expectedDurationMinutes": 30
                }
                """;
        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("QI-002: Incompatible station (no matching connector) → REJECTED outcome")
    void testSubmitRequest_noCompatibleCharger_returnsRejected() throws Exception {
        // Create station with TYPE2-only charger and a vehicle with CHADEMO
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "STN-QI-1",
                                  "name": "QI Station",
                                  "location": "Test Rd",
                                  "status": "OPERATIONAL",
                                  "maxPowerCapacityKw": 100.0
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/chargers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chargerId": "CHG-QI-1",
                                  "stationId": "STN-QI-1",
                                  "name": "QI Bay",
                                  "chargerType": "STANDARD_AC",
                                  "powerRatingKw": 22.0,
                                  "supportedConnectors": ["TYPE2"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "VEH-QI-1",
                                  "registrationNumber": "QI-CHAD-01",
                                  "ownerName": "QI User",
                                  "model": "Leaf",
                                  "connectorType": "CHADEMO",
                                  "batteryCapacityKwh": 40.0,
                                  "currentBatteryPercent": 20.0
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "VEH-QI-1",
                                  "stationId": "STN-QI-1",
                                  "currentBatteryPercent": 20.0,
                                  "requiredConnector": "CHADEMO",
                                  "expectedDurationMinutes": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> resp = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(resp.get("outcome")).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("QI-003: Submit request and cancel eligible request removes from queue")
    void testSubmitAndCancelRequest() throws Exception {
        // Station power cap smaller than sum of 2 chargers or 1 charger occupied -> wait -> cancel
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "STN-QI-2",
                                  "name": "QI Cancel Station",
                                  "location": "Test Rd 2",
                                  "status": "OPERATIONAL",
                                  "maxPowerCapacityKw": 60.0
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/chargers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "chargerId": "CHG-QI-2",
                                  "stationId": "STN-QI-2",
                                  "name": "QI DC Bay",
                                  "chargerType": "FAST_DC",
                                  "powerRatingKw": 60.0,
                                  "supportedConnectors": ["CCS"]
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "VEH-QI-2",
                                  "registrationNumber": "QI-CCS-02",
                                  "ownerName": "Driver 2",
                                  "model": "Ioniq 5",
                                  "connectorType": "CCS",
                                  "batteryCapacityKwh": 72.0,
                                  "currentBatteryPercent": 25.0
                                }
                                """))
                .andExpect(status().isCreated());

        MvcResult assignedRes = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "vehicleId": "VEH-QI-2",
                                  "stationId": "STN-QI-2",
                                  "currentBatteryPercent": 25.0,
                                  "requiredConnector": "CCS",
                                  "expectedDurationMinutes": 30
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();

        String requestId = (String) objectMapper.readValue(assignedRes.getResponse().getContentAsString(), Map.class)
                .get("requestId");

        mockMvc.perform(post("/api/requests/" + requestId + "/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("QI-004: GET /api/wait-time/estimate returns non-guaranteed simulation note")
    void testWaitTimeEstimateEndpoint() throws Exception {
        mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "stationId": "STN-QI-WAIT",
                                  "name": "QI Wait Station",
                                  "location": "Wait Rd",
                                  "status": "OPERATIONAL",
                                  "maxPowerCapacityKw": 100.0
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/wait-time/estimate")
                        .param("stationId", "STN-QI-WAIT")
                        .param("connectorType", "TYPE2")
                        .param("currentBatteryPercent", "30")
                        .param("expectedDurationMinutes", "45"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isGuaranteed").value(false))
                .andExpect(jsonPath("$.estimatedWaitMinutes").exists());
    }
}
