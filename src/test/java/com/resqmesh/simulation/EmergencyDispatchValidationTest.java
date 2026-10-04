package com.resqmesh.simulation;

import com.resqmesh.model.*;
import com.resqmesh.onboarding.TutorialManager;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationResult.DeliveryOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit and integration tests for Emergency Dispatch:
 * - Outcome categorization: Delivered, No Route Found, Failed
 * - Route hop counting and path formatting
 * - Validation rules: missing required fields, identical endpoints, offline/depleted nodes, disconnected mesh
 * - Dispatch lifecycle and battery impact verification
 */
class EmergencyDispatchValidationTest {

    private NetworkGraph graph;
    private SimulationEngine engine;
    private TutorialManager tutorialManager;

    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;
    private StudentPhone charlie; // Isolated node

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        tutorialManager = new TutorialManager();

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(10, 0), 100.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(20, 0), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(30, 0), 100.0);
        charlie = new StudentPhone("DEV-3", "Charlie's Phone (Isolated)", new Location(100, 100), 100.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(charlie);

        // Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);
    }

    @Test
    @DisplayName("Outcome: Successful delivery classifies as DELIVERED with correct hop count")
    void testDeliveredOutcomeClassification() {
        EmergencyMessage message = new EmergencyMessage(
                "MSG-1", alice, medical, "Immediate ambulance required", Priority.HIGH
        );

        SimulationResult result = engine.send(message);

        assertTrue(result.delivered());
        assertTrue(result.isDelivered());
        assertFalse(result.isNoRouteFound());
        assertFalse(result.isFailed());
        assertEquals(DeliveryOutcome.DELIVERED, result.getOutcome());

        // 4 nodes in route: Alice -> Bob -> Security -> Medical = 3 hops
        assertEquals(4, result.route().size());
        assertEquals(3, result.getHopCount());
        assertEquals(MessageStatus.DELIVERED, message.getStatus());

        // Verify battery was deducted along route
        assertEquals(98.0, alice.getBatteryLevel(), 0.01);
        assertEquals(98.0, bob.getBatteryLevel(), 0.01);
        assertEquals(98.0, security.getBatteryLevel(), 0.01);
        assertEquals(98.0, medical.getBatteryLevel(), 0.01);
        assertEquals(100.0, charlie.getBatteryLevel(), 0.01);
    }

    @Test
    @DisplayName("Outcome: Disconnected recipient classifies specifically as NO_ROUTE_FOUND")
    void testNoRouteFoundOutcomeClassification() {
        EmergencyMessage message = new EmergencyMessage(
                "MSG-2", alice, charlie, "Status check-in", Priority.NORMAL
        );

        SimulationResult result = engine.send(message);

        assertFalse(result.delivered());
        assertFalse(result.isDelivered());
        assertTrue(result.isNoRouteFound(), "Should specifically detect no route found");
        assertFalse(result.isFailed(), "isFailed() should be false for no route found");
        assertEquals(DeliveryOutcome.NO_ROUTE_FOUND, result.getOutcome());
        assertEquals(0, result.getHopCount());
        assertTrue(result.route().isEmpty());
        assertEquals(MessageStatus.FAILED, message.getStatus());

        // Batteries should not be consumed when no route is found
        assertEquals(100.0, alice.getBatteryLevel(), 0.01);
        assertEquals(100.0, charlie.getBatteryLevel(), 0.01);
    }

    @Test
    @DisplayName("Outcome: Offline sender classifies specifically as FAILED")
    void testFailedOutcomeSenderOffline() {
        alice.setStatus(DeviceStatus.OFFLINE);

        EmergencyMessage message = new EmergencyMessage(
                "MSG-3", alice, medical, "Distress ping", Priority.NORMAL
        );

        SimulationResult result = engine.send(message);

        assertFalse(result.delivered());
        assertFalse(result.isNoRouteFound());
        assertTrue(result.isFailed());
        assertEquals(DeliveryOutcome.FAILED, result.getOutcome());
        assertTrue(result.explanation().contains("unavailable"));
    }

    @Test
    @DisplayName("Outcome: Offline recipient classifies specifically as FAILED")
    void testFailedOutcomeRecipientOffline() {
        medical.setStatus(DeviceStatus.OFFLINE);

        EmergencyMessage message = new EmergencyMessage(
                "MSG-4", alice, medical, "Distress ping", Priority.NORMAL
        );

        SimulationResult result = engine.send(message);

        assertFalse(result.delivered());
        assertFalse(result.isNoRouteFound());
        assertTrue(result.isFailed());
        assertEquals(DeliveryOutcome.FAILED, result.getOutcome());
        assertTrue(result.explanation().contains("unavailable"));
    }

    @Test
    @DisplayName("Validation: Missing sender or recipient produces descriptive error")
    void testValidationMissingEndpoints() {
        String errBoth = tutorialManager.getDispatchValidationMessage(null, null, 5, 3);
        assertNotNull(errBoth);
        assertTrue(errBoth.contains("Incomplete Selection"));

        String errSender = tutorialManager.getDispatchValidationMessage(null, medical, 5, 3);
        assertNotNull(errSender);
        assertTrue(errSender.contains("Missing Sender"));

        String errRecipient = tutorialManager.getDispatchValidationMessage(alice, null, 5, 3);
        assertNotNull(errRecipient);
        assertTrue(errRecipient.contains("Missing Recipient"));
    }

    @Test
    @DisplayName("Validation: Identical sender and recipient is blocked")
    void testValidationIdenticalEndpoints() {
        String errSame = tutorialManager.getDispatchValidationMessage(alice, alice, 5, 3);
        assertNotNull(errSame);
        assertTrue(errSame.contains("cannot be the same device"));
    }

    @Test
    @DisplayName("Validation: Offline device selection is flagged with clear instructions")
    void testValidationOfflineDevice() {
        alice.setStatus(DeviceStatus.OFFLINE);
        String errOfflineSender = tutorialManager.getDispatchValidationMessage(alice, medical, 5, 3);
        assertNotNull(errOfflineSender);
        assertTrue(errOfflineSender.contains("Sender Offline"));
        assertTrue(errOfflineSender.contains("Alice's Phone"));
        alice.setStatus(DeviceStatus.ACTIVE);

        medical.setStatus(DeviceStatus.OFFLINE);
        String errOfflineRecipient = tutorialManager.getDispatchValidationMessage(alice, medical, 5, 3);
        assertNotNull(errOfflineRecipient);
        assertTrue(errOfflineRecipient.contains("Recipient Offline"));
        assertTrue(errOfflineRecipient.contains("Medical Center"));
        medical.setStatus(DeviceStatus.ACTIVE);
    }

    @Test
    @DisplayName("Validation: Depleted battery device is flagged with recharge hint")
    void testValidationDepletedBattery() {
        alice.consumeBattery(100.0);
        String errDepleted = tutorialManager.getDispatchValidationMessage(alice, medical, 5, 3);
        assertNotNull(errDepleted);
        assertTrue(errDepleted.contains("Depleted") || errDepleted.contains("Offline"));
        alice.recharge(100.0);
    }

    @Test
    @DisplayName("Validation: Network with zero links blocks dispatch with guidance")
    void testValidationZeroLinks() {
        String errNoLinks = tutorialManager.getDispatchValidationMessage(alice, medical, 5, 0);
        assertNotNull(errNoLinks);
        assertTrue(errNoLinks.contains("Mesh Disconnected"));
    }

    @Test
    @DisplayName("Direct 1-hop delivery computes 1 hop and 2 devices")
    void testDirectHopCount() {
        // Connect Alice directly to Bob
        EmergencyMessage message = new EmergencyMessage(
                "MSG-DIRECT", alice, bob, "Direct peer chat", Priority.LOW
        );

        SimulationResult result = engine.send(message);

        assertTrue(result.delivered());
        assertEquals(2, result.route().size());
        assertEquals(1, result.getHopCount());
        assertEquals(DeliveryOutcome.DELIVERED, result.getOutcome());
    }
}
