package com.resqmesh.simulation;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying SimulationEngine functionality:
 * - Multi-hop message delivery and battery deduction
 * - Failure when no route exists
 * - Failure when sender or recipient is offline
 * - Failure handling without application crashes
 */
class SimulationEngineTest {

    private NetworkGraph graph;
    private SimulationEngine engine;

    private StudentPhone alicePhone;
    private StudentPhone bobPhone;
    private SecurityStation securityPost;
    private MedicalStation medicalCenter;
    private StudentPhone charliePhone;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());

        alicePhone = new StudentPhone("DEV-A", "Alice's Phone", new Location(0, 0), 100.0);
        bobPhone = new StudentPhone("DEV-B", "Bob's Phone", new Location(5, 0), 100.0);
        securityPost = new SecurityStation("SEC-1", "Security Post", new Location(5, 5), 100.0);
        medicalCenter = new MedicalStation("MED-1", "Medical Center", new Location(10, 5), 100.0);
        charliePhone = new StudentPhone("DEV-C", "Charlie's Phone (Isolated)", new Location(50, 50), 100.0);

        graph.addDevice(alicePhone);
        graph.addDevice(bobPhone);
        graph.addDevice(securityPost);
        graph.addDevice(medicalCenter);
        graph.addDevice(charliePhone);

        // Topology:
        // Alice <---> Bob <---> Security <---> Medical
        graph.connect(alicePhone, bobPhone);
        graph.connect(bobPhone, securityPost);
        graph.connect(securityPost, medicalCenter);
    }

    @Test
    @DisplayName("Simulation: Successful multi-hop message delivery and battery deduction")
    void testSuccessfulMultiHopDelivery() {
        EmergencyMessage msg = new EmergencyMessage(
                "MSG-101",
                alicePhone,
                medicalCenter,
                "Requesting first aid supplies at Sector 2",
                Priority.NORMAL
        );

        assertEquals(MessageStatus.CREATED, msg.getStatus());
        assertEquals(100.0, alicePhone.getBatteryLevel(), 0.01);
        assertEquals(100.0, bobPhone.getBatteryLevel(), 0.01);
        assertEquals(100.0, securityPost.getBatteryLevel(), 0.01);
        assertEquals(100.0, medicalCenter.getBatteryLevel(), 0.01);

        SimulationResult result = engine.send(msg);

        // Verify result
        assertTrue(result.delivered(), "Message should be successfully delivered");
        assertEquals(MessageStatus.DELIVERED, msg.getStatus());
        assertNotNull(result.route());
        assertEquals(4, result.route().size(), "Route should consist of 4 devices (3 hops)");
        assertEquals(alicePhone, result.route().get(0));
        assertEquals(bobPhone, result.route().get(1));
        assertEquals(securityPost, result.route().get(2));
        assertEquals(medicalCenter, result.route().get(3));
        assertTrue(result.explanation().contains("successfully delivered"));

        // Verify battery deduction: 2.0% deducted for each device on the 4-node path
        assertEquals(98.0, alicePhone.getBatteryLevel(), 0.01);
        assertEquals(98.0, bobPhone.getBatteryLevel(), 0.01);
        assertEquals(98.0, securityPost.getBatteryLevel(), 0.01);
        assertEquals(98.0, medicalCenter.getBatteryLevel(), 0.01);
    }

    @Test
    @DisplayName("Simulation: Failure when no route exists to isolated recipient")
    void testFailureNoRoute() {
        EmergencyMessage msg = new EmergencyMessage(
                "MSG-102",
                alicePhone,
                charliePhone, // charliePhone has no connections
                "Are you safe?",
                Priority.NORMAL
        );

        SimulationResult result = engine.send(msg);

        assertFalse(result.delivered(), "Delivery should fail because no route exists");
        assertEquals(MessageStatus.FAILED, msg.getStatus());
        assertTrue(result.route().isEmpty(), "Failed result should contain an empty route");
        assertTrue(result.explanation().contains("No valid route found"));

        // Batteries should NOT be reduced since no transmission happened
        assertEquals(100.0, alicePhone.getBatteryLevel(), 0.01);
        assertEquals(100.0, charliePhone.getBatteryLevel(), 0.01);
    }

    @Test
    @DisplayName("Simulation: Failure when sender is offline")
    void testFailureSenderOffline() {
        // Drain sender battery completely
        alicePhone.consumeBattery(100.0);
        assertEquals(DeviceStatus.OFFLINE, alicePhone.getStatus());
        assertFalse(alicePhone.isAvailable());

        EmergencyMessage msg = new EmergencyMessage(
                "MSG-103",
                alicePhone,
                medicalCenter,
                "Emergency alert from drained phone",
                Priority.HIGH
        );

        SimulationResult result = engine.send(msg);

        assertFalse(result.delivered());
        assertEquals(MessageStatus.FAILED, msg.getStatus());
        assertTrue(result.route().isEmpty());
        assertTrue(result.explanation().contains("Sender 'Alice's Phone' is unavailable"));
    }

    @Test
    @DisplayName("Simulation: Failure when recipient is offline")
    void testFailureRecipientOffline() {
        // Set recipient to OFFLINE
        medicalCenter.setStatus(DeviceStatus.OFFLINE);
        assertFalse(medicalCenter.isAvailable());

        EmergencyMessage msg = new EmergencyMessage(
                "MSG-104",
                alicePhone,
                medicalCenter,
                "Hospital triage request",
                Priority.NORMAL
        );

        SimulationResult result = engine.send(msg);

        assertFalse(result.delivered());
        assertEquals(MessageStatus.FAILED, msg.getStatus());
        assertTrue(result.route().isEmpty());
        assertTrue(result.explanation().contains("Recipient 'Medical Center' is unavailable"));
    }

    @Test
    @DisplayName("Simulation: Clean handling of null message without crashing")
    void testNullMessageHandling() {
        SimulationResult result = engine.send(null);

        assertNotNull(result);
        assertFalse(result.delivered());
        assertTrue(result.route().isEmpty());
        assertTrue(result.explanation().contains("Cannot send a null"));
    }
}
