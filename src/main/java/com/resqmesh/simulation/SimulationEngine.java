package com.resqmesh.simulation;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.model.MessageStatus;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.RoutingStrategy;
import com.resqmesh.routing.ShortestPathStrategy;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Core simulation controller responsible for coordinating network message routing,
 * device battery consumption, and delivery lifecycle management.
 */
public class SimulationEngine {

    /**
     * Standard battery consumption per message transmission per device on the route.
     */
    public static final double BATTERY_COST_PER_TRANSMISSION = 2.0;

    private final NetworkGraph graph;
    private final RoutingStrategy routingStrategy;
    private double batteryCostPerTransmission = BATTERY_COST_PER_TRANSMISSION;

    /**
     * Constructs a SimulationEngine with a specific graph and routing strategy.
     *
     * @param graph           the network topology to simulate
     * @param routingStrategy the pathfinding algorithm to use
     */
    public SimulationEngine(NetworkGraph graph, RoutingStrategy routingStrategy) {
        if (graph == null) {
            throw new IllegalArgumentException("NetworkGraph cannot be null.");
        }
        if (routingStrategy == null) {
            throw new IllegalArgumentException("RoutingStrategy cannot be null.");
        }
        this.graph = graph;
        this.routingStrategy = routingStrategy;
    }

    /**
     * Constructs a SimulationEngine defaulting to the ShortestPathStrategy (BFS).
     *
     * @param graph the network topology to simulate
     */
    public SimulationEngine(NetworkGraph graph) {
        this(graph, new ShortestPathStrategy());
    }

    /**
     * Simulates sending an emergency message across the mesh network.
     * Lifecycle steps:
     * 1. Queues the message.
     * 2. Checks sender and recipient availability.
     * 3. Discovers a valid route using the routing strategy.
     * 4. If no route exists, marks FAILED and explains why.
     * 5. If route exists, marks FORWARDING, consumes battery (2 units per device),
     *    marks DELIVERED, and returns a success result.
     *
     * @param message the EmergencyMessage to transmit
     * @return SimulationResult containing delivery status, route, and explanation
     */
    public SimulationResult send(EmergencyMessage message) {
        if (message == null) {
            return SimulationResult.failure("Cannot send a null emergency message.");
        }

        // Step 1: Mark message as QUEUED in the buffer
        message.setStatus(MessageStatus.QUEUED);

        CommunicationDevice sender = message.getSender();
        CommunicationDevice recipient = message.getRecipient();

        // Step 2: Validate sender and recipient network registration
        if (!graph.containsDevice(sender)) {
            message.setStatus(MessageStatus.FAILED);
            return SimulationResult.failure(String.format("Sender '%s' is not registered in the network graph.", sender.getName()));
        }
        if (!graph.containsDevice(recipient)) {
            message.setStatus(MessageStatus.FAILED);
            return SimulationResult.failure(String.format("Recipient '%s' is not registered in the network graph.", recipient.getName()));
        }

        // Step 3: Check whether sender and recipient are available (not offline, battery > 0)
        if (!sender.isAvailable()) {
            message.setStatus(MessageStatus.FAILED);
            return SimulationResult.failure(String.format("Sender '%s' is unavailable (status: %s, battery: %.1f%%).",
                    sender.getName(), sender.getStatus(), sender.getBatteryLevel()));
        }
        if (!recipient.isAvailable()) {
            message.setStatus(MessageStatus.FAILED);
            return SimulationResult.failure(String.format("Recipient '%s' is unavailable (status: %s, battery: %.1f%%).",
                    recipient.getName(), recipient.getStatus(), recipient.getBatteryLevel()));
        }

        // Step 4: Use the routing strategy to discover an active path respecting priority
        List<CommunicationDevice> route = routingStrategy.findRoute(sender, recipient, graph, message.getPriority());

        // Step 5: Check if a path was found
        if (route == null || route.isEmpty()) {
            message.setStatus(MessageStatus.FAILED);
            return SimulationResult.failure(String.format(
                    "No valid route found from '%s' to '%s' for priority %s (path may be severed or intermediate relays cannot forward this priority).",
                    sender.getName(), recipient.getName(), message.getPriority()));
        }

        // Step 6: Mark FORWARDING as the message traverses intermediate devices
        message.setStatus(MessageStatus.FORWARDING);

        // Step 7: Simulate transmission and deduct battery per device along the route
        for (CommunicationDevice device : route) {
            device.consumeBattery(batteryCostPerTransmission);
        }

        // Step 8: Mark message as successfully DELIVERED
        message.setStatus(MessageStatus.DELIVERED);

        // Build human-readable explanation
        int hopCount = Math.max(0, route.size() - 1);
        String pathString = formatPath(route);
        String explanation = String.format(
                "Message successfully delivered from '%s' to '%s' in %d hop(s) [%d devices]. Path: %s",
                sender.getName(), recipient.getName(), hopCount, route.size(), pathString);

        return SimulationResult.success(explanation, route);
    }

    /**
     * Formats an ordered list of devices into a readable path string (e.g., "A -> B -> C").
     */
    private String formatPath(List<CommunicationDevice> route) {
        return route.stream()
                .map(CommunicationDevice::getName)
                .collect(Collectors.joining(" -> "));
    }

    public NetworkGraph getGraph() {
        return graph;
    }

    public RoutingStrategy getRoutingStrategy() {
        return routingStrategy;
    }

    public double getBatteryCostPerTransmission() {
        return batteryCostPerTransmission;
    }

    public void setBatteryCostPerTransmission(double batteryCostPerTransmission) {
        if (batteryCostPerTransmission < 0.0) {
            throw new IllegalArgumentException("Battery cost per transmission cannot be negative: " + batteryCostPerTransmission);
        }
        this.batteryCostPerTransmission = batteryCostPerTransmission;
    }
}
