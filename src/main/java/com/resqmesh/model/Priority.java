package com.resqmesh.model;

/**
 * Represents the urgency and priority level of a communication message.
 * Higher priority messages (such as CRITICAL emergency alerts) may follow
 * stricter forwarding rules or receive prioritization in queues.
 */
public enum Priority {
    /**
     * Routine or informational updates.
     */
    LOW,

    /**
     * Standard user-to-user communication.
     */
    NORMAL,

    /**
     * Urgent situational updates or warnings.
     */
    HIGH,

    /**
     * Life-safety emergency alerts requiring high-reliability forwarding.
     */
    CRITICAL
}
