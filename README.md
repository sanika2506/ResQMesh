# ResQMesh – Offline Communication Simulator

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-blue.svg?logo=java&logoColor=white)](https://openjfx.io/)
[![Build Tool](https://img.shields.io/badge/Build-Maven%20Wrapper-red.svg?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/JUnit%205-43%20Passed-brightgreen.svg?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

> **ResQMesh** is an offline peer-to-peer ad-hoc communication network simulator designed for disaster response scenarios where conventional cellular towers and internet backbones have failed. Built with modern Java, JavaFX, and clean Object-Oriented Architecture, it models multi-hop mesh message propagation, shortest-path BFS routing, battery drain telemetry, interactive visual network topology, and full JSON configuration persistence.

---

## 📸 Interface Showcase

```
+---------------------------------------------------------------------------------------------------------+
| [🛡️ ResQMesh]                                      [ 5 Active Nodes ] [ 3 Mesh Links ] [ 100% Delivery ] |
+-----------------------------------+---------------------------------------------------------------------+
| NAVIGATION                        | 🗺️ INTERACTIVE MESH TOPOLOGY CANVAS                                 |
| • 🗺️ Topology View                 |                                                                     |
| • 📋 Telemetry Table              |         [ 📱 Alice's Phone (#1 SENDER) ]                            |
| • 🚀 Dispatch Alert               |                    │ (glow)                                         |
| • 📜 Terminal Logs                |         [ 📱 Bob's Phone (#2 HOP) ]                                 |
|                                   |                    │ (glow)                                         |
| CONFIGURATION & ACTIONS           |         [ 🛡️ Security Post (#3 HOP) ]       [ 📱 Charlie (OFFLINE) ] |
| • 💾 Save Network (JSON)          |                    │ (glow)                                         |
| • 📂 Load Network (JSON)          |         [ 🏥 Medical Center (#4 RECIPIENT) ]                        |
| • ✨ New Simulation               |                                                                     |
|                                   |  [ Top-Right HUD Inspector: Live Device Telemetry & Battery ]        |
+-----------------------------------+---------------------------------------------------------------------+
| QUICK NODE CONTROLS               | 🚀 EMERGENCY DISPATCH & REAL-TIME PROPAGATION                       |
| • Add Student / Security / Medical| Sender: Alice's Phone ──▶ Target: Medical Center [ Priority: HIGH ] |
| • Connect Devices (Establish Link)| Payload: "Distress alert: Emergency triage required in Sector 2"    |
| • Toggle Online/Offline / Recharge| Result: ✔ DELIVERED (3 HOPS)  [ ⚡ -2.0% battery deducted per hop ] |
+-----------------------------------+---------------------------------------------------------------------+
```

---

## 🌟 Core Features

- **🗺️ Interactive Visual Network Topology Canvas:**
  - Registered devices render as draggable circular nodes with classification icons (`📱 Student Phone`, `🛡️ Security Post`, `🏥 Medical Center`).
  - Active bidirectional peer-to-peer links dynamically follow nodes with line bindings in real-time.
  - Distinct visual styling for **ONLINE** (`🟢` emerald border, 100% opacity) vs. **OFFLINE** (`🔴` dashed red border, dimmed opacity).
  - Floating **Node Inspector HUD** displays real-time telemetry (ID, battery health bar, operational state, peer links) on node selection.
  - Smooth radial/circular distribution algorithm with mouse drag-and-drop repositioning.

- **💾 Network Configuration Persistence (Save & Load):**
  - **Save Network:** Export the active network topology (device IDs, names, types, battery levels, online/offline status, 2D coordinates, and mesh connections) into formatted, human-readable JSON.
  - **Load Network:** Restore saved mesh configurations instantly with atomic state clearance (prevents ghost links or stale devices).
  - **Strict Validation:** Guards against malformed JSON, duplicate device IDs, missing nodes, self-connections, out-of-range battery values, and invalid device types.
  - **New Simulation:** One-click session reset that clears hop traces, refreshes counters, and restores device batteries.

- **⚡ Real-Time BFS Route Illumination:**
  - When an emergency message is dispatched, the shortest path computed via Breadcrumb BFS illuminates immediately across the network.
  - Route links glow in neon cyan/amber (`#22d3ee`, stroke width `3.5px`).
  - Participating nodes display sequential hop badges (`#1 SENDER`, `#2 HOP`, ..., `#N RECIPIENT`).
  - Unrelated or offline nodes remain unhighlighted; failed deliveries automatically clear previous route glow.

- **📡 Emergency Messaging & Priority Dispatch:**
  - Four transmission priority levels: `LOW`, `NORMAL`, `HIGH`, and `CRITICAL`.
  - Realistic forwarding policies: handheld `StudentPhone` devices can relay standard messages but cannot forward `CRITICAL` alerts (reserving critical relay to heavy-duty `SecurityStation` and `MedicalStation` hubs).
  - One-click disaster dispatch presets: *Medical Distress*, *Perimeter Breach*, *System Check*.

- **🔋 Realistic Energy Consumption Model:**
  - Each transmission consumes a realistic energy drain ($-2.0\%$ battery per hop) from all participating relay nodes.
  - Low battery warnings trigger at $\le 20\%$; devices auto-switch to `OFFLINE` at $0\%$ battery.
  - One-click *Recharge (100%)* and *Toggle Online/Offline* controls for dynamic failure simulations.

- **📊 Central Telemetry Table & Real-Time Activity Feed:**
  - Tabbed interface switching between the interactive visual topology canvas and a detailed telemetry `TableView`.
  - Live scrollable, timestamped terminal log tracking all dispatches, connection establishments, and node state transitions.
  - Top KPI cards displaying Total Devices, Network Health, Active Links, and Delivery Success Rate.

---

## 🏗️ Architecture & OOP Design Principles

ResQMesh is architected with strict adherence to clean Object-Oriented Programming (OOP) and design patterns:

```
com.resqmesh/
├── model/                # Domain Entities & Business Rules
│   ├── CommunicationDevice.java   [Abstract Base: Encapsulation, State Validation]
│   ├── StudentPhone.java          [Subclass: Cannot relay CRITICAL messages]
│   ├── SecurityStation.java       [Subclass: Full relay authorization]
│   ├── MedicalStation.java        [Subclass: Full relay authorization & triage hub]
│   ├── CommunicationLink.java     [Entity: Bidirectional connection channel]
│   ├── Location.java              [Value Object: 2D coordinates & Euclidean distance]
│   └── EmergencyMessage.java      [Entity: Payload, lifecycle status, and Priority]
│
├── routing/              # Graph & Pathfinding Algorithms
│   ├── NetworkGraph.java          [Adjacency List Graph: Node & Link management]
│   ├── RoutingStrategy.java       [Strategy Pattern Interface: findRoute()]
│   └── ShortestPathStrategy.java  [Concrete Strategy: Breadcrumb BFS Algorithm]
│
├── config/               # Persistence & Serialization (Step 10)
│   ├── NetworkConfigManager.java  [Manager: JSON export, import, parsing, validation]
│   ├── NetworkConfigDTO.java      [DTO Hierarchy: Network, Device, Location, Link]
│   └── ConfigurationException.java[Checked Exception: Validation & I/O errors]
│
├── simulation/           # Lifecycle & Simulation Coordination
│   ├── SimulationEngine.java      [Facade / Controller: Coordinates routing & energy]
│   ├── SimulationResult.java      [Immutable Record: Outcome, route, and diagnostics]
│   └── SimulationDemo.java        [Console demonstration test runner]
│
└── ui/                   # JavaFX Presentation Layer
    ├── App.java                   [Main Dashboard: BorderPane, cards, form handlers]
    ├── NetworkTopologyPane.java   [Custom StackPane Canvas: NodeVisual, LinkVisual]
    └── Launcher.java              [CLI / IDE bootstrap companion]
```

### Applied Design Patterns & OOP Concepts

1. **Abstraction & Polymorphism:**
   - `CommunicationDevice` defines abstract contracts like `canForward(EmergencyMessage)`. Different device implementations (`StudentPhone`, `SecurityStation`) apply specialized domain constraints polymorphically.
2. **Strategy Pattern:**
   - `RoutingStrategy` abstracts path discovery. The `SimulationEngine` delegates route discovery to `ShortestPathStrategy` (BFS) and can effortlessly swap in future strategies (e.g., AODV, Dijkstra) without altering simulation logic.
3. **Encapsulation:**
   - Battery levels, statuses, and network links are shielded with private fields, invariant validation checks, and immutable collections where appropriate.
4. **Single Responsibility Principle (SRP):**
   - Graph maintenance is isolated in `NetworkGraph`, route calculation in `ShortestPathStrategy`, packet propagation and battery drain in `SimulationEngine`, serialization and schema validation in `NetworkConfigManager`, and rendering in `NetworkTopologyPane`.
5. **Data Transfer Object (DTO) Pattern:**
   - `NetworkConfigDTO` separates internal graph references and JavaFX visual state from the external JSON schema, ensuring clean serialization without leaking transient UI state.

---

## 📂 Project Directory Structure

```text
ResQMesh/
├── pom.xml                                           # Maven configuration (Java 17/21, JavaFX 21, Gson 2.11)
├── .gitignore                                        # Excludes target/, IDE metadata (.idea, .vscode)
├── mvnw & mvnw.cmd                                   # Standalone Maven wrapper scripts
├── README.md                                         # Portfolio documentation and user guide
├── samples/                                          # Bundled disaster mesh topology configurations
│   └── campus_disaster_mesh.json                     # 6-node, 5-link multi-hop disaster network
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── resqmesh/
    │   │           ├── App.java                      # Main JavaFX dashboard application
    │   │           ├── Launcher.java                 # Non-modular execution bootstrap
    │   │           ├── config/
    │   │           │   ├── ConfigurationException.java# Custom checked validation exception
    │   │           │   ├── NetworkConfigDTO.java     # Serialization Data Transfer Objects
    │   │           │   └── NetworkConfigManager.java # JSON save, load, and schema validator
    │   │           ├── model/
    │   │           │   ├── CommunicationDevice.java  # Abstract base communication node
    │   │           │   ├── CommunicationLink.java    # Directed communication link
    │   │           │   ├── DeviceStatus.java         # Enum: ACTIVE, OFFLINE, LOW_BATTERY
    │   │           │   ├── EmergencyMessage.java     # Message payload and metadata
    │   │           │   ├── Location.java             # 2D coordinates & Euclidean metrics
    │   │           │   ├── MedicalStation.java       # Station device subclass
    │   │           │   ├── MessageStatus.java        # Enum: CREATED, DELIVERED, FAILED
    │   │           │   ├── Priority.java             # Enum: LOW, NORMAL, HIGH, CRITICAL
    │   │           │   ├── SecurityStation.java      # Station device subclass
    │   │           │   └── StudentPhone.java         # Handheld device subclass
    │   │           ├── routing/
    │   │           │   ├── NetworkGraph.java         # Adjacency-list network graph
    │   │           │   ├── RoutingStrategy.java      # Routing strategy interface
    │   │           │   └── ShortestPathStrategy.java # BFS shortest-path router
    │   │           ├── simulation/
    │   │           │   ├── SimulationDemo.java       # Standalone console simulation runner
    │   │           │   ├── SimulationEngine.java     # Engine managing hops and battery drain
    │   │           │   └── SimulationResult.java     # Outcome record (delivered, route, reason)
    │   │           └── ui/
    │   │               └── NetworkTopologyPane.java  # Interactive visual network topology canvas
    │   └── resources/
    │       ├── .gitkeep
    │       └── style.css                             # Dark cyber-command stylesheet
    └── test/
        ├── java/
        │   └── com/
        │       └── resqmesh/
        │           ├── config/
        │           │   └── NetworkConfigManagerTest.java        # Save, load, schema validation, state purge tests
        │           ├── model/
        │           │   └── DeviceModelTest.java                 # Device status and battery tests
        │           ├── routing/
        │           │   ├── NetworkGraphTest.java                # Graph connections & link tests
        │           │   └── ShortestPathStrategyTest.java        # BFS pathfinding & constraint tests
        │           ├── simulation/
        │           │   ├── SimulationEngineTest.java            # Engine delivery unit tests
        │           │   └── EmergencyDeliveryIntegrationTest.java# End-to-end multi-hop delivery tests
        │           └── ui/
        │               └── NetworkTopologyVisualTest.java       # Visual canvas & inspector tests
        └── resources/
```

---

## 💾 Network Configuration JSON Schema

ResQMesh uses a clean, portable JSON format to export and import network topologies. Users can load pre-configured scenarios or create custom disaster models:

```json
{
  "version": "1.0",
  "name": "Campus Disaster Response Mesh",
  "devices": [
    {
      "id": "1",
      "name": "Alice's Phone",
      "type": "StudentPhone",
      "batteryLevel": 100.0,
      "status": "ACTIVE",
      "location": { "x": 100.0, "y": 200.0 }
    },
    {
      "id": "4",
      "name": "Medical Center",
      "type": "MedicalStation",
      "batteryLevel": 98.0,
      "status": "ACTIVE",
      "location": { "x": 500.0, "y": 200.0 }
    }
  ],
  "connections": [
    { "sourceId": "1", "targetId": "2" },
    { "sourceId": "2", "targetId": "4" }
  ]
}
```

- **Validation Rules:**
  - `devices` must be non-empty and have unique IDs.
  - `batteryLevel` must be within $[0.0, 100.0]$.
  - `type` must be one of `StudentPhone`, `SecurityStation`, or `MedicalStation`.
  - `connections` source and target must point to valid registered devices, and self-loops (`sourceId == targetId`) are rejected.

---

## 🚀 Getting Started

### Prerequisites

- **Java Development Kit (JDK):** Version 17 or higher (Java 21 LTS recommended).
- **Operating System:** Windows, macOS, or Linux.
- **Maven:** Bundled via `mvnw` (no local Maven installation required).

### Option 1: Running the JavaFX Application (GUI)

Open a terminal in the project root directory:

```bash
# On Windows (PowerShell / Command Prompt)
.\mvnw.cmd javafx:run

# On Linux / macOS
./mvnw javafx:run
```

### Option 2: Running the Console Demonstration

To run the standalone terminal demo without opening the GUI:

```bash
# On Windows
.\mvnw.cmd test-compile exec:java -Dexec.mainClass="com.resqmesh.simulation.SimulationDemo"

# On Linux / macOS
./mvnw test-compile exec:java -Dexec.mainClass="com.resqmesh.simulation.SimulationDemo"
```

### Option 3: Running inside an IDE

1. Open the `ResQMesh` root directory in **IntelliJ IDEA**, **Eclipse**, or **VS Code**.
2. Allow Maven to import dependencies automatically.
3. Locate `src/main/java/com/resqmesh/Launcher.java` (or `App.java`), right-click, and select **Run 'Launcher.main()'**.

---

## 🧪 Automated Testing & Verification

The project includes **43 comprehensive automated tests** across persistence, unit, algorithm, and UI visual integration layers:

```bash
# Execute the full automated test suite
.\mvnw.cmd test
```

### Test Suite Matrix

| Test Class | Scope | Tests | Status |
| :--- | :--- | :---: | :---: |
| [`NetworkConfigManagerTest.java`](file:///src/test/java/com/resqmesh/config/NetworkConfigManagerTest.java) | Round-trip save/load, routing restoration, state wipe isolation, syntax errors, duplicate IDs, self-loops, and missing targets | 10 | **PASSED** |
| [`DeviceModelTest.java`](file:///src/test/java/com/resqmesh/model/DeviceModelTest.java) | Device creation, battery consumption, low-battery thresholds, and status auto-transitions | 5 | **PASSED** |
| [`NetworkGraphTest.java`](file:///src/test/java/com/resqmesh/routing/NetworkGraphTest.java) | Node registration, bidirectional connections, duplicate prevention, and link counting | 5 | **PASSED** |
| [`ShortestPathStrategyTest.java`](file:///src/test/java/com/resqmesh/routing/ShortestPathStrategyTest.java) | BFS shortest-path optimality, multi-hop routes, unreachable devices, and CRITICAL priority forwarding constraints | 6 | **PASSED** |
| [`SimulationEngineTest.java`](file:///src/test/java/com/resqmesh/simulation/SimulationEngineTest.java) | Message delivery, energy deductions, offline sender/recipient handling, and null safety | 5 | **PASSED** |
| [`EmergencyDeliveryIntegrationTest.java`](file:///src/test/java/com/resqmesh/simulation/EmergencyDeliveryIntegrationTest.java) | End-to-end Alice $\rightarrow$ Medical delivery, intermediate node offline failures, direct link bypass, multi-path BFS selection, and route reset | 7 | **PASSED** |
| [`NetworkTopologyVisualTest.java`](file:///src/test/java/com/resqmesh/ui/NetworkTopologyVisualTest.java) | Node/link rendering, online/offline visual styles, HUD inspector telemetry, sequenced hop badges, and graph synchronization | 5 | **PASSED** |
| **Total** | | **43** | **100% PASSED** |

---

## 🔮 Future Enhancements

- **Autonomous Dynamic Routing:** Integrate reactive ad-hoc routing protocols such as **AODV** (Ad hoc On-Demand Distance Vector) or **DSR** (Dynamic Source Routing).
- **Physical RF Simulation:** Incorporate signal attenuation, packet drop probability based on Euclidean distance, and radio frequency interference.
- **Geo-Spatial Map Integration:** Support OpenStreetMap tiles for overlaying campus buildings and GPS coordinates.
- **Export Telemetry:** Export incident logs and simulation metrics as JSON / CSV reports.

---

## 📄 License

This project is open-source software licensed under the **MIT License**.
