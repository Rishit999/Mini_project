package com.evchargeflow.common.dto;

public record PriorityBreakdown(
        int batteryScore,
        int departureScore,
        int waitingTimeScore,
        int totalScore
) {
}
