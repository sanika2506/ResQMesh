package com.resqmesh.routing;

import com.resqmesh.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test and demonstration for ShortestPathStrategy (BFS Routing).
 * Verifies direct paths, multi-hop routes, unreachable targets,
 * same-device routing, and priority-based forwarding constraints.
 */
class ShortestPathStrategyTest {

    private NetworkGraph graph;
    private RoutingStrategy router;

    private StudentPhone phoneA;
    private StudentPhone phoneB;
    private SecurityStation security;
    private MedicalStation medical;
    private StudentPhone isolatedPhone;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        router = new ShortestPathStrategy();

        // 4-device connected network + 1 isolated device
        phoneA = new StudentPhone("DEV-A", "Alice's Phone", new Location(0, 0), 100.0);
        phoneB = new StudentPhone("DEV-B", "Bob's Phone", new Location(10, 0), 90.0);
        security = new SecurityStation("SEC-1", "Central Security Hub", new Location(0, 10), 100.0);
        medical = new MedicalStation("MED-1", "Field Hospital", new Location(10, 10), 100.0);
        isolatedPhone = new StudentPhone("DEV-ISO", "Isolated Phone", new Location(100, 100), 50.0);

        graph.addDevice(phoneA);
        graph.addDevice(phoneB);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(isolatedPhone);

        // Topology:
        // PhoneA <-----> PhoneB
        //   ^              ^
        //   |              |
        // Security <--> Medical
        graph.connect(phoneA, phoneB);
        graph.connect(phoneA, security);
        graph.connect(security, medical);
        graph.connect(phoneB, medical);
    }

    @Test
    @DisplayName("Direct Route: 1-hop path between adjacent devices")
    void testDirectRoute() {
        List<CommunicationDevice> route = router.findRoute(phoneA, phoneB, graph, Priority.NORMAL);

        assertNotNull(route);
        assertEquals(2, route.size());
        assertEquals(phoneA, route.get(0));
        assertEquals(phoneB, route.get(1));
    }

    @Test
    @DisplayName("Multi-Hop Route: BFS finds shortest 2-hop route across mesh")
    void testMultiHopRoute() {
        // Disconnect direct link phoneA <-> phoneB to force multi-hop routing
        graph.disconnect(phoneA, phoneB);

        // Route must now travel: PhoneA -> Security -> Medical -> PhoneB (or similar shortest path)
        List<CommunicationDevice> route = router.findRoute(phoneA, medical, graph, Priority.NORMAL);

        assertNotNull(route);
        assertEquals(3, route.size()); // PhoneA -> Security -> Medical
        assertEquals(phoneA, route.get(0));
        assertEquals(security, route.get(1));
        assertEquals(medical, route.get(2));
    }

    @Test
    @DisplayName("Unreachable Destination: Returns empty list when no path exists")
    void testUnreachableDestination() {
        // isolatedPhone has no links in the graph
        List<CommunicationDevice> route = router.findRoute(phoneA, isolatedPhone, graph, Priority.HIGH);

        assertNotNull(route);
        assertTrue(route.isEmpty(), "Route to isolated device should be empty");
    }

    @Test
    @DisplayName("Same Device: Source and destination are identical")
    void testSameDeviceRoute() {
        List<CommunicationDevice> route = router.findRoute(phoneA, phoneA, graph, Priority.LOW);

        assertNotNull(route);
        assertEquals(1, route.size());
        assertEquals(phoneA, route.get(0));
    }

    @Test
    @DisplayName("Forwarding Rule Constraint: StudentPhone cannot relay CRITICAL messages")
    void testPriorityForwardingConstraint() {
        // Setup linear chain: PhoneA <---> PhoneB <---> Medical
        // Disconnect all other links
        graph.disconnect(phoneA, security);
        graph.disconnect(phoneB, medical);
        graph.disconnect(security, medical);
        graph.connect(phoneB, medical);

        // Chain is now: PhoneA <-> PhoneB <-> Medical
        // Case 1: NORMAL priority message -> PhoneB CAN forward
        List<CommunicationDevice> normalRoute = router.findRoute(phoneA, medical, graph, Priority.NORMAL);
        assertEquals(3, normalRoute.size());
        assertEquals(phoneA, normalRoute.get(0));
        assertEquals(phoneB, normalRoute.get(1));
        assertEquals(medical, normalRoute.get(2));

        // Case 2: CRITICAL priority message -> PhoneB (StudentPhone) CANNOT relay CRITICAL
        List<CommunicationDevice> criticalRoute = router.findRoute(phoneA, medical, graph, Priority.CRITICAL);
        assertTrue(criticalRoute.isEmpty(), "StudentPhone cannot relay CRITICAL messages, so no route should exist");

        // Case 3: Introduce SecurityStation as bypass: PhoneA <-> Security <-> Medical
        graph.connect(phoneA, security);
        graph.connect(security, medical);
        List<CommunicationDevice> bypassRoute = router.findRoute(phoneA, medical, graph, Priority.CRITICAL);
        assertEquals(3, bypassRoute.size());
        assertEquals(phoneA, bypassRoute.get(0));
        assertEquals(security, bypassRoute.get(1)); // Relayed via SecurityStation
        assertEquals(medical, bypassRoute.get(2));
    }

    @Test
    @DisplayName("Dynamic Link State: Deactivated or depleted devices are skipped")
    void testUnavailableDeviceBypass() {
        // Topology: PhoneA -> PhoneB -> Medical
        // And bypass: PhoneA -> Security -> Medical
        graph.disconnect(phoneA, phoneB);

        // Security station battery dies (depleted to 0)
        security.consumeBattery(100.0);
        assertFalse(security.isAvailable());

        // Without Security available, no path to Medical
        List<CommunicationDevice> route = router.findRoute(phoneA, medical, graph, Priority.NORMAL);
        assertTrue(route.isEmpty());

        // Reconnect PhoneB: PhoneA <-> PhoneB <-> Medical
        graph.connect(phoneA, phoneB);
        List<CommunicationDevice> rerouted = router.findRoute(phoneA, medical, graph, Priority.NORMAL);
        assertEquals(3, rerouted.size());
        assertEquals(phoneA, rerouted.get(0));
        assertEquals(phoneB, rerouted.get(1)); // Rerouted through PhoneB!
        assertEquals(medical, rerouted.get(2));
    }
}
