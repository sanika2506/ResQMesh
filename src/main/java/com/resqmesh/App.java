package com.resqmesh;

import com.resqmesh.config.ConfigurationException;
import com.resqmesh.config.NetworkConfigManager;
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
import com.resqmesh.ui.NetworkTopologyPane;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ResQMesh – Emergency Communication Dashboard
 * Step 8: Visual Network Topology Panel & Route Highlighting.
 * 
 * Features:
 * - Interactive visual network topology canvas with draggable nodes and dynamic links.
 * - Tabbed navigation between Visual Mesh Topology and Device Telemetry Table.
 * - Real-time BFS transmission path illumination.
 * - Live Node Inspector and synchronization across all controls.
 */
public class App extends Application {

    private static final String APP_TITLE = "ResQMesh – Emergency Communication Dashboard";
    private static final int DEFAULT_WIDTH = 1340;
    private static final int DEFAULT_HEIGHT = 880;

    // Backend Simulation Core
    private NetworkGraph graph;
    private SimulationEngine engine;

    // Observable State
    private ObservableList<CommunicationDevice> deviceObservableList;
    private int deviceIdCounter = 1;
    private int messageCounter = 1;
    private int totalMessagesDispatched = 0;
    private int successfulDeliveries = 0;

    // KPI Metric Labels
    private Label kpiTotalNodesLabel;
    private Label kpiOnlineHealthLabel;
    private Label kpiMeshLinksLabel;
    private Label kpiSuccessRateLabel;

    // Central Network Panels & Visualization
    private TabPane networkTabPane;
    private NetworkTopologyPane topologyPane;
    private TableView<CommunicationDevice> deviceTable;
    private ComboBox<CommunicationDevice> selectedDeviceComboBox;
    private Button dynamicToggleBtn;
    private Button rechargeBtn;
    private TextField addDeviceNameField;
    private ComboBox<String> addDeviceTypeSelect;
    private ComboBox<CommunicationDevice> linkDeviceAComboBox;
    private ComboBox<CommunicationDevice> linkDeviceBComboBox;

    // Emergency Messaging Controls
    private ComboBox<CommunicationDevice> senderComboBox;
    private ComboBox<CommunicationDevice> recipientComboBox;
    private ComboBox<Priority> priorityComboBox;
    private TextField messageTextField;

    // Delivery Outcome Showcase
    private Label resultBadge;
    private HBox resultRouteHBox;
    private Label resultExplanationLabel;
    private VBox outcomeCard;

    // Activity Feed
    private TextArea activityFeedArea;

    // Sidebar Live Monitor Labels
    private Label sidebarActiveNodesCountLabel;
    private Label sidebarLinksCountLabel;

    @Override
    public void start(Stage primaryStage) {
        // Initialize Backend
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        deviceObservableList = FXCollections.observableArrayList();

        // Root Layout: Dark Navy BorderPane
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #070b14;");

        // 1. Left Sidebar Navigation
        root.setLeft(createSidebar());

        // 2. Center Content Area (Top KPI Cards + Main Workspaces)
        VBox centerArea = new VBox(16);
        centerArea.setPadding(new Insets(18, 22, 22, 22));
        centerArea.setStyle("-fx-background-color: #070b14;");

        // Top KPI Metric Cards
        centerArea.getChildren().add(createKpiStatsRow());

        // Operational Deck: Split into Central Network Status (Left) & Dispatch/Feed (Right)
        HBox operationalDeck = new HBox(18);
        HBox.setHgrow(operationalDeck, javafx.scene.layout.Priority.ALWAYS);
        VBox.setVgrow(operationalDeck, javafx.scene.layout.Priority.ALWAYS);

        VBox networkStatusPanel = createNetworkStatusPanel();
        VBox dispatchAndLogsPanel = createDispatchAndLogsPanel();

        HBox.setHgrow(networkStatusPanel, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(dispatchAndLogsPanel, javafx.scene.layout.Priority.ALWAYS);
        networkStatusPanel.setPrefWidth(740);
        dispatchAndLogsPanel.setPrefWidth(540);

        operationalDeck.getChildren().addAll(networkStatusPanel, dispatchAndLogsPanel);
        centerArea.getChildren().add(operationalDeck);

        ScrollPane centerScrollPane = new ScrollPane(centerArea);
        centerScrollPane.setFitToWidth(true);
        centerScrollPane.setFitToHeight(true);
        centerScrollPane.setStyle("-fx-background: #070b14; -fx-background-color: #070b14; -fx-border-color: transparent;");

        root.setCenter(centerScrollPane);

        // Preload standard campus disaster relief network
        loadSampleNetwork();

        // Create Scene & Attach Stylesheet
        Scene scene = new Scene(root, DEFAULT_WIDTH, DEFAULT_HEIGHT);
        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            scene.getStylesheets().add(cssResource.toExternalForm());
        }

        primaryStage.setTitle(APP_TITLE);
        primaryStage.setScene(scene);
        primaryStage.setMinWidth(1140);
        primaryStage.setMinHeight(760);
        primaryStage.show();

        log("SYSTEM", "ResQMesh Emergency Command Dashboard with Visual Topology Canvas initialized.");
    }

    // --------------------------------------------------------------------------
    // 1. Navigation Sidebar
    // --------------------------------------------------------------------------
    private VBox createSidebar() {
        VBox sidebar = new VBox(16);
        sidebar.setPrefWidth(250);
        sidebar.getStyleClass().add("sidebar");

        // Brand Title & Shield
        VBox brandBox = new VBox(6);
        HBox brandHeader = new HBox(8);
        brandHeader.setAlignment(Pos.CENTER_LEFT);

        Label logoIcon = new Label("🛡️");
        logoIcon.setStyle("-fx-font-size: 20px;");

        Label brandTitle = new Label("ResQMesh");
        brandTitle.getStyleClass().add("sidebar-brand-title");

        brandHeader.getChildren().addAll(logoIcon, brandTitle);

        Label brandSub = new Label("OFFLINE MESH SIMULATOR");
        brandSub.getStyleClass().add("sidebar-badge");

        brandBox.getChildren().addAll(brandHeader, brandSub);

        // Navigation Items
        VBox navMenu = new VBox(6);
        Button navTopology = createNavButton("🗺️ Visual Topology", true);
        Button navTable = createNavButton("📋 Node Telemetry", false);
        Button navDispatch = createNavButton("🚨 Emergency Dispatch", false);
        Button navLogs = createNavButton("📜 Terminal Logs", false);

        navTopology.setOnAction(e -> {
            setActiveNav(navMenu, navTopology);
            if (networkTabPane != null) networkTabPane.getSelectionModel().select(0);
        });

        navTable.setOnAction(e -> {
            setActiveNav(navMenu, navTable);
            if (networkTabPane != null) networkTabPane.getSelectionModel().select(1);
        });

        navDispatch.setOnAction(e -> {
            setActiveNav(navMenu, navDispatch);
            if (messageTextField != null) messageTextField.requestFocus();
        });

        navLogs.setOnAction(e -> {
            setActiveNav(navMenu, navLogs);
            if (activityFeedArea != null) activityFeedArea.requestFocus();
        });

        navMenu.getChildren().addAll(navTopology, navTable, navDispatch, navLogs);

        // Live Mesh Health Monitor Widget
        VBox healthWidget = new VBox(8);
        healthWidget.getStyleClass().add("sidebar-widget");

        Label healthTitle = new Label("SYSTEM TELEMETRY");
        healthTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase;");

        sidebarActiveNodesCountLabel = new Label("Active Nodes: 0");
        sidebarActiveNodesCountLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        sidebarLinksCountLabel = new Label("Mesh Density: 0 Links");
        sidebarLinksCountLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        Label routingEngineLabel = new Label("Routing: Shortest Path (BFS)");
        routingEngineLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 600;");

        Label energyRuleLabel = new Label("Energy Drain: -2.0% / hop");
        energyRuleLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px;");

        healthWidget.getChildren().addAll(healthTitle, sidebarActiveNodesCountLabel, sidebarLinksCountLabel, routingEngineLabel, energyRuleLabel);

        // Topology & Configuration Actions
        VBox actionsBox = new VBox(8);
        Label actionsTitle = new Label("CONFIGURATION & ACTIONS");
        actionsTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Button saveBtn = new Button("💾 Save Network (JSON)");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-ghost");
        saveBtn.setOnAction(e -> handleSaveNetwork());

        Button loadBtn = new Button("📂 Load Network (JSON)");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-ghost");
        loadBtn.setOnAction(e -> handleLoadNetwork());

        Button newSimBtn = new Button("✨ New Simulation");
        newSimBtn.setMaxWidth(Double.MAX_VALUE);
        newSimBtn.setStyle("-fx-background-color: #0e223d; -fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12; -fx-border-color: #0284c7; -fx-border-radius: 6px;");
        newSimBtn.setOnAction(e -> handleNewSimulation());

        Button resetBtn = new Button("↺ Reset Sample Mesh");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.getStyleClass().add("btn-ghost");
        resetBtn.setOnAction(e -> {
            loadSampleNetwork();
            log("TOPOLOGY", "Reset to preloaded campus emergency mesh topology.");
        });

        Button clearBtn = new Button("✕ Clear Network Graph");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setStyle("-fx-background-color: #271419; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12;");
        clearBtn.setOnAction(e -> {
            graph.clear();
            refreshUI();
            log("TOPOLOGY", "Network graph cleared. All devices and links removed.");
        });

        actionsBox.getChildren().addAll(actionsTitle, saveBtn, loadBtn, newSimBtn, resetBtn, clearBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Sidebar Footer
        Label footerNote = new Label("ResQMesh v1.0 • College Mini-Project\nInteractive Graph Visualization • BFS");
        footerNote.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569; -fx-line-spacing: 2px;");

        sidebar.getChildren().addAll(brandBox, navMenu, healthWidget, actionsBox, spacer, footerNote);
        return sidebar;
    }

    private Button createNavButton(String title, boolean active) {
        Button btn = new Button(title);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.getStyleClass().add("sidebar-nav-btn");
        if (active) {
            btn.getStyleClass().add("sidebar-nav-btn-active");
        }
        return btn;
    }

    private void setActiveNav(VBox navMenu, Button activeBtn) {
        for (javafx.scene.Node node : navMenu.getChildren()) {
            if (node instanceof Button b) {
                b.getStyleClass().remove("sidebar-nav-btn-active");
            }
        }
        activeBtn.getStyleClass().add("sidebar-nav-btn-active");
    }

    // --------------------------------------------------------------------------
    // 2. Top KPI Metric Stats Row
    // --------------------------------------------------------------------------
    private HBox createKpiStatsRow() {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);

        kpiTotalNodesLabel = new Label("0");
        kpiOnlineHealthLabel = new Label("0 / 0");
        kpiMeshLinksLabel = new Label("0");
        kpiSuccessRateLabel = new Label("0 (0%)");

        VBox card1 = createKpiCard("TOTAL NODES", kpiTotalNodesLabel, "Registered mesh devices", "#22d3ee", "📱");
        VBox card2 = createKpiCard("ONLINE HEALTH", kpiOnlineHealthLabel, "Available routing nodes", "#10b981", "🟢");
        VBox card3 = createKpiCard("ACTIVE MESH LINKS", kpiMeshLinksLabel, "Bidirectional connections", "#818cf8", "🔗");
        VBox card4 = createKpiCard("DELIVERY SUCCESS", kpiSuccessRateLabel, "Dispatched alerts success", "#f59e0b", "⚡");

        HBox.setHgrow(card1, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(card2, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(card3, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(card4, javafx.scene.layout.Priority.ALWAYS);

        row.getChildren().addAll(card1, card2, card3, card4);
        return row;
    }

    private VBox createKpiCard(String title, Label valueLabel, String subtext, String colorHex, String icon) {
        VBox card = new VBox(3);
        card.getStyleClass().add("kpi-card");

        HBox topHBox = new HBox(6);
        topHBox.setAlignment(Pos.CENTER_LEFT);

        Label iconLbl = new Label(icon);
        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("kpi-title");

        topHBox.getChildren().addAll(iconLbl, titleLbl);

        valueLabel.getStyleClass().add("kpi-number");
        valueLabel.setStyle("-fx-text-fill: " + colorHex + ";");

        Label subtextLbl = new Label(subtext);
        subtextLbl.getStyleClass().add("kpi-subtext");

        card.getChildren().addAll(topHBox, valueLabel, subtextLbl);
        return card;
    }

    // --------------------------------------------------------------------------
    // 3. Central Network Status Panel (Interactive Topology Canvas + Table)
    // --------------------------------------------------------------------------
    private VBox createNetworkStatusPanel() {
        VBox col = new VBox(14);

        // Section A: Tabbed Network View (Interactive Topology Canvas vs Telemetry Table)
        VBox viewCard = createCard("Network Status & Topology", "Interactive mesh graph, live connection lines, and telemetry");
        VBox.setVgrow(viewCard, javafx.scene.layout.Priority.ALWAYS);

        networkTabPane = new TabPane();
        networkTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(networkTabPane, javafx.scene.layout.Priority.ALWAYS);

        // Tab 1: Interactive Visual Network Topology Canvas
        topologyPane = new NetworkTopologyPane(graph);
        topologyPane.setOnDeviceSelected(dev -> {
            selectedDeviceComboBox.setValue(dev);
            deviceTable.getSelectionModel().select(dev);
            updateToggleState(dev);
        });

        Tab topologyTab = new Tab("🗺️ Interactive Mesh Topology", topologyPane);

        // Tab 2: Active Nodes Table
        deviceTable = new TableView<>();
        deviceTable.setPlaceholder(new Label("No communication devices currently registered."));
        deviceTable.setPrefHeight(320);

        // Col 1: Name & ID
        TableColumn<CommunicationDevice, String> nameCol = new TableColumn<>("Device Name & ID");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName() + " [" + cell.getValue().getId() + "]"));

        // Col 2: Type Badge
        TableColumn<CommunicationDevice, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(130);
        typeCol.setCellValueFactory(cell -> {
            CommunicationDevice dev = cell.getValue();
            if (dev instanceof SecurityStation) return new SimpleStringProperty("🛡️ Security Post");
            if (dev instanceof MedicalStation) return new SimpleStringProperty("🏥 Medical Center");
            return new SimpleStringProperty("📱 Student Phone");
        });

        // Col 3: Battery Level
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
                            setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
                        } else if (val >= 20.0) {
                            setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold;");
                        } else {
                            setStyle("-fx-text-fill: #f43f5e; -fx-font-weight: bold;");
                        }
                    } catch (Exception ignored) {
                        setStyle("-fx-text-fill: #e2e8f0;");
                    }
                }
            }
        });

        // Col 4: Status Indicator Badge
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
                        setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-alignment: center;");
                    } else if ("LOW_BATTERY".equalsIgnoreCase(item)) {
                        setText("⚠️ LOW BATT");
                        setStyle("-fx-background-color: #451a03; -fx-text-fill: #fbbf24; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-alignment: center;");
                    } else {
                        setText("🔴 OFFLINE");
                        setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-weight: bold; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-alignment: center;");
                    }
                }
            }
        });

        // Col 5: Connected Peers
        TableColumn<CommunicationDevice, String> linksCol = new TableColumn<>("Connected Peers");
        linksCol.setPrefWidth(180);
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

        deviceTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedDeviceComboBox.setValue(newV);
                updateToggleState(newV);
                topologyPane.selectDevice(newV);
            }
        });

        Tab tableTab = new Tab("📋 Device Telemetry Table", deviceTable);
        networkTabPane.getTabs().addAll(topologyTab, tableTab);
        viewCard.getChildren().add(networkTabPane);

        // Section B: Node Control Strip
        HBox controlStrip = new HBox(12);
        controlStrip.setAlignment(Pos.CENTER_LEFT);

        Label selectLbl = new Label("Selected Node:");
        selectLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 600;");

        selectedDeviceComboBox = createDeviceComboBox();
        selectedDeviceComboBox.setPrefWidth(220);
        selectedDeviceComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            updateToggleState(newV);
            if (newV != null) {
                topologyPane.selectDevice(newV);
            }
        });

        dynamicToggleBtn = new Button("Toggle Online / Offline");
        dynamicToggleBtn.getStyleClass().add("btn-rose");
        dynamicToggleBtn.setOnAction(e -> handleToggleStatus());

        rechargeBtn = new Button("⚡ Recharge (100%)");
        rechargeBtn.getStyleClass().add("btn-emerald");
        rechargeBtn.setOnAction(e -> handleRecharge());

        controlStrip.getChildren().addAll(selectLbl, selectedDeviceComboBox, dynamicToggleBtn, rechargeBtn);
        viewCard.getChildren().add(controlStrip);

        // Section C: Grid for Provisioning & Linking
        HBox bottomGrid = new HBox(14);
        HBox.setHgrow(bottomGrid, javafx.scene.layout.Priority.ALWAYS);

        // Card C1: Add New Device
        VBox addDeviceCard = createCard("Add Virtual Device", "Register new handheld or base station");
        HBox.setHgrow(addDeviceCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane addGrid = new GridPane();
        addGrid.setHgap(8);
        addGrid.setVgap(8);

        addDeviceNameField = new TextField();
        addDeviceNameField.setPromptText("Device name (e.g. Quad Gate Post)");

        addDeviceTypeSelect = new ComboBox<>();
        addDeviceTypeSelect.getItems().addAll("Student Phone", "Security Station", "Medical Station");
        addDeviceTypeSelect.setValue("Student Phone");

        Button addBtn = new Button("➕ Add Node");
        addBtn.getStyleClass().add("btn-cyan");
        addBtn.setOnAction(e -> handleAddDevice());

        addGrid.add(createFormLabel("Name:"), 0, 0);
        addGrid.add(addDeviceNameField, 1, 0);
        addGrid.add(createFormLabel("Type:"), 0, 1);
        addGrid.add(addDeviceTypeSelect, 1, 1);
        addGrid.add(addBtn, 1, 2);

        addDeviceCard.getChildren().add(addGrid);

        // Card C2: Establish Mesh Link
        VBox connectCard = createCard("Connect Nodes", "Create bidirectional wireless mesh link");
        HBox.setHgrow(connectCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane linkGrid = new GridPane();
        linkGrid.setHgap(8);
        linkGrid.setVgap(8);

        linkDeviceAComboBox = createDeviceComboBox();
        linkDeviceBComboBox = createDeviceComboBox();

        Button linkBtn = new Button("🔗 Connect Link");
        linkBtn.getStyleClass().add("btn-blue");
        linkBtn.setOnAction(e -> handleConnectDevices());

        linkGrid.add(createFormLabel("Node A:"), 0, 0);
        linkGrid.add(linkDeviceAComboBox, 1, 0);
        linkGrid.add(createFormLabel("Node B:"), 0, 1);
        linkGrid.add(linkDeviceBComboBox, 1, 1);
        linkGrid.add(linkBtn, 1, 2);

        connectCard.getChildren().add(linkGrid);

        bottomGrid.getChildren().addAll(addDeviceCard, connectCard);

        col.getChildren().addAll(viewCard, bottomGrid);
        return col;
    }

    private void updateToggleState(CommunicationDevice dev) {
        if (dev == null) {
            dynamicToggleBtn.setText("Toggle Online / Offline");
            dynamicToggleBtn.setStyle("");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            dynamicToggleBtn.setText("🟢 Bring ONLINE");
            dynamicToggleBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        } else {
            dynamicToggleBtn.setText("🔴 Take OFFLINE");
            dynamicToggleBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
        }
    }

    // --------------------------------------------------------------------------
    // 4. Dedicated Emergency Dispatch & Activity Feed Panel
    // --------------------------------------------------------------------------
    private VBox createDispatchAndLogsPanel() {
        VBox col = new VBox(14);

        // Card 1: Dispatch Emergency Alert Deck
        VBox dispatchCard = createCard("Emergency Dispatch Console", "Select route endpoints and priority to trigger BFS propagation");

        GridPane dispatchGrid = new GridPane();
        dispatchGrid.setHgap(10);
        dispatchGrid.setVgap(10);

        senderComboBox = createDeviceComboBox();
        recipientComboBox = createDeviceComboBox();

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        priorityComboBox.setValue(Priority.NORMAL);

        messageTextField = new TextField();
        messageTextField.setPromptText("Enter emergency alert or status details...");

        dispatchGrid.add(createFormLabel("Sender Node:"), 0, 0);
        dispatchGrid.add(senderComboBox, 1, 0);
        dispatchGrid.add(createFormLabel("Priority:"), 2, 0);
        dispatchGrid.add(priorityComboBox, 3, 0);

        dispatchGrid.add(createFormLabel("Target Node:"), 0, 1);
        dispatchGrid.add(recipientComboBox, 1, 1);
        dispatchGrid.add(createFormLabel("Payload:"), 2, 1);
        dispatchGrid.add(messageTextField, 3, 1);

        // Quick Preset Chips
        HBox presetRow = new HBox(6);
        presetRow.setAlignment(Pos.CENTER_LEFT);
        Label quickLbl = new Label("Quick Presets:");
        quickLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");

        Button presetMed = new Button("🏥 Medical Aid");
        presetMed.getStyleClass().add("btn-preset");
        presetMed.setOnAction(e -> messageTextField.setText("URGENT: Medical assistance requested at Quad Block 4"));

        Button presetSec = new Button("⚠️ Hazard Alert");
        presetSec.getStyleClass().add("btn-preset");
        presetSec.setOnAction(e -> messageTextField.setText("HAZARD: Structural blockage reported near Gate 2"));

        Button presetPing = new Button("ℹ️ Node Ping");
        presetPing.getStyleClass().add("btn-preset");
        presetPing.setOnAction(e -> messageTextField.setText("STATUS: Node heartbeat and path liveness ping"));

        presetRow.getChildren().addAll(quickLbl, presetMed, presetSec, presetPing);

        Button dispatchBtn = new Button("🚀 DISPATCH EMERGENCY ALERT");
        dispatchBtn.getStyleClass().add("btn-dispatch");
        dispatchBtn.setMaxWidth(Double.MAX_VALUE);
        dispatchBtn.setOnAction(e -> handleSendMessage());

        dispatchCard.getChildren().addAll(dispatchGrid, presetRow, dispatchBtn);

        // Card 2: Live Propagation Outcome & Breadcrumb Hop Route
        outcomeCard = createCard("Simulation Delivery Outcome", "BFS shortest-path discovery & energy consumption analysis");
        outcomeCard.getStyleClass().clear();
        outcomeCard.getStyleClass().add("dash-card-highlight");

        HBox outcomeStatusRow = new HBox(10);
        outcomeStatusRow.setAlignment(Pos.CENTER_LEFT);

        Label outcomeTitle = new Label("STATUS:");
        outcomeTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 12px;");

        resultBadge = new Label("WAITING FOR DISPATCH");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");

        outcomeStatusRow.getChildren().addAll(outcomeTitle, resultBadge);

        // Breadcrumb Trail
        VBox routeSection = new VBox(6);
        Label routeHeader = new Label("Propagation Path (Hop-by-Hop):");
        routeHeader.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700;");

        resultRouteHBox = new HBox(6);
        resultRouteHBox.setAlignment(Pos.CENTER_LEFT);
        renderEmptyRoute();

        ScrollPane routeScrollPane = new ScrollPane(resultRouteHBox);
        routeScrollPane.setFitToHeight(true);
        routeScrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        routeScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        routeScrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 2 0;");

        routeSection.getChildren().addAll(routeHeader, routeScrollPane);

        resultExplanationLabel = new Label("Select sender, recipient, and priority, then click Dispatch to simulate mesh propagation.");
        resultExplanationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        resultExplanationLabel.setWrapText(true);

        outcomeCard.getChildren().addAll(outcomeStatusRow, routeSection, resultExplanationLabel);

        // Card 3: Terminal Activity Feed
        VBox logCard = createCard("Terminal Activity Feed", "Real-time system events, dispatches, and link changes");
        VBox.setVgrow(logCard, javafx.scene.layout.Priority.ALWAYS);

        activityFeedArea = new TextArea();
        activityFeedArea.setEditable(false);
        activityFeedArea.setWrapText(true);
        activityFeedArea.getStyleClass().add("terminal-area");
        activityFeedArea.setPrefHeight(180);
        VBox.setVgrow(activityFeedArea, javafx.scene.layout.Priority.ALWAYS);

        HBox logActionRow = new HBox(10);
        logActionRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearLogBtn = new Button("Clear Feed");
        clearLogBtn.getStyleClass().add("btn-ghost");
        clearLogBtn.setOnAction(e -> activityFeedArea.clear());

        logActionRow.getChildren().add(clearLogBtn);
        logCard.getChildren().addAll(activityFeedArea, logActionRow);

        col.getChildren().addAll(dispatchCard, outcomeCard, logCard);
        return col;
    }

    private void renderEmptyRoute() {
        resultRouteHBox.getChildren().clear();
        Label placeholder = new Label("(No transmission path simulated yet)");
        placeholder.setStyle("-fx-text-fill: #475569; -fx-font-size: 12px; -fx-font-style: italic;");
        resultRouteHBox.getChildren().add(placeholder);
    }

    private void renderHopRoute(List<CommunicationDevice> route) {
        resultRouteHBox.getChildren().clear();
        if (route == null || route.isEmpty()) {
            Label blocked = new Label("⛔ [ Path Blocked / No Feasible Route Found ]");
            blocked.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
            resultRouteHBox.getChildren().add(blocked);
            return;
        }

        for (int i = 0; i < route.size(); i++) {
            CommunicationDevice dev = route.get(i);
            String icon = (dev instanceof SecurityStation) ? "🛡️" : (dev instanceof MedicalStation) ? "🏥" : "📱";
            Label hopLabel = new Label(icon + " " + dev.getName());
            hopLabel.setStyle("-fx-background-color: #0c213d; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 4 9; -fx-background-radius: 5px; -fx-border-color: #0284c7; -fx-border-radius: 5px;");

            resultRouteHBox.getChildren().add(hopLabel);

            if (i < route.size() - 1) {
                Label arrow = new Label("──▶");
                arrow.setStyle("-fx-text-fill: #22d3ee; -fx-font-weight: 900; -fx-font-size: 11px;");
                resultRouteHBox.getChildren().add(arrow);
            }
        }
    }

    // --------------------------------------------------------------------------
    // 5. Simulation Handlers & Validation
    // --------------------------------------------------------------------------
    private void handleSendMessage() {
        CommunicationDevice sender = senderComboBox.getValue();
        CommunicationDevice recipient = recipientComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        String content = messageTextField.getText();

        if (sender == null || recipient == null) {
            showAlert("Incomplete Form", "Please select both a Sender node and a Target node.");
            return;
        }

        if (sender.equals(recipient)) {
            showAlert("Invalid Route", "Sender and Target cannot be the same device. Please select two distinct nodes.");
            return;
        }

        if (content == null || content.trim().isEmpty()) {
            content = "Emergency broadcast from " + sender.getName();
        }

        String msgId = "MSG-" + (messageCounter++);
        totalMessagesDispatched++;

        // Reset previous route visualization before dispatching new simulation
        if (topologyPane != null) {
            topologyPane.clearRouteHighlight();
        }

        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content.trim(), priority);
        log("DISPATCH", String.format("[%s] Initiated from '%s' to '%s' | Priority: %s", msgId, sender.getName(), recipient.getName(), priority));

        // Call SimulationEngine
        SimulationResult result = engine.send(message);

        // Update Visual Outcome & Topology Graph Highlighting
        if (result.delivered()) {
            successfulDeliveries++;
            int hops = Math.max(0, result.route().size() - 1);
            resultBadge.setText("✔ DELIVERED (" + hops + " HOPS)");
            resultBadge.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #059669; -fx-border-radius: 4px;");

            renderHopRoute(result.route());
            if (topologyPane != null) {
                topologyPane.highlightRoute(result.route());
            }
            resultExplanationLabel.setText(result.explanation() + "\n⚡ Impact: -2.0% battery deducted from all nodes along the transmission path.");

            String routeStr = result.route().stream().map(CommunicationDevice::getName).collect(Collectors.joining(" -> "));
            log("SUCCESS", String.format("[%s] Successfully delivered via %d hops: %s", msgId, hops, routeStr));
        } else {
            resultBadge.setText("✖ DELIVERY FAILED");
            resultBadge.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #dc2626; -fx-border-radius: 4px;");

            renderHopRoute(result.route());
            if (topologyPane != null) {
                topologyPane.clearRouteHighlight();
            }
            resultExplanationLabel.setText(result.explanation() + "\n💡 Diagnostic: Verify if intermediate nodes are offline, depleted of battery, or restricted from forwarding this priority.");

            log("FAILED", String.format("[%s] Delivery failed. Reason: %s", msgId, result.explanation()));
        }

        refreshUI();
    }

    private void handleToggleStatus() {
        CommunicationDevice dev = selectedDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device to toggle its operational state.");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            if (dev.getBatteryLevel() <= 0) {
                showAlert("Battery Depleted", "Cannot turn device online: Battery is 0%. Please use 'Recharge (100%)' first.");
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

        updateToggleState(dev);
        refreshUI();
    }

    private void handleRecharge() {
        CommunicationDevice dev = selectedDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Selection", "Please select a device to recharge.");
            return;
        }

        dev.recharge(100.0);
        log("NODE", "Device '" + dev.getName() + "' battery recharged to 100% (ACTIVE).");
        updateToggleState(dev);
        refreshUI();
    }

    private void handleAddDevice() {
        String name = addDeviceNameField.getText();
        String type = addDeviceTypeSelect.getValue();

        if (name == null || name.trim().isEmpty()) {
            name = type + " #" + deviceIdCounter;
        }

        String id = "DEV-" + (deviceIdCounter++);
        Location loc = new Location((deviceIdCounter * 14) % 100, (deviceIdCounter * 20) % 100);

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
        CommunicationDevice devA = linkDeviceAComboBox.getValue();
        CommunicationDevice devB = linkDeviceBComboBox.getValue();

        if (devA == null || devB == null) {
            showAlert("Selection Missing", "Please select both devices to establish a mesh link.");
            return;
        }

        if (devA.equals(devB)) {
            showAlert("Invalid Connection", "Cannot connect a device to itself. Select two different nodes.");
            return;
        }

        boolean connected = graph.connect(devA, devB);
        if (connected) {
            refreshUI();
            log("LINK", String.format("Established mesh link: '%s' <───> '%s'", devA.getName(), devB.getName()));
        } else {
            showAlert("Already Linked", "Devices '" + devA.getName() + "' and '" + devB.getName() + "' are already directly connected.");
        }
    }

    // --------------------------------------------------------------------------
    // 6. Network Configuration Persistence & Simulation Lifecycle
    // --------------------------------------------------------------------------
    private void handleSaveNetwork() {
        if (graph.getDeviceCount() == 0) {
            showAlert("Cannot Save", "The network is empty. Register at least one device before saving.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save ResQMesh Network Topology");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Configuration Files (*.json)", "*.json"));
        fileChooser.setInitialFileName("resqmesh-network.json");

        File file = fileChooser.showSaveDialog(null);
        if (file != null) {
            try {
                NetworkConfigManager.saveToFile(graph, file, "ResQMesh Simulation Topology");
                log("CONFIG", String.format("Saved network configuration (%d nodes, %d links) to '%s'",
                        graph.getDeviceCount(), graph.getTotalLinkCount() / 2, file.getName()));
                showAlert("Network Saved", String.format("Successfully saved %d devices and %d connections to:\n%s",
                        graph.getDeviceCount(), graph.getTotalLinkCount() / 2, file.getAbsolutePath()));
            } catch (Exception ex) {
                log("ERROR", "Failed to save configuration: " + ex.getMessage());
                showAlert("Save Failed", "Could not write configuration to disk:\n" + ex.getMessage());
            }
        }
    }

    private void handleLoadNetwork() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load ResQMesh Network Topology");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Configuration Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            try {
                // Clear previous simulation outcomes & route highlights prior to loading new config
                if (topologyPane != null) {
                    topologyPane.clearRouteHighlight();
                }
                renderEmptyRoute();
                resultBadge.setText("WAITING FOR DISPATCH");
                resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
                resultExplanationLabel.setText("Loaded new network configuration. Ready for simulation dispatch.");

                NetworkConfigManager.loadFromFile(file, graph);

                // Update device counter to prevent ID collisions on new device additions
                deviceIdCounter = Math.max(deviceIdCounter, graph.getDeviceCount() + 1);

                refreshUI();

                // Select first device if available
                if (!graph.getAllDevices().isEmpty()) {
                    List<CommunicationDevice> list = new ArrayList<>(graph.getAllDevices());
                    CommunicationDevice first = list.get(0);
                    selectedDeviceComboBox.setValue(first);
                    senderComboBox.setValue(first);
                    if (list.size() > 1) {
                        recipientComboBox.setValue(list.get(1));
                    }
                    if (topologyPane != null) {
                        topologyPane.selectDevice(first);
                    }
                }

                log("CONFIG", String.format("Loaded network topology from '%s': %d nodes, %d links.",
                        file.getName(), graph.getDeviceCount(), graph.getTotalLinkCount() / 2));
                showAlert("Network Loaded", String.format("Successfully loaded configuration '%s':\n• %d Devices Registered\n• %d Active Mesh Connections",
                        file.getName(), graph.getDeviceCount(), graph.getTotalLinkCount() / 2));
            } catch (ConfigurationException ex) {
                log("ERROR", "Invalid configuration file: " + ex.getMessage());
                showAlert("Configuration Validation Error", "The selected file is not a valid ResQMesh configuration:\n\n" + ex.getMessage());
            } catch (Exception ex) {
                log("ERROR", "Failed to load configuration file: " + ex.getMessage());
                showAlert("Load Failed", "Error reading configuration file:\n\n" + ex.getMessage());
            }
        }
    }

    private void handleNewSimulation() {
        totalMessagesDispatched = 0;
        successfulDeliveries = 0;
        messageCounter = 1;

        if (topologyPane != null) {
            topologyPane.clearRouteHighlight();
        }
        renderEmptyRoute();
        resultBadge.setText("WAITING FOR DISPATCH");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
        resultExplanationLabel.setText("New simulation initiated. All devices recharged to 100% and delivery metrics reset.");

        // Recharge all nodes in current network to 100% active state
        for (CommunicationDevice dev : graph.getAllDevices()) {
            dev.recharge(100.0);
        }

        refreshUI();

        log("SIMULATION", "Started new simulation session. All devices recharged to 100% and transmission counters reset.");
    }

    // --------------------------------------------------------------------------
    // 7. Helpers & Utilities
    // --------------------------------------------------------------------------
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
        graph.addDevice(charlie);

        // Chain: Alice <-> Bob <-> Security <-> Medical
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);

        deviceIdCounter = 4;
        refreshUI();

        senderComboBox.setValue(alice);
        recipientComboBox.setValue(medical);
        selectedDeviceComboBox.setValue(alice);
        linkDeviceAComboBox.setValue(medical);
        linkDeviceBComboBox.setValue(charlie);
        updateToggleState(alice);
        if (topologyPane != null) {
            topologyPane.selectDevice(alice);
        }
    }

    private void refreshUI() {
        deviceObservableList.setAll(graph.getAllDevices());
        deviceTable.refresh();
        if (topologyPane != null) {
            topologyPane.refresh();
        }

        int totalDevices = graph.getDeviceCount();
        long activeDevices = graph.getAllDevices().stream().filter(CommunicationDevice::isAvailable).count();
        int totalLinks = graph.getTotalLinkCount() / 2;

        kpiTotalNodesLabel.setText(String.valueOf(totalDevices));
        kpiOnlineHealthLabel.setText(String.format("%d / %d", activeDevices, totalDevices));
        kpiMeshLinksLabel.setText(String.valueOf(totalLinks));

        if (totalMessagesDispatched > 0) {
            double rate = (successfulDeliveries * 100.0) / totalMessagesDispatched;
            kpiSuccessRateLabel.setText(String.format("%d (%.0f%%)", totalMessagesDispatched, rate));
        } else {
            kpiSuccessRateLabel.setText("0 (0%)");
        }

        // Sync sidebar telemetry
        sidebarActiveNodesCountLabel.setText("Active Nodes: " + activeDevices + " / " + totalDevices);
        sidebarLinksCountLabel.setText("Mesh Density: " + totalLinks + " Links");
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

    private VBox createCard(String title, String subtitle) {
        VBox card = new VBox(6);
        card.getStyleClass().add("dash-card");

        VBox titleBox = new VBox(2);
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("card-title");

        Label subLabel = new Label(subtitle);
        subLabel.getStyleClass().add("card-subtitle");

        titleBox.getChildren().addAll(titleLabel, subLabel);
        card.getChildren().add(titleBox);

        return card;
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");
        return lbl;
    }

    private void log(String category, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        activityFeedArea.appendText(String.format("[%s] [%-8s] %s\n", timestamp, category, message));
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
