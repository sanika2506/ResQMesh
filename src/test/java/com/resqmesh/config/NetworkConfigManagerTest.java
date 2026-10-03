package com.resqmesh.config;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated Unit Tests for NetworkConfigManager (Step 10):
 * - Network configuration export and import round-trip
 * - Routing execution and battery deduction after configuration load
 * - Total cleanup of prior network state on load
 * - Schema validation: malformed JSON, duplicate IDs, unknown connection targets, self-loops
 * - Battery level clamping and status derivation
 * - Missing file error handling
 */
public class NetworkConfigManagerTest {

    private NetworkGraph graph;
    private StudentPhone alice;
    private StudentPhone bob;
    private SecurityStation security;
    private MedicalStation medical;

    @BeforeEach
    void setUp() {
        graph = new NetworkGraph();

        alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(10, 15), 95.0);
        bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(25, 30), 85.0);
        security = new SecurityStation("SEC-1", "Security Post", new Location(40, 45), 100.0);
        medical = new MedicalStation("MED-1", "Medical Center", new Location(60, 65), 100.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);

        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);
    }

    @Test
    @DisplayName("Save & Load: Complete topology round-trip restores all nodes, types, and links")
    void testSaveAndReloadRoundTrip(@TempDir Path tempDir) throws Exception {
        File file = tempDir.resolve("campus_mesh.json").toFile();

        // Save
        NetworkConfigManager.saveToFile(graph, file, "Campus Disaster Relief Mesh");
        assertTrue(file.exists() && file.length() > 0, "JSON file should be created on disk");

        // Load into fresh graph
        NetworkGraph loadedGraph = new NetworkGraph();
        NetworkConfigManager.loadFromFile(file, loadedGraph);

        // Verify devices
        assertEquals(4, loadedGraph.getDeviceCount());
        CommunicationDevice loadedAlice = loadedGraph.getDeviceById("DEV-1");
        CommunicationDevice loadedBob = loadedGraph.getDeviceById("DEV-2");
        CommunicationDevice loadedSecurity = loadedGraph.getDeviceById("SEC-1");
        CommunicationDevice loadedMedical = loadedGraph.getDeviceById("MED-1");

        assertNotNull(loadedAlice);
        assertNotNull(loadedBob);
        assertNotNull(loadedSecurity);
        assertNotNull(loadedMedical);

        // Verify types and polymorphism
        assertTrue(loadedAlice instanceof StudentPhone);
        assertTrue(loadedSecurity instanceof SecurityStation);
        assertTrue(loadedMedical instanceof MedicalStation);

        // Verify battery and location
        assertEquals(95.0, loadedAlice.getBatteryLevel(), 0.01);
        assertEquals(10.0, loadedAlice.getLocation().getX(), 0.01);
        assertEquals(15.0, loadedAlice.getLocation().getY(), 0.01);

        // Verify connections
        assertEquals(6, loadedGraph.getTotalLinkCount(), "3 bidirectional connections = 6 directed links");
        assertTrue(loadedGraph.hasConnection(loadedAlice, loadedBob));
        assertTrue(loadedGraph.hasConnection(loadedBob, loadedAlice));
        assertTrue(loadedGraph.hasConnection(loadedBob, loadedSecurity));
        assertTrue(loadedGraph.hasConnection(loadedSecurity, loadedMedical));
        assertFalse(loadedGraph.hasConnection(loadedAlice, loadedMedical), "No direct connection between Alice and Medical");
    }

    @Test
    @DisplayName("Routing after Load: BFS pathfinding and message delivery succeed on restored graph")
    void testRoutingAfterLoad() throws Exception {
        String json = NetworkConfigManager.exportToJson(graph, "Test Config");

        NetworkGraph targetGraph = new NetworkGraph();
        NetworkConfigManager.loadFromJson(json, targetGraph);

        SimulationEngine engine = new SimulationEngine(targetGraph, new ShortestPathStrategy());

        CommunicationDevice src = targetGraph.getDeviceById("DEV-1");
        CommunicationDevice dst = targetGraph.getDeviceById("MED-1");

        EmergencyMessage msg = new EmergencyMessage("MSG-RESTORED-1", src, dst, "Post-restore test", Priority.NORMAL);
        SimulationResult result = engine.send(msg);

        assertTrue(result.delivered(), "Message must be delivered across restored graph");
        assertEquals(4, result.route().size(), "Route must traverse 4 devices (3 hops)");
        assertEquals("DEV-1", result.route().get(0).getId());
        assertEquals("DEV-2", result.route().get(1).getId());
        assertEquals("SEC-1", result.route().get(2).getId());
        assertEquals("MED-1", result.route().get(3).getId());

        // Battery deduction check (-2.0% from 95.0% -> 93.0%)
        assertEquals(93.0, src.getBatteryLevel(), 0.01);
    }

    @Test
    @DisplayName("State Isolation: Loading new configuration wipes prior graph connections and devices")
    void testLoadReplacesOldNetworkCompletely() throws Exception {
        // Initial graph has old devices
        NetworkGraph targetGraph = new NetworkGraph();
        StudentPhone old1 = new StudentPhone("OLD-1", "Stale Device 1", new Location(0, 0), 100.0);
        StudentPhone old2 = new StudentPhone("OLD-2", "Stale Device 2", new Location(5, 5), 100.0);
        targetGraph.addDevice(old1);
        targetGraph.addDevice(old2);
        targetGraph.connect(old1, old2);

        assertEquals(2, targetGraph.getDeviceCount());
        assertEquals(2, targetGraph.getTotalLinkCount());

        // Prepare JSON with completely different devices
        String json = "{\n" +
                "  \"name\": \"Fresh Topology\",\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"devices\": [\n" +
                "    { \"id\": \"NEW-1\", \"name\": \"Alpha\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 80.0, \"status\": \"ACTIVE\" },\n" +
                "    { \"id\": \"NEW-2\", \"name\": \"Beta\", \"type\": \"SECURITY_STATION\", \"batteryLevel\": 90.0, \"status\": \"ACTIVE\" }\n" +
                "  ],\n" +
                "  \"connections\": [\n" +
                "    { \"nodeA\": \"NEW-1\", \"nodeB\": \"NEW-2\" }\n" +
                "  ]\n" +
                "}";

        NetworkConfigManager.loadFromJson(json, targetGraph);

        // Verify old nodes are 100% purged
        assertEquals(2, targetGraph.getDeviceCount());
        assertNull(targetGraph.getDeviceById("OLD-1"));
        assertNull(targetGraph.getDeviceById("OLD-2"));

        assertNotNull(targetGraph.getDeviceById("NEW-1"));
        assertNotNull(targetGraph.getDeviceById("NEW-2"));
        assertEquals(2, targetGraph.getTotalLinkCount(), "Only the single new connection should exist");
    }

    @Test
    @DisplayName("Validation: Malformed JSON syntax is rejected with descriptive ConfigurationException")
    void testValidationMalformedJson() {
        String badJson = "{ \"name\": \"Broken\", \"devices\": [ { broken json } ] }";

        ConfigurationException ex = assertThrows(ConfigurationException.class, () ->
                NetworkConfigManager.loadFromJson(badJson, new NetworkGraph()));
        assertTrue(ex.getMessage().contains("Malformed JSON syntax"));
    }

    @Test
    @DisplayName("Validation: Duplicate device IDs are detected and rejected")
    void testValidationDuplicateDeviceIds() {
        String duplicateIdJson = "{\n" +
                "  \"devices\": [\n" +
                "    { \"id\": \"DUP-1\", \"name\": \"First Device\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 100.0 },\n" +
                "    { \"id\": \"DUP-1\", \"name\": \"Second Device With Same ID\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 90.0 }\n" +
                "  ]\n" +
                "}";

        ConfigurationException ex = assertThrows(ConfigurationException.class, () ->
                NetworkConfigManager.loadFromJson(duplicateIdJson, new NetworkGraph()));
        assertTrue(ex.getMessage().contains("Duplicate device ID detected"));
    }

    @Test
    @DisplayName("Validation: Connections referencing unknown device IDs are rejected")
    void testValidationConnectionUnknownDevice() {
        String unknownEndpointJson = "{\n" +
                "  \"devices\": [\n" +
                "    { \"id\": \"DEV-A\", \"name\": \"Node A\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 100.0 }\n" +
                "  ],\n" +
                "  \"connections\": [\n" +
                "    { \"nodeA\": \"DEV-A\", \"nodeB\": \"NON_EXISTENT_ID\" }\n" +
                "  ]\n" +
                "}";

        ConfigurationException ex = assertThrows(ConfigurationException.class, () ->
                NetworkConfigManager.loadFromJson(unknownEndpointJson, new NetworkGraph()));
        assertTrue(ex.getMessage().contains("references unknown device ID"));
    }

    @Test
    @DisplayName("Validation: Self-connecting loops (nodeA == nodeB) are rejected")
    void testValidationSelfConnectingLoop() {
        String selfLoopJson = "{\n" +
                "  \"devices\": [\n" +
                "    { \"id\": \"DEV-X\", \"name\": \"Node X\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 100.0 }\n" +
                "  ],\n" +
                "  \"connections\": [\n" +
                "    { \"nodeA\": \"DEV-X\", \"nodeB\": \"DEV-X\" }\n" +
                "  ]\n" +
                "}";

        ConfigurationException ex = assertThrows(ConfigurationException.class, () ->
                NetworkConfigManager.loadFromJson(selfLoopJson, new NetworkGraph()));
        assertTrue(ex.getMessage().contains("cannot be connected to itself"));
    }

    @Test
    @DisplayName("Error Handling: Missing configuration file throws FileNotFoundException")
    void testMissingFileHandling() {
        File missingFile = new File("non_existent_topology_xyz123.json");
        assertThrows(FileNotFoundException.class, () ->
                NetworkConfigManager.loadFromFile(missingFile, new NetworkGraph()));
    }

    @Test
    @DisplayName("Data Sanitization: Battery clamping and 0% battery offline status derivation")
    void testBatteryClampingAndStatusDerivation() throws Exception {
        String json = "{\n" +
                "  \"devices\": [\n" +
                "    { \"id\": \"DEV-HIGH\", \"name\": \"Overcharged\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": 150.0 },\n" +
                "    { \"id\": \"DEV-DEAD\", \"name\": \"Depleted Phone\", \"type\": \"STUDENT_PHONE\", \"batteryLevel\": -10.0, \"status\": \"ACTIVE\" }\n" +
                "  ]\n" +
                "}";

        NetworkGraph targetGraph = new NetworkGraph();
        NetworkConfigManager.loadFromJson(json, targetGraph);

        CommunicationDevice high = targetGraph.getDeviceById("DEV-HIGH");
        CommunicationDevice dead = targetGraph.getDeviceById("DEV-DEAD");

        assertEquals(100.0, high.getBatteryLevel(), 0.01, "Battery level > 100 should be clamped to 100.0");
        assertEquals(0.0, dead.getBatteryLevel(), 0.01, "Battery level < 0 should be clamped to 0.0");
        assertEquals(DeviceStatus.OFFLINE, dead.getStatus(), "0% battery device must automatically derive OFFLINE status");
    }

    @Test
    @DisplayName("Sample File: Successfully loads bundled campus_disaster_mesh.json")
    void testLoadSampleScenarioFile() throws Exception {
        File sampleFile = new File("samples/campus_disaster_mesh.json");
        assertTrue(sampleFile.exists(), "Sample scenario file must exist");

        NetworkGraph targetGraph = new NetworkGraph();
        NetworkConfigManager.loadFromFile(sampleFile, targetGraph);

        assertEquals(6, targetGraph.getDeviceCount());
        assertEquals(10, targetGraph.getTotalLinkCount(), "5 bidirectional connections = 10 directed links");

        CommunicationDevice alice = targetGraph.getDeviceById("DEV-1");
        CommunicationDevice hospital = targetGraph.getDeviceById("MED-1");
        CommunicationDevice charlie = targetGraph.getDeviceById("DEV-3");

        assertNotNull(alice);
        assertNotNull(hospital);
        assertNotNull(charlie);

        assertEquals(DeviceStatus.OFFLINE, charlie.getStatus());
        assertEquals(0, targetGraph.getLinks(charlie).size(), "Charlie is disconnected/isolated");

        // Verify routing works through loaded sample mesh
        SimulationEngine engine = new SimulationEngine(targetGraph, new ShortestPathStrategy());
        EmergencyMessage msg = new EmergencyMessage("MSG-SAMPLE-1", alice, hospital, "Urgent triage needed", Priority.NORMAL);
        SimulationResult result = engine.send(msg);

        assertTrue(result.delivered(), "Message across sample mesh must succeed");
        assertEquals(4, result.route().size(), "Shortest path should have 4 devices (3 hops)");
    }
}
