package com.resqmesh.model;

/**
 * Tracks the lifecycle states of a message as it traverses the offline mesh network.
 */
public enum MessageStatus {
    /**
     * Message has been composed but not yet placed into the outgoing queue.
     */
    CREATED,

    /**
     * Message is stored in a device's buffer waiting for an available peer or route.
     */
    QUEUED,

    /**
     * Message is currently in transit between intermediate relay devices.
     */
    FORWARDING,

    /**
     * Message has successfully reached its intended destination device.
     */
    DELIVERED,

    /**
     * Message could not reach its destination (e.g., hop limit exceeded, no route).
     */
    FAILED
}
