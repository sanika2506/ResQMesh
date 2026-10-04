package com.resqmesh.simulation;

import com.resqmesh.model.CommunicationDevice;

import java.util.Collections;
import java.util.List;

/**
 * Encapsulates the outcome of attempting to transmit an emergency message through the mesh network.
 * Contains delivery status, an explanation message, and the ordered device route taken.
 */
public record SimulationResult(
        boolean delivered,
        String explanation,
        List<CommunicationDevice> route
) {

    /**
     * Compact constructor ensuring defensive copies and non-null values.
     */
    public SimulationResult {
        route = (route == null) ? Collections.emptyList() : Collections.unmodifiableList(route);
        explanation = (explanation == null) ? "" : explanation;
    }

    // JavaBean-style getter aliases for maximum compatibility and student friendliness
    public boolean isDelivered() {
        return delivered;
    }

    public String getExplanation() {
        return explanation;
    }

    public List<CommunicationDevice> getRoute() {
        return route;
    }

    /**
     * High-level classification of transmission result.
     */
    public enum DeliveryOutcome {
        DELIVERED,
        NO_ROUTE_FOUND,
        FAILED
    }

    /**
     * Checks if this delivery failed specifically because no feasible path connects sender to recipient.
     *
     * @return true if routing failed to find an active path
     */
    public boolean isNoRouteFound() {
        if (delivered) {
            return false;
        }
        if (explanation == null) {
            return false;
        }
        String lower = explanation.toLowerCase();
        return lower.contains("no valid route found") || lower.contains("no route found") || lower.contains("no feasible route") || lower.contains("path may be severed");
    }

    /**
     * Checks if delivery failed due to device unavailability, registration errors, or invalid arguments.
     *
     * @return true if failure occurred before pathfinding or due to inactive endpoints
     */
    public boolean isFailed() {
        return !delivered && !isNoRouteFound();
    }

    /**
     * Returns the categorized delivery outcome.
     *
     * @return DELIVERED, NO_ROUTE_FOUND, or FAILED
     */
    public DeliveryOutcome getOutcome() {
        if (delivered) {
            return DeliveryOutcome.DELIVERED;
        }
        if (isNoRouteFound()) {
            return DeliveryOutcome.NO_ROUTE_FOUND;
        }
        return DeliveryOutcome.FAILED;
    }

    /**
     * Returns the number of transmission hops (edges traversed).
     *
     * @return hop count (0 if empty or direct failure)
     */
    public int getHopCount() {
        return (route == null || route.isEmpty()) ? 0 : Math.max(0, route.size() - 1);
    }

    /**
     * Convenience factory for creating a successful simulation result.
     *
     * @param explanation human-readable summary of the delivery
     * @param route       the ordered path of devices traversed
     * @return successful SimulationResult
     */
    public static SimulationResult success(String explanation, List<CommunicationDevice> route) {
        return new SimulationResult(true, explanation, route);
    }

    /**
     * Convenience factory for creating a failed simulation result with an empty route.
     *
     * @param explanation the reason for delivery failure
     * @return failed SimulationResult
     */
    public static SimulationResult failure(String explanation) {
        return new SimulationResult(false, explanation, Collections.emptyList());
    }
}

