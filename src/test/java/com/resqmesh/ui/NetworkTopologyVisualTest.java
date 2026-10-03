package com.resqmesh.ui;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.DeviceStatus;
import com.resqmesh.model.Location;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.SecurityStation;
import com.resqmesh.model.StudentPhone;
import com.resqmesh.routing.NetworkGraph;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Unit & Visual Integration Tests for NetworkTopologyPane.
 * Verifies:
 * - Registered devices render as visual nodes
 * - Active connections render as dynamic links
 * - Online vs Offline node styles
 * - Node Inspector HUD updates on selection
 * - BFS route illumination with sequenced hop badges (#1, #2, ...)
 * - Route reset and graph synchronization
 */
public class NetworkTopologyVisualTest {

    private NetworkGraph graph;
    private NetworkTopologyPane topologyPane;

    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;
    private StudentPhone charlie;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // JavaFX Platform already initialized
        }
    }

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();

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

        // Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        topologyPane = new NetworkTopologyPane(graph);
        topologyPane.refresh();
    }

    @Test
    @DisplayName("Topology: All 5 devices and 3 links are visually rendered")
    void testInitialRendering() {
        assertEquals(5, topologyPane.getDeviceNodeMap().size(), "Canvas should contain visuals for all 5 registered devices");
        assertEquals(3, topologyPane.getLinkVisuals().size(), "Canvas should contain 3 active bidirectional communication links");

        assertNotNull(topologyPane.getNodeVisual(alice));
        assertNotNull(topologyPane.getNodeVisual(bob));
        assertNotNull(topologyPane.getNodeVisual(security));
        assertNotNull(topologyPane.getNodeVisual(medical));
        assertNotNull(topologyPane.getNodeVisual(charlie));
    }

    @Test
    @DisplayName("Topology: Online vs Offline devices have distinct visual styling")
    void testOnlineVsOfflineVisualStyles() {
        NetworkTopologyPane.NodeVisual aliceVisual = topologyPane.getNodeVisual(alice);
        NetworkTopologyPane.NodeVisual charlieVisual = topologyPane.getNodeVisual(charlie);

        // Initially both are ONLINE
        assertEquals(1.0, aliceVisual.getOpacity(), 0.01);
        assertEquals(1.0, charlieVisual.getOpacity(), 0.01);

        // Toggle Charlie to OFFLINE
        charlie.setStatus(DeviceStatus.OFFLINE);
        charlieVisual.updateNodeStyle();

        // Charlie should now have dimmed opacity and offline styling
        assertEquals(0.70, charlieVisual.getOpacity(), 0.01);
        assertEquals(1.0, aliceVisual.getOpacity(), 0.01);
    }

    @Test
    @DisplayName("Topology: Node Inspector displays correct device telemetry on selection")
    void testNodeInspectorSelection() {
        topologyPane.selectDevice(security);

        assertEquals(security, topologyPane.getSelectedDevice());
        assertTrue(topologyPane.isInspectorVisible(), "Inspector card HUD should be visible");
        assertEquals("Security Post", topologyPane.getInspectorTitle());
        assertTrue(topologyPane.getInspectorId().contains("SEC-1"));
        assertTrue(topologyPane.getInspectorType().contains("Security Station"));
        assertTrue(topologyPane.getInspectorBattery().contains("100.0%"));
        assertTrue(topologyPane.getInspectorStatus().contains("ONLINE"));
        assertTrue(topologyPane.getInspectorPeers().contains("2 peers")); // Connected to Bob and Medical
    }

    @Test
    @DisplayName("Topology: Highlighting BFS route illuminates sequential links and hop badges")
    void testRouteHighlighting() {
        List<CommunicationDevice> route = Arrays.asList(alice, bob, security, medical);

        topologyPane.highlightRoute(route);

        assertEquals(route, topologyPane.getActiveRoute());

        // Verify hop sequence numbers: Alice (#1), Bob (#2), Security (#3), Medical (#4)
        assertEquals(1, topologyPane.getNodeHopIndex(alice));
        assertEquals(2, topologyPane.getNodeHopIndex(bob));
        assertEquals(3, topologyPane.getNodeHopIndex(security));
        assertEquals(4, topologyPane.getNodeHopIndex(medical));

        // Isolated Charlie is not in route
        assertEquals(-1, topologyPane.getNodeHopIndex(charlie));

        // Verify sequential links are highlighted
        assertTrue(topologyPane.isLinkHighlighted(alice, bob), "Link Alice <-> Bob should be highlighted");
        assertTrue(topologyPane.isLinkHighlighted(bob, security), "Link Bob <-> Security should be highlighted");
        assertTrue(topologyPane.isLinkHighlighted(security, medical), "Link Security <-> Medical should be highlighted");

        // Clear route highlight
        topologyPane.clearRouteHighlight();
        assertTrue(topologyPane.getActiveRoute().isEmpty());
        assertEquals(-1, topologyPane.getNodeHopIndex(alice));
        assertEquals(-1, topologyPane.getNodeHopIndex(bob));
        assertFalse(topologyPane.isLinkHighlighted(alice, bob));
    }

    @Test
    @DisplayName("Topology: Adding device or link updates visualization dynamically")
    void testGraphMutationSynchronization() {
        StudentPhone david = new StudentPhone("DEV-4", "David's Phone", new Location(25, 25), 90.0);
        graph.addDevice(david);
        graph.connect(alice, david);

        topologyPane.refresh();

        assertEquals(6, topologyPane.getDeviceNodeMap().size(), "Device count should increase to 6");
        assertEquals(4, topologyPane.getLinkVisuals().size(), "Link count should increase to 4");
        assertTrue(topologyPane.isLinkHighlighted(alice, david) == false, "New link should exist and be unhighlighted by default");
        assertNotNull(topologyPane.getNodeVisual(david));
    }
}
