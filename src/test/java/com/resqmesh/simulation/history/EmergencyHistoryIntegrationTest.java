package com.resqmesh.simulation.history;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import com.resqmesh.simulation.SimulationResult.DeliveryOutcome;
import com.resqmesh.simulation.timeline.SimulationRecord;
import com.resqmesh.simulation.timeline.SimulationTimeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Emergency History and Reports Integration Tests")
public class EmergencyHistoryIntegrationTest {

    private NetworkGraph graph;
    private SimulationEngine engine;
    private ObservableList<SimulationRecord> sessionHistory;
    private EmergencyHistoryManager historyManager;

    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;
    private StudentPhone isolated;

    @BeforeAll
    static void initJavaFX() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
            // Already initialized
        }
    }

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        sessionHistory = FXCollections.observableArrayList();
        historyManager = new EmergencyHistoryManager(sessionHistory);

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(10, 10), 100.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(30, 20), 100.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(50, 40), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(70, 60), 100.0);
        isolated = new StudentPhone("DEV-3", "Isolated Node", new Location(90, 80), 80.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(isolated);

        // Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);
    }

    private SimulationRecord dispatch(String msgId, CommunicationDevice sender, CommunicationDevice recipient,
                                      String content, Priority priority) {
        EmergencyMessage msg = new EmergencyMessage(msgId, sender, recipient, content, priority);
        SimulationResult result = engine.send(msg);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        SimulationRecord record = new SimulationRecord(msg, result, timeline);
        historyManager.addRecord(record);
        return record;
    }

    @Test
    @DisplayName("Successful multi-hop dispatch automatically creates a complete history record")
    void testSuccessfulDispatchHistoryRecording() {
        SimulationRecord record = dispatch("MSG-100", alice, medical, "Severe injury in Lab 3", Priority.NORMAL);

        assertEquals(1, historyManager.getTotalCount());
        assertEquals(1, sessionHistory.size());

        SimulationRecord retrieved = historyManager.getRecords().get(0);
        assertSame(record, retrieved);
        assertEquals("MSG-100", retrieved.getId());
        assertEquals("Alice's Phone", retrieved.getSenderName());
        assertEquals("Medical Center", retrieved.getRecipientName());
        assertEquals("Severe injury in Lab 3", retrieved.getMessageContent());
        assertEquals("NORMAL", retrieved.getPriorityName());
        assertTrue(retrieved.isDelivered());
        assertEquals(DeliveryOutcome.DELIVERED, retrieved.getOutcome());
        assertEquals("Delivered", retrieved.getStatusDisplay());
        assertEquals(3, retrieved.getHopCount()); // Alice -> Bob -> Security -> Medical (3 hops)
        assertTrue(retrieved.getRoutePath().contains("Alice's Phone ──▶ Bob's Phone ──▶ Security Post ──▶ Medical Center"));
        assertNotNull(retrieved.getTimestamp());
    }

    @Test
    @DisplayName("No route found dispatch records correct outcome and status")
    void testNoRouteDispatchHistoryRecording() {
        // Alice to Isolated node (no connecting links)
        SimulationRecord record = dispatch("MSG-200", alice, isolated, "Checking isolated node", Priority.NORMAL);

        assertEquals(1, historyManager.getTotalCount());
        assertFalse(record.isDelivered());
        assertEquals(DeliveryOutcome.NO_ROUTE_FOUND, record.getOutcome());
        assertEquals("No Route Found", record.getStatusDisplay());
        assertEquals(0, record.getHopCount());
        assertEquals(1, historyManager.getNoRouteCount());
        assertEquals(0, historyManager.getDeliveredCount());
        assertEquals(0.0, historyManager.getDeliveryRate(), 0.001);
    }

    @Test
    @DisplayName("Failed dispatch due to offline endpoint records failed outcome")
    void testFailedDispatchHistoryRecording() {
        medical.setStatus(DeviceStatus.OFFLINE);

        SimulationRecord record = dispatch("MSG-300", alice, medical, "Critical alert", Priority.CRITICAL);

        assertEquals(1, historyManager.getTotalCount());
        assertFalse(record.isDelivered());
        assertEquals(DeliveryOutcome.FAILED, record.getOutcome());
        assertEquals("Failed", record.getStatusDisplay());
        assertEquals(1, historyManager.getFailedCount());
        assertEquals(0, historyManager.getNoRouteCount());
    }

    @Test
    @DisplayName("FilteredList with EmergencyHistoryManager filters live simulation records")
    void testFilteredHistoryListIntegration() {
        dispatch("MSG-01", alice, medical, "First aid needed at Quad", Priority.HIGH);
        dispatch("MSG-02", alice, isolated, "Ping isolated device", Priority.NORMAL);
        dispatch("MSG-03", bob, security, "Suspicious activity near gate", Priority.HIGH);

        FilteredList<SimulationRecord> filtered = new FilteredList<>(sessionHistory, p -> true);
        assertEquals(3, filtered.size());

        // 1. Filter by keyword "Quad"
        filtered.setPredicate(historyManager.createFilterPredicate("Quad", null, null));
        assertEquals(1, filtered.size());
        assertEquals("MSG-01", filtered.get(0).getId());

        // 2. Filter by status "DELIVERED"
        filtered.setPredicate(historyManager.createFilterPredicate(null, "DELIVERED", null));
        assertEquals(2, filtered.size()); // MSG-01 and MSG-03

        // 3. Filter by status "NO ROUTE FOUND"
        filtered.setPredicate(historyManager.createFilterPredicate(null, "NO ROUTE", null));
        assertEquals(1, filtered.size());
        assertEquals("MSG-02", filtered.get(0).getId());

        // 4. Filter by device "Bob's Phone"
        filtered.setPredicate(historyManager.createFilterPredicate(null, null, "Bob's Phone"));
        assertEquals(1, filtered.size());
        assertEquals("MSG-03", filtered.get(0).getId());

        // 5. Reset filter
        filtered.setPredicate(historyManager.createFilterPredicate(null, "ALL", "ALL"));
        assertEquals(3, filtered.size());
    }

    @Test
    @DisplayName("Summary KPI calculations and report generation match live dispatch records")
    void testSummaryReportAndKpis() {
        dispatch("MSG-01", alice, medical, "Med SOS", Priority.HIGH);
        dispatch("MSG-02", bob, security, "Security SOS", Priority.NORMAL);
        dispatch("MSG-03", alice, isolated, "Lost SOS", Priority.NORMAL);

        assertEquals(3, historyManager.getTotalCount());
        assertEquals(2, historyManager.getDeliveredCount());
        assertEquals(1, historyManager.getNoRouteCount());
        assertEquals(0, historyManager.getFailedCount());
        assertEquals(66.66, historyManager.getDeliveryRate(), 0.1);

        String report = historyManager.generateSummaryReport();
        assertTrue(report.contains("Total Dispatches : 3"));
        assertTrue(report.contains("Delivered        : 2 (66.7%)"));
        assertTrue(report.contains("No Route Found   : 1"));
        assertTrue(report.contains("MSG-01"));
        assertTrue(report.contains("MSG-02"));
        assertTrue(report.contains("MSG-03"));

        // Clear history
        historyManager.clear();
        assertTrue(sessionHistory.isEmpty());
        assertEquals(0, historyManager.getTotalCount());
        assertEquals(0, historyManager.getDeliveredCount());
        assertEquals(0.0, historyManager.getDeliveryRate(), 0.001);
    }
}
