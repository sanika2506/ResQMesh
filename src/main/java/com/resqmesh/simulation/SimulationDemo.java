package com.resqmesh.simulation;

import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;

/**
 * Console demonstration verifying the Step 5 Simulation Engine:
 * 1. Successful multi-hop message delivery with battery deduction.
 * 2. Clean failure when no route exists to a disconnected device.
 * 3. Clean failure when sender or recipient is offline.
 */
public class SimulationDemo {

    public static void main(String[] args) {
        System.out.println("=================================================================");
        System.out.println("     ResQMesh - Offline Communication Simulator (Engine Demo)     ");
        System.out.println("=================================================================\n");

        // Step 1: Initialize Network Graph and Simulation Engine
        NetworkGraph graph = new NetworkGraph();
        SimulationEngine engine = new SimulationEngine(graph, new ShortestPathStrategy());

        // Step 2: Create Devices
        StudentPhone alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(0, 0), 100.0);
        StudentPhone bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(5, 0), 100.0);
        SecurityStation security = new SecurityStation("SEC-1", "Security Post", new Location(10, 0), 100.0);
        MedicalStation medical = new MedicalStation("MED-1", "Medical Center", new Location(15, 0), 100.0);
        StudentPhone david = new StudentPhone("DEV-3", "David's Phone (Isolated)", new Location(50, 50), 100.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(david); // Notice: david is not connected to any other device

        // Build Topology: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        System.out.println("Network Topology Configured:");
        System.out.println("  Alice's Phone <---> Bob's Phone <---> Security Post <---> Medical Center");
        System.out.println("  David's Phone is ISOLATED (no connections)\n");

        // -------------------------------------------------------------
        // SCENARIO 1: Successful Multi-Hop Delivery
        // -------------------------------------------------------------
        System.out.println("-----------------------------------------------------------------");
        System.out.println("TEST 1: Multi-Hop Message Delivery (Alice -> Medical Center)");
        System.out.println("-----------------------------------------------------------------");

        EmergencyMessage msg1 = new EmergencyMessage(
                "MSG-001",
                alice,
                medical,
                "Need medical assistance at building entrance!",
                Priority.HIGH
        );

        System.out.println("Initial Message Status: " + msg1.getStatus());
        System.out.println("Alice Battery: " + alice.getBatteryLevel() + "%");
        System.out.println("Bob Battery:   " + bob.getBatteryLevel() + "%");

        SimulationResult result1 = engine.send(msg1);

        System.out.println("Delivery Result:  " + (result1.delivered() ? "SUCCESS [DELIVERED]" : "FAILED"));
        System.out.println("Final Message Status: " + msg1.getStatus());
        System.out.println("Explanation:      " + result1.explanation());
        System.out.println("Batteries after transmission (-2% per device):");
        System.out.println("  Alice: " + alice.getBatteryLevel() + "%");
        System.out.println("  Bob:   " + bob.getBatteryLevel() + "%");
        System.out.println("  Security: " + security.getBatteryLevel() + "%");
        System.out.println("  Medical:  " + medical.getBatteryLevel() + "%\n");

        // -------------------------------------------------------------
        // SCENARIO 2: Failure When No Route Exists
        // -------------------------------------------------------------
        System.out.println("-----------------------------------------------------------------");
        System.out.println("TEST 2: Delivery to Unreachable Destination (Alice -> David)");
        System.out.println("-----------------------------------------------------------------");

        EmergencyMessage msg2 = new EmergencyMessage(
                "MSG-002",
                alice,
                david,
                "Check-in request for David",
                Priority.NORMAL
        );

        SimulationResult result2 = engine.send(msg2);

        System.out.println("Delivery Result:  " + (result2.delivered() ? "SUCCESS" : "FAILED (Cleanly Handled)"));
        System.out.println("Final Message Status: " + msg2.getStatus());
        System.out.println("Explanation:      " + result2.explanation());
        System.out.println("Route Size:       " + result2.route().size() + " (Empty list as expected)\n");

        // -------------------------------------------------------------
        // SCENARIO 3: Failure When Sender Is Offline
        // -------------------------------------------------------------
        System.out.println("-----------------------------------------------------------------");
        System.out.println("TEST 3: Delivery When Sender Is Offline (Depleted Battery)");
        System.out.println("-----------------------------------------------------------------");

        // Completely drain Alice's battery
        alice.consumeBattery(100.0);
        System.out.println("Alice's Status:   " + alice.getStatus() + " (Battery: " + alice.getBatteryLevel() + "%)");

        EmergencyMessage msg3 = new EmergencyMessage(
                "MSG-003",
                alice,
                bob,
                "Can you hear me?",
                Priority.LOW
        );

        SimulationResult result3 = engine.send(msg3);

        System.out.println("Delivery Result:  " + (result3.delivered() ? "SUCCESS" : "FAILED (Cleanly Handled)"));
        System.out.println("Final Message Status: " + msg3.getStatus());
        System.out.println("Explanation:      " + result3.explanation() + "\n");
        // -------------------------------------------------------------
        // SCENARIO 4: Failure When Recipient Is Offline
        // -------------------------------------------------------------
        System.out.println("-----------------------------------------------------------------");
        System.out.println("TEST 4: Delivery When Recipient Is Offline");
        System.out.println("-----------------------------------------------------------------");

        // Recharge Alice so the sender is active
        alice.recharge(100.0);
        // Take recipient offline
        medical.setStatus(DeviceStatus.OFFLINE);
        System.out.println("Alice's Status:   " + alice.getStatus() + " (Battery: " + alice.getBatteryLevel() + "%)");
        System.out.println("Medical's Status: " + medical.getStatus() + " (Offline for emergency maintenance)");

        EmergencyMessage msg4 = new EmergencyMessage(
                "MSG-004",
                alice,
                medical,
                "Requesting triage team to Sector 4",
                Priority.HIGH
        );

        SimulationResult result4 = engine.send(msg4);

        System.out.println("Delivery Result:  " + (result4.delivered() ? "SUCCESS" : "FAILED (Cleanly Handled)"));
        System.out.println("Final Message Status: " + msg4.getStatus());
        System.out.println("Explanation:      " + result4.explanation());
        System.out.println("Route Size:       " + result4.route().size() + " (Empty list as expected)\n");

        System.out.println("=================================================================");
        System.out.println("     All Simulation Engine Verification Scenarios Completed!     ");
        System.out.println("=================================================================");
    }
}
