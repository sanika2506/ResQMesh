package com.resqmesh.model;

/**
 * Represents a fixed or mobile campus security post / base station.
 * Acts as a reliable infrastructure node capable of relaying all levels of emergency traffic,
 * including CRITICAL life-safety distress signals.
 */
public class SecurityStation extends CommunicationDevice {

    /**
     * Constructs a SecurityStation with a custom battery level.
     *
     * @param id           unique station identifier
     * @param name         station name (e.g., "Campus North Security Post")
     * @param location     physical location coordinates
     * @param batteryLevel current battery percentage (0.0 to 100.0)
     */
    public SecurityStation(String id, String name, Location location, double batteryLevel) {
        super(id, name, location, batteryLevel);
    }

    /**
     * Constructs a SecurityStation defaulting to full battery (100%).
     *
     * @param id       unique station identifier
     * @param name     station name
     * @param location physical location coordinates
     */
    public SecurityStation(String id, String name, Location location) {
        super(id, name, location);
    }

    /**
     * Determines whether the security station can forward a message.
     * Security stations have the authority and capacity to forward ALL priorities
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
