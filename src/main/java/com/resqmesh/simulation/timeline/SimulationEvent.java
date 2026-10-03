package com.resqmesh.simulation.timeline;

import com.resqmesh.model.CommunicationDevice;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Represents a single discrete event along an emergency transmission timeline.
 * Encapsulates timing, participating nodes, narrative telemetry, and lifecycle status.
 */
public class SimulationEvent {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final int sequenceNumber;
    private final long elapsedMs;
    private final String timestamp;
    private final SimulationEventType type;
    private final CommunicationDevice sourceDevice;
    private final CommunicationDevice targetDevice;
    private final String description;
    private final int hopIndex;
    private final int totalHops;

    public SimulationEvent(int sequenceNumber,
                           long elapsedMs,
                           String timestamp,
                           SimulationEventType type,
                           CommunicationDevice sourceDevice,
                           CommunicationDevice targetDevice,
                           String description,
                           int hopIndex,
                           int totalHops) {
        this.sequenceNumber = sequenceNumber;
        this.elapsedMs = elapsedMs;
        this.timestamp = (timestamp != null) ? timestamp : LocalTime.now().format(TIME_FORMATTER);
        this.type = type;
        this.sourceDevice = sourceDevice;
        this.targetDevice = targetDevice;
        this.description = (description != null) ? description : "";
        this.hopIndex = hopIndex;
        this.totalHops = totalHops;
    }

    public SimulationEvent(int sequenceNumber,
                           long elapsedMs,
                           SimulationEventType type,
                           CommunicationDevice sourceDevice,
                           CommunicationDevice targetDevice,
                           String description,
                           int hopIndex,
                           int totalHops) {
        this(sequenceNumber, elapsedMs, LocalTime.now().format(TIME_FORMATTER), type, sourceDevice, targetDevice, description, hopIndex, totalHops);
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    public String getFormattedElapsed() {
        return "+" + elapsedMs + "ms";
    }

    public String getTimestamp() {
        return timestamp;
    }

    public SimulationEventType getType() {
        return type;
    }

    public CommunicationDevice getSourceDevice() {
        return sourceDevice;
    }

    public String getSourceDeviceName() {
        return (sourceDevice != null) ? sourceDevice.getName() : "-";
    }

    public CommunicationDevice getTargetDevice() {
        return targetDevice;
    }

    public String getTargetDeviceName() {
        return (targetDevice != null) ? targetDevice.getName() : "-";
    }

    public String getDescription() {
        return description;
    }

    public int getHopIndex() {
        return hopIndex;
    }

    public int getTotalHops() {
        return totalHops;
    }

    public boolean isTerminal() {
        return type == SimulationEventType.MESSAGE_DELIVERED || type == SimulationEventType.DELIVERY_FAILED;
    }

    @Override
    public String toString() {
        return String.format("[#%d | %s | +%dms] %s: %s",
                sequenceNumber, timestamp, elapsedMs, type, description);
    }
}
