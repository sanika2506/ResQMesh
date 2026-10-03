package com.resqmesh.simulation.timeline;

/**
 * Enumerates distinct lifecycle milestones during an emergency message transmission
 * through the mesh network.
 */
public enum SimulationEventType {
    /**
     * Emergency message initiated by the sender with priority and payload.
     */
    MESSAGE_CREATED("Message Created", "📨"),

    /**
     * BFS pathfinding algorithm discovers an active shortest path to the target.
     */
    ROUTE_DISCOVERED("Route Discovered", "🗺️"),

    /**
     * Message is relayed through an intermediate mesh node along the route.
     */
    HOP_FORWARDING("Hop Forwarding", "⚡"),

    /**
     * Message successfully reaches the target destination node.
     */
    MESSAGE_DELIVERED("Delivered", "✔"),

    /**
     * Transmission failed (e.g. sender/recipient offline, route severed, priority restriction).
     */
    DELIVERY_FAILED("Delivery Failed", "✖");

    private final String displayName;
    private final String icon;

    SimulationEventType(String displayName, String icon) {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIcon() {
        return icon;
    }

    @Override
    public String toString() {
        return icon + " " + displayName;
    }
}
