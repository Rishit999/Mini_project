package com.evchargeflow.common.dto;

import com.evchargeflow.common.enums.AllocationOutcome;

public record AllocationResult(
        AllocationOutcome outcome,
        String requestId,
        String stationId,
        String assignedChargerId,
        String assignedChargerName,
        Integer queuePosition,
        Double estimatedWaitMinutes,
        PriorityBreakdown priorityBreakdown,
        String reason,
        String sessionId
) {
}
