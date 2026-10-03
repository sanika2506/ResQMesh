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
}
