package com.evchargeflow.engine2.priority;

import com.evchargeflow.common.dto.PriorityBreakdown;
import com.evchargeflow.engine1.entity.VehicleRequest;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Engine 2: Calculates the 0-100 priority score from battery urgency (max 50),
 * departure urgency (max 30), and waiting-time urgency (max 20).
 */
@Component
public class PriorityCalculator {

    public PriorityBreakdown calculate(VehicleRequest request, Instant now) {
        int batteryScore = calculateBatteryScore(request.getCurrentBatteryPercent());
        int departureScore = calculateDepartureScore(request.getDesiredDepartureTime(), now);
        int waitingTimeScore = calculateWaitingTimeScore(request.getArrivalTime(), now);
        int total = batteryScore + departureScore + waitingTimeScore;
        return new PriorityBreakdown(batteryScore, departureScore, waitingTimeScore, total);
    }

    public int calculateBatteryScore(double currentBatteryPercent) {
        if (currentBatteryPercent <= 10.0) {
            return 50;
        }
        if (currentBatteryPercent <= 20.0) {
            return 40;
        }
        if (currentBatteryPercent <= 30.0) {
            return 30;
        }
        if (currentBatteryPercent <= 40.0) {
            return 20;
        }
        if (currentBatteryPercent <= 60.0) {
            return 10;
        }
        return 0;
    }

    public int calculateDepartureScore(Instant desiredDepartureTime, Instant now) {
        if (desiredDepartureTime == null) {
            return 0;
        }
        double minutesRemaining = Duration.between(now, desiredDepartureTime).toSeconds() / 60.0;
        if (minutesRemaining <= 30.0) {
            return 30;
        }
        if (minutesRemaining <= 60.0) {
            return 25;
        }
        if (minutesRemaining <= 120.0) {
            return 15;
        }
        if (minutesRemaining <= 240.0) {
            return 5;
        }
        return 0;
    }

    public int calculateWaitingTimeScore(Instant arrivalTime, Instant now) {
        if (arrivalTime == null) {
            return 0;
        }
        double waitedMinutes = Math.max(0.0, Duration.between(arrivalTime, now).toSeconds() / 60.0);
        if (waitedMinutes >= 60.0) {
            return 20;
        }
        if (waitedMinutes >= 45.0) {
            return 15;
        }
        if (waitedMinutes >= 30.0) {
            return 10;
        }
        if (waitedMinutes >= 15.0) {
            return 5;
        }
        return 0;
    }

    public void applyToRequest(VehicleRequest request, Instant now) {
        PriorityBreakdown breakdown = calculate(request, now);
        request.setBatteryScore(breakdown.batteryScore());
        request.setDepartureScore(breakdown.departureScore());
        request.setWaitingTimeScore(breakdown.waitingTimeScore());
        request.setPriorityScore(breakdown.totalScore());
    }
}
