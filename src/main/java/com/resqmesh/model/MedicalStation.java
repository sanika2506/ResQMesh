package com.resqmesh.model;

/**
 * Represents a medical triage post, health center terminal, or field ambulance unit.
 * Acts as a vital emergency node capable of relaying all levels of emergency traffic,
 * including CRITICAL patient distress and medical evacuation requests.
 */
public class MedicalStation extends CommunicationDevice {

    /**
     * Constructs a MedicalStation with a custom battery level.
     *
     * @param id           unique station identifier
     * @param name         station name (e.g., "Field Triage Unit 1")
     * @param location     physical location coordinates
     * @param batteryLevel current battery percentage (0.0 to 100.0)
     */
    public MedicalStation(String id, String name, Location location, double batteryLevel) {
        super(id, name, location, batteryLevel);
    }

    /**
     * Constructs a MedicalStation defaulting to full battery (100%).
     *
     * @param id       unique station identifier
     * @param name     station name
     * @param location physical location coordinates
     */
    public MedicalStation(String id, String name, Location location) {
        super(id, name, location);
    }

    /**
     * Determines whether the medical station can forward a message.
     * Medical stations are permitted to forward ALL priorities
     * (LOW, NORMAL, HIGH, and CRITICAL) as long as the station is available.
     *
     * @param priority the urgency level of the message
     * @return true if the station is available and priority is non-null; false otherwise
     */
    @Override
    public boolean canForward(Priority priority) {
        if (!isAvailable() || priority == null) {
            return false;
        }
        return true;
    }
}
