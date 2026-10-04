package com.resqmesh.model;

import java.util.Objects;

/**
 * Abstract base class representing a generic communication node in the ResQMesh network.
 * Demonstrates:
 * - Abstraction: Exposes common device contracts while leaving forwarding policies to specific device types.
 * - Encapsulation: Protects internal state (battery, status, location) with private fields and validated methods.
 */
public abstract class CommunicationDevice {

    public static final double LOW_BATTERY_THRESHOLD = 20.0;
    public static final double MAX_BATTERY = 100.0;
    public static final double MIN_BATTERY = 0.0;

    private String id;
    private String name;
    private Location location;
    private double batteryLevel; // Stored as percentage: 0.0% to 100.0%
    private DeviceStatus status;

    /**
     * Primary constructor to initialize a communication device.
     *
     * @param id           unique device identifier (non-empty)
     * @param name         human-readable device name (non-empty)
     * @param location     current spatial location (non-null)
     * @param batteryLevel battery percentage (clamped between 0.0 and 100.0)
     */
    public CommunicationDevice(String id, String name, Location location, double batteryLevel) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be null or empty.");
        }
        if (location == null) {
            throw new IllegalArgumentException("Device location cannot be null.");
        }

        this.id = id.trim();
        this.name = name.trim();
        this.location = location;
        this.batteryLevel = Math.max(MIN_BATTERY, Math.min(MAX_BATTERY, batteryLevel));

        // Auto-assign initial status based on starting battery level
        if (this.batteryLevel <= MIN_BATTERY) {
            this.status = DeviceStatus.OFFLINE;
        } else if (this.batteryLevel <= LOW_BATTERY_THRESHOLD) {
            this.status = DeviceStatus.LOW_BATTERY;
        } else {
            this.status = DeviceStatus.ACTIVE;
        }
    }

    /**
     * Convenience constructor defaulting initial battery level to 100%.
     *
     * @param id       unique device identifier
     * @param name     device name
     * @param location current spatial location
     */
    public CommunicationDevice(String id, String name, Location location) {
        this(id, name, location, MAX_BATTERY);
    }

    // -------------------------------------------------------------
    // Battery Management
    // -------------------------------------------------------------

    /**
     * Consumes a specific amount of battery power and automatically adjusts device status.
     *
     * @param amount the percentage of battery consumed (must be non-negative)
     */
    public void consumeBattery(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Battery consumption amount cannot be negative.");
        }

        this.batteryLevel = Math.max(MIN_BATTERY, this.batteryLevel - amount);

        // Update status based on depleted battery
        if (this.batteryLevel <= MIN_BATTERY) {
            this.status = DeviceStatus.OFFLINE;
        } else if (this.batteryLevel <= LOW_BATTERY_THRESHOLD && this.status == DeviceStatus.ACTIVE) {
            this.status = DeviceStatus.LOW_BATTERY;
        }
    }

    /**
     * Recharges the device battery and updates its operational status.
     *
     * @param amount the percentage of battery to add (must be non-negative)
     */
    public void recharge(double amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Recharge amount cannot be negative.");
        }

        this.batteryLevel = Math.min(MAX_BATTERY, this.batteryLevel + amount);

        // Transition back to functional states if previously offline or low battery
        if (this.batteryLevel > LOW_BATTERY_THRESHOLD) {
            this.status = DeviceStatus.ACTIVE;
        } else if (this.batteryLevel > MIN_BATTERY) {
            this.status = DeviceStatus.LOW_BATTERY;
        }
    }

    // -------------------------------------------------------------
    // Operational Availability & Abstract Forwarding Contract
    // -------------------------------------------------------------

    /**
     * Checks if the device is currently capable of participating in network communication.
     * A device is available if it is not OFFLINE and has battery remaining.
     *
     * @return true if device can participate, false otherwise
     */
    public boolean isAvailable() {
        return this.status != DeviceStatus.OFFLINE && this.batteryLevel > MIN_BATTERY;
    }

    /**
     * Abstract policy method deciding whether this device can forward a message of a given priority.
     * Implemented by specific subclasses to demonstrate runtime polymorphism.
     *
     * @param priority the urgency level of the message to forward
     * @return true if permitted to forward, false otherwise
     */
    public abstract boolean canForward(Priority priority);

    // -------------------------------------------------------------
    // Getters and Setters
    // -------------------------------------------------------------

    public String getId() {
        return id;
    }

    public void setId(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty.");
        }
        this.id = id.trim();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Device name cannot be null or empty.");
        }
        this.name = name.trim();
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        if (location == null) {
            throw new IllegalArgumentException("Location cannot be null.");
        }
        this.location = location;
    }

    public double getBatteryLevel() {
        return batteryLevel;
    }

    public void setBatteryLevel(double batteryLevel) {
        this.batteryLevel = Math.max(MIN_BATTERY, Math.min(MAX_BATTERY, batteryLevel));
        if (this.batteryLevel <= MIN_BATTERY) {
            this.status = DeviceStatus.OFFLINE;
        } else if (this.batteryLevel <= LOW_BATTERY_THRESHOLD && this.status == DeviceStatus.ACTIVE) {
            this.status = DeviceStatus.LOW_BATTERY;
        } else if (this.batteryLevel > LOW_BATTERY_THRESHOLD && this.status == DeviceStatus.LOW_BATTERY) {
            this.status = DeviceStatus.ACTIVE;
        }
    }

    public DeviceStatus getStatus() {
        return status;
    }

    public void setStatus(DeviceStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("DeviceStatus cannot be null.");
        }
        this.status = status;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CommunicationDevice that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("%s[id='%s', name='%s', battery=%.1f%%, status=%s, loc=%s]",
                getClass().getSimpleName(), id, name, batteryLevel, status, location);
    }
}
