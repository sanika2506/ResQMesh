package com.resqmesh.simulation;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.simulation.history.EmergencyHistoryManager;
import com.resqmesh.simulation.timeline.SimulationRecord;
import com.resqmesh.simulation.timeline.SimulationTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression & Integration Tests for ResQMesh Device Lifecycle, Link Disconnection,
 * Node Deletion Cascading, and Emergency Dispatch Integration.
 */
public class DeviceLifecycleIntegrationTest {

    private NetworkGraph graph;
    private SimulationEngine engine;
    private EmergencyHistoryManager historyManager;

    private StudentPhone nodeA;
    private StudentPhone nodeB;
    private SecurityStation nodeC;
    private MedicalStation nodeD;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph);
        historyManager = new EmergencyHistoryManager();

        nodeA = new StudentPhone("DEV-A", "Alpha Phone", new Location(10, 10), 100.0);
        nodeB = new StudentPhone("DEV-B", "Bravo Relay", new Location(20, 20), 100.0);
        nodeC = new SecurityStation("SEC-C", "Charlie Security", new Location(30, 30), 100.0);
        nodeD = new MedicalStation("MED-D", "Delta Hospital", new Location(40, 40), 100.0);

        graph.addDevice(nodeA);
        graph.addDevice(nodeB);
        graph.addDevice(nodeC);
        graph.addDevice(nodeD);

        // Linear path: A <-> B <-> C <-> D
        graph.connect(nodeA, nodeB);
        graph.connect(nodeB, nodeC);
        graph.connect(nodeC, nodeD);
    }

    @Test
    @DisplayName("Lifecycle: Connecting, renaming, and disconnecting devices")
    void testConnectRenameDisconnectLifecycle() {
        assertEquals(4, graph.getDeviceCount());
        assertEquals(3, graph.getTotalLinkCount() / 2);
        assertTrue(graph.areConnected(nodeA, nodeB));
        assertTrue(graph.areConnected(nodeB, nodeC));
        assertTrue(graph.areConnected(nodeC, nodeD));

        // Rename node B
        nodeB.setName("Bravo Relay (Renamed)");
        assertEquals("Bravo Relay (Renamed)", nodeB.getName());
        assertTrue(graph.areConnected(nodeA, nodeB), "Adjacency must be preserved after renaming");

        // Disconnect link between B and C
        boolean disconnected = graph.disconnect(nodeB, nodeC);
        assertTrue(disconnected, "Link between B and C should be successfully severed");
        assertFalse(graph.areConnected(nodeB, nodeC));
        assertEquals(2, graph.getTotalLinkCount() / 2, "Mesh should now have 2 links");

        // Attempting to disconnect an already disconnected link returns false
        assertFalse(graph.disconnect(nodeB, nodeC));
    }

    @Test
    @DisplayName("Lifecycle: Deleting a device cascades and removes all connected links")
    void testDeviceDeletionCascading() {
        assertEquals(3, graph.getTotalLinkCount() / 2);

        // Delete intermediate node C (connected to B and D)
        boolean removed = graph.removeDevice(nodeC);
        assertTrue(removed, "Node C should be successfully deleted");
        assertEquals(3, graph.getDeviceCount(), "Device count should reduce to 3");
        assertNull(graph.getDeviceById("SEC-C"));

        // Links connected to C should be cleaned up automatically
        assertEquals(1, graph.getTotalLinkCount() / 2, "Only link between A and B should remain");
        assertTrue(graph.areConnected(nodeA, nodeB));
        assertFalse(graph.areConnected(nodeB, nodeC));

        // Removing non-existent device returns false
        assertFalse(graph.removeDevice(nodeC));
    }

    @Test
    @DisplayName("Dispatch: Severed route after disconnection falls back or fails gracefully")
    void testDispatchAfterLinkDisconnection() {
        // Initial dispatch: A -> D via B, C
        EmergencyMessage msg1 = new EmergencyMessage("MSG-1", nodeA, nodeD, "Initial distress alert", Priority.NORMAL);
        SimulationResult res1 = engine.send(msg1);
        assertTrue(res1.isDelivered());
        assertEquals(4, res1.route().size());

        // Now disconnect B <-> C
        graph.disconnect(nodeB, nodeC);

        // Dispatch A -> D now has no route
        EmergencyMessage msg2 = new EmergencyMessage("MSG-2", nodeA, nodeD, "Second alert after disconnection", Priority.NORMAL);
        SimulationResult res2 = engine.send(msg2);
        assertTrue(res2.isNoRouteFound());

        // Connect alternate route directly: B <-> D
        graph.connect(nodeB, nodeD);
        EmergencyMessage msg3 = new EmergencyMessage("MSG-3", nodeA, nodeD, "Third alert via alternate route", Priority.NORMAL);
        SimulationResult res3 = engine.send(msg3);
        assertTrue(res3.isDelivered());
        assertEquals(List.of(nodeA, nodeB, nodeD), res3.route());
    }

    @Test
    @DisplayName("Dispatch: Offline or depleted nodes fail validation or routing")
    void testDispatchOfflineAndDepletedNodes() {
        // Toggle node B offline
        nodeB.setStatus(DeviceStatus.OFFLINE);
        assertFalse(nodeB.isAvailable());

        EmergencyMessage msg = new EmergencyMessage("MSG-OFF", nodeA, nodeD, "Test offline relay", Priority.NORMAL);
        SimulationResult res = engine.send(msg);
        assertTrue(res.isNoRouteFound(), "Message cannot route through offline node B");

        // Bring B back online with 0% battery
        nodeB.setStatus(DeviceStatus.ACTIVE);
        nodeB.setBatteryLevel(0.0);
        assertFalse(nodeB.isAvailable(), "0% battery node cannot forward");

        SimulationResult res2 = engine.send(msg);
        assertTrue(res2.isNoRouteFound(), "Message cannot route through depleted node B");

        // Recharge node B to 100%
        nodeB.recharge(100.0);
        assertTrue(nodeB.isAvailable());
        SimulationResult res3 = engine.send(msg);
        assertTrue(res3.isDelivered(), "Message should now be delivered successfully");
    }

    @Test
    @DisplayName("History: Accurate tracking of dispatches across lifecycle changes")
    void testHistoryTrackingAcrossLifecycle() {
        // 1. Success dispatch
        EmergencyMessage msg1 = new EmergencyMessage("MSG-1", nodeA, nodeD, "Successful alert", Priority.NORMAL);
        SimulationResult res1 = engine.send(msg1);
        SimulationRecord rec1 = new SimulationRecord(msg1, res1, SimulationTimeline.fromSimulation(msg1, res1));
        historyManager.addRecord(rec1);

        // 2. Disconnect and run failed dispatch
        graph.disconnect(nodeB, nodeC);
        EmergencyMessage msg2 = new EmergencyMessage("MSG-2", nodeA, nodeD, "Severed alert", Priority.HIGH);
        SimulationResult res2 = engine.send(msg2);
        SimulationRecord rec2 = new SimulationRecord(msg2, res2, SimulationTimeline.fromSimulation(msg2, res2));
        historyManager.addRecord(rec2);

        assertEquals(2, historyManager.getTotalCount());
        assertEquals(1, historyManager.getDeliveredCount());
        assertEquals(1, historyManager.getNoRouteCount());
        assertEquals(50.0, historyManager.getDeliveryRate(), 0.01);

        // Verify device filter matches by node name
        List<SimulationRecord> alphaRecords = historyManager.filter(null, null, "Alpha Phone");
        assertEquals(2, alphaRecords.size());

        // Verify summary report contains diagnostic KPIs
        String summary = historyManager.generateSummaryReport();
        assertTrue(summary.contains("Total Dispatches : 2"));
        assertTrue(summary.contains("Delivered        : 1 (50.0%)"));
    }
}
