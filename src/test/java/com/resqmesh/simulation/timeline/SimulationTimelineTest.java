package com.resqmesh.simulation.timeline;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated tests for SimulationTimeline and SimulationRecord (Step 11).
 * Verifies:
 * - Chronological event ordering for delivered transmissions (Created -> Discovered -> Hops -> Delivered)
 * - Chronological event ordering for failed transmissions (Created -> Failed, no false hops)
 * - Route consistency between BFS path and hop forwarding events
 * - Non-destructive historical review (battery levels and network graph unaltered during review)
 * - Single-hop and multi-hop timeline event counts and duration calculations
 */
public class SimulationTimelineTest {

    private NetworkGraph graph;
    private SimulationEngine engine;

    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(20, 20), 100.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(40, 40), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(60, 60), 100.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);

        // Chain: Alice <-> Bob <-> Security <-> Medical (3 hops)
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);
    }

    @Test
    @DisplayName("Timeline: Multi-hop delivery generates strictly ordered sequence of events")
    void testSuccessfulDeliveryEventOrdering() {
        EmergencyMessage msg = new EmergencyMessage("MSG-1", alice, medical, "Critical triage needed", Priority.HIGH);
        SimulationResult result = engine.send(msg);

        assertTrue(result.isDelivered(), "Message should be successfully delivered");
        assertEquals(4, result.getRoute().size(), "Route should traverse 4 nodes (3 hops)");

        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        assertNotNull(timeline);
        assertTrue(timeline.isDelivered());
        assertEquals("MSG-1", timeline.getMessageId());

        List<SimulationEvent> events = timeline.getEvents();
        // Expected events:
        // 1. MESSAGE_CREATED
        // 2. ROUTE_DISCOVERED
        // 3. HOP_FORWARDING (Alice -> Bob)
        // 4. HOP_FORWARDING (Bob -> Security)
        // 5. HOP_FORWARDING (Security -> Medical)
        // 6. MESSAGE_DELIVERED
        assertEquals(6, events.size(), "Multi-hop delivery should have exactly 6 timeline events");

        // Event 1: Created
        assertEquals(1, events.get(0).getSequenceNumber());
        assertEquals(SimulationEventType.MESSAGE_CREATED, events.get(0).getType());
        assertEquals(0, events.get(0).getElapsedMs());
        assertEquals(alice, events.get(0).getSourceDevice());
        assertEquals(medical, events.get(0).getTargetDevice());

        // Event 2: Route Discovered
        assertEquals(2, events.get(1).getSequenceNumber());
        assertEquals(SimulationEventType.ROUTE_DISCOVERED, events.get(1).getType());
        assertTrue(events.get(1).getElapsedMs() > events.get(0).getElapsedMs());

        // Event 3: Hop 1 (Alice -> Bob)
        assertEquals(SimulationEventType.HOP_FORWARDING, events.get(2).getType());
        assertEquals(alice, events.get(2).getSourceDevice());
        assertEquals(bob, events.get(2).getTargetDevice());
        assertEquals(1, events.get(2).getHopIndex());

        // Event 4: Hop 2 (Bob -> Security)
        assertEquals(SimulationEventType.HOP_FORWARDING, events.get(3).getType());
        assertEquals(bob, events.get(3).getSourceDevice());
        assertEquals(security, events.get(3).getTargetDevice());
        assertEquals(2, events.get(3).getHopIndex());

        // Event 5: Hop 3 (Security -> Medical)
        assertEquals(SimulationEventType.HOP_FORWARDING, events.get(4).getType());
        assertEquals(security, events.get(4).getSourceDevice());
        assertEquals(medical, events.get(4).getTargetDevice());
        assertEquals(3, events.get(4).getHopIndex());

        // Event 6: Delivered
        assertEquals(SimulationEventType.MESSAGE_DELIVERED, events.get(5).getType());
        assertEquals(medical, events.get(5).getSourceDevice());
        assertTrue(events.get(5).isTerminal());

        // Verify elapsed times increase strictly monotonically
        for (int i = 0; i < events.size() - 1; i++) {
            assertTrue(events.get(i).getElapsedMs() < events.get(i + 1).getElapsedMs(),
                    "Event elapsed times must increase strictly monotonically");
        }
    }

    @Test
    @DisplayName("Timeline: Failed delivery records failure without intermediate hop events")
    void testFailedDeliveryEventOrdering() {
        // Disconnect Bob from Security to sever the mesh
        graph.disconnect(bob, security);

        EmergencyMessage msg = new EmergencyMessage("MSG-FAIL", alice, medical, "Severed mesh test", Priority.NORMAL);
        SimulationResult result = engine.send(msg);

        assertFalse(result.isDelivered(), "Message delivery should fail due to severed link");

        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        assertFalse(timeline.isDelivered());

        List<SimulationEvent> events = timeline.getEvents();
        assertEquals(2, events.size(), "Failed delivery timeline should contain only Created and Delivery Failed events");

        // Event 1: Created
        assertEquals(SimulationEventType.MESSAGE_CREATED, events.get(0).getType());
        assertEquals(0, events.get(0).getElapsedMs());

        // Event 2: Delivery Failed
        assertEquals(SimulationEventType.DELIVERY_FAILED, events.get(1).getType());
        assertTrue(events.get(1).isTerminal());
        assertTrue(events.get(1).getDescription().contains("Delivery failed"));

        // No hop events must exist
        boolean hasHop = events.stream().anyMatch(e -> e.getType() == SimulationEventType.HOP_FORWARDING);
        assertFalse(hasHop, "Failed delivery timeline must NEVER contain hop forwarding events");
    }

    @Test
    @DisplayName("Timeline: Hop forwarding events exactly reflect BFS route pairs")
    void testRouteConsistencyWithBFS() {
        EmergencyMessage msg = new EmergencyMessage("MSG-ROUTE", alice, medical, "Route integrity check", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);

        List<CommunicationDevice> bfsRoute = result.getRoute();
        List<SimulationEvent> hopEvents = timeline.getEvents().stream()
                .filter(e -> e.getType() == SimulationEventType.HOP_FORWARDING)
                .toList();

        assertEquals(bfsRoute.size() - 1, hopEvents.size(), "Number of hop events must match BFS link count");

        for (int i = 0; i < hopEvents.size(); i++) {
            SimulationEvent hop = hopEvents.get(i);
            assertEquals(bfsRoute.get(i), hop.getSourceDevice(),
                    "Hop source device must match BFS route node at index " + i);
            assertEquals(bfsRoute.get(i + 1), hop.getTargetDevice(),
                    "Hop target device must match BFS route node at index " + (i + 1));
        }
    }

    @Test
    @DisplayName("Timeline: Single-hop direct transmission timeline structure")
    void testSingleHopDeliveryTimeline() {
        EmergencyMessage msg = new EmergencyMessage("MSG-DIRECT", alice, bob, "Direct line", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);

        // 1. Created, 2. Route, 3. Hop 1 (Alice -> Bob), 4. Delivered
        assertEquals(4, timeline.size());
        assertEquals(SimulationEventType.HOP_FORWARDING, timeline.getEvent(2).getType());
        assertEquals(alice, timeline.getEvent(2).getSourceDevice());
        assertEquals(bob, timeline.getEvent(2).getTargetDevice());
    }

    @Test
    @DisplayName("Timeline: Historical review does not alter device battery levels")
    void testReviewPreviousSimulationDoesNotAlterBattery() {
        EmergencyMessage msg = new EmergencyMessage("MSG-HIST", alice, medical, "Battery invariance test", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        SimulationRecord record = new SimulationRecord(msg, result, timeline);

        // Record battery states right after transmission
        double aliceBattery = alice.getBatteryLevel();
        double bobBattery = bob.getBatteryLevel();
        double secBattery = security.getBatteryLevel();
        double medBattery = medical.getBatteryLevel();

        // Perform multiple reviews and inspection of timeline events
        for (int i = 0; i < 5; i++) {
            assertNotNull(record.getSummary());
            assertEquals(timeline.size(), record.getTimeline().size());
            for (SimulationEvent event : record.getTimeline().getEvents()) {
                assertNotNull(event.getDescription());
                assertNotNull(event.getSourceDeviceName());
            }
        }

        // Verify battery levels have NOT changed at all during review
        assertEquals(aliceBattery, alice.getBatteryLevel(), 0.0001, "Alice battery must not change during review");
        assertEquals(bobBattery, bob.getBatteryLevel(), 0.0001, "Bob battery must not change during review");
        assertEquals(secBattery, security.getBatteryLevel(), 0.0001, "Security battery must not change during review");
        assertEquals(medBattery, medical.getBatteryLevel(), 0.0001, "Medical battery must not change during review");
    }

    @Test
    @DisplayName("Timeline: SimulationRecord summary formatting")
    void testSimulationRecordSummaryFormatting() {
        EmergencyMessage msg = new EmergencyMessage("MSG-SUMM", alice, medical, "Summary test", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        SimulationRecord record = new SimulationRecord(msg, result, timeline);

        String summary = record.getSummary();
        assertTrue(summary.contains("[MSG-SUMM]"));
        assertTrue(summary.contains("Alice's Phone"));
        assertTrue(summary.contains("Medical Center"));
        assertTrue(summary.contains("DELIVERED"));
        assertTrue(summary.contains("3 hops"));
    }

    @Test
    @DisplayName("Timeline: Defensive immutability of events list")
    void testDefensiveImmutability() {
        EmergencyMessage msg = new EmergencyMessage("MSG-IMMUTABLE", alice, bob, "Immutable", Priority.NORMAL);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);

        assertThrows(UnsupportedOperationException.class, () -> {
            timeline.getEvents().add(new SimulationEvent(99, 9999, SimulationEventType.DELIVERY_FAILED, alice, bob, "Hacked", 0, 0));
        }, "Timeline events list must be unmodifiable");

        assertThrows(UnsupportedOperationException.class, () -> {
            timeline.getRoute().add(alice);
        }, "Timeline route list must be unmodifiable");
    }
}
