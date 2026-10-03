package com.resqmesh.model;

/**
 * Represents the operational status of a communication device in the mesh network.
 */
public enum DeviceStatus {
    /**
     * Device is powered on, functional, and ready to participate in network operations.
     */
    ACTIVE,

    /**
     * Device is powered down, out of battery, or disconnected from the network.
     */
    OFFLINE,

    /**
     * Device has low battery power remaining and may conserve resources.
     */
    LOW_BATTERY
}
