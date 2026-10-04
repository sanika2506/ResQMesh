package com.resqmesh.routing;

import com.resqmesh.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test and demonstration for CommunicationLink and NetworkGraph.
 */
class NetworkGraphTest {

    private NetworkGraph graph;
    private StudentPhone phoneA;
    private StudentPhone phoneB;
    private SecurityStation security;
    private MedicalStation medical;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        phoneA = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        phoneB = new StudentPhone("DEV-2", "Bob's Phone", new Location(3, 4), 80.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(10, 10), 100.0);
        medical = new MedicalStation("MED-1", "Triage Station", new Location(20, 20), 100.0);
    }

    @Test
    @DisplayName("CommunicationLink: Distance and usable() status validation")
    void testCommunicationLinkUsability() {
        CommunicationLink link = new CommunicationLink(phoneA, phoneB);

        // Distance: (0,0) to (3,4) = 5.0
        assertEquals(5.0, link.getDistance(), 0.001);
        assertEquals(phoneA, link.getSource());
        assertEquals(phoneB, link.getDestination());
        assertTrue(link.isActive());
        assertTrue(link.usable());

        // Deactivating the link makes it unusable
        link.setActive(false);
        assertFalse(link.usable());
        link.setActive(true);
        assertTrue(link.usable());

        // If source runs out of battery, link becomes unusable
        phoneA.consumeBattery(100.0);
        assertFalse(phoneA.isAvailable());
        assertFalse(link.usable());

        // If source recharges, link becomes usable again
        phoneA.recharge(50.0);
        assertTrue(phoneA.isAvailable());
        assertTrue(link.usable());

        // Self-link is prohibited
        assertThrows(IllegalArgumentException.class, () -> new CommunicationLink(phoneA, phoneA));
    }

    @Test
    @DisplayName("NetworkGraph: Adding devices and querying all devices")
    void testAddDevice() {
        assertTrue(graph.addDevice(phoneA));
        assertTrue(graph.addDevice(phoneB));
        // Duplicate addition returns false
        assertFalse(graph.addDevice(phoneA));

        assertEquals(2, graph.getDeviceCount());
        assertTrue(graph.containsDevice(phoneA));
        assertTrue(graph.containsDevice(phoneB));
        assertFalse(graph.containsDevice(security));
        assertFalse(graph.containsDevice(medical));

        Set<CommunicationDevice> allDevices = graph.getAllDevices();
        assertEquals(2, allDevices.size());
        assertTrue(allDevices.contains(phoneA));
        assertTrue(allDevices.contains(phoneB));

        // Adding null throws IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> graph.addDevice(null));
    }

    @Test
    @DisplayName("NetworkGraph: Bidirectional connections and duplicate prevention")
    void testBidirectionalConnect() {
        // Connect phoneA and phoneB
        assertTrue(graph.connect(phoneA, phoneB));

        // Both devices should automatically be registered in graph
        assertEquals(2, graph.getDeviceCount());

        // Both directions should exist
        assertTrue(graph.hasConnection(phoneA, phoneB));
        assertTrue(graph.hasConnection(phoneB, phoneA));

        // PhoneA should have 1 link pointing to PhoneB
        List<CommunicationLink> linksA = graph.getLinks(phoneA);
        assertEquals(1, linksA.size());
        assertEquals(phoneB, linksA.get(0).getDestination());

        // PhoneB should have 1 link pointing to PhoneA
        List<CommunicationLink> linksB = graph.getLinks(phoneB);
        assertEquals(1, linksB.size());
        assertEquals(phoneA, linksB.get(0).getDestination());

        // Total directed links = 2
        assertEquals(2, graph.getTotalLinkCount());

        // Duplicate connect attempt must return false and not add new links
        assertFalse(graph.connect(phoneA, phoneB));
        assertFalse(graph.connect(phoneB, phoneA));
        assertEquals(1, graph.getLinks(phoneA).size());
        assertEquals(1, graph.getLinks(phoneB).size());
        assertEquals(2, graph.getTotalLinkCount());
    }

    @Test
    @DisplayName("NetworkGraph: Connecting multiple devices and disconnection")
    void testMeshTopology() {
        // Create a 4-node ring mesh: PhoneA <-> PhoneB <-> Security <-> Medical <-> PhoneA
        graph.connect(phoneA, phoneB);
        graph.connect(phoneB, security);
        graph.connect(security, medical);
        graph.connect(medical, phoneA);

        assertEquals(4, graph.getDeviceCount());
        assertEquals(8, graph.getTotalLinkCount()); // 4 pairs * 2 directions = 8 directed links

        // Medical is connected to both Security and PhoneA
        assertEquals(2, graph.getLinks(medical).size());
        assertTrue(graph.hasConnection(medical, phoneA));
        assertTrue(graph.hasConnection(medical, security));

        // PhoneA is connected to both PhoneB and Medical
        assertEquals(2, graph.getLinks(phoneA).size());

        // Disconnect phoneA and phoneB
        assertTrue(graph.disconnect(phoneA, phoneB));
        assertFalse(graph.hasConnection(phoneA, phoneB));
        assertFalse(graph.hasConnection(phoneB, phoneA));
        assertEquals(1, graph.getLinks(phoneA).size());
        assertEquals(6, graph.getTotalLinkCount());
    }

    @Test
    @DisplayName("NetworkGraph: Self-connection and null validation")
    void testInvalidConnections() {
        assertThrows(IllegalArgumentException.class, () -> graph.connect(phoneA, phoneA));
        assertThrows(IllegalArgumentException.class, () -> graph.connect(phoneA, null));
        assertThrows(IllegalArgumentException.class, () -> graph.connect(null, phoneB));
    }

    @Test
    @DisplayName("NetworkGraph: Remove device cascades to remove all associated links")
    void testRemoveDeviceCascadesLinks() {
        graph.connect(phoneA, phoneB);
        graph.connect(phoneB, security);
        graph.connect(security, medical);

        assertEquals(4, graph.getDeviceCount());
        assertEquals(6, graph.getTotalLinkCount()); // 3 bidirectional links = 6 directed

        // Remove phoneB
        assertTrue(graph.removeDevice(phoneB));
        assertEquals(3, graph.getDeviceCount());
        assertFalse(graph.containsDevice(phoneB));
        assertNull(graph.getDeviceById("DEV-2"));

        // phoneA should now have 0 links, security should have only link to medical
        assertEquals(0, graph.getConnectionCount(phoneA));
        assertEquals(1, graph.getConnectionCount(security));
        assertEquals(2, graph.getTotalLinkCount()); // only security <-> medical left
        assertFalse(graph.hasConnection(phoneA, phoneB));
        assertFalse(graph.hasConnection(security, phoneB));

        // Removing non-existent device returns false
        assertFalse(graph.removeDevice(phoneB));
    }

    @Test
    @DisplayName("NetworkGraph: Remove device by ID string")
    void testRemoveDeviceById() {
        graph.addDevice(phoneA);
        graph.addDevice(phoneB);

        assertTrue(graph.removeDeviceById("DEV-1"));
        assertEquals(1, graph.getDeviceCount());
        assertNull(graph.getDeviceById("DEV-1"));

        // Non-existent ID returns false
        assertFalse(graph.removeDeviceById("UNKNOWN-99"));
    }

    @Test
    @DisplayName("NetworkGraph: Update device ID maintains map integrity and detects duplicates")
    void testUpdateDeviceId() {
        graph.connect(phoneA, phoneB);

        // Update ID of phoneA to DEV-100
        assertTrue(graph.updateDeviceId(phoneA, "DEV-100"));
        assertEquals("DEV-100", phoneA.getId());
        assertEquals(phoneA, graph.getDeviceById("DEV-100"));
        assertNull(graph.getDeviceById("DEV-1"));

        // Links should still be preserved
        assertTrue(graph.hasConnection(phoneA, phoneB));
        assertTrue(graph.hasConnection(phoneB, phoneA));

        // Attempting to change to existing ID (DEV-2) should fail
        assertFalse(graph.updateDeviceId(phoneA, "DEV-2"));
        assertEquals("DEV-100", phoneA.getId());

        // Same ID returns true without error
        assertTrue(graph.updateDeviceId(phoneA, "DEV-100"));

        // Null or blank throws
        assertThrows(IllegalArgumentException.class, () -> graph.updateDeviceId(phoneA, ""));
        assertThrows(IllegalArgumentException.class, () -> graph.updateDeviceId(phoneA, null));
    }

    @Test
    @DisplayName("NetworkGraph: Query neighbors and connection counts")
    void testGetNeighborsAndConnectionCount() {
        graph.connect(phoneA, phoneB);
        graph.connect(phoneA, security);

        assertEquals(2, graph.getConnectionCount(phoneA));
        List<CommunicationDevice> neighbors = graph.getNeighbors(phoneA);
        assertEquals(2, neighbors.size());
        assertTrue(neighbors.contains(phoneB));
        assertTrue(neighbors.contains(security));

        assertEquals(0, graph.getConnectionCount(medical));
        assertTrue(graph.getNeighbors(medical).isEmpty());
    }
}
