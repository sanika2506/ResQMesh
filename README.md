# ResQMesh – Offline Communication Simulator

ResQMesh is an offline ad-hoc peer-to-peer communication network simulator designed to model emergency and disaster relief scenarios where traditional cellular and internet infrastructures are unavailable.

---

## 🛠️ Tech Stack & Requirements

- **Language:** Java 17+ (Tested on Java 21 LTS)
- **GUI Framework:** JavaFX 21 (Controls, Graphics, FXML, Base)
- **Build Tool:** Apache Maven
- **Architecture:** Object-Oriented Programming (OOP) principles

---

## 📂 Project Structure

```text
ResQMesh/
├── pom.xml                                   # Maven configuration file (dependencies, plugins, compiler settings)
├── .gitignore                                # Excludes build artifacts (target/) and IDE caches
├── mvnw & mvnw.cmd                           # Maven wrapper scripts
├── README.md                                 # Project documentation and run guide
└── src/
    ├── main/
    │   └── java/
    │       └── com/
    │           └── resqmesh/
    │               ├── App.java              # Main JavaFX application and UI entry point
    │               ├── Launcher.java         # Auxiliary launcher to bypass module-path restrictions
    │               ├── model/                # Core OOP domain models
    │               │   ├── CommunicationDevice.java  # Abstract base device node
    │               │   ├── CommunicationLink.java    # Directed communication channel
    │               │   ├── DeviceStatus.java         # Enum: ACTIVE, OFFLINE, LOW_BATTERY
    │               │   ├── EmergencyMessage.java     # Message payload, priority, and status
    │               │   ├── Location.java             # 2D coordinates & Euclidean distance
    │               │   ├── MedicalStation.java       # Station subclass (handles all priorities)
    │               │   ├── MessageStatus.java        # Enum: CREATED, QUEUED, etc.
    │               │   ├── Priority.java             # Enum: LOW, NORMAL, HIGH, CRITICAL
    │               │   ├── SecurityStation.java      # Station subclass (handles all priorities)
    │               │   └── StudentPhone.java         # Handheld subclass (cannot forward CRITICAL)
    │               ├── routing/
    │                   ├── NetworkGraph.java         # Adjacency list mesh network graph
    │                   ├── RoutingStrategy.java      # Strategy interface for pathfinding
    │                   └── ShortestPathStrategy.java # BFS shortest-hop route discovery
    │               └── simulation/
    │                   ├── SimulationDemo.java       # Standalone console demonstration runner
    │                   ├── SimulationEngine.java     # Transmission, battery & lifecycle coordinator
    │                   └── SimulationResult.java     # Delivery outcome record
    └── test/
        └── java/
            └── com/
                └── resqmesh/
                    ├── model/
                    │   └── DeviceModelTest.java          # Model unit tests
                    ├── routing/
                    │   ├── NetworkGraphTest.java         # Graph & link unit tests
                    │   └── ShortestPathStrategyTest.java # BFS routing algorithm unit tests
                    └── simulation/
                        └── SimulationEngineTest.java     # Simulation engine unit tests
```

### Explanation of Files:

1. **`pom.xml`**:
   The Project Object Model file for Maven. It defines:
   - Target Java release (`17` / compatible with Java 21).
   - JavaFX dependencies: `javafx-controls`, `javafx-graphics`, `javafx-fxml`, and `javafx-base` (version `21.0.2`).
   - `maven-compiler-plugin`: Ensures standardized compilation flags.
   - `javafx-maven-plugin`: Configured with the main class `com.resqmesh.App` to enable running the application via `mvn javafx:run`.

2. **`src/main/java/com/resqmesh/App.java`**:
   - The primary JavaFX GUI `Application` subclass for the simulator.
   - Connected directly to `SimulationEngine` and `NetworkGraph`.
   - Features:
     - Real-time `TableView` displaying device names, types, battery %, status, and connected peers.
     - Controls to add virtual devices (`StudentPhone`, `SecurityStation`, `MedicalStation`).
     - Mesh link connection controls to connect two selected devices bidirectionally.
     - Device state toggles (Online/Offline) and 100% battery recharge.
     - Emergency message dispatch form (Sender, Recipient, Priority, Payload).
     - Simulation outcome card displaying real-time delivery status, multi-hop route path, and explanations.
     - Live, scrollable timestamped activity log.
     - Top stats bar with total devices, active online devices, and total mesh links.

3. **`src/main/java/com/resqmesh/Launcher.java`**:
   - A companion entry class that calls `App.main(args)`.
   - Useful when executing directly from IDEs (such as VS Code or Eclipse) without module-path VM flags, avoiding the common JavaFX runtime components error.

4. **`.gitignore`**:
   - Keeps git tracking clean by ignoring Maven output directories (`target/`) and IDE metadata files (`.idea`, `.vscode`, etc.).

---

## 🚀 How to Run the Project

### Option 1: Using Maven (Recommended)

In your terminal (PowerShell or Command Prompt) inside the `ResQMesh` project folder:

```bash
mvn javafx:run
```

To clean and compile before running:
```bash
mvn clean compile javafx:run
```

### Option 2: Using an IDE (IntelliJ IDEA / Eclipse / VS Code)

- Open the `ResQMesh` folder as a Maven project.
- Maven will automatically detect the dependencies and SDK.
- Right-click `Launcher.java` (or `App.java`) and click **Run**.
