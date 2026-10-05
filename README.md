# ResQMesh

Offline Emergency Communication Simulator

[![Java](https://img.shields.io/badge/Java-17%20%7C%2021-orange.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![JavaFX](https://img.shields.io/badge/JavaFX-21.0.6-blue.svg?logo=java&logoColor=white)](https://openjfx.io/)
[![Build Tool](https://img.shields.io/badge/Build-Maven%20Wrapper-red.svg?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Tests](https://img.shields.io/badge/JUnit%205-120%20Passed-brightgreen.svg?logo=junit5&logoColor=white)](https://junit.org/junit5/)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

---

## Problem Statement

During natural disasters (earthquakes, floods, severe storms) or emergency campus power outages, conventional communication infrastructure—such as cellular towers and internet service providers—often becomes disabled or congested.

**ResQMesh** simulates an **offline ad-hoc mesh communication network** where standard mobile devices and dedicated emergency stations communicate peer-to-peer without relying on central cellular towers, internet routers, or external servers. The project models device battery degradation, priority-based forwarding rules, and shortest-path graph routing to deliver emergency messages across surviving nodes.

---

## Features

- **Ad-Hoc Node & Link Modeling:**
  - Models student phones, security posts, and medical triage stations in a 2D coordinate space.
  - Bidirectional communication channels with active/inactive status and distance calculation.
- **Priority-Based Emergency Routing:**
  - Four priority levels: `LOW`, `NORMAL`, `HIGH`, and `CRITICAL`.
  - Subclass forwarding constraints: handheld student phones relay routine and high-priority messages, but reject `CRITICAL` alerts to protect battery and route life-safety data through dedicated stations.
- **Breadth-First Search (BFS) Shortest-Path Discovery:**
  - Finds the route with the minimum number of transmission hops.
  - Automatically avoids offline devices, drained batteries, broken links, and ineligible relay nodes.
- **Dynamic Energy Consumption Model:**
  - Deducts battery power (2.0% per hop) from each device participating in message delivery.
  - Devices automatically enter `LOW_BATTERY` ($\le 20\%$) and `OFFLINE` ($0\%$) states.
- **Interactive JavaFX Dashboard & 2D Canvas:**
  - Draggable device nodes with real-time connection line tracking and status styling.
  - Real-time route illumination highlighting participating devices and hop sequence badges.
  - Telemetry cards displaying active node count, mesh link count, and delivery success rate.
- **Simulation Replay Engine & Event Timeline:**
  - Step-by-step playback with Play, Pause, Reset, and speed controls (`0.5x`, `1.0x`, `1.5x`, `2.0x`).
  - Chronological event timeline recording message milestones (`QUEUED`, `FORWARDING`, `DELIVERED`, `FAILED`).
- **Configuration & Scenario Persistence:**
  - Save and load complete network topologies to/from JSON using Google Gson.
  - Built-in validation guarding against duplicate IDs, missing targets, and self-loops.
- **Standalone Console Demo:**
  - Headless command-line execution (`SimulationDemo`) demonstrating 4 routing scenarios without opening a GUI.

---

## Technologies

- **Java (JDK 17 / 21):** Core language utilizing modern Java features (records, streams, lambdas, enhanced switch expressions).
- **JavaFX (21.0.6):** GUI controls, layouts, shapes, and custom animations (`javafx-controls`).
- **Maven:** Build automation, dependency management, and lifecycle packaging (`mvnw` wrapper included).
- **JUnit 5 (5.10.2):** Automated unit and integration testing suite.
- **Google Gson (2.11.0):** JSON serialization and deserialization for network topologies and settings.

---

## OOP Concepts

The project demonstrates all core Object-Oriented Programming principles:

### 1. Encapsulation
- All fields in domain classes (`CommunicationDevice`, `CommunicationLink`, `EmergencyMessage`, `Location`, `NetworkGraph`) are marked `private`.
- State mutation is strictly validated through public methods (e.g., `consumeBattery()` ensures consumption is non-negative and clamps battery to $[0.0, 100.0]$).
- Internal collections are protected from external tampering by returning unmodifiable collections (`Collections.unmodifiableList()`, `Collections.unmodifiableSet()`).

### 2. Abstraction
- **Abstract Base Class:** `CommunicationDevice` defines the generic contract for all network nodes (`isAvailable()`, `consumeBattery()`, `recharge()`), hiding internal battery and status logic while declaring the abstract method `canForward(Priority)`.
- **Strategy Interface:** `RoutingStrategy` abstracts the pathfinding algorithm. The simulation engine communicates only through this interface, decoupling the routing algorithm from the graph representation.

### 3. Inheritance
- `CommunicationDevice` serves as the abstract parent class.
- `StudentPhone`, `MedicalStation`, and `SecurityStation` extend `CommunicationDevice`, reusing state (ID, name, location, battery level, operational status) via `super(...)` and adding specialized behavior.

### 4. Polymorphism
- **Runtime Polymorphism (Method Overriding):**
  - `StudentPhone` overrides `canForward(Priority)` to return `false` if `priority == Priority.CRITICAL`.
  - `MedicalStation` and `SecurityStation` override `canForward(Priority)` to accept all priorities.
  - During BFS traversal, `neighbor.canForward(priority)` dynamically executes the appropriate subclass implementation at runtime.
- **Interface Polymorphism:**
  - `ShortestPathStrategy` implements `RoutingStrategy`. The `SimulationEngine` accepts any `RoutingStrategy` implementation, allowing alternative algorithms (e.g., Dijkstra) to be swapped without changing the engine.

### 5. Association, Aggregation, and Composition
- **Association:** `CommunicationLink` connects two separate `CommunicationDevice` instances (`source` and `destination`).
- **Aggregation:** `NetworkGraph` maintains a collection of `CommunicationDevice` nodes and `CommunicationLink` edges. Devices can be created and exist independently of the graph.
- **Composition:** `EmergencyMessage` encapsulates message ID, sender, recipient, timestamp, payload content, priority, and status as a cohesive unit.

---

## DSA Concepts

The routing subsystem applies fundamental Data Structures and Algorithms:

| Concept | Implementation in ResQMesh | Purpose / Complexity |
| :--- | :--- | :--- |
| **Graph** | `NetworkGraph` | Models the peer-to-peer mesh network topology of nodes (devices) and edges (links). |
| **Adjacency List** | `Map<CommunicationDevice, List<CommunicationLink>>` | Implemented with `LinkedHashMap` for deterministic order and efficient neighbor retrieval ($O(V + E)$ space). |
| **Breadth-First Search (BFS)** | `ShortestPathStrategy.findRoute()` | Traverses graph level-by-level to guarantee the path with the minimum number of hops ($O(V + E)$ time). |
| **Queue (FIFO)** | `java.util.Queue` via `ArrayDeque` | Manages the exploration frontier in BFS order. |
| **Visited Set** | `Set<CommunicationDevice>` via `HashSet` | Provides $O(1)$ cycle detection, preventing infinite loops in cyclic mesh topologies. |
| **Parent / Predecessor Map** | `Map<CommunicationDevice, CommunicationDevice>` via `HashMap` | Records `child -> parent` mappings during BFS exploration for later backtracking. |
| **Path Reconstruction** | Backtracking + `Collections.reverse()` | Backtracks from the destination node to the source via the parent map, then reverses the list to produce the route `[source, ..., target]`. |
| **Euclidean Distance** | `Location.distanceTo()` via `Math.hypot()` | Computes physical coordinate distance between device nodes. |

---

## Core Classes

### Model Layer (`com.resqmesh.model`)
- **`CommunicationDevice`:** Abstract base class managing device identity, location, battery level, and operational state.
- **`StudentPhone`:** Handheld peer device that refuses to forward `CRITICAL` priority messages.
- **`SecurityStation` & `MedicalStation`:** Infrastructure hubs authorized to forward all emergency message priorities.
- **`CommunicationLink`:** Represents a directed link between two devices with a `usable()` health check.
- **`Location`:** 2D coordinate container `(x, y)` with distance calculations.
- **`EmergencyMessage`:** Message packet tracking sender, recipient, text, priority, and lifecycle status.
- **`DeviceStatus`:** Enum representing `ACTIVE`, `LOW_BATTERY`, or `OFFLINE`.
- **`Priority`:** Enum representing `LOW`, `NORMAL`, `HIGH`, or `CRITICAL`.
- **`MessageStatus`:** Enum representing `CREATED`, `QUEUED`, `FORWARDING`, `DELIVERED`, or `FAILED`.

### Routing Layer (`com.resqmesh.routing`)
- **`NetworkGraph`:** Graph data structure managing device registration and bidirectional link connections.
- **`RoutingStrategy`:** Strategy pattern interface for route discovery.
- **`ShortestPathStrategy`:** BFS implementation finding the minimum-hop route.

### Simulation Layer (`com.resqmesh.simulation`)
- **`SimulationEngine`:** Coordinates message dispatch, path discovery, battery deduction, and outcome generation.
- **`SimulationResult`:** Immutable record encapsulating delivery status, route devices, and diagnostics.
- **`SimulationDemo`:** Console application demonstrating multi-hop transmission, offline failures, and disconnected topologies.

### Application & UI Layer (`com.resqmesh.ui`, `com.resqmesh`)
- **`App`:** Main JavaFX application providing dashboard, network management, dispatch console, and telemetry views.
- **`NetworkTopologyPane`:** Visual canvas rendering draggable nodes, dynamic connection lines, and glowing route paths.
- **`Launcher`:** Application entry point wrapper for JavaFX execution.

---

## How the System Works

When an emergency message is dispatched, it transitions through the following lifecycle:

```
[1. Compose EmergencyMessage]
           │
           ▼
[2. Mark Status: QUEUED]
           │
           ▼
[3. Validate Registrations & Availability]
    - Are sender and recipient in the NetworkGraph?
    - Are both devices available (battery > 0% and status != OFFLINE)?
           │
           ├─ No ──► [Mark FAILED] ──► Return SimulationResult.failure
           │
           ▼ Yes
[4. Execute RoutingStrategy.findRoute(sender, recipient, graph, priority)]
    - BFS queues sender, explores active neighbors level-by-level
    - Filters out broken links and offline devices
    - Checks neighbor.canForward(priority) for intermediate hops
    - Backtracks from recipient to sender using parent map
           │
           ├─ No Route Found ──► [Mark FAILED] ──► Return SimulationResult.failure
           │
           ▼ Route Found
[5. Mark Status: FORWARDING]
           │
           ▼
[6. Deduct Energy: -2.0% battery from every device along the path]
           │
           ▼
[7. Mark Status: DELIVERED]
           │
           ▼
[8. Return SimulationResult.success(explanation, route)]
```

---

## How to Run

The project includes the Maven Wrapper (`mvnw` for Linux/macOS, `mvnw.cmd` for Windows). No separate Maven installation is required.

### 1. Run All Tests
```bash
# Windows
.\mvnw.cmd test

# Linux / macOS
./mvnw test
```

### 2. Launch the JavaFX Application (GUI Desktop)
```bash
# Windows
.\mvnw.cmd javafx:run

# Linux / macOS
./mvnw javafx:run
```

### 3. Launch the Web Browser Edition (Phone & Laptop)
```bash
# Windows
.\mvnw.cmd compile '-Dexec.mainClass=com.resqmesh.web.ResQMeshWebServer' exec:java

# Linux / macOS
./mvnw compile -Dexec.mainClass="com.resqmesh.web.ResQMeshWebServer" exec:java
```
Open **`http://localhost:8080/`** in your browser.

### 4. Run the Console Demonstration (CLI)
```bash
# Windows
.\mvnw.cmd compile
java -cp target/classes com.resqmesh.simulation.SimulationDemo

# Linux / macOS
./mvnw compile
java -cp target/classes com.resqmesh.simulation.SimulationDemo
```

### 5. Package the Project into a JAR
```bash
# Windows
.\mvnw.cmd package -DskipTests

# Linux / macOS
./mvnw package -DskipTests
```
The compiled JAR is generated in `target/resqmesh-simulator-1.0-SNAPSHOT.jar`.


---

## Test Results

The test suite was executed using JUnit 5 and the Maven Surefire Plugin.

**Execution Summary:**
- **Total Tests Run:** `120`
- **Failures:** `0`
- **Errors:** `0`
- **Skipped:** `0`
- **Build Status:** `BUILD SUCCESS`

### Test Breakdown by Test Class

| # | Test Class | Package | Tests Run | Result |
| :---: | :--- | :--- | :---: | :---: |
| 1 | `AppSettingsManagerTest` | `com.resqmesh.config` | 6 | **PASSED** |
| 2 | `AppSettingsTest` | `com.resqmesh.config` | 8 | **PASSED** |
| 3 | `NetworkConfigManagerTest` | `com.resqmesh.config` | 14 | **PASSED** |
| 4 | `DeviceModelTest` | `com.resqmesh.model` | 5 | **PASSED** |
| 5 | `TutorialManagerTest` | `com.resqmesh.onboarding` | 8 | **PASSED** |
| 6 | `NetworkGraphTest` | `com.resqmesh.routing` | 9 | **PASSED** |
| 7 | `ShortestPathStrategyTest` | `com.resqmesh.routing` | 6 | **PASSED** |
| 8 | `DeviceLifecycleIntegrationTest` | `com.resqmesh.simulation` | 5 | **PASSED** |
| 9 | `EmergencyDeliveryIntegrationTest` | `com.resqmesh.simulation` | 7 | **PASSED** |
| 10 | `EmergencyDispatchValidationTest` | `com.resqmesh.simulation` | 10 | **PASSED** |
| 11 | `SimulationEngineTest` | `com.resqmesh.simulation` | 5 | **PASSED** |
| 12 | `EmergencyHistoryIntegrationTest` | `com.resqmesh.simulation.history` | 5 | **PASSED** |
| 13 | `EmergencyHistoryManagerTest` | `com.resqmesh.simulation.history` | 8 | **PASSED** |
| 14 | `ReplayControllerTest` | `com.resqmesh.simulation.timeline` | 8 | **PASSED** |
| 15 | `SimulationTimelineTest` | `com.resqmesh.simulation.timeline` | 7 | **PASSED** |
| 16 | `NetworkTopologyVisualTest` | `com.resqmesh.ui` | 9 | **PASSED** |
| | **Total** | | **120** | **100% PASSED** |

---

## Project Structure

```text
ResQMesh/
├── pom.xml                                   # Maven configuration and dependencies
├── .gitignore                                # Git ignore rules (build output, logs, IDE metadata)
├── mvnw / mvnw.cmd                           # Maven wrapper scripts
├── README.md                                 # Project documentation
├── samples/
│   └── campus_disaster_mesh.json             # Sample network configuration file
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/resqmesh/
    │   │       ├── App.java                  # Main JavaFX dashboard application
    │   │       ├── Launcher.java             # JavaFX bootstrap launcher
    │   │       ├── config/                   # Settings and network JSON persistence
    │   │       │   ├── AppSettings.java
    │   │       │   ├── AppSettingsManager.java
    │   │       │   ├── ConfigurationException.java
    │   │       │   ├── NetworkConfigDTO.java
    │   │       │   └── NetworkConfigManager.java
    │   │       ├── model/                    # Domain model and OOP hierarchy
    │   │       │   ├── CommunicationDevice.java  # Abstract base class
    │   │       │   ├── CommunicationLink.java    # Directed communication channel
    │   │       │   ├── DeviceStatus.java         # ACTIVE, LOW_BATTERY, OFFLINE
    │   │       │   ├── EmergencyMessage.java     # Message payload entity
    │   │       │   ├── Location.java             # 2D coordinates & Euclidean distance
    │   │       │   ├── MedicalStation.java       # Station subclass
    │   │       │   ├── MessageStatus.java        # Message lifecycle states
    │   │       │   ├── Priority.java             # Message priority levels
    │   │       │   ├── SecurityStation.java      # Station subclass
    │   │       │   └── StudentPhone.java         # Phone subclass (rejects CRITICAL)
    │   │       ├── onboarding/               # Interactive tutorial state machine
    │   │       │   ├── TutorialManager.java
    │   │       │   └── TutorialStep.java
    │   │       ├── routing/                  # Graph data structure and BFS algorithm
    │   │       │   ├── NetworkGraph.java         # Adjacency list graph
    │   │       │   ├── RoutingStrategy.java      # Strategy interface
    │   │       │   └── ShortestPathStrategy.java # BFS routing implementation
    │   │       ├── simulation/               # Simulation engine and replay
    │   │       │   ├── SimulationDemo.java       # Console demonstration runner
    │   │       │   ├── SimulationEngine.java     # Transmission & energy coordinator
    │   │       │   ├── SimulationResult.java     # Delivery result record
    │   │       │   ├── history/
    │   │       │   │   └── EmergencyHistoryManager.java
    │   │       │   └── timeline/
    │   │       │       ├── ReplayController.java
    │   │       │       ├── ReplayState.java
    │   │       │       ├── SimulationEvent.java
    │   │       │       ├── SimulationEventType.java
    │   │       │       ├── SimulationRecord.java
    │   │       │       └── SimulationTimeline.java
    │   │       ├── ui/
    │   │       │   └── NetworkTopologyPane.java  # Visual topology canvas
    │   │       └── web/
    │   │           └── ResQMeshWebServer.java    # Embedded browser HTTP server (port 8080)
    │   └── resources/
    │       ├── style.css                         # Dark-theme application stylesheet
    │       └── web/
    │           └── index.html                    # Responsive browser UI for mobile & desktop
    └── test/
        └── java/com/resqmesh/                # 120 automated JUnit 5 tests
            ├── config/
            ├── model/
            ├── onboarding/
            ├── routing/
            ├── simulation/
            └── ui/
```

---

## Example Scenario

### Scenario: Emergency Medical Dispatch

Consider the following campus disaster network topology:

$$\text{Alice's Phone (Student)} \longleftrightarrow \text{Security Post (Security)} \longleftrightarrow \text{Medical Station (Medical)}$$

1. **Setup:**
   - **Sender:** Alice's Phone (`StudentPhone`, battery: $100\%$, status: `ACTIVE`).
   - **Intermediate Relay:** Security Post (`SecurityStation`, battery: $100\%$, status: `ACTIVE`).
   - **Recipient:** Medical Center (`MedicalStation`, battery: $100\%$, status: `ACTIVE`).
   - **Message:** `"Student injured near North Quad"` with `Priority.HIGH`.

2. **BFS Traversal:**
   - **Step 1 (Source):** Alice's Phone is placed into the `queue`, marked in the `visited` set, and mapped to `null` in `previousMap`.
   - **Step 2 (Explore Alice):** Alice's Phone is polled. Its neighbor is Security Post.
     - Security Post is available (`battery > 0%`, not `OFFLINE`).
     - Security Post is not yet visited.
     - Security Post can forward `HIGH` priority messages (`canForward(Priority.HIGH) == true`).
     - Security Post is added to `visited`, mapped with `previousMap.put(Security, Alice)`, and enqueued.
   - **Step 3 (Explore Security):** Security Post is polled. Its neighbors are Alice's Phone (already visited) and Medical Center.
     - Medical Center is the target destination.
     - Medical Center is available.
     - Medical Center is added to `visited`, mapped with `previousMap.put(Medical, Security)`, and enqueued.
   - **Step 4 (Target Reached):** Medical Center matches the target. BFS search terminates.

3. **Path Reconstruction:**
   - Backtracks from Medical Center: `Medical` $\rightarrow$ `Security` $\rightarrow$ `Alice`.
   - Reverses the backtracked list to yield the forward route:
     $$\text{Alice's Phone} \longrightarrow \text{Security Post} \longrightarrow \text{Medical Station}$$
   - Hop count: **2 hops** across 3 devices.

4. **Energy Deduction & Delivery:**
   - Alice's Phone battery: $100.0\% - 2.0\% = 98.0\%$.
   - Security Post battery: $100.0\% - 2.0\% = 98.0\%$.
   - Medical Center battery: $100.0\% - 2.0\% = 98.0\%$.
   - Message status transitions to `DELIVERED`.
   - Returns a successful `SimulationResult` with the complete path and delivery explanation.
