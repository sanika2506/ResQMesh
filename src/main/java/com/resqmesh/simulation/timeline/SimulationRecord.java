package com.resqmesh.simulation.timeline;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.simulation.SimulationResult;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Historical snapshot of a completed simulation dispatch session.
 * Retains the emergency message, outcome, and event timeline for non-destructive review and replay.
 */
public class SimulationRecord {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final String id;
    private final String timestamp;
    private final EmergencyMessage message;
    private final SimulationResult result;
    private final SimulationTimeline timeline;

    public SimulationRecord(EmergencyMessage message, SimulationResult result, SimulationTimeline timeline) {
        this(message.getId(), LocalTime.now().format(TIME_FORMATTER), message, result, timeline);
    }

    public SimulationRecord(String id, String timestamp, EmergencyMessage message, SimulationResult result, SimulationTimeline timeline) {
        this.id = id;
        this.timestamp = (timestamp != null) ? timestamp : LocalTime.now().format(TIME_FORMATTER);
        this.message = message;
        this.result = result;
        this.timeline = timeline;
    }

    public String getId() {
        return id;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public EmergencyMessage getMessage() {
        return message;
    }

    public SimulationResult getResult() {
        return result;
    }

    public SimulationTimeline getTimeline() {
        return timeline;
    }

    public boolean isDelivered() {
        return result.isDelivered();
    }

    public int getHopCount() {
        return Math.max(0, result.getRoute().size() - 1);
    }

    public String getSenderName() {
        return (message != null && message.getSender() != null) ? message.getSender().getName() : "Unknown";
    }

    public String getRecipientName() {
        return (message != null && message.getRecipient() != null) ? message.getRecipient().getName() : "Unknown";
    }

    public String getMessageContent() {
        return (message != null && message.getContent() != null) ? message.getContent() : "";
    }

    public String getPriorityName() {
        return (message != null && message.getPriority() != null) ? message.getPriority().name() : "NORMAL";
    }

    public SimulationResult.DeliveryOutcome getOutcome() {
        if (result != null) {
            return result.getOutcome();
        }
        return isDelivered() ? SimulationResult.DeliveryOutcome.DELIVERED : SimulationResult.DeliveryOutcome.FAILED;
    }

    public String getStatusDisplay() {
        SimulationResult.DeliveryOutcome outcome = getOutcome();
        if (outcome == SimulationResult.DeliveryOutcome.DELIVERED) {
            return "Delivered";
        } else if (outcome == SimulationResult.DeliveryOutcome.NO_ROUTE_FOUND) {
            return "No Route Found";
        } else {
            return "Failed";
        }
    }

    public String getRoutePath() {
        if (result != null && result.getRoute() != null && !result.getRoute().isEmpty()) {
            return result.getRoute().stream()
                    .map(CommunicationDevice::getName)
                    .collect(Collectors.joining(" ──▶ "));
        }
        if (isDelivered()) {
            return "Direct Link";
        }
        return "None (No route)";
    }

    public String getSummary() {
        String outcome = isDelivered()
                ? String.format("✔ DELIVERED (%d hop%s)", getHopCount(), getHopCount() == 1 ? "" : "s")
                : "✖ FAILED";
        return String.format("[%s] %s | %s ──▶ %s (%s)",
                id,
                timestamp,
                message.getSender().getName(),
                message.getRecipient().getName(),
                outcome);
    }

    @Override
    public String toString() {
        return getSummary();
    }
}
