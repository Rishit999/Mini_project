package com.evchargeflow.engine2;

import com.evchargeflow.common.enums.ConnectorType;
import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.engine1.entity.ChargingStation;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine2.priority.PriorityCalculator;
import com.evchargeflow.engine2.queue.QueueManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;

/**
 * ENGINE 4 — Testing & Validation Engine
 * Unit tests for QueueManager (Engine 2 — Logic & Rule Engine)
 *
 * Test Cases: QM-001 through QM-006
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TC-E2-05: QueueManager Unit Tests")
class QueueManagerTest {

    @Mock
    private VehicleRequestRepository requestRepository;

    private PriorityCalculator priorityCalculator;
    private Clock fixedClock;
    private QueueManager queueManager;
    private static final Instant NOW = Instant.parse("2024-01-15T10:00:00Z");

    private final List<VehicleRequest> storedQueue = new ArrayList<>();

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(NOW, ZoneOffset.UTC);
        priorityCalculator = new PriorityCalculator();
        queueManager = new QueueManager(requestRepository, priorityCalculator, fixedClock);

        lenient().when(requestRepository.save(any(VehicleRequest.class))).thenAnswer(inv -> {
            VehicleRequest req = inv.getArgument(0);
            storedQueue.removeIf(existing -> existing.getRequestId().equals(req.getRequestId()));
            if (req.getStatus() == RequestStatus.QUEUED) {
                storedQueue.add(req);
            }
            return req;
        });
        lenient().when(requestRepository.findByStatusIn(Set.of(RequestStatus.QUEUED)))
                .thenAnswer(inv -> new ArrayList<>(storedQueue));
    }

    private VehicleRequest makeRequest(String id, String stationId, double batteryPct, Instant arrival, Instant departure) {
        ChargingStation station = new ChargingStation();
        station.setStationId(stationId);

        VehicleRequest req = new VehicleRequest();
        req.setRequestId(id);
        req.setStation(station);
        req.setCurrentBatteryPercent(batteryPct);
        req.setRequiredConnector(ConnectorType.CCS);
        req.setExpectedDurationMinutes(30);
        req.setArrivalTime(arrival);
        req.setDesiredDepartureTime(departure);
        req.setStatus(RequestStatus.QUEUED);
        return req;
    }

    @Test
    @DisplayName("QM-001: Higher priority vehicle ranks first in ordered queue")
    void testEnqueue_priorityOrdering() {
        VehicleRequest low = makeRequest("REQ-LOW", "STN-01", 65.0, NOW.minus(5, ChronoUnit.MINUTES), null);
        VehicleRequest high = makeRequest("REQ-HIGH", "STN-01", 8.0, NOW.minus(5, ChronoUnit.MINUTES), NOW.plus(20, ChronoUnit.MINUTES));

        queueManager.enqueue(low, NOW);
        queueManager.enqueue(high, NOW);

        List<VehicleRequest> ordered = queueManager.getOrderedQueue(NOW);
        assertThat(ordered).extracting(VehicleRequest::getRequestId)
                .containsExactly("REQ-HIGH", "REQ-LOW");
    }

    @Test
    @DisplayName("QM-002: Tie-breaking uses earlier arrival then smaller requestId")
    void testTieBreaking_arrivalAndRequestId() {
        Instant sameTime = NOW.minus(10, ChronoUnit.MINUTES);
        VehicleRequest rB = makeRequest("REQ-B", "STN-01", 25.0, sameTime, null);
        VehicleRequest rA = makeRequest("REQ-A", "STN-01", 25.0, sameTime, null);
        VehicleRequest rEarly = makeRequest("REQ-C", "STN-01", 25.0, NOW.minus(12, ChronoUnit.MINUTES), null);

        queueManager.enqueue(rB, NOW);
        queueManager.enqueue(rA, NOW);
        queueManager.enqueue(rEarly, NOW);

        List<VehicleRequest> ordered = queueManager.getOrderedQueue(NOW);
        assertThat(ordered).extracting(VehicleRequest::getRequestId)
                .containsExactly("REQ-C", "REQ-A", "REQ-B");
    }

    @Test
    @DisplayName("QM-003: Priority updates dynamically as waiting time passes")
    void testRefreshPriorities_waitingTimeIncreasesScore() {
        VehicleRequest req = makeRequest("REQ-W", "STN-01", 50.0, NOW, null);
        queueManager.enqueue(req, NOW);
        assertThat(req.getPriorityScore()).isEqualTo(10); // battery 50% = 10, wait 0m = 0

        Instant later = NOW.plus(65, ChronoUnit.MINUTES);
        queueManager.refreshPriorities(later);

        assertThat(queueManager.getOrderedQueue(later).get(0).getPriorityScore()).isEqualTo(30); // 10 + 20 wait
    }

    @Test
    @DisplayName("QM-004: Station-filtered queue returns only matching station items")
    void testGetOrderedQueueForStation() {
        queueManager.enqueue(makeRequest("REQ-1", "STN-01", 15.0, NOW, null), NOW);
        queueManager.enqueue(makeRequest("REQ-2", "STN-02", 10.0, NOW, null), NOW);

        List<VehicleRequest> stn1 = queueManager.getOrderedQueueForStation("STN-01", NOW);
        assertThat(stn1).hasSize(1);
        assertThat(stn1.get(0).getRequestId()).isEqualTo("REQ-1");
    }

    @Test
    @DisplayName("QM-005: getQueuePosition returns 1-based position or null when absent")
    void testGetQueuePosition() {
        queueManager.enqueue(makeRequest("REQ-1", "STN-01", 50.0, NOW, null), NOW);
        queueManager.enqueue(makeRequest("REQ-2", "STN-01", 10.0, NOW, null), NOW);

        assertThat(queueManager.getQueuePosition("REQ-2", NOW)).isEqualTo(1);
        assertThat(queueManager.getQueuePosition("REQ-1", NOW)).isEqualTo(2);
        assertThat(queueManager.getQueuePosition("REQ-UNKNOWN", NOW)).isNull();
    }

    @Test
    @DisplayName("QM-006: removeById removes item from in-memory heap")
    void testRemoveById() {
        queueManager.enqueue(makeRequest("REQ-1", "STN-01", 20.0, NOW, null), NOW);
        assertThat(queueManager.size()).isEqualTo(1);

        boolean removed = queueManager.removeById("REQ-1");
        assertThat(removed).isTrue();
        assertThat(queueManager.size()).isEqualTo(0);
    }
}
