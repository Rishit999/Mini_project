package com.evchargeflow.engine4;

import com.evchargeflow.EVChargeFlowApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
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
 * End-to-End Flow Tests: Vehicle creation -> Station & Charger creation ->
 * Charging Request submission -> Priority Breakdown -> Session start & completion.
 *
 * Test Cases: E2E-001 through E2E-008
 */
@SpringBootTest(classes = EVChargeFlowApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("TC-E4-01: End-to-End Flow Tests")
class EndToEndFlowTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    static String createdVehicleId;
    static String createdStationId;
    static String createdChargerId;
    static String createdRequestId;
    static String createdSessionId;

    @Test
    @Order(1)
    @DisplayName("E2E-001: Create a new vehicle → 201, ID returned")
    void step1_createVehicle() throws Exception {
        String body = """
                {
                  "registrationNumber": "E2E-TEST-001",
                  "ownerName": "Integration Tester",
                  "model": "Tesla Model 3",
                  "connectorType": "TYPE2",
                  "batteryCapacityKwh": 75.0,
                  "currentBatteryPercent": 15.0
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> resp = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        createdVehicleId = (String) resp.get("vehicleId");
        assertThat(createdVehicleId).isNotNull().isNotBlank();
    }

    @Test
    @Order(2)
    @DisplayName("E2E-002: Create station and compatible charger → 201")
    void step2_createStationAndCharger() throws Exception {
        String stationBody = """
                {
                  "stationId": "STN-E2E-1",
                  "name": "E2E Test Station",
                  "location": "Test Blvd",
                  "status": "OPERATIONAL",
                  "maxPowerCapacityKw": 150.0
                }
                """;
        MvcResult stResult = mockMvc.perform(post("/api/stations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(stationBody))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> stResp = objectMapper.readValue(stResult.getResponse().getContentAsString(), Map.class);
        createdStationId = (String) stResp.get("stationId");
        assertThat(createdStationId).isEqualTo("STN-E2E-1");

        String chargerBody = """
                {
                  "chargerId": "CHG-E2E-1",
                  "stationId": "STN-E2E-1",
                  "name": "E2E Bay 1",
                  "chargerType": "STANDARD_AC",
                  "powerRatingKw": 22.0,
                  "supportedConnectors": ["TYPE2"]
                }
                """;
        MvcResult chResult = mockMvc.perform(post("/api/chargers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(chargerBody))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> chResp = objectMapper.readValue(chResult.getResponse().getContentAsString(), Map.class);
        createdChargerId = (String) chResp.get("chargerId");
        assertThat(createdChargerId).isEqualTo("CHG-E2E-1");
    }

    @Test
    @Order(3)
    @DisplayName("E2E-003: Submit charging request → ASSIGNED with priority breakdown")
    void step3_submitChargingRequest() throws Exception {
        String body = String.format("""
                {
                  "vehicleId": "%s",
                  "stationId": "%s",
                  "currentBatteryPercent": 15.0,
                  "requiredConnector": "TYPE2",
                  "expectedDurationMinutes": 45
                }
                """, createdVehicleId, createdStationId);

        MvcResult result = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> resp = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        assertThat(resp.get("outcome")).isEqualTo("ASSIGNED");
        assertThat(resp.get("assignedChargerId")).isEqualTo(createdChargerId);
        createdRequestId = (String) resp.get("requestId");

        Map<?, ?> breakdown = (Map<?, ?>) resp.get("priorityBreakdown");
        assertThat(breakdown).isNotNull();
        // Battery 15% -> 40 pts
        assertThat(((Number) breakdown.get("batteryScore")).intValue()).isEqualTo(40);
        assertThat(((Number) breakdown.get("totalScore")).intValue()).isEqualTo(40);
    }

    @Test
    @Order(4)
    @DisplayName("E2E-004: Start charging session from assigned request → ACTIVE")
    void step4_startSession() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sessions/start/" + createdRequestId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.simulated").value(true))
                .andReturn();

        Map<?, ?> resp = objectMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        createdSessionId = (String) resp.get("sessionId");
        assertThat(createdSessionId).isNotNull().isNotBlank();
    }

    @Test
    @Order(5)
    @DisplayName("E2E-005: Duplicate active request for same vehicle → rejected/conflict")
    void step5_duplicateRequest_rejected() throws Exception {
        String body = String.format("""
                {
                  "vehicleId": "%s",
                  "stationId": "%s",
                  "currentBatteryPercent": 15.0,
                  "requiredConnector": "TYPE2",
                  "expectedDurationMinutes": 30
                }
                """, createdVehicleId, createdStationId);

        mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(6)
    @DisplayName("E2E-006: Second vehicle queued when compatible bay is occupied → WAITING")
    void step6_secondVehicle_queuedWhenOccupied() throws Exception {
        String v2Body = """
                {
                  "registrationNumber": "E2E-TEST-002",
                  "ownerName": "Wait Tester",
                  "model": "MG ZS EV",
                  "connectorType": "TYPE2",
                  "batteryCapacityKwh": 50.0,
                  "currentBatteryPercent": 8.0
                }
                """;
        MvcResult v2Res = mockMvc.perform(post("/api/vehicles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(v2Body))
                .andExpect(status().isCreated())
                .andReturn();
        String v2Id = (String) objectMapper.readValue(v2Res.getResponse().getContentAsString(), Map.class).get("vehicleId");

        String reqBody = String.format("""
                {
                  "vehicleId": "%s",
                  "stationId": "%s",
                  "currentBatteryPercent": 8.0,
                  "requiredConnector": "TYPE2",
                  "expectedDurationMinutes": 30
                }
                """, v2Id, createdStationId);

        MvcResult r2 = mockMvc.perform(post("/api/requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reqBody))
                .andExpect(status().isCreated())
                .andReturn();

        Map<?, ?> resp = objectMapper.readValue(r2.getResponse().getContentAsString(), Map.class);
        assertThat(resp.get("outcome")).isEqualTo("WAITING");
        assertThat(resp.get("queuePosition")).isEqualTo(1);
    }

    @Test
    @Order(7)
    @DisplayName("E2E-007: Complete active session → COMPLETED and releases charger for waiting vehicle")
    void step7_completeSession_freesCharger() throws Exception {
        mockMvc.perform(post("/api/sessions/" + createdSessionId + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        // Waiting vehicle in queue should have been automatically allocated
        mockMvc.perform(get("/api/queue").param("stationId", createdStationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @Order(8)
    @DisplayName("E2E-008: GET /api/analytics → 200 OK with computed metrics")
    void step8_getAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedSessions").isNumber())
                .andExpect(jsonPath("$.chargerUsage").isArray());
    }
}
