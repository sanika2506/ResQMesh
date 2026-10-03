package com.resqmesh.model;

import java.util.Objects;

/**
 * Represents a directional communication channel between two mesh devices.
 * Demonstrates:
 * - Association / Composition: Connects a source and destination CommunicationDevice.
 * - Encapsulation: Protects link state and provides a validated usable() check.
 */
public class CommunicationLink {

    private final CommunicationDevice source;
    private final CommunicationDevice destination;
    private boolean active;

    /**
     * Constructs an active communication link between two devices.
     *
     * @param source      the transmitting device (non-null)
     * @param destination the receiving device (non-null, different from source)
     * @throws IllegalArgumentException if either device is null or if source equals destination
     */
    public CommunicationLink(CommunicationDevice source, CommunicationDevice destination) {
        this(source, destination, true);
    }

    /**
     * Constructs a communication link with an explicit initial connection status.
     *
     * @param source      the transmitting device (non-null)
     * @param destination the receiving device (non-null, different from source)
     * @param active      the initial state of the physical/wireless link
     * @throws IllegalArgumentException if either device is null or if source equals destination
     */
    public CommunicationLink(CommunicationDevice source, CommunicationDevice destination, boolean active) {
        if (source == null) {
            throw new IllegalArgumentException("Source device cannot be null.");
        }
        if (destination == null) {
            throw new IllegalArgumentException("Destination device cannot be null.");
        }
        if (source.equals(destination)) {
            throw new IllegalArgumentException("Cannot create a communication link from a device to itself.");
        }

        this.source = source;
        this.destination = destination;
        this.active = active;
    }

    /**
     * Determines whether this communication link is currently usable for transmitting data.
     * A link is usable if and only if:
     * 1. The link connection status itself is active.
     * 2. The source device is available (not offline, battery > 0).
     * 3. The destination device is available (not offline, battery > 0).
     *
     * @return true if the link and both connected devices are operational; false otherwise
     */
    public boolean usable() {
        return this.active && this.source.isAvailable() && this.destination.isAvailable();
    }

    /**
     * Calculates the physical Euclidean distance between the source and destination devices.
     *
     * @return distance in simulation coordinate units
     */
    public double getDistance() {
        return this.source.getLocation().distanceTo(this.destination.getLocation());
    }

    public CommunicationDevice getSource() {
        return source;
    }

    public CommunicationDevice getDestination() {
        return destination;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunicationLink that)) return false;
        return Objects.equals(source, that.source) && Objects.equals(destination, that.destination);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, destination);
    }

    @Override
    public String toString() {
        return String.format("Link[%s (%s) -> %s (%s) | active=%s, usable=%s, dist=%.1f]",
                source.getName(), source.getId(),
                destination.getName(), destination.getId(),
                active, usable(), getDistance());
    }
}
