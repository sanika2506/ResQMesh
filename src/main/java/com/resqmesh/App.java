package com.resqmesh;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.DeviceStatus;
import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.model.Location;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.Priority;
import com.resqmesh.model.SecurityStation;
import com.resqmesh.model.StudentPhone;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * Main JavaFX GUI application for the ResQMesh Simulator.
 * Step 6: Full User Interface Integration.
 * Connects UI controls directly to SimulationEngine and NetworkGraph.
 */
public class App extends Application {

    private static final String APP_TITLE = "ResQMesh – Offline Communication Simulator";
    private static final int DEFAULT_WIDTH = 1180;
    private static final int DEFAULT_HEIGHT = 760;

    // Backend Core
    private NetworkGraph graph;
    private SimulationEngine engine;

    // Observable UI Data
    private ObservableList<CommunicationDevice> deviceObservableList;
    private int deviceIdCounter = 1;
    private int messageCounter = 1;

    // UI Components
    private TableView<CommunicationDevice> deviceTable;
    private ComboBox<CommunicationDevice> connectDeviceAComboBox;
    private ComboBox<CommunicationDevice> connectDeviceBComboBox;
    private ComboBox<CommunicationDevice> senderComboBox;
    private ComboBox<CommunicationDevice> recipientComboBox;
    private ComboBox<Priority> priorityComboBox;
    private ComboBox<CommunicationDevice> toggleDeviceComboBox;
    private TextField messageTextField;
    private TextArea activityLogArea;

    // Result Display Labels
    private Label resultBadge;
    private Label resultRouteLabel;
    private Label resultExplanationLabel;
    private Label statsLabel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Backend
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        deviceObservableList = FXCollections.observableArrayList();

        // Build UI Layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #0f172a;"); // Sleek dark slate background

        root.setTop(createHeader());

        // Split view: Left = Network & Device Management; Right = Dispatch & Activity Log
        HBox mainContent = new HBox(20);
        mainContent.setPadding(new Insets(15, 20, 20, 20));
        HBox.setHgrow(mainContent, javafx.scene.layout.Priority.ALWAYS);

        VBox leftColumn = createLeftColumn();
        VBox rightColumn = createRightColumn();

        HBox.setHgrow(leftColumn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rightColumn, javafx.scene.layout.Priority.ALWAYS);
        leftColumn.setPrefWidth(660);
        rightColumn.setPrefWidth(480);

        mainContent.getChildren().addAll(leftColumn, rightColumn);
        root.setCenter(mainContent);

        // Preload default campus emergency mesh network for immediate testing
        loadSampleNetwork();

        // Create scene and show
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(960);
        primaryStage.setMinHeight(640);
        primaryStage.show();

        log("System initialized. Preloaded campus emergency mesh network.");
    }

    // -------------------------------------------------------------
    // Top Header
    // -------------------------------------------------------------
    private VBox createHeader() {
        VBox headerBox = new VBox(6);
        headerBox.setPadding(new Insets(16, 20, 12, 20));
        headerBox.setStyle("-fx-background-color: #1e293b; -fx-border-color: #334155; -fx-border-width: 0 0 1 0;");

        HBox topRow = new HBox(15);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("ResQMesh");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #38bdf8; -fx-font-family: 'Segoe UI', sans-serif;");

        Label badgeLabel = new Label("Offline Ad-Hoc Mesh Simulator");
        badgeLabel.setStyle("-fx-background-color: #0369a1; -fx-text-fill: #e0f2fe; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8 3 8; -fx-background-radius: 4px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        statsLabel = new Label("Devices: 0 (0 Online) | Mesh Links: 0");
        statsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 600;");

        Button loadSampleBtn = new Button("Reset Sample Mesh");
        loadSampleBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #e2e8f0; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6px;");
        loadSampleBtn.setOnAction(e -> {
            loadSampleNetwork();
            log("Reset to default sample emergency network topology.");
        });

        Button clearAllBtn = new Button("Clear Graph");
        clearAllBtn.setStyle("-fx-background-color: #451a03; -fx-text-fill: #fde047; -fx-font-size: 12px; -fx-cursor: hand; -fx-background-radius: 6px;");
        clearAllBtn.setOnAction(e -> {
            graph.clear();
            refreshUI();
            log("Network graph cleared. All devices and links removed.");
        });

        topRow.getChildren().addAll(titleLabel, badgeLabel, spacer, statsLabel, loadSampleBtn, clearAllBtn);
        headerBox.getChildren().add(topRow);

        return headerBox;
    }

    // -------------------------------------------------------------
    // Left Column: Network Devices Table & Management Forms
    // -------------------------------------------------------------
    private VBox createLeftColumn() {
        VBox col = new VBox(15);

        // Section A: Network Devices Table
        VBox tableCard = createCardContainer("Active Network Devices & Mesh Links");
        deviceTable = new TableView<>();
        deviceTable.setPlaceholder(new Label("No devices registered in the network graph."));
        deviceTable.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 6px;");
        deviceTable.setPrefHeight(250);

        // Column 1: Name & ID
        TableColumn<CommunicationDevice, String> nameCol = new TableColumn<>("Device Name (ID)");
        nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName() + " [" + cell.getValue().getId() + "]"));

        // Column 2: Device Type
        TableColumn<CommunicationDevice, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(120);
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getClass().getSimpleName()));

        // Column 3: Battery Level
        TableColumn<CommunicationDevice, String> batteryCol = new TableColumn<>("Battery");
        batteryCol.setPrefWidth(80);
        batteryCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f%%", cell.getValue().getBatteryLevel())));

        // Column 4: Operational Status
        TableColumn<CommunicationDevice, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(100);
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        statusCol.setCellFactory(colData -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
                    } else if ("LOW_BATTERY".equalsIgnoreCase(item)) {
                        setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Column 5: Connected Peers
        TableColumn<CommunicationDevice, String> linksCol = new TableColumn<>("Connected Peers");
        linksCol.setPrefWidth(170);
        linksCol.setCellValueFactory(cell -> {
            String peers = graph.getLinks(cell.getValue()).stream()
                    .map(l -> l.getDestination().getName())
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(peers.isEmpty() ? "(Isolated)" : peers);
        });

        deviceTable.getColumns().addAll(nameCol, typeCol, batteryCol, statusCol, linksCol);
        deviceTable.setItems(deviceObservableList);

        // When a row is selected in the table, sync it with the toggle dropdown
        deviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                toggleDeviceComboBox.setValue(newVal);
            }
        });

        tableCard.getChildren().add(deviceTable);

        // Section B: Device State Toggle & Battery Controls
        HBox deviceActionRow = new HBox(10);
        deviceActionRow.setAlignment(Pos.CENTER_LEFT);

        Label toggleLabel = new Label("Selected Device:");
        toggleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");

        toggleDeviceComboBox = createDeviceComboBox();
        toggleDeviceComboBox.setPrefWidth(220);

        Button toggleStatusBtn = new Button("Toggle Online / Offline");
        toggleStatusBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        toggleStatusBtn.setOnAction(e -> handleToggleStatus());

        Button rechargeBtn = new Button("Recharge 100%");
        rechargeBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        rechargeBtn.setOnAction(e -> handleRecharge());

        deviceActionRow.getChildren().addAll(toggleLabel, toggleDeviceComboBox, toggleStatusBtn, rechargeBtn);
        tableCard.getChildren().add(deviceActionRow);

        // Section C: Form to Add New Device
        VBox addDeviceCard = createCardContainer("Add Virtual Device");
        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(10);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g., Alice's Phone, Gate 1 Post");
        nameField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 4px;");

        ComboBox<String> typeSelect = new ComboBox<>();
        typeSelect.getItems().addAll("Student Phone", "Security Station", "Medical Station");
        typeSelect.setValue("Student Phone");
        typeSelect.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155;");

        Button addDeviceBtn = new Button("Add Device");
        addDeviceBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        addDeviceBtn.setOnAction(e -> {
            String name = nameField.getText();
            String type = typeSelect.getValue();
            if (name == null || name.trim().isEmpty()) {
                name = type + " #" + deviceIdCounter;
            }

            String id = "DEV-" + (deviceIdCounter++);
            Location loc = new Location((deviceIdCounter * 10) % 100, (deviceIdCounter * 15) % 100);
            CommunicationDevice newDev;
            if ("Security Station".equals(type)) {
                newDev = new SecurityStation(id, name.trim(), loc, 100.0);
            } else if ("Medical Station".equals(type)) {
                newDev = new MedicalStation(id, name.trim(), loc, 100.0);
            } else {
                newDev = new StudentPhone(id, name.trim(), loc, 100.0);
            }

            graph.addDevice(newDev);
            nameField.clear();
            refreshUI();
            log("Added " + type + ": '" + newDev.getName() + "' [" + newDev.getId() + "] at " + loc);
        });

        addGrid.add(createFormLabel("Device Name:"), 0, 0);
        addGrid.add(nameField, 1, 0);
        addGrid.add(createFormLabel("Device Type:"), 2, 0);
        addGrid.add(typeSelect, 3, 0);
        addGrid.add(addDeviceBtn, 4, 0);

        addDeviceCard.getChildren().add(addGrid);

        // Section D: Form to Connect Two Devices
        VBox connectCard = createCardContainer("Connect Devices (Establish Mesh Link)");
        HBox connectRow = new HBox(10);
        connectRow.setAlignment(Pos.CENTER_LEFT);

        connectDeviceAComboBox = createDeviceComboBox();
        connectDeviceBComboBox = createDeviceComboBox();

        Button connectBtn = new Button("Connect Link");
        connectBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        connectBtn.setOnAction(e -> {
            CommunicationDevice devA = connectDeviceAComboBox.getValue();
            CommunicationDevice devB = connectDeviceBComboBox.getValue();

            if (devA == null || devB == null) {
                showAlert("Selection Missing", "Please select both devices to establish a mesh link.");
                return;
            }
            if (devA.equals(devB)) {
                showAlert("Invalid Connection", "Cannot connect a device to itself.");
                return;
            }

            boolean connected = graph.connect(devA, devB);
            if (connected) {
                refreshUI();
                log(String.format("Mesh link established: '%s' <---> '%s'", devA.getName(), devB.getName()));
            } else {
                showAlert("Connection Exists", "Devices are already directly connected.");
            }
        });

        connectRow.getChildren().addAll(
                createFormLabel("Device A:"), connectDeviceAComboBox,
                createFormLabel("Device B:"), connectDeviceBComboBox,
                connectBtn
        );
        connectCard.getChildren().add(connectRow);

        col.getChildren().addAll(tableCard, addDeviceCard, connectCard);
        return col;
    }

    // -------------------------------------------------------------
    // Right Column: Message Dispatch, Results & Activity Log
    // -------------------------------------------------------------
    private VBox createRightColumn() {
        VBox col = new VBox(15);

        // Section 1: Dispatch Emergency Message Form
        VBox dispatchCard = createCardContainer("Dispatch Emergency Message");
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        senderComboBox = createDeviceComboBox();
        recipientComboBox = createDeviceComboBox();

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        priorityComboBox.setValue(Priority.NORMAL);
        priorityComboBox.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155;");

        messageTextField = new TextField();
        messageTextField.setPromptText("Enter emergency alert or request details...");
        messageTextField.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155; -fx-border-radius: 4px;");

        Button sendBtn = new Button("Send Emergency Message");
        sendBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 8 16; -fx-cursor: hand; -fx-background-radius: 6px;");
        sendBtn.setMaxWidth(Double.MAX_VALUE);
        sendBtn.setOnAction(e -> handleSendMessage());

        formGrid.add(createFormLabel("Sender:"), 0, 0);
        formGrid.add(senderComboBox, 1, 0);
        formGrid.add(createFormLabel("Priority:"), 2, 0);
        formGrid.add(priorityComboBox, 3, 0);

        formGrid.add(createFormLabel("Recipient:"), 0, 1);
        formGrid.add(recipientComboBox, 1, 1);
        formGrid.add(createFormLabel("Payload:"), 2, 1);
        formGrid.add(messageTextField, 3, 1);

        dispatchCard.getChildren().addAll(formGrid, sendBtn);

        // Section 2: Delivery Outcome Card
        VBox outcomeCard = createCardContainer("Last Simulation Delivery Result");
        outcomeCard.setStyle(outcomeCard.getStyle() + "; -fx-background-color: #172554;");

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label outcomeTitle = new Label("Status:");
        outcomeTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold;");

        resultBadge = new Label("WAITING FOR DISPATCH");
        resultBadge.setStyle("-fx-background-color: #334155; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px;");

        statusRow.getChildren().addAll(outcomeTitle, resultBadge);

        resultRouteLabel = new Label("Route: (None)");
        resultRouteLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: 600; -fx-font-size: 12px;");
        resultRouteLabel.setWrapText(true);

        resultExplanationLabel = new Label("Ready to simulate message transmission across the mesh network.");
        resultExplanationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        resultExplanationLabel.setWrapText(true);

        outcomeCard.getChildren().addAll(statusRow, resultRouteLabel, resultExplanationLabel);

        // Section 3: Activity & Event Log
        VBox logCard = createCardContainer("Mesh Activity & Event Log");
        VBox.setVgrow(logCard, javafx.scene.layout.Priority.ALWAYS);

        activityLogArea = new TextArea();
        activityLogArea.setEditable(false);
        activityLogArea.setWrapText(true);
        activityLogArea.setStyle("-fx-control-inner-background: #090d16; -fx-text-fill: #38bdf8; -fx-font-family: 'Consolas', monospace; -fx-font-size: 11px; -fx-border-color: #334155; -fx-border-radius: 4px;");
        VBox.setVgrow(activityLogArea, javafx.scene.layout.Priority.ALWAYS);

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.setStyle("-fx-background-color: #334155; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-cursor: hand;");
        clearLogBtn.setOnAction(e -> activityLogArea.clear());

        logCard.getChildren().addAll(activityLogArea, clearLogBtn);

        col.getChildren().addAll(dispatchCard, outcomeCard, logCard);
        return col;
    }

    // -------------------------------------------------------------
    // Core Handlers
    // -------------------------------------------------------------
    private void handleSendMessage() {
        CommunicationDevice sender = senderComboBox.getValue();
        CommunicationDevice recipient = recipientComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        String content = messageTextField.getText();

        if (sender == null || recipient == null) {
            showAlert("Incomplete Form", "Please select both a sender and a recipient device.");
            return;
        }

        if (sender.equals(recipient)) {
            showAlert("Invalid Route", "Sender and recipient cannot be the same device.");
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            content = "Emergency request from " + sender.getName();
        }

        String msgId = "MSG-" + (messageCounter++);
        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content.trim(), priority);

        log(String.format("Dispatched [%s] from '%s' to '%s' | Priority: %s", msgId, sender.getName(), recipient.getName(), priority));

        // Call SimulationEngine
        SimulationResult result = engine.send(message);

        // Update Outcome Display
        if (result.delivered()) {
            resultBadge.setText("DELIVERED");
            resultBadge.setStyle("-fx-background-color: #065f46; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px;");

            String routeStr = result.route().stream()
                    .map(CommunicationDevice::getName)
                    .collect(Collectors.joining(" -> "));
            resultRouteLabel.setText("Discovered Route: " + routeStr);
            resultExplanationLabel.setText(result.explanation() + " (-2.0% battery applied to all nodes on route).");

            log(String.format("Delivery SUCCESS: [%s] delivered via %d hops. Path: %s", msgId, Math.max(0, result.route().size() - 1), routeStr));
        } else {
            resultBadge.setText("FAILED");
            resultBadge.setStyle("-fx-background-color: #7f1d1d; -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px;");

            resultRouteLabel.setText("Route: None (Path blocked or unreachable)");
            resultExplanationLabel.setText(result.explanation());

            log(String.format("Delivery FAILED: [%s]. Reason: %s", msgId, result.explanation()));
        }

        refreshUI();
    }

    private void handleToggleStatus() {
        CommunicationDevice dev = toggleDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device to toggle online/offline state.");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            if (dev.getBatteryLevel() <= 0) {
                showAlert("Battery Depleted", "Cannot turn online: battery is 0%. Please recharge first.");
                log("Failed to bring " + dev.getName() + " online: battery is 0%.");
                return;
            }
            dev.setStatus(dev.getBatteryLevel() <= CommunicationDevice.LOW_BATTERY_THRESHOLD
                    ? DeviceStatus.LOW_BATTERY : DeviceStatus.ACTIVE);
            log("Device '" + dev.getName() + "' toggled to ONLINE (" + dev.getStatus() + ").");
        } else {
            dev.setStatus(DeviceStatus.OFFLINE);
            log("Device '" + dev.getName() + "' toggled to OFFLINE.");
        }

        refreshUI();
    }

    private void handleRecharge() {
        CommunicationDevice dev = toggleDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device to recharge.");
            return;
        }

        dev.recharge(100.0);
        log("Device '" + dev.getName() + "' battery recharged to 100% (ACTIVE).");
        refreshUI();
    }

    // -------------------------------------------------------------
    // Helper Methods & UI Utilities
    // -------------------------------------------------------------
    private void loadSampleNetwork() {
        graph.clear();
        deviceIdCounter = 1;

        StudentPhone alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(10, 10), 100.0);
        StudentPhone bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(30, 20), 100.0);
        SecurityStation security = new SecurityStation("SEC-1", "Security Post", new Location(50, 40), 100.0);
        MedicalStation medical = new MedicalStation("MED-1", "Medical Center", new Location(70, 60), 100.0);
        StudentPhone charlie = new StudentPhone("DEV-3", "Charlie's Phone (Isolated)", new Location(90, 80), 80.0);

        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(charlie); // Isolated node for testing failure

        // Create Mesh Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        deviceIdCounter = 4;
        refreshUI();

        // Preset defaults in dropdowns
        senderComboBox.setValue(alice);
        recipientComboBox.setValue(medical);
        toggleDeviceComboBox.setValue(alice);
    }

    private void refreshUI() {
        deviceObservableList.setAll(graph.getAllDevices());
        deviceTable.refresh();

        // Update Stats
        int totalDevices = graph.getDeviceCount();
        long activeDevices = graph.getAllDevices().stream().filter(CommunicationDevice::isAvailable).count();
        int totalLinks = graph.getTotalLinkCount() / 2; // Bidirectional pairs
        statsLabel.setText(String.format("Devices: %d (%d Online) | Mesh Links: %d", totalDevices, activeDevices, totalLinks));
    }

    private ComboBox<CommunicationDevice> createDeviceComboBox() {
        ComboBox<CommunicationDevice> box = new ComboBox<>(deviceObservableList);
        box.setStyle("-fx-background-color: #0f172a; -fx-text-fill: white; -fx-border-color: #334155;");
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(CommunicationDevice d) {
                if (d == null) return "-- Select Device --";
                String state = (d.getStatus() == DeviceStatus.OFFLINE) ? "OFFLINE" : String.format("%.0f%%", d.getBatteryLevel());
                return d.getName() + " (" + state + ")";
            }

            @Override
            public CommunicationDevice fromString(String string) {
                return null;
            }
        });
        return box;
    }

    private VBox createCardContainer(String title) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(14));
        card.setStyle(
                "-fx-background-color: #1e293b; " +
                "-fx-background-radius: 8px; " +
                "-fx-border-color: #334155; " +
                "-fx-border-radius: 8px; " +
                "-fx-border-width: 1px;"
        );

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 14px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI', sans-serif;");
        card.getChildren().add(titleLabel);

        return card;
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");
        return lbl;
    }

    private void log(String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        activityLogArea.appendText("[" + timestamp + "] " + message + "\n");
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("ResQMesh Notice");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
