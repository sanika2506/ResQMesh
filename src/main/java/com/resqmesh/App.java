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
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main JavaFX GUI application for the ResQMesh Simulator.
 * Step 7: UI Design and Usability Improvements.
 * Polished, beginner-friendly emergency command & control dashboard.
 */
public class App extends Application {

    private static final String APP_TITLE = "ResQMesh – Offline Communication Simulator";
    private static final int DEFAULT_WIDTH = 1240;
    private static final int DEFAULT_HEIGHT = 820;

    // Backend Simulation Core
    private NetworkGraph graph;
    private SimulationEngine engine;

    // Observable State
    private ObservableList<CommunicationDevice> deviceObservableList;
    private int deviceIdCounter = 1;
    private int messageCounter = 1;
    private int totalMessagesSent = 0;
    private int successfulDeliveries = 0;

    // Top Stat Labels
    private Label statTotalNodesLabel;
    private Label statOnlineNodesLabel;
    private Label statMeshLinksLabel;
    private Label statMessagesCountLabel;

    // Left Column Controls (Network & Device Management)
    private TableView<CommunicationDevice> deviceTable;
    private ComboBox<CommunicationDevice> toggleDeviceComboBox;
    private Button toggleStatusBtn;
    private Button rechargeBtn;
    private TextField addDeviceNameField;
    private ComboBox<String> addDeviceTypeSelect;
    private ComboBox<CommunicationDevice> connectDeviceAComboBox;
    private ComboBox<CommunicationDevice> connectDeviceBComboBox;

    // Right Column Controls (Messaging & Results)
    private ComboBox<CommunicationDevice> senderComboBox;
    private ComboBox<CommunicationDevice> recipientComboBox;
    private ComboBox<Priority> priorityComboBox;
    private TextField messageTextField;
    private Button sendEmergencyBtn;

    // Outcome Card Controls
    private Label resultBadge;
    private HBox resultRouteContainer;
    private Label resultExplanationLabel;
    private VBox outcomeCard;

    // Activity Log
    private TextArea activityLogArea;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Backend
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        deviceObservableList = FXCollections.observableArrayList();

        // Build Master Layout
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #090d16;");

        root.setTop(createHeaderAndStats());

        // Split Layout: Left (Network & Nodes) | Right (Dispatch & Diagnostics)
        HBox mainContent = new HBox(18);
        mainContent.setPadding(new Insets(14, 20, 20, 20));
        HBox.setHgrow(mainContent, javafx.scene.layout.Priority.ALWAYS);

        VBox leftColumn = createLeftColumn();
        VBox rightColumn = createRightColumn();

        HBox.setHgrow(leftColumn, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(rightColumn, javafx.scene.layout.Priority.ALWAYS);
        leftColumn.setPrefWidth(680);
        rightColumn.setPrefWidth(520);

        mainContent.getChildren().addAll(leftColumn, rightColumn);
        root.setCenter(mainContent);

        // Preload default campus emergency mesh network
        loadSampleNetwork();

        // Create Scene and Apply External CSS if available
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1080);
        primaryStage.setMinHeight(720);
        primaryStage.show();

        log("SYSTEM", "ResQMesh Simulation Engine initialized. Preloaded campus emergency mesh topology.");
    }

    // -------------------------------------------------------------
    // Top Header & Metric Stats Cards
    // -------------------------------------------------------------
    private VBox createHeaderAndStats() {
        VBox headerContainer = new VBox(12);
        headerContainer.setPadding(new Insets(16, 20, 12, 20));
        headerContainer.setStyle("-fx-background-color: #0f172a; -fx-border-color: #1e293b; -fx-border-width: 0 0 1 0;");

        // Row 1: Brand & Global Actions
        HBox brandRow = new HBox(16);
        brandRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLabel = new Label("ResQMesh");
        titleLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-font-family: 'Segoe UI', sans-serif;");

        Label subtitleBadge = new Label("Offline Ad-Hoc Mesh Simulator");
        subtitleBadge.setStyle("-fx-background-color: #0369a1; -fx-text-fill: #e0f2fe; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 6px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button resetSampleBtn = new Button("↺ Reset Sample Mesh");
        resetSampleBtn.getStyleClass().add("btn-secondary");
        resetSampleBtn.setOnAction(e -> {
            loadSampleNetwork();
            log("TOPOLOGY", "Reset to standard campus disaster relief network.");
        });

        Button clearAllBtn = new Button("✕ Clear Graph");
        clearAllBtn.setStyle("-fx-background-color: #3b1616; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 12;");
        clearAllBtn.setOnAction(e -> {
            graph.clear();
            refreshUI();
            log("TOPOLOGY", "Network graph cleared. All devices and links wiped.");
        });

        brandRow.getChildren().addAll(titleLabel, subtitleBadge, spacer, resetSampleBtn, clearAllBtn);

        // Row 2: Four Key Metric Stat Cards
        HBox statsRow = new HBox(12);
        statsRow.setAlignment(Pos.CENTER_LEFT);

        statTotalNodesLabel = new Label("0");
        statOnlineNodesLabel = new Label("0");
        statMeshLinksLabel = new Label("0");
        statMessagesCountLabel = new Label("0 (0%)");

        VBox cardTotal = createStatCard("TOTAL NODES", statTotalNodesLabel, "#38bdf8", "📱");
        VBox cardOnline = createStatCard("ONLINE NODES", statOnlineNodesLabel, "#34d399", "🟢");
        VBox cardLinks = createStatCard("ACTIVE MESH LINKS", statMeshLinksLabel, "#818cf8", "🔗");
        VBox cardSent = createStatCard("MESSAGES SENT", statMessagesCountLabel, "#f59e0b", "📨");

        HBox.setHgrow(cardTotal, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(cardOnline, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(cardLinks, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(cardSent, javafx.scene.layout.Priority.ALWAYS);

        statsRow.getChildren().addAll(cardTotal, cardOnline, cardLinks, cardSent);

        headerContainer.getChildren().addAll(brandRow, statsRow);
        return headerContainer;
    }

    private VBox createStatCard(String title, Label valueLabel, String accentColor, String icon) {
        VBox card = new VBox(2);
        card.getStyleClass().add("stat-card");

        HBox topHBox = new HBox(6);
        topHBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLabel = new Label(icon);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("stat-title");

        topHBox.getChildren().addAll(iconLabel, titleLabel);

        valueLabel.getStyleClass().add("stat-number");
        valueLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        card.getChildren().addAll(topHBox, valueLabel);
        return card;
    }

    // -------------------------------------------------------------
    // Left Column: Device Table & Network Management Cards
    // -------------------------------------------------------------
    private VBox createLeftColumn() {
        VBox col = new VBox(14);

        // Card A: Active Network Devices Table
        VBox tableCard = createCardContainer("Active Mesh Devices & Connection Status", "📡");
        VBox.setVgrow(tableCard, javafx.scene.layout.Priority.ALWAYS);

        deviceTable = new TableView<>();
        deviceTable.setPlaceholder(new Label("No communication devices registered in the network."));
        deviceTable.setPrefHeight(260);

        // Col 1: Name & ID
        TableColumn<CommunicationDevice, String> nameCol = new TableColumn<>("Device Name (ID)");
        nameCol.setPrefWidth(165);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName() + " [" + cell.getValue().getId() + "]"));

        // Col 2: Type with Subclass Icon
        TableColumn<CommunicationDevice, String> typeCol = new TableColumn<>("Device Type");
        typeCol.setPrefWidth(130);
        typeCol.setCellValueFactory(cell -> {
            CommunicationDevice dev = cell.getValue();
            if (dev instanceof SecurityStation) return new SimpleStringProperty("🛡️ Security Post");
            if (dev instanceof MedicalStation) return new SimpleStringProperty("🏥 Medical Center");
            return new SimpleStringProperty("📱 Student Phone");
        });

        // Col 3: Battery Level (Percentage & Color Cue)
        TableColumn<CommunicationDevice, String> batteryCol = new TableColumn<>("Battery");
        batteryCol.setPrefWidth(90);
        batteryCol.setCellValueFactory(cell -> new SimpleStringProperty(String.format("%.1f%%", cell.getValue().getBatteryLevel())));
        batteryCol.setCellFactory(colData -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    try {
                        double val = Double.parseDouble(item.replace("%", "").trim());
                        if (val > 50.0) {
                            setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
                        } else if (val >= 20.0) {
                            setStyle("-fx-text-fill: #fbbf24; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                        }
                    } catch (Exception ignored) {
                        setStyle("-fx-text-fill: #cbd5e1;");
                    }
                }
            }
        });

        // Col 4: Visual Status Badge (ACTIVE, LOW_BATTERY, OFFLINE)
        TableColumn<CommunicationDevice, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(110);
        statusCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getStatus().name()));
        statusCol.setCellFactory(colData -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("ACTIVE".equalsIgnoreCase(item)) {
                        setText("🟢 ONLINE");
                        setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4px; -fx-alignment: center;");
                    } else if ("LOW_BATTERY".equalsIgnoreCase(item)) {
                        setText("⚠️ LOW BATT");
                        setStyle("-fx-background-color: #451a03; -fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4px; -fx-alignment: center;");
                    } else {
                        setText("🔴 OFFLINE");
                        setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-weight: bold; -fx-padding: 3 6; -fx-background-radius: 4px; -fx-alignment: center;");
                    }
                }
            }
        });

        // Col 5: Connected Peers
        TableColumn<CommunicationDevice, String> linksCol = new TableColumn<>("Connected Peers");
        linksCol.setPrefWidth(170);
        linksCol.setCellValueFactory(cell -> {
            String peers = graph.getLinks(cell.getValue()).stream()
                    .map(l -> l.getDestination().getName())
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(peers.isEmpty() ? "(Isolated Node)" : peers);
        });

        deviceTable.getColumns().add(nameCol);
        deviceTable.getColumns().add(typeCol);
        deviceTable.getColumns().add(batteryCol);
        deviceTable.getColumns().add(statusCol);
        deviceTable.getColumns().add(linksCol);
        deviceTable.setItems(deviceObservableList);

        // Row Selection updates Selected Device Dropdown
        deviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                toggleDeviceComboBox.setValue(newVal);
                updateToggleButtonsState(newVal);
            }
        });

        tableCard.getChildren().add(deviceTable);

        // Section B: Device Controls (Toggle Status & Recharge)
        HBox deviceActionRow = new HBox(10);
        deviceActionRow.setAlignment(Pos.CENTER_LEFT);

        Label toggleLabel = new Label("Selected Node:");
        toggleLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");

        toggleDeviceComboBox = createDeviceComboBox();
        toggleDeviceComboBox.setPrefWidth(210);
        toggleDeviceComboBox.valueProperty().addListener((obs, oldV, newV) -> updateToggleButtonsState(newV));

        toggleStatusBtn = new Button("Toggle Online / Offline");
        toggleStatusBtn.getStyleClass().add("btn-danger");
        toggleStatusBtn.setOnAction(e -> handleToggleStatus());

        rechargeBtn = new Button("⚡ Recharge 100%");
        rechargeBtn.getStyleClass().add("btn-success");
        rechargeBtn.setOnAction(e -> handleRecharge());

        deviceActionRow.getChildren().addAll(toggleLabel, toggleDeviceComboBox, toggleStatusBtn, rechargeBtn);
        tableCard.getChildren().add(deviceActionRow);

        // Card C: Add Virtual Device
        VBox addDeviceCard = createCardContainer("Add Virtual Device to Network", "➕");
        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(8);

        addDeviceNameField = new TextField();
        addDeviceNameField.setPromptText("e.g. Alice's Phone, Gate 1 Post");
        addDeviceNameField.setPrefWidth(180);

        addDeviceTypeSelect = new ComboBox<>();
        addDeviceTypeSelect.getItems().addAll("Student Phone", "Security Station", "Medical Station");
        addDeviceTypeSelect.setValue("Student Phone");
        addDeviceTypeSelect.setPrefWidth(150);

        Button addDeviceBtn = new Button("➕ Add Device");
        addDeviceBtn.getStyleClass().add("btn-primary");
        addDeviceBtn.setOnAction(e -> handleAddDevice());

        addGrid.add(createFormLabel("Device Name:"), 0, 0);
        addGrid.add(addDeviceNameField, 1, 0);
        addGrid.add(createFormLabel("Device Type:"), 2, 0);
        addGrid.add(addDeviceTypeSelect, 3, 0);
        addGrid.add(addDeviceBtn, 4, 0);

        addDeviceCard.getChildren().add(addGrid);

        // Card D: Connect Devices (Establish Mesh Link)
        VBox connectCard = createCardContainer("Establish Mesh Link (Bidirectional)", "🔗");
        HBox connectRow = new HBox(10);
        connectRow.setAlignment(Pos.CENTER_LEFT);

        connectDeviceAComboBox = createDeviceComboBox();
        connectDeviceAComboBox.setPrefWidth(190);

        connectDeviceBComboBox = createDeviceComboBox();
        connectDeviceBComboBox.setPrefWidth(190);

        Button connectBtn = new Button("🔗 Connect Link");
        connectBtn.getStyleClass().add("btn-primary");
        connectBtn.setOnAction(e -> handleConnectDevices());

        connectRow.getChildren().addAll(
                createFormLabel("Node A:"), connectDeviceAComboBox,
                createFormLabel("Node B:"), connectDeviceBComboBox,
                connectBtn
        );
        connectCard.getChildren().add(connectRow);

        col.getChildren().addAll(tableCard, addDeviceCard, connectCard);
        return col;
    }

    private void updateToggleButtonsState(CommunicationDevice dev) {
        if (dev == null) {
            toggleStatusBtn.setText("Toggle Online / Offline");
            toggleStatusBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white;");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            toggleStatusBtn.setText("🟢 Switch to ONLINE");
            toggleStatusBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        } else {
            toggleStatusBtn.setText("🔴 Switch to OFFLINE");
            toggleStatusBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        }
    }

    // -------------------------------------------------------------
    // Right Column: Message Dispatch, Visual Route & Event Log
    // -------------------------------------------------------------
    private VBox createRightColumn() {
        VBox col = new VBox(14);

        // Card 1: Dispatch Emergency Message Form
        VBox dispatchCard = createCardContainer("Dispatch Emergency Alert", "🚨");
        GridPane formGrid = new GridPane();
        formGrid.setHgap(10);
        formGrid.setVgap(10);

        senderComboBox = createDeviceComboBox();
        senderComboBox.setPrefWidth(170);

        recipientComboBox = createDeviceComboBox();
        recipientComboBox.setPrefWidth(170);

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        priorityComboBox.setValue(Priority.NORMAL);
        priorityComboBox.setPrefWidth(140);

        messageTextField = new TextField();
        messageTextField.setPromptText("Enter emergency alert or request details...");
        messageTextField.setPrefWidth(220);

        // One-Click Preset Chips for Quick Testing
        HBox presetChipsRow = new HBox(6);
        presetChipsRow.setAlignment(Pos.CENTER_LEFT);
        Label presetsLabel = new Label("Quick Presets:");
        presetsLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Button chipMed = new Button("🏥 Medical Aid Needed");
        chipMed.getStyleClass().add("btn-chip");
        chipMed.setOnAction(e -> messageTextField.setText("Medical evacuation required at Sector 4"));

        Button chipSec = new Button("⚠️ Security Alert");
        chipSec.getStyleClass().add("btn-chip");
        chipSec.setOnAction(e -> messageTextField.setText("Structural hazard identified near Gate 2"));

        Button chipStatus = new Button("ℹ️ Node Ping");
        chipStatus.getStyleClass().add("btn-chip");
        chipStatus.setOnAction(e -> messageTextField.setText("Periodic heartbeat and network connectivity check"));

        presetChipsRow.getChildren().addAll(presetsLabel, chipMed, chipSec, chipStatus);

        sendEmergencyBtn = new Button("🚀 Send Emergency Message");
        sendEmergencyBtn.getStyleClass().add("btn-primary");
        sendEmergencyBtn.setMaxWidth(Double.MAX_VALUE);
        sendEmergencyBtn.setStyle(sendEmergencyBtn.getStyle() + "; -fx-font-size: 13px; -fx-padding: 9 16;");
        sendEmergencyBtn.setOnAction(e -> handleSendMessage());

        formGrid.add(createFormLabel("Sender:"), 0, 0);
        formGrid.add(senderComboBox, 1, 0);
        formGrid.add(createFormLabel("Priority:"), 2, 0);
        formGrid.add(priorityComboBox, 3, 0);

        formGrid.add(createFormLabel("Recipient:"), 0, 1);
        formGrid.add(recipientComboBox, 1, 1);
        formGrid.add(createFormLabel("Payload:"), 2, 1);
        formGrid.add(messageTextField, 3, 1);

        dispatchCard.getChildren().addAll(formGrid, presetChipsRow, sendEmergencyBtn);

        // Card 2: Visual Simulation Outcome & Hop-by-Hop Route
        outcomeCard = createCardContainer("Last Simulation Delivery Result", "📊");
        outcomeCard.getStyleClass().clear();
        outcomeCard.getStyleClass().add("dashboard-card-highlight");

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label outcomeTitle = new Label("Outcome:");
        outcomeTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: bold; -fx-font-size: 12px;");

        resultBadge = new Label("WAITING FOR MESSAGE DISPATCH");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px;");

        statusRow.getChildren().addAll(outcomeTitle, resultBadge);

        // Breadcrumb Hop Trail Container
        VBox routeSection = new VBox(4);
        Label routeHeader = new Label("Transmission Path (Hops):");
        routeHeader.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 600;");

        resultRouteContainer = new HBox(6);
        resultRouteContainer.setAlignment(Pos.CENTER_LEFT);
        resultRouteContainer.setPadding(new Insets(4, 0, 4, 0));
        renderEmptyRoute();

        routeSection.getChildren().addAll(routeHeader, resultRouteContainer);

        resultExplanationLabel = new Label("Select sender, recipient, and priority, then click Send to simulate mesh propagation.");
        resultExplanationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        resultExplanationLabel.setWrapText(true);

        outcomeCard.getChildren().addAll(statusRow, routeSection, resultExplanationLabel);

        // Card 3: Activity & Event Log
        VBox logCard = createCardContainer("Mesh Activity & Event Log", "📜");
        VBox.setVgrow(logCard, javafx.scene.layout.Priority.ALWAYS);

        activityLogArea = new TextArea();
        activityLogArea.setEditable(false);
        activityLogArea.setWrapText(true);
        activityLogArea.getStyleClass().add("activity-log");
        activityLogArea.setPrefHeight(170);
        VBox.setVgrow(activityLogArea, javafx.scene.layout.Priority.ALWAYS);

        HBox logActionRow = new HBox(10);
        logActionRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearLogBtn = new Button("Clear Log");
        clearLogBtn.getStyleClass().add("btn-secondary");
        clearLogBtn.setOnAction(e -> activityLogArea.clear());

        logActionRow.getChildren().add(clearLogBtn);
        logCard.getChildren().addAll(activityLogArea, logActionRow);

        col.getChildren().addAll(dispatchCard, outcomeCard, logCard);
        return col;
    }

    private void renderEmptyRoute() {
        resultRouteContainer.getChildren().clear();
        Label placeholder = new Label("(No transmission path simulated yet)");
        placeholder.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px; -fx-font-style: italic;");
        resultRouteContainer.getChildren().add(placeholder);
    }

    private void renderHopRoute(List<CommunicationDevice> route) {
        resultRouteContainer.getChildren().clear();
        if (route == null || route.isEmpty()) {
            Label blocked = new Label("⛔ [ Path Blocked / No Route Discovered ]");
            blocked.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px;");
            resultRouteContainer.getChildren().add(blocked);
            return;
        }

        for (int i = 0; i < route.size(); i++) {
            CommunicationDevice dev = route.get(i);
            String icon = (dev instanceof SecurityStation) ? "🛡️" : (dev instanceof MedicalStation) ? "🏥" : "📱";
            Label hopLabel = new Label(icon + " " + dev.getName());
            hopLabel.setStyle("-fx-background-color: #1e3a5f; -fx-text-fill: #e0f2fe; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-border-color: #0284c7; -fx-border-radius: 4px;");

            resultRouteContainer.getChildren().add(hopLabel);

            if (i < route.size() - 1) {
                Label arrow = new Label("──▶");
                arrow.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 11px;");
                resultRouteContainer.getChildren().add(arrow);
            }
        }
    }

    // -------------------------------------------------------------
    // Core Handlers & User Validation
    // -------------------------------------------------------------
    private void handleSendMessage() {
        CommunicationDevice sender = senderComboBox.getValue();
        CommunicationDevice recipient = recipientComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        String content = messageTextField.getText();

        // Friendly Validation
        if (sender == null || recipient == null) {
            showAlert("Incomplete Form", "Please select both a Sender and a Recipient device from the dropdowns.");
            return;
        }

        if (sender.equals(recipient)) {
            showAlert("Invalid Route", "Sender and Recipient cannot be the same device. Please pick different devices.");
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            content = "Emergency broadcast from " + sender.getName();
        }

        String msgId = "MSG-" + (messageCounter++);
        totalMessagesSent++;

        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content.trim(), priority);
        log("DISPATCH", String.format("[%s] from '%s' to '%s' | Priority: %s", msgId, sender.getName(), recipient.getName(), priority));

        // Call SimulationEngine
        SimulationResult result = engine.send(message);

        // Update Visual Outcome
        if (result.delivered()) {
            successfulDeliveries++;
            resultBadge.setText("✔ DELIVERED (" + Math.max(0, result.route().size() - 1) + " HOPS)");
            resultBadge.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #059669; -fx-border-radius: 4px;");

            renderHopRoute(result.route());
            resultExplanationLabel.setText(result.explanation() + " (-2.0% battery deducted from all intermediate and terminal nodes).");

            String routeStr = result.route().stream().map(CommunicationDevice::getName).collect(Collectors.joining(" -> "));
            log("SUCCESS", String.format("[%s] Delivered across %d hops via path: %s", msgId, Math.max(0, result.route().size() - 1), routeStr));
        } else {
            resultBadge.setText("✖ DELIVERY FAILED");
            resultBadge.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #dc2626; -fx-border-radius: 4px;");

            renderHopRoute(result.route());
            resultExplanationLabel.setText(result.explanation() + "\n💡 Tip: Check if intermediate nodes are offline, depleted of battery, or restricted from forwarding this priority.");

            log("FAILED", String.format("[%s] Delivery failed. Reason: %s", msgId, result.explanation()));
        }

        refreshUI();
    }

    private void handleToggleStatus() {
        CommunicationDevice dev = toggleDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device from the table or dropdown to toggle its operational state.");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            if (dev.getBatteryLevel() <= 0) {
                showAlert("Battery Depleted", "Cannot turn device online: Battery is 0%. Please use 'Recharge 100%' first.");
                log("NODE", "Cannot bring " + dev.getName() + " online: 0% battery.");
                return;
            }
            dev.setStatus(dev.getBatteryLevel() <= CommunicationDevice.LOW_BATTERY_THRESHOLD
                    ? DeviceStatus.LOW_BATTERY : DeviceStatus.ACTIVE);
            log("NODE", "Device '" + dev.getName() + "' is now ONLINE (" + dev.getStatus() + ").");
        } else {
            dev.setStatus(DeviceStatus.OFFLINE);
            log("NODE", "Device '" + dev.getName() + "' is now OFFLINE.");
        }

        updateToggleButtonsState(dev);
        refreshUI();
    }

    private void handleRecharge() {
        CommunicationDevice dev = toggleDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device from the table or dropdown to recharge.");
            return;
        }

        dev.recharge(100.0);
        log("NODE", "Device '" + dev.getName() + "' battery recharged to 100% (ACTIVE).");
        updateToggleButtonsState(dev);
        refreshUI();
    }

    private void handleAddDevice() {
        String name = addDeviceNameField.getText();
        String type = addDeviceTypeSelect.getValue();

        if (name == null || name.trim().isEmpty()) {
            name = type + " #" + deviceIdCounter;
        }

        String id = "DEV-" + (deviceIdCounter++);
        Location loc = new Location((deviceIdCounter * 12) % 100, (deviceIdCounter * 18) % 100);

        CommunicationDevice newDev;
        if ("Security Station".equals(type)) {
            newDev = new SecurityStation(id, name.trim(), loc, 100.0);
        } else if ("Medical Station".equals(type)) {
            newDev = new MedicalStation(id, name.trim(), loc, 100.0);
        } else {
            newDev = new StudentPhone(id, name.trim(), loc, 100.0);
        }

        graph.addDevice(newDev);
        addDeviceNameField.clear();
        refreshUI();

        log("NODE", "Registered new " + type + ": '" + newDev.getName() + "' [" + newDev.getId() + "] at " + loc);
    }

    private void handleConnectDevices() {
        CommunicationDevice devA = connectDeviceAComboBox.getValue();
        CommunicationDevice devB = connectDeviceBComboBox.getValue();

        if (devA == null || devB == null) {
            showAlert("Selection Missing", "Please select two devices to connect together.");
            return;
        }

        if (devA.equals(devB)) {
            showAlert("Invalid Connection", "Cannot connect a device to itself. Select two distinct devices.");
            return;
        }

        boolean connected = graph.connect(devA, devB);
        if (connected) {
            refreshUI();
            log("LINK", String.format("Established bidirectional link: '%s' <───> '%s'", devA.getName(), devB.getName()));
        } else {
            showAlert("Already Linked", "Devices '" + devA.getName() + "' and '" + devB.getName() + "' are already directly connected.");
        }
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
        graph.addDevice(charlie); // Isolated node for failure scenarios

        // Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        deviceIdCounter = 4;
        refreshUI();

        // Defaults in selectors
        senderComboBox.setValue(alice);
        recipientComboBox.setValue(medical);
        toggleDeviceComboBox.setValue(alice);
        connectDeviceAComboBox.setValue(medical);
        connectDeviceBComboBox.setValue(charlie);
        updateToggleButtonsState(alice);
    }

    private void refreshUI() {
        deviceObservableList.setAll(graph.getAllDevices());
        deviceTable.refresh();

        // Update Top Metric Stats
        int totalDevices = graph.getDeviceCount();
        long activeDevices = graph.getAllDevices().stream().filter(CommunicationDevice::isAvailable).count();
        int totalLinks = graph.getTotalLinkCount() / 2; // Bidirectional pairs

        statTotalNodesLabel.setText(String.valueOf(totalDevices));
        statOnlineNodesLabel.setText(String.format("%d / %d", activeDevices, totalDevices));
        statMeshLinksLabel.setText(String.valueOf(totalLinks));

        if (totalMessagesSent > 0) {
            double successRate = (successfulDeliveries * 100.0) / totalMessagesSent;
            statMessagesCountLabel.setText(String.format("%d (%.0f%%)", totalMessagesSent, successRate));
        } else {
            statMessagesCountLabel.setText("0 (0%)");
        }
    }

    private ComboBox<CommunicationDevice> createDeviceComboBox() {
        ComboBox<CommunicationDevice> box = new ComboBox<>(deviceObservableList);
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(CommunicationDevice d) {
                if (d == null) return "-- Select Device --";
                String state = (d.getStatus() == DeviceStatus.OFFLINE) ? "🔴 OFFLINE" : String.format("🟢 %.0f%%", d.getBatteryLevel());
                return d.getName() + " (" + state + ")";
            }

            @Override
            public CommunicationDevice fromString(String string) {
                return null;
            }
        });
        return box;
    }

    private VBox createCardContainer(String title, String icon) {
        VBox card = new VBox(10);
        card.getStyleClass().add("dashboard-card");

        HBox titleBox = new HBox(8);
        titleBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        iconLbl.getStyleClass().add("section-icon");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("section-title");

        titleBox.getChildren().addAll(iconLbl, titleLabel);
        card.getChildren().add(titleBox);

        return card;
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");
        return lbl;
    }

    private void log(String category, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        activityLogArea.appendText(String.format("[%s] [%-8s] %s\n", timestamp, category, message));
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
