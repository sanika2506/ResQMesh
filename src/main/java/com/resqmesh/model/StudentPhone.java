package com.resqmesh.model;

/**
 * Represents a student's personal handheld smartphone participating in the ad-hoc mesh.
 * Demonstrates:
 * - Inheritance: Inherits all properties (ID, battery, location) from CommunicationDevice.
 * - Polymorphism: Implements custom forwarding rules (student phones are ad-hoc peers
 *   that forward routine, normal, and high-priority messages, but CANNOT forward CRITICAL
 *   messages to ensure critical traffic is only routed through reliable infrastructure).
 */
public class StudentPhone extends CommunicationDevice {

    /**
     * Constructs a StudentPhone with an initial battery level.
     *
     * @param id           unique device identifier
     * @param name         device name (e.g., "Alice's Phone")
     * @param location     initial spatial location
     * @param batteryLevel initial battery percentage (0.0 to 100.0)
     */
    public StudentPhone(String id, String name, Location location, double batteryLevel) {
        super(id, name, location, batteryLevel);
    }

    /**
     * Constructs a StudentPhone with full battery (100%).
     *
     * @param id       unique device identifier
     * @param name     device name
     * @param location initial spatial location
     */
    public StudentPhone(String id, String name, Location location) {
        super(id, name, location);
    }

    /**
     * Determines whether this student phone can forward a message.
     * Rules:
     * 1. Must be currently available (not offline, battery > 0).
     * 2. Cannot forward CRITICAL messages (restricted to dedicated emergency stations).
     *
     * @param priority the urgency level of the message
     * @return true if available and priority is not CRITICAL; false otherwise
     */
    @Override
    public boolean canForward(Priority priority) {
        if (!isAvailable() || priority == null) {
            return false;
        }
        return priority != Priority.CRITICAL;
    }
}
