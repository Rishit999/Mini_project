package com.evchargeflow.engine2.queue;

import com.evchargeflow.common.enums.RequestStatus;
import com.evchargeflow.engine1.entity.VehicleRequest;
import com.evchargeflow.engine1.repository.VehicleRequestRepository;
import com.evchargeflow.engine2.datastructures.CustomPriorityMaxHeap;
import com.evchargeflow.engine2.priority.PriorityCalculator;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Engine 2: Manages the priority waiting queue using CustomPriorityMaxHeap
 * with deterministic tie-breaking (score desc -> arrivalTime asc -> requestId asc).
 */
@Service
public class QueueManager {

    private static final Comparator<VehicleRequest> REQUEST_PRIORITY_COMPARATOR = (a, b) -> {
        int scoreCmp = Integer.compare(a.getPriorityScore(), b.getPriorityScore());
        if (scoreCmp != 0) {
            return scoreCmp;
        }
        int arrivalCmp = b.getArrivalTime().compareTo(a.getArrivalTime());
        if (arrivalCmp != 0) {
            return arrivalCmp;
        }
        return b.getRequestId().compareTo(a.getRequestId());
    };

    private final VehicleRequestRepository requestRepository;
    private final PriorityCalculator priorityCalculator;
    private final Clock clock;
    private final CustomPriorityMaxHeap<VehicleRequest> heap = new CustomPriorityMaxHeap<>(REQUEST_PRIORITY_COMPARATOR);

    public QueueManager(
            VehicleRequestRepository requestRepository,
            PriorityCalculator priorityCalculator,
            Clock clock) {
        this.requestRepository = requestRepository;
        this.priorityCalculator = priorityCalculator;
        this.clock = clock;
    }

    @PostConstruct
    public synchronized void initializeFromDatabase() {
        syncFromDatabase(clock.instant());
    }

    @Transactional
    public synchronized void syncFromDatabase(Instant now) {
        heap.clear();
        List<VehicleRequest> queued = requestRepository.findByStatusIn(Set.of(RequestStatus.QUEUED));
        for (VehicleRequest req : queued) {
            priorityCalculator.applyToRequest(req, now);
            requestRepository.save(req);
            heap.insert(req);
        }
    }

    @Transactional
    public synchronized void enqueue(VehicleRequest request, Instant now) {
        priorityCalculator.applyToRequest(request, now);
        request.setStatus(RequestStatus.QUEUED);
        VehicleRequest saved = requestRepository.save(request);
        removeById(saved.getRequestId());
        heap.insert(saved);
    }

    @Transactional
    public synchronized void refreshPriorities(Instant now) {
        List<VehicleRequest> dbQueued = requestRepository.findByStatusIn(Set.of(RequestStatus.QUEUED));
        heap.clear();
        for (VehicleRequest req : dbQueued) {
            priorityCalculator.applyToRequest(req, now);
            requestRepository.save(req);
            heap.insert(req);
        }
        heap.rebuild();
    }

    @Transactional
    public synchronized List<VehicleRequest> getOrderedQueue(Instant now) {
        refreshPriorities(now);
        return heap.toSortedList();
    }

    @Transactional
    public synchronized List<VehicleRequest> getOrderedQueueForStation(String stationId, Instant now) {
        return getOrderedQueue(now).stream()
                .filter(req -> req.getStation().getStationId().equals(stationId))
                .toList();
    }

    @Transactional
    public synchronized Integer getQueuePosition(String requestId, Instant now) {
        List<VehicleRequest> ordered = getOrderedQueue(now);
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).getRequestId().equals(requestId)) {
                return i + 1;
            }
        }
        return null;
    }

    public synchronized boolean removeById(String requestId) {
        for (VehicleRequest item : heap.toSortedList()) {
            if (item.getRequestId().equals(requestId)) {
                return heap.remove(item);
            }
        }
        return false;
    }

    public synchronized int size() {
        return heap.size();
    }

    public static Comparator<VehicleRequest> getComparator() {
        return REQUEST_PRIORITY_COMPARATOR;
    }
}
