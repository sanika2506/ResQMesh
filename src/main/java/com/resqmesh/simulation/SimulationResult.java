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
