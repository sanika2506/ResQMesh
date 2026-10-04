package com.resqmesh.simulation.history;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.model.Location;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.Priority;
import com.resqmesh.model.SecurityStation;
import com.resqmesh.model.StudentPhone;
import com.resqmesh.simulation.SimulationResult;
import com.resqmesh.simulation.timeline.SimulationRecord;
import com.resqmesh.simulation.timeline.SimulationTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Emergency History and Reports Manager Tests")
class EmergencyHistoryManagerTest {

    private EmergencyHistoryManager historyManager;
    private StudentPhone phone;
    private SecurityStation security;
    private MedicalStation hospital;

    @BeforeEach
    void setUp() {
        historyManager = new EmergencyHistoryManager();
        phone = new StudentPhone("DEV-1", "Student Phone", new Location(10, 10), 85.0);
        security = new SecurityStation("DEV-2", "Campus Security", new Location(20, 20), 90.0);
        hospital = new MedicalStation("DEV-3", "Field Hospital", new Location(30, 30), 95.0);
    }

    private SimulationRecord createRecord(String id,
                                          CommunicationDevice sender,
                                          CommunicationDevice recipient,
                                          String content,
                                          Priority priority,
                                          boolean delivered,
                                          String explanation,
                                          List<CommunicationDevice> route) {
        EmergencyMessage msg = new EmergencyMessage(id, sender, recipient, content, priority);
        SimulationResult result = new SimulationResult(delivered, explanation, route);
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(msg, result);
        return new SimulationRecord(msg, result, timeline);
    }

    @Test
    @DisplayName("Initial state is empty")
    void testInitialState() {
        assertTrue(historyManager.isEmpty());
        assertEquals(0, historyManager.getTotalCount());
        assertEquals(0, historyManager.getDeliveredCount());
        assertEquals(0, historyManager.getFailedCount());
        assertEquals(0, historyManager.getNoRouteCount());
        assertEquals(0.0, historyManager.getDeliveryRate(), 0.001);
        assertEquals(0.0, historyManager.getAverageHops(), 0.001);
    }

    @Test
    @DisplayName("Records are prepended (newest at index 0)")
    void testAddRecordOrder() {
        SimulationRecord r1 = createRecord("MSG-1", phone, security, "SOS 1", Priority.HIGH, true, "Delivered", List.of(phone, security));
        SimulationRecord r2 = createRecord("MSG-2", security, hospital, "SOS 2", Priority.CRITICAL, true, "Delivered", List.of(security, hospital));

        historyManager.addRecord(r1);
        historyManager.addRecord(r2);

        assertEquals(2, historyManager.getTotalCount());
        assertEquals("MSG-2", historyManager.getRecords().get(0).getId());
        assertEquals("MSG-1", historyManager.getRecords().get(1).getId());
    }

    @Test
    @DisplayName("Statistics calculation for delivered, failed, and no-route outcomes")
    void testStatisticsCalculations() {
        // 1. Delivered 2 hops
        SimulationRecord r1 = createRecord("MSG-1", phone, hospital, "Medical SOS", Priority.CRITICAL, true,
                "Delivered via relay", List.of(phone, security, hospital));
        // 2. Delivered 1 hop
        SimulationRecord r2 = createRecord("MSG-2", phone, security, "Hazard Alert", Priority.NORMAL, true,
                "Direct connection", List.of(phone, security));
        // 3. No Route Found
        SimulationRecord r3 = createRecord("MSG-3", phone, hospital, "Evac Request", Priority.HIGH, false,
                "No feasible route found across active nodes to destination.", List.of());
        // 4. Failed
        SimulationRecord r4 = createRecord("MSG-4", phone, security, "Offline Test", Priority.NORMAL, false,
                "Transmission failed: recipient is offline.", List.of());

        historyManager.addRecord(r1);
        historyManager.addRecord(r2);
        historyManager.addRecord(r3);
        historyManager.addRecord(r4);

        assertEquals(4, historyManager.getTotalCount());
        assertEquals(2, historyManager.getDeliveredCount());
        assertEquals(1, historyManager.getNoRouteCount());
        assertEquals(1, historyManager.getFailedCount());
        assertEquals(50.0, historyManager.getDeliveryRate(), 0.001);

        // Average hops for delivered (r1 = 2 hops, r2 = 1 hop -> avg 1.5)
        assertEquals(1.5, historyManager.getAverageHops(), 0.001);
    }

    @Test
    @DisplayName("Filter by status: DELIVERED, FAILED, NO_ROUTE")
    void testFilterByStatus() {
        SimulationRecord r1 = createRecord("MSG-1", phone, security, "SOS 1", Priority.HIGH, true, "Delivered", List.of(phone, security));
        SimulationRecord r2 = createRecord("MSG-2", phone, hospital, "SOS 2", Priority.HIGH, false,
                "No route found across active nodes.", List.of());
        SimulationRecord r3 = createRecord("MSG-3", phone, security, "SOS 3", Priority.NORMAL, false,
                "Device offline", List.of());

        historyManager.addRecord(r1);
        historyManager.addRecord(r2);
        historyManager.addRecord(r3);

        List<SimulationRecord> deliveredOnly = historyManager.filter(null, "DELIVERED", null);
        assertEquals(1, deliveredOnly.size());
        assertEquals("MSG-1", deliveredOnly.get(0).getId());

        List<SimulationRecord> noRouteOnly = historyManager.filter(null, "NO_ROUTE", null);
        assertEquals(1, noRouteOnly.size());
        assertEquals("MSG-2", noRouteOnly.get(0).getId());

        List<SimulationRecord> failedOnly = historyManager.filter(null, "FAILED", null);
        assertEquals(1, failedOnly.size());
        assertEquals("MSG-3", failedOnly.get(0).getId());

        List<SimulationRecord> all = historyManager.filter(null, "ALL", null);
        assertEquals(3, all.size());
    }

    @Test
    @DisplayName("Filter by keyword search query across content, IDs, and device names")
    void testFilterBySearchQuery() {
        SimulationRecord r1 = createRecord("MSG-100", phone, security, "Severe structural hazard in library", Priority.CRITICAL, true, "Delivered", List.of(phone, security));
        SimulationRecord r2 = createRecord("MSG-200", phone, hospital, "First aid needed in courtyard", Priority.HIGH, true, "Delivered", List.of(phone, security, hospital));

        historyManager.addRecord(r1);
        historyManager.addRecord(r2);

        // Match content
        List<SimulationRecord> hazardResults = historyManager.filter("structural", null, null);
        assertEquals(1, hazardResults.size());
        assertEquals("MSG-100", hazardResults.get(0).getId());

        // Match ID
        List<SimulationRecord> idResults = historyManager.filter("200", null, null);
        assertEquals(1, idResults.size());
        assertEquals("MSG-200", idResults.get(0).getId());

        // Match Device name
        List<SimulationRecord> hospitalResults = historyManager.filter("hospital", null, null);
        assertEquals(1, hospitalResults.size());
        assertEquals("MSG-200", hospitalResults.get(0).getId());
    }

    @Test
    @DisplayName("Filter by device selection")
    void testFilterByDevice() {
        SimulationRecord r1 = createRecord("MSG-1", phone, security, "SOS 1", Priority.HIGH, true, "Delivered", List.of(phone, security));
        SimulationRecord r2 = createRecord("MSG-2", security, hospital, "SOS 2", Priority.NORMAL, true, "Delivered", List.of(security, hospital));

        historyManager.addRecord(r1);
        historyManager.addRecord(r2);

        // Hospital appears in r2 only
        List<SimulationRecord> hospitalFilter = historyManager.filter(null, null, "Field Hospital");
        assertEquals(1, hospitalFilter.size());
        assertEquals("MSG-2", hospitalFilter.get(0).getId());

        // Security appears in both r1 and r2
        List<SimulationRecord> securityFilter = historyManager.filter(null, null, "Campus Security");
        assertEquals(2, securityFilter.size());
    }

    @Test
    @DisplayName("Clear history removes all records and resets stats")
    void testClearHistory() {
        SimulationRecord r1 = createRecord("MSG-1", phone, security, "SOS 1", Priority.HIGH, true, "Delivered", List.of(phone, security));
        historyManager.addRecord(r1);
        assertFalse(historyManager.isEmpty());

        historyManager.clear();
        assertTrue(historyManager.isEmpty());
        assertEquals(0, historyManager.getTotalCount());
        assertEquals(0, historyManager.getDeliveredCount());
        assertEquals(0.0, historyManager.getDeliveryRate(), 0.001);
    }

    @Test
    @DisplayName("Summary report generates readable content")
    void testGenerateSummaryReport() {
        SimulationRecord r1 = createRecord("MSG-1", phone, security, "Earthquake alert", Priority.CRITICAL, true, "Delivered", List.of(phone, security));
        historyManager.addRecord(r1);

        String report = historyManager.generateSummaryReport();
        assertNotNull(report);
        assertTrue(report.contains("RESQMESH EMERGENCY DISPATCH REPORT"));
        assertTrue(report.contains("MSG-1"));
        assertTrue(report.contains("Earthquake alert"));
        assertTrue(report.contains("Total Dispatches : 1"));
    }
}
