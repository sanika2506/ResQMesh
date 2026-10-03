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
import com.resqmesh.simulation.timeline.SimulationEvent;
import com.resqmesh.simulation.timeline.SimulationRecord;
import com.resqmesh.simulation.timeline.SimulationTimeline;
import com.resqmesh.ui.NetworkTopologyPane;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
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
 * User Experience & Guided Workflow Overhaul.
 *
 * Four distinct, uncluttered primary workspaces:
 * 1. Dashboard: Guided 3-step workflow, KPI cards, network health overview, quick dispatch & outcome snapshot.
 * 2. Network & Devices: Dedicated spacious topology canvas, telemetry table, node controls, and easy provisioning.
 * 3. Emergency Dispatch: Distraction-free dispatch console with presets, human-friendly outcome, and collapsible technical details.
 * 4. Activity History: Timeline event audit trail and real-time terminal feed.
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

    // Section Views & Navigation
    private StackPane viewContainer;
    private ScrollPane dashboardScrollPane;
    private ScrollPane networkScrollPane;
    private ScrollPane dispatchScrollPane;
    private ScrollPane activityScrollPane;

    private Button navDashboardBtn;
    private Button navNetworkBtn;
    private Button navDispatchBtn;
    private Button navActivityBtn;
    private final List<Button> navButtons = new ArrayList<>();

    // Guided Workflow Step Badges / Status Labels (Dashboard)
    private Label step1StatusLabel;
    private Label step2StatusLabel;
    private Label step3StatusLabel;

    // Dashboard Overview Metric Labels
    private Label kpiTotalNodesLabel;
    private Label kpiOnlineHealthLabel;
    private Label kpiMeshLinksLabel;
    private Label kpiSuccessRateLabel;
    private Label dashboardPhonesCountLabel;
    private Label dashboardSecurityCountLabel;
    private Label dashboardMedicalCountLabel;
    private Label dashboardActiveOnlineCountLabel;
    private Label dashboardOfflineCountLabel;

    // Central Network Panels & Visualization
    private TabPane networkTabPane;
    private NetworkTopologyPane topologyPane;
    private TableView<CommunicationDevice> deviceTable;
    private ComboBox<CommunicationDevice> selectedDeviceComboBox;
    private Button dynamicToggleBtn;
    private Button rechargeBtn;
    private Label selectedDeviceStatusLabel;

    // Add & Connect Devices Forms
    private TextField addDeviceNameField;
    private ComboBox<String> addDeviceTypeSelect;
    private Label addDeviceRoleDescLabel;
    private Label addDeviceFeedbackLabel;

    private ComboBox<CommunicationDevice> linkDeviceAComboBox;
    private ComboBox<CommunicationDevice> linkDeviceBComboBox;
    private Label connectFeedbackLabel;

    // Emergency Messaging Controls
    private ComboBox<CommunicationDevice> senderComboBox;
    private ComboBox<CommunicationDevice> recipientComboBox;
    private ComboBox<Priority> priorityComboBox;
    private TextField messageTextField;
    private Label priorityDescLabel;

    // Contextual Guidance Banner for Dispatch
    private HBox dispatchGuidanceBanner;
    private Label dispatchGuidanceLabel;

    // Delivery Outcome Showcase
    private Label resultBadge;
    private HBox resultRouteHBox;
    private Label resultExplanationLabel;
    private VBox outcomeCard;
    private Button replayFromOutcomeBtn;
    private Button viewTimelineFromOutcomeBtn;

    // Collapsible Technical Diagnostics in Outcome
    private VBox technicalDetailsBox;
    private Label technicalDetailsContent;
    private Button toggleTechDetailsBtn;
    private boolean techDetailsVisible = false;

    // Activity Feed & Terminal
    private TextArea activityFeedArea;

    // Sidebar Live Monitor Labels
    private Label sidebarActiveNodesCountLabel;
    private Label sidebarLinksCountLabel;

    // Step 11: Event Timeline & Simulation History
    private final ObservableList<SimulationRecord> simulationHistory = FXCollections.observableArrayList();
    private final ObservableList<SimulationEvent> currentTimelineEvents = FXCollections.observableArrayList();
    private ComboBox<SimulationRecord> historyComboBox;
    private TableView<SimulationEvent> timelineTable;
    private Label timelineSummaryLabel;
    private Label eventDetailLabel;
    private Tab topologyTab;
    private Tab tableTab;

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

        // 2. Center View Container (Holds the 4 clean sections)
        viewContainer = new StackPane();
        viewContainer.setStyle("-fx-background-color: #070b14;");

        // Initialize Shared Network Topology Canvas & Device Table
        topologyPane = new NetworkTopologyPane(graph);
        topologyPane.setOnDeviceSelected(dev -> {
            if (selectedDeviceComboBox != null) {
                selectedDeviceComboBox.setValue(dev);
            }
            if (deviceTable != null) {
                deviceTable.getSelectionModel().select(dev);
            }
            updateToggleState(dev);
        });

        deviceTable = createDeviceTable();

        // Build the 4 Primary Workspaces
        dashboardScrollPane = wrapInScrollPane(createDashboardView());
        networkScrollPane = wrapInScrollPane(createNetworkView());
        dispatchScrollPane = wrapInScrollPane(createDispatchView());
        activityScrollPane = wrapInScrollPane(createActivityView());

        // Default to Dashboard
        showView(dashboardScrollPane, navDashboardBtn);
        root.setCenter(viewContainer);

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

        log("SYSTEM", "ResQMesh Emergency Command Dashboard initialized.");
    }

    private ScrollPane wrapInScrollPane(VBox content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background: #070b14; -fx-background-color: #070b14; -fx-border-color: transparent;");
        return sp;
    }

    private void showView(ScrollPane viewScrollPane, Button activeNavBtn) {
        viewContainer.getChildren().setAll(viewScrollPane);
        for (Button btn : navButtons) {
            btn.getStyleClass().remove("sidebar-nav-btn-active");
        }
        if (activeNavBtn != null) {
            activeNavBtn.getStyleClass().add("sidebar-nav-btn-active");
        }
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

        // Navigation Menu (4 Clear Primary Sections)
        VBox navMenu = new VBox(6);
        navDashboardBtn = createNavButton("📊 Dashboard", true);
        navNetworkBtn = createNavButton("🗺️ Network & Devices", false);
        navDispatchBtn = createNavButton("🚨 Emergency Dispatch", false);
        navActivityBtn = createNavButton("📜 Activity & History", false);

        navButtons.clear();
        navButtons.addAll(List.of(navDashboardBtn, navNetworkBtn, navDispatchBtn, navActivityBtn));

        navDashboardBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        navNetworkBtn.setOnAction(e -> showView(networkScrollPane, navNetworkBtn));
        navDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));
        navActivityBtn.setOnAction(e -> showView(activityScrollPane, navActivityBtn));

        navMenu.getChildren().addAll(navDashboardBtn, navNetworkBtn, navDispatchBtn, navActivityBtn);

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

        // Configuration & Persistence Actions
        VBox actionsBox = new VBox(8);
        Label actionsTitle = new Label("CONFIGURATION & ACTIONS");
        actionsTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Button newSimBtn = new Button("✨ New Simulation");
        newSimBtn.setMaxWidth(Double.MAX_VALUE);
        newSimBtn.setStyle("-fx-background-color: #0e223d; -fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12; -fx-border-color: #0284c7; -fx-border-radius: 6px;");
        newSimBtn.setOnAction(e -> handleNewSimulation());

        Button saveBtn = new Button("💾 Save Network (JSON)");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-ghost");
        saveBtn.setOnAction(e -> handleSaveNetwork());

        Button loadBtn = new Button("📂 Load Network (JSON)");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-ghost");
        loadBtn.setOnAction(e -> handleLoadNetwork());

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

        actionsBox.getChildren().addAll(actionsTitle, newSimBtn, saveBtn, loadBtn, resetBtn, clearBtn);

        Region spacer = new Region();
        VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        // Sidebar Footer
        Label footerNote = new Label("ResQMesh v1.0 • Disaster Response Mesh\nShortest-Path BFS • Multi-Hop Relay");
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

    // --------------------------------------------------------------------------
    // 2. View 1: 📊 Dashboard Workspace
    // --------------------------------------------------------------------------
    private VBox createDashboardView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Emergency Command Dashboard",
                "High-level network health, guided workflow progress, and quick simulation control"));

        // Step-by-Step Guided Workflow Banner
        view.getChildren().add(createWorkflowStepperCard());

        // Top KPI Metric Stats Row
        view.getChildren().add(createKpiStatsRow());

        // Dashboard Main Deck: Two Balanced Cards
        HBox deck = new HBox(16);
        HBox.setHgrow(deck, javafx.scene.layout.Priority.ALWAYS);

        // Left Card: Live Network Composition & Health
        VBox healthCard = createCard("Network Architecture & Health", "Current device classification, active connections, and power state");
        healthCard.setPrefWidth(580);
        HBox.setHgrow(healthCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane healthGrid = new GridPane();
        healthGrid.setHgap(16);
        healthGrid.setVgap(12);

        Label phoneIcon = new Label("📱 Student Phones (Handheld):");
        phoneIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dashboardPhonesCountLabel = new Label("0");
        dashboardPhonesCountLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label secIcon = new Label("🛡️ Security Stations (Fixed Post):");
        secIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dashboardSecurityCountLabel = new Label("0");
        dashboardSecurityCountLabel.setStyle("-fx-text-fill: #818cf8; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label medIcon = new Label("🏥 Medical Centers (Triage Hub):");
        medIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dashboardMedicalCountLabel = new Label("0");
        dashboardMedicalCountLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label onlineIcon = new Label("🟢 Operational Nodes:");
        onlineIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dashboardActiveOnlineCountLabel = new Label("0 Online");
        dashboardActiveOnlineCountLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 12px;");

        Label offlineIcon = new Label("🔴 Offline / Depleted Nodes:");
        offlineIcon.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px;");
        dashboardOfflineCountLabel = new Label("0 Offline");
        dashboardOfflineCountLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 12px;");

        healthGrid.add(phoneIcon, 0, 0);
        healthGrid.add(dashboardPhonesCountLabel, 1, 0);
        healthGrid.add(secIcon, 0, 1);
        healthGrid.add(dashboardSecurityCountLabel, 1, 1);
        healthGrid.add(medIcon, 0, 2);
        healthGrid.add(dashboardMedicalCountLabel, 1, 2);
        healthGrid.add(onlineIcon, 0, 3);
        healthGrid.add(dashboardActiveOnlineCountLabel, 1, 3);
        healthGrid.add(offlineIcon, 0, 4);
        healthGrid.add(dashboardOfflineCountLabel, 1, 4);

        Button openTopologyBtn = new Button("🗺️ Open Interactive Topology & Node Controls ➔");
        openTopologyBtn.getStyleClass().add("btn-cyan");
        openTopologyBtn.setMaxWidth(Double.MAX_VALUE);
        openTopologyBtn.setOnAction(e -> {
            showView(networkScrollPane, navNetworkBtn);
            if (networkTabPane != null) {
                networkTabPane.getSelectionModel().select(topologyTab);
            }
        });

        healthCard.getChildren().addAll(healthGrid, new Region(), openTopologyBtn);

        // Right Card: Quick Simulation Action Deck
        VBox quickActionCard = createCard("Emergency Dispatch Gateway", "Test BFS transmission propagation or review latest outcome");
        quickActionCard.setPrefWidth(680);
        HBox.setHgrow(quickActionCard, javafx.scene.layout.Priority.ALWAYS);

        Label quickDesc = new Label("Simulate priority distress alerts across the peer mesh. The shortest route is calculated dynamically using Breadcrumb BFS.");
        quickDesc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        quickDesc.setWrapText(true);

        HBox quickActionButtons = new HBox(10);
        Button goToDispatchBtn = new Button("🚀 Go to Emergency Dispatch Console ➔");
        goToDispatchBtn.getStyleClass().add("btn-dispatch");
        HBox.setHgrow(goToDispatchBtn, javafx.scene.layout.Priority.ALWAYS);
        goToDispatchBtn.setMaxWidth(Double.MAX_VALUE);
        goToDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));

        Button quickTimelineBtn = new Button("⏱️ View Activity Timeline ➔");
        quickTimelineBtn.getStyleClass().add("btn-ghost");
        quickTimelineBtn.setOnAction(e -> showView(activityScrollPane, navActivityBtn));

        quickActionButtons.getChildren().addAll(goToDispatchBtn, quickTimelineBtn);

        // Add Outcome Card to Quick Action Deck
        outcomeCard = createOutcomeCard();

        quickActionCard.getChildren().addAll(quickDesc, outcomeCard, quickActionButtons);

        deck.getChildren().addAll(healthCard, quickActionCard);
        view.getChildren().add(deck);

        return view;
    }

    private VBox createWorkflowStepperCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("workflow-stepper");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("📍");
        Label title = new Label("GUIDED WORKFLOW");
        title.setStyle("-fx-font-size: 11px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase;");
        Label subtitle = new Label("— Follow these 3 simple steps to simulate disaster mesh communication");
        subtitle.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");
        header.getChildren().addAll(icon, title, subtitle);

        HBox stepsRow = new HBox(12);
        stepsRow.setAlignment(Pos.CENTER);

        // Step 1: Add Devices
        step1StatusLabel = new Label("✔ 5 Registered");
        VBox step1 = createStepCard("STEP 1", "Add Devices", step1StatusLabel,
                "Register phones, security posts & medical stations", e -> showView(networkScrollPane, navNetworkBtn));

        Label arrow1 = new Label("──▶");
        arrow1.getStyleClass().add("step-arrow");

        // Step 2: Connect Devices
        step2StatusLabel = new Label("✔ 3 Links Active");
        VBox step2 = createStepCard("STEP 2", "Connect Mesh", step2StatusLabel,
                "Establish bidirectional wireless links between nodes", e -> showView(networkScrollPane, navNetworkBtn));

        Label arrow2 = new Label("──▶");
        arrow2.getStyleClass().add("step-arrow");

        // Step 3: Send Alert
        step3StatusLabel = new Label("⚡ Ready to Dispatch");
        VBox step3 = createStepCard("STEP 3", "Send Emergency Alert", step3StatusLabel,
                "Broadcast priority distress alerts along the shortest path", e -> showView(dispatchScrollPane, navDispatchBtn));

        HBox.setHgrow(step1, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(step2, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(step3, javafx.scene.layout.Priority.ALWAYS);

        stepsRow.getChildren().addAll(step1, arrow1, step2, arrow2, step3);
        card.getChildren().addAll(header, stepsRow);
        return card;
    }

    private VBox createStepCard(String stepNum, String title, Label statusLabel, String desc, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        VBox card = new VBox(5);
        card.getStyleClass().add("step-card");

        HBox top = new HBox(6);
        top.setAlignment(Pos.CENTER_LEFT);
        Label badge = new Label(stepNum);
        badge.getStyleClass().add("step-badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        statusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #34d399;");
        top.getChildren().addAll(badge, spacer, statusLabel);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("step-title");

        Label descLbl = new Label(desc);
        descLbl.getStyleClass().add("step-desc");
        descLbl.setWrapText(true);

        card.getChildren().addAll(top, titleLbl, descLbl);
        card.setOnMouseClicked(e -> onAction.handle(new javafx.event.ActionEvent()));
        return card;
    }

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
    // 3. View 2: 🗺️ Network & Devices Workspace
    // --------------------------------------------------------------------------
    private VBox createNetworkView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Network Topology & Devices",
                "Interactive visual mesh layout, node telemetry, power management, and connection provisioning"));

        // Tabbed Canvas Area: Interactive Topology Canvas vs Telemetry Table
        VBox canvasContainerCard = createCard("Network Visualization & Node Telemetry",
                "Drag nodes to reposition • Click to inspect • Links follow nodes dynamically in real-time");
        canvasContainerCard.setPrefHeight(460);

        networkTabPane = new TabPane();
        networkTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(networkTabPane, javafx.scene.layout.Priority.ALWAYS);

        topologyTab = new Tab("🗺️ Interactive Mesh Topology", topologyPane);
        tableTab = new Tab("📋 Registered Devices Telemetry Table", deviceTable);
        networkTabPane.getTabs().addAll(topologyTab, tableTab);

        canvasContainerCard.getChildren().add(networkTabPane);
        view.getChildren().add(canvasContainerCard);

        // Node Power & State Control Toolbar Strip
        HBox controlStrip = new HBox(12);
        controlStrip.setAlignment(Pos.CENTER_LEFT);
        controlStrip.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 10px 14px; -fx-border-color: #192742; -fx-border-radius: 8px;");

        Label selectLbl = new Label("Selected Node:");
        selectLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 700;");

        selectedDeviceComboBox = createDeviceComboBox();
        selectedDeviceComboBox.setPrefWidth(240);
        selectedDeviceComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            updateToggleState(newV);
            if (newV != null && topologyPane != null) {
                topologyPane.selectDevice(newV);
            }
        });

        dynamicToggleBtn = new Button("Toggle Online / Offline");
        dynamicToggleBtn.getStyleClass().add("btn-rose");
        dynamicToggleBtn.setOnAction(e -> handleToggleStatus());

        rechargeBtn = new Button("⚡ Recharge to 100%");
        rechargeBtn.getStyleClass().add("btn-emerald");
        rechargeBtn.setOnAction(e -> handleRecharge());

        selectedDeviceStatusLabel = new Label("");
        selectedDeviceStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        controlStrip.getChildren().addAll(selectLbl, selectedDeviceComboBox, dynamicToggleBtn, rechargeBtn, selectedDeviceStatusLabel);
        view.getChildren().add(controlStrip);

        // Side-by-Side Device Management Cards
        HBox managementDeck = new HBox(16);
        HBox.setHgrow(managementDeck, javafx.scene.layout.Priority.ALWAYS);

        // Card 1: Add New Virtual Device
        VBox addCard = createCard("Add Virtual Device (Step 1)", "Register a new student handheld or base station to the mesh");
        HBox.setHgrow(addCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane addGrid = new GridPane();
        addGrid.setHgap(10);
        addGrid.setVgap(10);

        addDeviceNameField = new TextField();
        addDeviceNameField.setPromptText("Device name (e.g. Science Quad Relay)");

        addDeviceTypeSelect = new ComboBox<>();
        addDeviceTypeSelect.getItems().addAll("Student Phone", "Security Station", "Medical Station");
        addDeviceTypeSelect.setValue("Student Phone");

        addDeviceRoleDescLabel = new Label("📱 Student Phone: Handheld node. Relays standard alerts (-2.0% battery/hop). Cannot relay CRITICAL alerts.");
        addDeviceRoleDescLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-line-spacing: 1px;");
        addDeviceRoleDescLabel.setWrapText(true);

        addDeviceTypeSelect.valueProperty().addListener((obs, oldV, newV) -> {
            if ("Security Station".equals(newV)) {
                addDeviceRoleDescLabel.setText("🛡️ Security Station: Fixed base post. Full relay authorization for all priorities including CRITICAL.");
            } else if ("Medical Station".equals(newV)) {
                addDeviceRoleDescLabel.setText("🏥 Medical Center: Fixed emergency triage center. High-capacity destination and full-priority relay.");
            } else {
                addDeviceRoleDescLabel.setText("📱 Student Phone: Handheld node. Relays standard alerts (-2.0% battery/hop). Cannot relay CRITICAL alerts.");
            }
        });

        Button addBtn = new Button("➕ Register Node");
        addBtn.getStyleClass().add("btn-cyan");
        addBtn.setOnAction(e -> handleAddDevice());

        addDeviceFeedbackLabel = new Label("");
        addDeviceFeedbackLabel.getStyleClass().add("feedback-msg");

        addGrid.add(createFormLabel("Device Name:"), 0, 0);
        addGrid.add(addDeviceNameField, 1, 0);
        addGrid.add(createFormLabel("Device Role:"), 0, 1);
        addGrid.add(addDeviceTypeSelect, 1, 1);
        addGrid.add(addDeviceRoleDescLabel, 1, 2);
        addGrid.add(addBtn, 1, 3);
        addGrid.add(addDeviceFeedbackLabel, 1, 4);

        addCard.getChildren().add(addGrid);

        // Card 2: Connect Nodes with Mesh Link
        VBox connectCard = createCard("Connect Nodes (Step 2)", "Establish bidirectional wireless mesh link between two devices");
        HBox.setHgrow(connectCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane linkGrid = new GridPane();
        linkGrid.setHgap(10);
        linkGrid.setVgap(10);

        linkDeviceAComboBox = createDeviceComboBox();
        linkDeviceBComboBox = createDeviceComboBox();

        Label linkHelpLabel = new Label("🔗 Links allow emergency alerts to hop across nodes. Alerts route across the shortest path of connected links.");
        linkHelpLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-line-spacing: 1px;");
        linkHelpLabel.setWrapText(true);

        Button linkBtn = new Button("🔗 Connect Wireless Link");
        linkBtn.getStyleClass().add("btn-blue");
        linkBtn.setOnAction(e -> handleConnectDevices());

        connectFeedbackLabel = new Label("");
        connectFeedbackLabel.getStyleClass().add("feedback-msg");

        linkGrid.add(createFormLabel("Source Node:"), 0, 0);
        linkGrid.add(linkDeviceAComboBox, 1, 0);
        linkGrid.add(createFormLabel("Connect With:"), 0, 1);
        linkGrid.add(linkDeviceBComboBox, 1, 1);
        linkGrid.add(linkHelpLabel, 1, 2);
        linkGrid.add(linkBtn, 1, 3);
        linkGrid.add(connectFeedbackLabel, 1, 4);

        connectCard.getChildren().add(linkGrid);

        managementDeck.getChildren().addAll(addCard, connectCard);
        view.getChildren().add(managementDeck);

        return view;
    }

    private TableView<CommunicationDevice> createDeviceTable() {
        TableView<CommunicationDevice> table = new TableView<>();
        table.setPlaceholder(new Label("No communication devices currently registered."));
        table.setPrefHeight(360);

        // Col 1: Name & ID
        TableColumn<CommunicationDevice, String> nameCol = new TableColumn<>("Device Name & ID");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getName() + " [" + cell.getValue().getId() + "]"));

        // Col 2: Type Badge
        TableColumn<CommunicationDevice, String> typeCol = new TableColumn<>("Type");
        typeCol.setPrefWidth(140);
        typeCol.setCellValueFactory(cell -> {
            CommunicationDevice dev = cell.getValue();
            if (dev instanceof SecurityStation) return new SimpleStringProperty("🛡️ Security Post");
            if (dev instanceof MedicalStation) return new SimpleStringProperty("🏥 Medical Center");
            return new SimpleStringProperty("📱 Student Phone");
        });

        // Col 3: Battery Level
        TableColumn<CommunicationDevice, String> batteryCol = new TableColumn<>("Battery");
        batteryCol.setPrefWidth(95);
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
        statusCol.setPrefWidth(120);
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
        linksCol.setPrefWidth(220);
        linksCol.setCellValueFactory(cell -> {
            String peers = graph.getLinks(cell.getValue()).stream()
                    .map(l -> l.getDestination().getName())
                    .collect(Collectors.joining(", "));
            return new SimpleStringProperty(peers.isEmpty() ? "(Isolated Node)" : peers);
        });

        table.getColumns().add(nameCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(batteryCol);
        table.getColumns().add(statusCol);
        table.getColumns().add(linksCol);
        table.setItems(deviceObservableList);

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                selectedDeviceComboBox.setValue(newV);
                updateToggleState(newV);
                if (topologyPane != null) {
                    topologyPane.selectDevice(newV);
                }
            }
        });

        return table;
    }

    // --------------------------------------------------------------------------
    // 4. View 3: 🚨 Emergency Dispatch Workspace
    // --------------------------------------------------------------------------
    private VBox createDispatchView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Emergency Dispatch Console",
                "Select route endpoints and priority to trigger shortest-path BFS mesh propagation"));

        // Contextual Guidance Banner
        dispatchGuidanceBanner = new HBox(8);
        dispatchGuidanceBanner.setAlignment(Pos.CENTER_LEFT);
        dispatchGuidanceBanner.getStyleClass().add("guide-banner");
        dispatchGuidanceLabel = new Label("💡 Select a Sender and Target node to broadcast an emergency distress alert via multi-hop mesh routing.");
        dispatchGuidanceLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px;");
        dispatchGuidanceBanner.getChildren().add(dispatchGuidanceLabel);
        view.getChildren().add(dispatchGuidanceBanner);

        // Dispatch Console Card
        VBox dispatchCard = createCard("Emergency Dispatch Console (Step 3)",
                "Select origin sender, destination target, and priority level");

        GridPane dispatchGrid = new GridPane();
        dispatchGrid.setHgap(14);
        dispatchGrid.setVgap(12);

        senderComboBox = createDeviceComboBox();
        recipientComboBox = createDeviceComboBox();

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        priorityComboBox.setValue(Priority.NORMAL);

        priorityDescLabel = new Label("🔵 Normal Priority: Standard emergency message relay.");
        priorityDescLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");

        priorityComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV == Priority.CRITICAL) {
                priorityDescLabel.setText("🔴 CRITICAL: Life-safety alert. Handheld phones cannot forward; relays exclusively through Security and Medical Stations.");
                priorityDescLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11px;");
            } else if (newV == Priority.HIGH) {
                priorityDescLabel.setText("🟠 HIGH: Urgent distress alert. Broadcasted across all operational relay nodes.");
                priorityDescLabel.setStyle("-fx-text-fill: #fcd34d; -fx-font-size: 11px;");
            } else if (newV == Priority.LOW) {
                priorityDescLabel.setText("🟢 LOW: Routine status check-in and connectivity ping.");
                priorityDescLabel.setStyle("-fx-text-fill: #86efac; -fx-font-size: 11px;");
            } else {
                priorityDescLabel.setText("🔵 NORMAL: Standard emergency message relay.");
                priorityDescLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
            }
        });

        messageTextField = new TextField();
        messageTextField.setPromptText("Enter emergency alert or status details...");
        messageTextField.setPrefWidth(320);

        dispatchGrid.add(createFormLabel("Sender Node (Origin):"), 0, 0);
        dispatchGrid.add(senderComboBox, 1, 0);
        dispatchGrid.add(createFormLabel("Target Node (Recipient):"), 2, 0);
        dispatchGrid.add(recipientComboBox, 3, 0);

        dispatchGrid.add(createFormLabel("Transmission Priority:"), 0, 1);
        VBox priorityBox = new VBox(4, priorityComboBox, priorityDescLabel);
        dispatchGrid.add(priorityBox, 1, 1);

        dispatchGrid.add(createFormLabel("Alert Message Content:"), 2, 1);
        dispatchGrid.add(messageTextField, 3, 1);

        // Quick Preset Chips
        HBox presetRow = new HBox(8);
        presetRow.setAlignment(Pos.CENTER_LEFT);
        Label quickLbl = new Label("1-Click Disaster Presets:");
        quickLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 700;");

        Button presetMed = new Button("🏥 Medical Aid");
        presetMed.getStyleClass().add("btn-preset");
        presetMed.setOnAction(e -> {
            messageTextField.setText("URGENT: Medical assistance requested at Quad Block 4");
            priorityComboBox.setValue(Priority.HIGH);
        });

        Button presetSec = new Button("⚠️ Hazard Alert");
        presetSec.getStyleClass().add("btn-preset");
        presetSec.setOnAction(e -> {
            messageTextField.setText("HAZARD: Structural blockage reported near Gate 2");
            priorityComboBox.setValue(Priority.NORMAL);
        });

        Button presetPing = new Button("ℹ️ Node Heartbeat");
        presetPing.getStyleClass().add("btn-preset");
        presetPing.setOnAction(e -> {
            messageTextField.setText("STATUS: Node heartbeat and path liveness ping");
            priorityComboBox.setValue(Priority.LOW);
        });

        presetRow.getChildren().addAll(quickLbl, presetMed, presetSec, presetPing);

        Button dispatchBtn = new Button("🚀 DISPATCH EMERGENCY ALERT");
        dispatchBtn.getStyleClass().add("btn-dispatch");
        dispatchBtn.setMaxWidth(Double.MAX_VALUE);
        dispatchBtn.setOnAction(e -> handleSendMessage());

        dispatchCard.getChildren().addAll(dispatchGrid, presetRow, dispatchBtn);
        view.getChildren().add(dispatchCard);

        return view;
    }

    private VBox createOutcomeCard() {
        VBox card = createCard("Simulation Delivery Outcome", "BFS shortest-path discovery & energy consumption analysis");
        card.getStyleClass().clear();
        card.getStyleClass().add("dash-card-highlight");

        HBox outcomeStatusRow = new HBox(10);
        outcomeStatusRow.setAlignment(Pos.CENTER_LEFT);

        Label outcomeTitle = new Label("STATUS:");
        outcomeTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 12px;");

        resultBadge = new Label("WAITING FOR DISPATCH");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");

        Region outcomeSpacer = new Region();
        HBox.setHgrow(outcomeSpacer, javafx.scene.layout.Priority.ALWAYS);

        replayFromOutcomeBtn = new Button("🎬 Watch Replay on Canvas");
        replayFromOutcomeBtn.setStyle("-fx-background-color: #0e223d; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #0284c7; -fx-border-radius: 5px; -fx-padding: 4 10; -fx-cursor: hand;");
        replayFromOutcomeBtn.setDisable(true);
        replayFromOutcomeBtn.setOnAction(e -> {
            showView(networkScrollPane, navNetworkBtn);
            if (networkTabPane != null) {
                networkTabPane.getSelectionModel().select(topologyTab);
            }
            if (topologyPane != null) {
                topologyPane.playReplay();
            }
        });

        viewTimelineFromOutcomeBtn = new Button("⏱️ View Timeline");
        viewTimelineFromOutcomeBtn.getStyleClass().add("btn-ghost");
        viewTimelineFromOutcomeBtn.setOnAction(e -> showView(activityScrollPane, navActivityBtn));

        outcomeStatusRow.getChildren().addAll(outcomeTitle, resultBadge, outcomeSpacer, replayFromOutcomeBtn, viewTimelineFromOutcomeBtn);

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

        // Collapsible Technical Diagnostics
        toggleTechDetailsBtn = new Button("🔍 [ Show Technical Route Details ]");
        toggleTechDetailsBtn.getStyleClass().add("btn-ghost");
        toggleTechDetailsBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");

        technicalDetailsBox = new VBox(6);
        technicalDetailsBox.getStyleClass().add("tech-details-box");
        technicalDetailsBox.setVisible(false);
        technicalDetailsBox.setManaged(false);

        technicalDetailsContent = new Label("No simulation executed yet.");
        technicalDetailsContent.setStyle("-fx-text-fill: #94a3b8; -fx-font-family: monospace; -fx-font-size: 11px;");
        technicalDetailsContent.setWrapText(true);
        technicalDetailsBox.getChildren().add(technicalDetailsContent);

        toggleTechDetailsBtn.setOnAction(e -> {
            techDetailsVisible = !techDetailsVisible;
            technicalDetailsBox.setVisible(techDetailsVisible);
            technicalDetailsBox.setManaged(techDetailsVisible);
            toggleTechDetailsBtn.setText(techDetailsVisible ? "🔍 [ Hide Technical Route Details ]" : "🔍 [ Show Technical Route Details ]");
        });

        card.getChildren().addAll(outcomeStatusRow, routeSection, resultExplanationLabel, toggleTechDetailsBtn, technicalDetailsBox);
        return card;
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
    // 5. View 4: 📜 Activity History & Event Timeline Workspace
    // --------------------------------------------------------------------------
    private VBox createActivityView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Simulation History & Activity Logs",
                "Non-destructive historical replay, discrete event milestone timeline, and real-time terminal audit feed"));

        // Historical Run Selection Bar
        HBox historyBar = new HBox(12);
        historyBar.setAlignment(Pos.CENTER_LEFT);
        historyBar.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 10px 14px; -fx-border-color: #192742; -fx-border-radius: 8px;");

        Label historyLbl = new Label("Simulation Run:");
        historyLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");

        historyComboBox = new ComboBox<>();
        historyComboBox.setItems(simulationHistory);
        historyComboBox.setPrefWidth(340);
        historyComboBox.setPromptText("No simulations recorded yet");
        historyComboBox.setStyle("-fx-font-size: 11px; -fx-background-color: #0f172a; -fx-text-fill: #38bdf8;");

        historyComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                currentTimelineEvents.setAll(newV.getTimeline().getEvents());
                updateTimelineDetails(newV);
                if (topologyPane != null) {
                    topologyPane.loadReplay(newV);
                }
            }
        });

        Button replayHistoryBtn = new Button("🎬 Watch Replay on Canvas");
        replayHistoryBtn.getStyleClass().add("btn-cyan");
        replayHistoryBtn.setOnAction(e -> {
            SimulationRecord sel = historyComboBox.getValue();
            if (sel != null) {
                showView(networkScrollPane, navNetworkBtn);
                if (networkTabPane != null) {
                    networkTabPane.getSelectionModel().select(topologyTab);
                }
                if (topologyPane != null) {
                    topologyPane.playReplay();
                }
            } else {
                showAlert("No Simulation Selected", "Please select a recorded simulation run from the dropdown first.");
            }
        });

        timelineSummaryLabel = new Label("Dispatched emergency transmissions will record discrete milestone events here.");
        timelineSummaryLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 600;");

        historyBar.getChildren().addAll(historyLbl, historyComboBox, replayHistoryBtn, timelineSummaryLabel);
        view.getChildren().add(historyBar);

        // Tabbed Activity Workspace: Timeline Table vs Live Terminal
        TabPane activityTabs = new TabPane();
        activityTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(activityTabs, javafx.scene.layout.Priority.ALWAYS);

        // Tab 1: Event Timeline
        VBox timelineTabContent = createTimelinePanel();
        Tab timelineSubTab = new Tab("⏱️ Step-by-Step Event Timeline", timelineTabContent);

        // Tab 2: Terminal Logs
        VBox terminalTabContent = createTerminalLogsPanel();
        Tab terminalSubTab = new Tab("📜 Terminal Activity Feed", terminalTabContent);

        activityTabs.getTabs().addAll(timelineSubTab, terminalSubTab);
        view.getChildren().add(activityTabs);

        return view;
    }

    private VBox createTimelinePanel() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: #070c17; -fx-background-radius: 10px; -fx-border-color: #192742; -fx-border-radius: 10px;");
        container.setPrefHeight(380);

        timelineTable = new TableView<>();
        timelineTable.setItems(currentTimelineEvents);
        timelineTable.setPlaceholder(new Label("No simulation events recorded yet. Dispatch an alert to populate the timeline."));
        timelineTable.getStyleClass().add("device-table");
        VBox.setVgrow(timelineTable, javafx.scene.layout.Priority.ALWAYS);

        TableColumn<SimulationEvent, Integer> seqCol = new TableColumn<>("Step #");
        seqCol.setPrefWidth(60);
        seqCol.setCellValueFactory(new PropertyValueFactory<>("sequenceNumber"));

        TableColumn<SimulationEvent, String> elapsedCol = new TableColumn<>("Time");
        elapsedCol.setPrefWidth(85);
        elapsedCol.setCellValueFactory(new PropertyValueFactory<>("formattedElapsed"));
        elapsedCol.setStyle("-fx-alignment: center; -fx-font-family: monospace; -fx-text-fill: #22d3ee; -fx-font-weight: bold;");

        TableColumn<SimulationEvent, String> timestampCol = new TableColumn<>("Timestamp");
        timestampCol.setPrefWidth(95);
        timestampCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));

        TableColumn<SimulationEvent, String> typeCol = new TableColumn<>("Event Type");
        typeCol.setPrefWidth(140);
        typeCol.setCellValueFactory(cell -> new SimpleStringProperty(cell.getValue().getType().toString()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Delivered")) {
                        setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold;");
                    } else if (item.contains("Failed")) {
                        setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold;");
                    } else if (item.contains("Forwarding")) {
                        setStyle("-fx-text-fill: #06b6d4; -fx-font-weight: bold;");
                    } else if (item.contains("Discovered")) {
                        setStyle("-fx-text-fill: #a855f7; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-weight: bold;");
                    }
                }
            }
        });

        TableColumn<SimulationEvent, String> srcCol = new TableColumn<>("Source Node");
        srcCol.setPrefWidth(140);
        srcCol.setCellValueFactory(new PropertyValueFactory<>("sourceDeviceName"));

        TableColumn<SimulationEvent, String> dstCol = new TableColumn<>("Target Node");
        dstCol.setPrefWidth(140);
        dstCol.setCellValueFactory(new PropertyValueFactory<>("targetDeviceName"));

        TableColumn<SimulationEvent, String> descCol = new TableColumn<>("Narrative Telemetry & Reason");
        descCol.setPrefWidth(380);
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        timelineTable.getColumns().add(seqCol);
        timelineTable.getColumns().add(elapsedCol);
        timelineTable.getColumns().add(timestampCol);
        timelineTable.getColumns().add(typeCol);
        timelineTable.getColumns().add(srcCol);
        timelineTable.getColumns().add(dstCol);
        timelineTable.getColumns().add(descCol);

        eventDetailLabel = new Label("Select an event above to view detailed diagnostics.");
        eventDetailLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");

        timelineTable.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                eventDetailLabel.setText(String.format("Step %d (%s @ %s): %s",
                        newV.getSequenceNumber(), newV.getFormattedElapsed(), newV.getTimestamp(), newV.getDescription()));
            }
        });

        container.getChildren().addAll(timelineTable, eventDetailLabel);
        return container;
    }

    private VBox createTerminalLogsPanel() {
        VBox container = new VBox(10);
        container.setPadding(new Insets(12));
        container.setStyle("-fx-background-color: #070c17; -fx-background-radius: 10px; -fx-border-color: #192742; -fx-border-radius: 10px;");
        container.setPrefHeight(380);

        activityFeedArea = new TextArea();
        activityFeedArea.setEditable(false);
        activityFeedArea.setWrapText(true);
        activityFeedArea.getStyleClass().add("terminal-area");
        VBox.setVgrow(activityFeedArea, javafx.scene.layout.Priority.ALWAYS);

        HBox logActionRow = new HBox(10);
        logActionRow.setAlignment(Pos.CENTER_RIGHT);

        Button clearLogBtn = new Button("Clear Feed");
        clearLogBtn.getStyleClass().add("btn-ghost");
        clearLogBtn.setOnAction(e -> activityFeedArea.clear());

        logActionRow.getChildren().add(clearLogBtn);
        container.getChildren().addAll(activityFeedArea, logActionRow);
        return container;
    }

    private void updateTimelineDetails(SimulationRecord record) {
        if (record == null) return;
        boolean delivered = record.isDelivered();
        timelineSummaryLabel.setText(String.format("[%s] %s | %s | %d Events (%d ms)",
                record.getId(),
                record.getMessage().getPriority(),
                delivered ? "✔ DELIVERED (" + record.getHopCount() + " hops)" : "✖ FAILED",
                record.getTimeline().size(),
                record.getTimeline().getTotalDurationMs()));
        if (delivered) {
            timelineSummaryLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 700;");
        } else {
            timelineSummaryLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 700;");
        }
    }

    // --------------------------------------------------------------------------
    // 6. Action Handlers: Dispatch, Nodes, Links & Configurations
    // --------------------------------------------------------------------------
    private void handleSendMessage() {
        CommunicationDevice sender = senderComboBox.getValue();
        CommunicationDevice recipient = recipientComboBox.getValue();
        Priority priority = priorityComboBox.getValue();
        String content = messageTextField.getText();

        if (sender == null || recipient == null) {
            showAlert("Incomplete Form", "Please select both an Origin Sender node and a Target Recipient node.");
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

        if (topologyPane != null) {
            topologyPane.clearRouteHighlight();
        }

        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content.trim(), priority);
        log("DISPATCH", String.format("[%s] Initiated from '%s' to '%s' | Priority: %s", msgId, sender.getName(), recipient.getName(), priority));

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

            technicalDetailsContent.setText(String.format(
                    "ALGORITHM: Breadcrumb BFS Shortest-Path Discovery\n" +
                    "MESSAGE ID: %s | PRIORITY: %s\n" +
                    "CALCULATED ROUTE (%d hops):\n  %s\n" +
                    "BATTERY IMPACT: -2.0%% deducted per hop (%d nodes drained)\n" +
                    "STATUS: Delivered successfully to destination.",
                    msgId, priority, hops, routeStr, result.route().size()));
        } else {
            resultBadge.setText("✖ DELIVERY FAILED");
            resultBadge.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #dc2626; -fx-border-radius: 4px;");

            renderHopRoute(result.route());
            if (topologyPane != null) {
                topologyPane.clearRouteHighlight();
            }
            resultExplanationLabel.setText(result.explanation() + "\n💡 Fix: Verify intermediate nodes are online, have sufficient battery (>0%), and can forward this priority.");

            log("FAILED", String.format("[%s] Delivery failed. Reason: %s", msgId, result.explanation()));

            technicalDetailsContent.setText(String.format(
                    "ALGORITHM: Breadcrumb BFS Shortest-Path Discovery\n" +
                    "MESSAGE ID: %s | PRIORITY: %s\n" +
                    "FAILURE DIAGNOSTIC: %s\n" +
                    "STATUS: BFS search completed without finding a viable route.",
                    msgId, priority, result.explanation()));
        }

        // Timeline and Historical Recording
        SimulationTimeline timeline = SimulationTimeline.fromSimulation(message, result);
        SimulationRecord record = new SimulationRecord(message, result, timeline);
        simulationHistory.add(0, record);

        if (historyComboBox != null) {
            historyComboBox.setValue(record);
        }
        currentTimelineEvents.setAll(timeline.getEvents());
        updateTimelineDetails(record);

        if (replayFromOutcomeBtn != null) {
            replayFromOutcomeBtn.setDisable(false);
        }

        if (topologyPane != null) {
            topologyPane.loadReplay(record);
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
                showAlert("Battery Depleted", "Cannot turn device online: Battery is 0%. Please use 'Recharge to 100%' first.");
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
        addDeviceFeedbackLabel.setText("✔ Registered " + type + ": '" + newDev.getName() + "' [" + newDev.getId() + "] successfully!");
        addDeviceFeedbackLabel.setStyle("-fx-text-fill: #34d399;");

        refreshUI();
        selectedDeviceComboBox.setValue(newDev);

        log("NODE", "Registered new " + type + ": '" + newDev.getName() + "' [" + newDev.getId() + "] at " + loc);
    }

    private void handleConnectDevices() {
        CommunicationDevice devA = linkDeviceAComboBox.getValue();
        CommunicationDevice devB = linkDeviceBComboBox.getValue();

        if (devA == null || devB == null) {
            connectFeedbackLabel.setText("⚠️ Please select both devices to establish a mesh link.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        if (devA.equals(devB)) {
            connectFeedbackLabel.setText("⚠️ Cannot connect a device to itself. Choose two distinct devices.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f87171;");
            return;
        }

        boolean connected = graph.connect(devA, devB);
        if (connected) {
            connectFeedbackLabel.setText("✔ Established mesh link: '" + devA.getName() + "' <───> '" + devB.getName() + "'");
            connectFeedbackLabel.setStyle("-fx-text-fill: #34d399;");
            refreshUI();
            log("LINK", String.format("Established mesh link: '%s' <───> '%s'", devA.getName(), devB.getName()));
        } else {
            connectFeedbackLabel.setText("ℹ️ Devices '" + devA.getName() + "' and '" + devB.getName() + "' are already directly connected.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #38bdf8;");
        }
    }

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
                if (topologyPane != null) {
                    topologyPane.resetReplay();
                    topologyPane.clearRouteHighlight();
                }
                if (replayFromOutcomeBtn != null) {
                    replayFromOutcomeBtn.setDisable(true);
                }
                renderEmptyRoute();
                resultBadge.setText("WAITING FOR DISPATCH");
                resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
                resultExplanationLabel.setText("Loaded new network configuration. Ready for simulation dispatch.");

                NetworkConfigManager.loadFromFile(file, graph);
                deviceIdCounter = Math.max(deviceIdCounter, graph.getDeviceCount() + 1);

                refreshUI();

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
            topologyPane.resetReplay();
            topologyPane.clearRouteHighlight();
        }
        if (replayFromOutcomeBtn != null) {
            replayFromOutcomeBtn.setDisable(true);
        }
        renderEmptyRoute();
        resultBadge.setText("WAITING FOR DISPATCH");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
        resultExplanationLabel.setText("New simulation initiated. All devices recharged to 100% and delivery metrics reset.");

        for (CommunicationDevice dev : graph.getAllDevices()) {
            dev.recharge(100.0);
        }

        refreshUI();
        log("SIMULATION", "Started new simulation session. All devices recharged to 100% and transmission counters reset.");
    }

    private void updateToggleState(CommunicationDevice dev) {
        if (dev == null) {
            dynamicToggleBtn.setText("Toggle Online / Offline");
            dynamicToggleBtn.setStyle("");
            if (selectedDeviceStatusLabel != null) selectedDeviceStatusLabel.setText("");
            return;
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            dynamicToggleBtn.setText("🟢 Bring ONLINE");
            dynamicToggleBtn.setStyle("-fx-background-color: #059669; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
            if (selectedDeviceStatusLabel != null) {
                selectedDeviceStatusLabel.setText("Status: 🔴 OFFLINE (" + String.format("%.0f%%", dev.getBatteryLevel()) + ")");
                selectedDeviceStatusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
            }
        } else {
            dynamicToggleBtn.setText("🔴 Take OFFLINE");
            dynamicToggleBtn.setStyle("-fx-background-color: #e11d48; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-background-radius: 6px;");
            if (selectedDeviceStatusLabel != null) {
                selectedDeviceStatusLabel.setText("Status: 🟢 ONLINE (" + String.format("%.0f%%", dev.getBatteryLevel()) + ")");
                selectedDeviceStatusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px;");
            }
        }
    }

    // --------------------------------------------------------------------------
    // 7. Helpers & State Synchronization
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
        if (deviceTable != null) {
            deviceTable.refresh();
        }
        if (topologyPane != null) {
            topologyPane.refresh();
        }

        int totalDevices = graph.getDeviceCount();
        long activeDevices = graph.getAllDevices().stream().filter(CommunicationDevice::isAvailable).count();
        long offlineDevices = totalDevices - activeDevices;
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

        // Update Dashboard Architecture Overview
        long phoneCount = graph.getAllDevices().stream().filter(d -> d instanceof StudentPhone).count();
        long secCount = graph.getAllDevices().stream().filter(d -> d instanceof SecurityStation).count();
        long medCount = graph.getAllDevices().stream().filter(d -> d instanceof MedicalStation).count();

        if (dashboardPhonesCountLabel != null) dashboardPhonesCountLabel.setText(String.valueOf(phoneCount));
        if (dashboardSecurityCountLabel != null) dashboardSecurityCountLabel.setText(String.valueOf(secCount));
        if (dashboardMedicalCountLabel != null) dashboardMedicalCountLabel.setText(String.valueOf(medCount));
        if (dashboardActiveOnlineCountLabel != null) dashboardActiveOnlineCountLabel.setText(activeDevices + " Nodes Active");
        if (dashboardOfflineCountLabel != null) dashboardOfflineCountLabel.setText(offlineDevices + " Offline");

        // Sync sidebar telemetry
        sidebarActiveNodesCountLabel.setText("Active Nodes: " + activeDevices + " / " + totalDevices);
        sidebarLinksCountLabel.setText("Mesh Density: " + totalLinks + " Links");

        // Update Guided Workflow Stepper Badges
        if (step1StatusLabel != null) {
            if (totalDevices < 2) {
                step1StatusLabel.setText("⚠️ " + totalDevices + " Added (Need ≥2)");
                step1StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #f59e0b;");
            } else {
                step1StatusLabel.setText("✔ " + totalDevices + " Registered");
                step1StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #34d399;");
            }
        }

        if (step2StatusLabel != null) {
            if (totalLinks < 1) {
                step2StatusLabel.setText("⚠️ 0 Links (Disconnected)");
                step2StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #f59e0b;");
            } else {
                step2StatusLabel.setText("✔ " + totalLinks + " Links Active");
                step2StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #34d399;");
            }
        }

        if (step3StatusLabel != null) {
            if (totalDevices >= 2 && totalLinks >= 1 && activeDevices >= 2) {
                step3StatusLabel.setText("⚡ Ready to Dispatch");
                step3StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #22d3ee;");
            } else {
                step3StatusLabel.setText("⏳ Setup Incomplete");
                step3StatusLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #94a3b8;");
            }
        }

        // Update Contextual Dispatch Guidance Banner
        if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
            if (totalDevices < 2) {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
                dispatchGuidanceLabel.setText("⚠️ Setup Incomplete: You need at least 2 registered devices in the network before dispatching an alert. Click 'Network & Devices' in the sidebar to add nodes.");
            } else if (totalLinks < 1) {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
                dispatchGuidanceLabel.setText("⚠️ Mesh Disconnected: Registered devices currently have no active links between them. Connect devices in 'Network & Devices' to establish communication paths.");
            } else {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner");
                dispatchGuidanceLabel.setText("💡 Routing Tip: Emergency alerts propagate along the shortest active path discovered via BFS. Participating nodes deduct -2.0% battery per hop.");
            }
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

    private VBox createCard(String title, String subtitle) {
        VBox card = new VBox(8);
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

    private VBox createSectionHeader(String title, String subtitle) {
        VBox header = new VBox(3);
        Label t = new Label(title);
        t.getStyleClass().add("section-header-title");
        Label s = new Label(subtitle);
        s.getStyleClass().add("section-header-subtitle");
        header.getChildren().addAll(t, s);
        return header;
    }

    private Label createFormLabel(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");
        return lbl;
    }

    private void log(String category, String message) {
        String timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        if (activityFeedArea != null) {
            activityFeedArea.appendText(String.format("[%s] [%-8s] %s\n", timestamp, category, message));
        }
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
