package com.resqmesh.simulation;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.ui.NetworkTopologyPane;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Tests for Emergency Message Delivery, BFS Shortest Path,
 * Activity Logging, KPI Counters, and Topology Visual Route Illumination.
 * 
 * Verifies all required scenarios:
 * 1. End-to-end multi-hop delivery from Alice to Medical Center.
 * 2. Visual Outcome panel formatting & telemetry updates.
 * 3. BFS route illumination and hop sequence badges on NetworkTopologyPane.
 * 4. Delivery failure when sender is offline.
 * 5. Delivery failure when recipient is offline.
 * 6. Delivery failure when an intermediate node is offline (path blocked).
 * 7. Direct 1-hop connection between sender and recipient.
 * 8. Multiple paths shortest-path selection (BFS optimality).
 * 9. Route highlighting reset on subsequent dispatches.
 */
public class EmergencyDeliveryIntegrationTest {

    private NetworkGraph graph;
    private SimulationEngine engine;
    private NetworkTopologyPane topologyPane;

    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;
    private StudentPhone charlie;

    // Simulated dashboard metrics
    private int totalMessagesDispatched = 0;
    private int successfulDeliveries = 0;
    private final List<String> activityLog = new ArrayList<>();

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(10, 10), 100.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(30, 20), 100.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(50, 40), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(70, 60), 100.0);
        charlie = new StudentPhone("DEV-3", "Charlie's Phone (Isolated)", new Location(90, 80), 80.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(charlie);

        // Chain: Alice <---> Bob <---> Security <---> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        topologyPane = new NetworkTopologyPane(graph);
        topologyPane.refresh();

        totalMessagesDispatched = 0;
        successfulDeliveries = 0;
        activityLog.clear();
    }

    /**
     * Simulates dispatch logic identical to App.java handleSendMessage().
     */
    private SimulationResult simulateDispatch(CommunicationDevice sender, CommunicationDevice recipient,
                                              String content, Priority priority) {
        String msgId = "MSG-" + (++totalMessagesDispatched);
        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content, priority);

        activityLog.add(String.format("[DISPATCH] [%s] Initiated from '%s' to '%s' | Priority: %s",
                msgId, sender.getName(), recipient.getName(), priority));

        // Reset previous route highlighting before dispatch
        topologyPane.clearRouteHighlight();

        SimulationResult result = engine.send(message);

        if (result.delivered()) {
            successfulDeliveries++;
            int hops = Math.max(0, result.route().size() - 1);
            topologyPane.highlightRoute(result.route());

            String routeStr = result.route().stream().map(CommunicationDevice::getName).collect(Collectors.joining(" -> "));
            activityLog.add(String.format("[SUCCESS] [%s] Successfully delivered via %d hops: %s", msgId, hops, routeStr));
        } else {
            topologyPane.clearRouteHighlight();
            activityLog.add(String.format("[FAILED] [%s] Delivery failed. Reason: %s", msgId, result.explanation()));
        }

        topologyPane.refresh();
        return result;
    }

    @Test
    @DisplayName("Scenario 1: Successful delivery from Alice to Medical Center with BFS route highlighting")
    void testAliceToMedicalDeliveryAndVisualization() {
        SimulationResult result = simulateDispatch(alice, medical, "Distress Alert: Medical assistance needed", Priority.NORMAL);

        // 1. Verify simulation result
        assertTrue(result.delivered(), "Message from Alice to Medical Center must be delivered");
        assertEquals(4, result.route().size(), "Route must consist of 4 devices (3 hops)");
        assertEquals(alice, result.route().get(0));
        assertEquals(bob, result.route().get(1));
        assertEquals(security, result.route().get(2));
        assertEquals(medical, result.route().get(3));

        // 2. Verify battery deduction along the route
        assertEquals(98.0, alice.getBatteryLevel(), 0.01);
        assertEquals(98.0, bob.getBatteryLevel(), 0.01);
        assertEquals(98.0, security.getBatteryLevel(), 0.01);
        assertEquals(98.0, medical.getBatteryLevel(), 0.01);
        assertEquals(80.0, charlie.getBatteryLevel(), 0.01, "Charlie was not on route, battery must remain intact");

        // 3. Verify visual topology route illumination
        assertEquals(result.route(), topologyPane.getActiveRoute());
        assertEquals(1, topologyPane.getNodeHopIndex(alice), "Alice should have hop badge #1 (SENDER)");
        assertEquals(2, topologyPane.getNodeHopIndex(bob), "Bob should have hop badge #2");
        assertEquals(3, topologyPane.getNodeHopIndex(security), "Security should have hop badge #3");
        assertEquals(4, topologyPane.getNodeHopIndex(medical), "Medical should have hop badge #4 (RECIPIENT)");
        assertEquals(-1, topologyPane.getNodeHopIndex(charlie), "Charlie should not be on the active route");

        assertTrue(topologyPane.isLinkHighlighted(alice, bob), "Link Alice <-> Bob must be illuminated");
        assertTrue(topologyPane.isLinkHighlighted(bob, security), "Link Bob <-> Security must be illuminated");
        assertTrue(topologyPane.isLinkHighlighted(security, medical), "Link Security <-> Medical must be illuminated");

        // 4. Verify telemetry counters and activity log
        assertEquals(1, totalMessagesDispatched);
        assertEquals(1, successfulDeliveries);
        double rate = (successfulDeliveries * 100.0) / totalMessagesDispatched;
        assertEquals(100.0, rate, 0.01);

        assertTrue(activityLog.stream().anyMatch(log -> log.contains("[SUCCESS]") && log.contains("via 3 hops")));
    }

    @Test
    @DisplayName("Scenario 2: Failure when sender is offline")
    void testSenderOffline() {
        alice.setStatus(DeviceStatus.OFFLINE);

        SimulationResult result = simulateDispatch(alice, medical, "SOS message", Priority.HIGH);

        assertFalse(result.delivered(), "Delivery should fail when sender is offline");
        assertTrue(result.route().isEmpty(), "Route should be empty on sender failure");
        assertTrue(result.explanation().contains("Sender 'Alice's Phone' is unavailable"));

        // Topology route highlighting must be cleared
        assertTrue(topologyPane.getActiveRoute().isEmpty());
        assertEquals(-1, topologyPane.getNodeHopIndex(alice));
        assertEquals(-1, topologyPane.getNodeHopIndex(medical));
        assertFalse(topologyPane.isLinkHighlighted(alice, bob));

        // Battery should remain untouched
        assertEquals(100.0, bob.getBatteryLevel(), 0.01);
        assertEquals(100.0, medical.getBatteryLevel(), 0.01);

        // Metrics verification
        assertEquals(1, totalMessagesDispatched);
        assertEquals(0, successfulDeliveries);
        assertTrue(activityLog.stream().anyMatch(log -> log.contains("[FAILED]") && log.contains("Sender 'Alice's Phone' is unavailable")));
    }

    @Test
    @DisplayName("Scenario 3: Failure when recipient is offline")
    void testRecipientOffline() {
        medical.setStatus(DeviceStatus.OFFLINE);

        SimulationResult result = simulateDispatch(alice, medical, "Critical triage", Priority.HIGH);

        assertFalse(result.delivered());
        assertTrue(result.route().isEmpty());
        assertTrue(result.explanation().contains("Recipient 'Medical Center' is unavailable"));

        // Topology must have zero route highlights
        assertTrue(topologyPane.getActiveRoute().isEmpty());
        assertEquals(-1, topologyPane.getNodeHopIndex(alice));
        assertEquals(-1, topologyPane.getNodeHopIndex(bob));

        assertEquals(1, totalMessagesDispatched);
        assertEquals(0, successfulDeliveries);
        assertTrue(activityLog.stream().anyMatch(log -> log.contains("[FAILED]") && log.contains("Recipient 'Medical Center' is unavailable")));
    }

    @Test
    @DisplayName("Scenario 4: Failure when intermediate node is offline (path blocked)")
    void testIntermediateNodeOffline() {
        // Break chain by taking Security Post offline
        security.setStatus(DeviceStatus.OFFLINE);

        SimulationResult result = simulateDispatch(alice, medical, "Emergency signal", Priority.NORMAL);

        assertFalse(result.delivered(), "Delivery must fail because the only bridging relay is offline");
        assertTrue(result.route().isEmpty());
        assertTrue(result.explanation().contains("No valid route found"));

        // Topology should clear route highlighting
        assertTrue(topologyPane.getActiveRoute().isEmpty());
        assertEquals(-1, topologyPane.getNodeHopIndex(alice));
        assertEquals(-1, topologyPane.getNodeHopIndex(bob));
        assertEquals(-1, topologyPane.getNodeHopIndex(security));
        assertEquals(-1, topologyPane.getNodeHopIndex(medical));

        // Security Post visual reflects offline styling
        assertEquals(0.70, topologyPane.getNodeVisual(security).getOpacity(), 0.01);

        assertEquals(1, totalMessagesDispatched);
        assertEquals(0, successfulDeliveries);
    }

    @Test
    @DisplayName("Scenario 5: Direct connection between sender and recipient (1 hop)")
    void testDirectConnectionDelivery() {
        // Establish direct link between Alice and Medical Center
        graph.connect(alice, medical);
        topologyPane.refresh();

        SimulationResult result = simulateDispatch(alice, medical, "Direct SOS broadcast", Priority.CRITICAL);

        assertTrue(result.delivered());
        assertEquals(2, result.route().size(), "Direct route must consist of 2 devices (1 hop)");
        assertEquals(alice, result.route().get(0));
        assertEquals(medical, result.route().get(1));

        // Battery deduction: only Alice and Medical deducted
        assertEquals(98.0, alice.getBatteryLevel(), 0.01);
        assertEquals(98.0, medical.getBatteryLevel(), 0.01);
        assertEquals(100.0, bob.getBatteryLevel(), 0.01, "Bob should not be deducted");
        assertEquals(100.0, security.getBatteryLevel(), 0.01, "Security should not be deducted");

        // Visualization verification
        assertEquals(1, topologyPane.getNodeHopIndex(alice));
        assertEquals(2, topologyPane.getNodeHopIndex(medical));
        assertEquals(-1, topologyPane.getNodeHopIndex(bob));
        assertEquals(-1, topologyPane.getNodeHopIndex(security));

        assertTrue(topologyPane.isLinkHighlighted(alice, medical), "Direct link Alice <-> Medical must be illuminated");
        assertFalse(topologyPane.isLinkHighlighted(alice, bob), "Old indirect link should not be illuminated");

        assertEquals(1, successfulDeliveries);
        assertTrue(activityLog.stream().anyMatch(log -> log.contains("Successfully delivered via 1 hops")));
    }

    @Test
    @DisplayName("Scenario 6: Multiple routes selects the shortest BFS path")
    void testMultipleRoutesShortestPathSelection() {
        // Setup two alternative paths from Alice to Medical Center:
        // Path A (4 hops): Alice <-> Hub1 <-> Hub2 <-> Hub3 <-> Medical
        // Path B (2 hops): Alice <-> Bob <-> Medical
        // Remove existing security post link to isolate test topology
        graph.disconnect(security, medical);

        SecurityStation hub1 = new SecurityStation("H1", "Relay Hub 1", new Location(20, 50), 100.0);
        SecurityStation hub2 = new SecurityStation("H2", "Relay Hub 2", new Location(40, 50), 100.0);
        SecurityStation hub3 = new SecurityStation("H3", "Relay Hub 3", new Location(60, 50), 100.0);

        graph.addDevice(hub1);
        graph.addDevice(hub2);
        graph.addDevice(hub3);

        // Path A: 4 hops
        graph.connect(alice, hub1);
        graph.connect(hub1, hub2);
        graph.connect(hub2, hub3);
        graph.connect(hub3, medical);

        // Path B: 2 hops (Alice <-> Bob <-> Medical)
        graph.connect(bob, medical);

        topologyPane.refresh();

        SimulationResult result = simulateDispatch(alice, medical, "Route comparison", Priority.NORMAL);

        assertTrue(result.delivered());
        // BFS MUST choose the 2-hop route (Alice -> Bob -> Medical) over the 4-hop route
        assertEquals(3, result.route().size(), "BFS must select the 2-hop path (3 devices)");
        assertEquals(alice, result.route().get(0));
        assertEquals(bob, result.route().get(1));
        assertEquals(medical, result.route().get(2));

        // Verify visualization highlights only Path B
        assertEquals(1, topologyPane.getNodeHopIndex(alice));
        assertEquals(2, topologyPane.getNodeHopIndex(bob));
        assertEquals(3, topologyPane.getNodeHopIndex(medical));

        assertEquals(-1, topologyPane.getNodeHopIndex(hub1));
        assertEquals(-1, topologyPane.getNodeHopIndex(hub2));
        assertEquals(-1, topologyPane.getNodeHopIndex(hub3));

        assertTrue(topologyPane.isLinkHighlighted(alice, bob));
        assertTrue(topologyPane.isLinkHighlighted(bob, medical));
        assertFalse(topologyPane.isLinkHighlighted(alice, hub1));
        assertFalse(topologyPane.isLinkHighlighted(hub1, hub2));
    }

    @Test
    @DisplayName("Scenario 7: Route highlighting properly resets when subsequent dispatch fails")
    void testRouteResetOnNewDispatch() {
        // Step 1: Successful dispatch illuminates route
        SimulationResult result1 = simulateDispatch(alice, medical, "First message", Priority.NORMAL);
        assertTrue(result1.delivered());
        assertEquals(1, topologyPane.getNodeHopIndex(alice));
        assertTrue(topologyPane.isLinkHighlighted(alice, bob));

        // Step 2: Now sever the network by taking Bob offline
        bob.setStatus(DeviceStatus.OFFLINE);

        // Step 3: Second dispatch fails
        SimulationResult result2 = simulateDispatch(alice, medical, "Second message (broken link)", Priority.NORMAL);
        assertFalse(result2.delivered());

        // Step 4: Verify route highlight is completely cleared (no residual highlights)
        assertTrue(topologyPane.getActiveRoute().isEmpty(), "Active route must be empty after failed dispatch");
        assertEquals(-1, topologyPane.getNodeHopIndex(alice));
        assertEquals(-1, topologyPane.getNodeHopIndex(bob));
        assertEquals(-1, topologyPane.getNodeHopIndex(security));
        assertEquals(-1, topologyPane.getNodeHopIndex(medical));

        assertFalse(topologyPane.isLinkHighlighted(alice, bob), "Previous link highlight must be cleared");
        assertFalse(topologyPane.isLinkHighlighted(bob, security), "Previous link highlight must be cleared");
        assertFalse(topologyPane.isLinkHighlighted(security, medical), "Previous link highlight must be cleared");

        // Counters
        assertEquals(2, totalMessagesDispatched);
        assertEquals(1, successfulDeliveries);
        double rate = (successfulDeliveries * 100.0) / totalMessagesDispatched;
        assertEquals(50.0, rate, 0.01);
    }
}
