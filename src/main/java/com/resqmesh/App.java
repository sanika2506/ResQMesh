package com.resqmesh;

import com.resqmesh.config.AppSettings;
import com.resqmesh.config.AppSettingsManager;
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
import com.resqmesh.onboarding.TutorialManager;
import com.resqmesh.onboarding.TutorialStep;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import com.resqmesh.simulation.timeline.SimulationEvent;
import com.resqmesh.simulation.history.EmergencyHistoryManager;
import com.resqmesh.simulation.timeline.SimulationRecord;
import com.resqmesh.simulation.timeline.SimulationTimeline;
import com.resqmesh.ui.NetworkTopologyPane;

import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.util.StringConverter;

import java.io.File;
import java.net.URL;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ResQMesh – Emergency Communication Dashboard
 * User Experience & Guided Workflow Overhaul.
 *
 * Five distinct, uncluttered primary workspaces:
 * 1. Dashboard: Guided 3-step workflow, KPI cards, network health overview, quick dispatch & outcome snapshot.
 * 2. Network & Devices: Dedicated spacious topology canvas, telemetry table, node controls, and easy provisioning.
 * 3. Emergency Dispatch: Distraction-free dispatch console with presets, human-friendly outcome, and collapsible technical details.
 * 4. Activity History: Timeline event audit trail and real-time terminal feed.
 * 5. Help & Quick Guide: Comprehensive beginner reference, device specifications, relay rules, and troubleshooting FAQ.
 */
public class App extends Application {

    private static final String APP_TITLE = "ResQMesh – Emergency Communication Dashboard";
    private static final int DEFAULT_WIDTH = 1340;
    private static final int DEFAULT_HEIGHT = 880;

    private Stage primaryStage;

    // Backend Simulation Core
    private NetworkGraph graph;
    private SimulationEngine engine;

    // Observable State
    private ObservableList<CommunicationDevice> deviceObservableList;
    private int deviceIdCounter = 1;
    private int messageCounter = 1;
    private int totalMessagesDispatched = 0;
    private int successfulDeliveries = 0;

    // Step 12: Onboarding & Interactive Guidance
    private final TutorialManager tutorialManager = new TutorialManager();
    private VBox tutorialBanner;
    private Label tutorialBadgeLabel;
    private Label tutorialTitleLabel;
    private Label tutorialDescLabel;
    private Label tutorialHintLabel;
    private Button tutorialPrevBtn;
    private Button tutorialNextBtn;
    private Button tutorialRestartBtn;
    private Button tutorialSkipBtn;

    // Section Views & Navigation (7 Clean Workspaces)
    private StackPane viewContainer;
    private ScrollPane dashboardScrollPane;
    private ScrollPane networkScrollPane;
    private ScrollPane dispatchScrollPane;
    private ScrollPane historyScrollPane;
    private ScrollPane activityScrollPane;
    private ScrollPane helpScrollPane;
    private ScrollPane settingsScrollPane;

    private Button navDashboardBtn;
    private Button navNetworkBtn;
    private Button navDispatchBtn;
    private Button navHistoryBtn;
    private Button navActivityBtn;
    private Button navHelpBtn;
    private Button navSettingsBtn;
    private final List<Button> navButtons = new ArrayList<>();

    // Settings Management & Form Controls
    private final AppSettingsManager settingsManager = new AppSettingsManager();
    private Label energyRuleLabel;
    private Spinner<Double> batteryDrainSpinner;
    private ComboBox<Priority> defaultPriorityComboBox;
    private ComboBox<String> replaySpeedComboBox;
    private CheckBox confirmNetworkReplacementCheck;
    private CheckBox confirmClearHistoryCheck;
    private CheckBox confirmClearNetworkCheck;
    private CheckBox showTutorialBannerCheck;
    private CheckBox autoSelectRecipientCheck;
    private Label settingsFeedbackLabel;

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

    // Contextual Guidance & Feedback for Dispatch
    private HBox dispatchGuidanceBanner;
    private Label dispatchGuidanceLabel;
    private Label dispatchInlineErrorLabel;
    private Button sendEmergencyBtn;
    private Button clearDispatchBtn;

    // Delivery Outcome Showcase
    private Label resultBadge;
    private HBox resultRouteHBox;
    private Label resultExplanationLabel;
    private VBox outcomeCard;
    private Button replayFromOutcomeBtn;
    private Button viewTimelineFromOutcomeBtn;
    private Label outcomeStatusMetricLabel;
    private Label outcomeHopsMetricLabel;
    private Label outcomeNodesMetricLabel;
    private Label outcomePriorityMetricLabel;
    private Label outcomeReadableRouteLabel;

    // Dashboard Outcome Snapshot
    private Label dashboardOutcomeBadge;
    private Label dashboardOutcomeExplanation;

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
    private final EmergencyHistoryManager historyManager = new EmergencyHistoryManager(simulationHistory);

    // Emergency History Table & Filter Components
    private TableView<SimulationRecord> historyTableView;
    private FilteredList<SimulationRecord> filteredHistoryList;
    private SortedList<SimulationRecord> sortedHistoryList;
    private TextField historySearchField;
    private ComboBox<String> historyStatusFilter;
    private ComboBox<String> historyDeviceFilter;

    // History Top Summary KPI Labels
    private Label histTotalKpiLabel;
    private Label histDeliveredKpiLabel;
    private Label histNoRouteKpiLabel;
    private Label histFailedKpiLabel;
    private Label histRateKpiLabel;

    // History Detail Inspector Components
    private VBox historyDetailCard;
    private Label historyDetailIdLabel;
    private Label historyDetailTimeBadge;
    private Label historyDetailStatusBadge;
    private Label historyDetailPriorityBadge;
    private Label historyDetailSenderLabel;
    private Label historyDetailRecipientLabel;
    private Label historyDetailMessageContent;
    private HBox historyDetailRouteHBox;
    private Label historyDetailExplanationLabel;
    private Button historyReplayBtn;
    private Button historyTimelineBtn;

    // Event Timeline
    private final ObservableList<SimulationEvent> currentTimelineEvents = FXCollections.observableArrayList();
    private ComboBox<SimulationRecord> historyComboBox;
    private TableView<SimulationEvent> timelineTable;
    private Label timelineSummaryLabel;
    private Label eventDetailLabel;
    private Tab topologyTab;
    private Tab tableTab;

    @Override
    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Initialize Backend
        graph = new NetworkGraph();
        engine = new SimulationEngine(graph, new ShortestPathStrategy());
        AppSettings initialSettings = settingsManager.getSettings();
        engine.setBatteryCostPerTransmission(initialSettings.getBatteryDrainPerHop());
        deviceObservableList = FXCollections.observableArrayList();

        // Root Layout: Dark Navy BorderPane
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #070b14;");

        // Top Navigation Menu Bar
        root.setTop(createMenuBar());

        // 1. Left Sidebar Navigation
        root.setLeft(createSidebar());

        // 2. Center View Container (Holds the 7 clean sections)
        viewContainer = new StackPane();
        viewContainer.setStyle("-fx-background-color: #070b14;");

        // Initialize Shared Network Topology Canvas & Device Table
        topologyPane = new NetworkTopologyPane(graph);
        topologyPane.setReplaySpeed(initialSettings.getReplaySpeed());
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

        // Build the 7 Primary Workspaces
        dashboardScrollPane = wrapInScrollPane(createDashboardView());
        networkScrollPane = wrapInScrollPane(createNetworkView());
        dispatchScrollPane = wrapInScrollPane(createDispatchView());
        historyScrollPane = wrapInScrollPane(createHistoryView());
        activityScrollPane = wrapInScrollPane(createActivityView());
        helpScrollPane = wrapInScrollPane(createHelpView());
        settingsScrollPane = wrapInScrollPane(createSettingsView());

        // Automatically update History KPI metrics when simulation records change
        simulationHistory.addListener((javafx.collections.ListChangeListener<SimulationRecord>) c -> {
            updateHistorySummaryKpis();
        });

        // Default to Dashboard
        showView(dashboardScrollPane, navDashboardBtn);

        // Center Content Stack: Tutorial Banner on top + Workspaces Container underneath
        VBox centerContent = new VBox();
        centerContent.setStyle("-fx-background-color: #070b14;");
        tutorialBanner = createTutorialBanner();
        tutorialBanner.setVisible(initialSettings.isShowTutorialBanner());
        tutorialBanner.setManaged(initialSettings.isShowTutorialBanner());
        VBox.setVgrow(viewContainer, javafx.scene.layout.Priority.ALWAYS);
        centerContent.getChildren().addAll(tutorialBanner, viewContainer);
        root.setCenter(centerContent);

        // Preload standard campus disaster relief network safely (no prompt on initial launch)
        loadSampleNetwork(false);

        // Initialize tutorial banner display
        updateTutorialBanner();

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

        // Navigation Menu (7 Clear Primary Sections)
        VBox navMenu = new VBox(6);
        navDashboardBtn = createNavButton("📊 Dashboard", true);
        navNetworkBtn = createNavButton("🗺️ Network & Devices", false);
        navDispatchBtn = createNavButton("🚨 Emergency Dispatch", false);
        navHistoryBtn = createNavButton("📋 Emergency History", false);
        navActivityBtn = createNavButton("⏱️ Timeline & Activity", false);
        navHelpBtn = createNavButton("📖 Quick User Guide", false);
        navSettingsBtn = createNavButton("⚙️ Settings", false);

        setTooltip(navDashboardBtn, "Overview dashboard with guided 3-step workflow, KPI metrics, and outcome snapshot");
        setTooltip(navNetworkBtn, "Interactive visual topology canvas, node power control, and device provisioning");
        setTooltip(navDispatchBtn, "Emergency message dispatch console with priority settings and BFS route computation");
        setTooltip(navHistoryBtn, "Filterable audit log of emergency dispatches, route performance, and summary reports");
        setTooltip(navActivityBtn, "Chronological event timeline audit log and real-time terminal feed");
        setTooltip(navHelpBtn, "Complete step-by-step user guide, basic workflow, offline recovery, and specifications");
        setTooltip(navSettingsBtn, "Configure simulation parameters, safety confirmation prompts, and interface preferences");

        navButtons.clear();
        navButtons.addAll(List.of(navDashboardBtn, navNetworkBtn, navDispatchBtn, navHistoryBtn, navActivityBtn, navHelpBtn, navSettingsBtn));

        navDashboardBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        navNetworkBtn.setOnAction(e -> showView(networkScrollPane, navNetworkBtn));
        navDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));
        navHistoryBtn.setOnAction(e -> showView(historyScrollPane, navHistoryBtn));
        navActivityBtn.setOnAction(e -> showView(activityScrollPane, navActivityBtn));
        navHelpBtn.setOnAction(e -> showView(helpScrollPane, navHelpBtn));
        navSettingsBtn.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));

        navMenu.getChildren().addAll(navDashboardBtn, navNetworkBtn, navDispatchBtn, navHistoryBtn, navActivityBtn, navHelpBtn, navSettingsBtn);

        // Live Mesh Health Monitor Widget
        VBox healthWidget = new VBox(8);
        healthWidget.getStyleClass().add("sidebar-widget");

        Label healthTitle = new Label("SYSTEM TELEMETRY");
        healthTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase;");

        sidebarActiveNodesCountLabel = new Label("Active Nodes: 0");
        sidebarActiveNodesCountLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        setTooltip(sidebarActiveNodesCountLabel, "Operational nodes available for message forwarding");

        sidebarLinksCountLabel = new Label("Mesh Density: 0 Links");
        sidebarLinksCountLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        setTooltip(sidebarLinksCountLabel, "Active bidirectional wireless channels across the mesh");

        Label routingEngineLabel = new Label("Routing: Shortest Path (BFS)");
        routingEngineLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 600;");
        setTooltip(routingEngineLabel, "Dynamic shortest path discovery using Breadth-First Search");

        energyRuleLabel = new Label(String.format("Energy Drain: -%.1f%% / hop", settingsManager.getSettings().getBatteryDrainPerHop()));
        energyRuleLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-size: 11px;");
        setTooltip(energyRuleLabel, "Battery deduction per node traversed in the message path (configurable in Settings)");

        healthWidget.getChildren().addAll(healthTitle, sidebarActiveNodesCountLabel, sidebarLinksCountLabel, routingEngineLabel, energyRuleLabel);

        // Configuration & Persistence Actions
        VBox actionsBox = new VBox(8);
        Label actionsTitle = new Label("CONFIGURATION & ACTIONS");
        actionsTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Button welcomeBtn = new Button("👋 Welcome & Tutorial");
        welcomeBtn.setMaxWidth(Double.MAX_VALUE);
        welcomeBtn.setStyle("-fx-background-color: #0b223d; -fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12; -fx-border-color: #0284c7; -fx-border-radius: 6px;");
        welcomeBtn.setOnAction(e -> showWelcomeDialog());
        setTooltip(welcomeBtn, "Open Welcome Overview and interactive tutorial launcher");

        Button newSimBtn = new Button("✨ New Simulation");
        newSimBtn.setMaxWidth(Double.MAX_VALUE);
        newSimBtn.setStyle("-fx-background-color: #0e223d; -fx-text-fill: #38bdf8; -fx-font-size: 12px; -fx-font-weight: bold; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12; -fx-border-color: #0284c7; -fx-border-radius: 6px;");
        newSimBtn.setOnAction(e -> handleNewSimulation());
        setTooltip(newSimBtn, "Recharge all nodes to 100% and reset transmission counters");

        Button saveBtn = new Button("💾 Save Network (JSON)");
        saveBtn.setMaxWidth(Double.MAX_VALUE);
        saveBtn.getStyleClass().add("btn-ghost");
        saveBtn.setOnAction(e -> handleSaveNetwork());
        setTooltip(saveBtn, "Export current network topology to a JSON file");

        Button loadBtn = new Button("📂 Load Network (JSON)");
        loadBtn.setMaxWidth(Double.MAX_VALUE);
        loadBtn.getStyleClass().add("btn-ghost");
        loadBtn.setOnAction(e -> handleLoadNetwork());
        setTooltip(loadBtn, "Import and restore a saved network topology from JSON");

        Button resetBtn = new Button("↺ Reset Sample Mesh");
        resetBtn.setMaxWidth(Double.MAX_VALUE);
        resetBtn.getStyleClass().add("btn-ghost");
        resetBtn.setOnAction(e -> {
            loadSampleNetwork();
            log("TOPOLOGY", "Reset to preloaded campus emergency mesh topology.");
        });
        setTooltip(resetBtn, "Safely load or restore standard 5-node campus disaster network");

        Button clearBtn = new Button("✕ Clear Network Graph");
        clearBtn.setMaxWidth(Double.MAX_VALUE);
        clearBtn.setStyle("-fx-background-color: #271419; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 7 12;");
        clearBtn.setOnAction(e -> handleClearNetworkWithConfirmation());
        setTooltip(clearBtn, "Remove all devices and links to start building a custom network");

        actionsBox.getChildren().addAll(actionsTitle, welcomeBtn, newSimBtn, saveBtn, loadBtn, resetBtn, clearBtn);

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

        Button quickSaveBtn = new Button("💾 Save");
        quickSaveBtn.getStyleClass().add("btn-ghost");
        quickSaveBtn.setOnAction(e -> handleSaveNetwork());
        setTooltip(quickSaveBtn, "Save active network configuration to JSON");

        Button quickLoadBtn = new Button("📂 Load");
        quickLoadBtn.getStyleClass().add("btn-ghost");
        quickLoadBtn.setOnAction(e -> handleLoadNetwork());
        Button quickGuideBtn = new Button("📖 Quick Guide");
        quickGuideBtn.getStyleClass().add("btn-ghost");
        quickGuideBtn.setOnAction(e -> showView(helpScrollPane, navHelpBtn));
        setTooltip(quickGuideBtn, "Open Quick User Guide and basic workflow steps");

        Button quickSettingsBtn = new Button("⚙️ Settings");
        quickSettingsBtn.getStyleClass().add("btn-ghost");
        quickSettingsBtn.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));
        setTooltip(quickSettingsBtn, "Configure simulation parameters and safety confirmations");

        quickActionButtons.getChildren().addAll(goToDispatchBtn, quickTimelineBtn, quickSaveBtn, quickLoadBtn, quickGuideBtn, quickSettingsBtn);

        // Add Dashboard Outcome Snapshot to Quick Action Deck
        VBox dashOutcome = createDashboardOutcomeCard();

        quickActionCard.getChildren().addAll(quickDesc, dashOutcome, quickActionButtons);

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

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, javafx.scene.layout.Priority.ALWAYS);
        Button guideLinkBtn = new Button("📖 Quick User Guide ➔");
        guideLinkBtn.getStyleClass().add("btn-ghost");
        guideLinkBtn.setStyle("-fx-font-size: 11px; -fx-padding: 3 9;");
        guideLinkBtn.setOnAction(e -> showView(helpScrollPane, navHelpBtn));
        setTooltip(guideLinkBtn, "View comprehensive step-by-step instructions");

        header.getChildren().addAll(icon, title, subtitle, headerSpacer, guideLinkBtn);

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
        setTooltip(card, "Click to navigate to " + title + " workspace");
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

        setTooltip(card1, "Total number of communication devices registered in the simulation network");
        setTooltip(card2, "Operational nodes currently online and able to relay messages vs total devices");
        setTooltip(card3, "Total active bidirectional wireless connections forming the mesh");
        setTooltip(card4, "Total messages dispatched and percentage successfully delivered");

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

        // Topology Management & Persistence Action Bar
        HBox networkActionsBar = new HBox(10);
        networkActionsBar.setAlignment(Pos.CENTER_LEFT);
        networkActionsBar.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-border-color: #192742; -fx-border-radius: 8px;");

        Label netBarLabel = new Label("Topology Actions:");
        netBarLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");

        Button netSaveBtn = new Button("💾 Save Network (JSON)");
        netSaveBtn.getStyleClass().add("btn-blue");
        netSaveBtn.setOnAction(e -> handleSaveNetwork());
        setTooltip(netSaveBtn, "Export current network topology to a JSON configuration file");

        Button netLoadBtn = new Button("📂 Load Network (JSON)");
        netLoadBtn.getStyleClass().add("btn-cyan");
        netLoadBtn.setOnAction(e -> handleLoadNetwork());
        setTooltip(netLoadBtn, "Import and restore a saved network topology from a JSON file");

        Button netResetBtn = new Button("↺ Reset Sample Mesh");
        netResetBtn.getStyleClass().add("btn-ghost");
        netResetBtn.setOnAction(e -> loadSampleNetwork());
        setTooltip(netResetBtn, "Reset network to standard 5-node campus disaster mesh");

        Button netClearBtn = new Button("✕ Clear Graph");
        netClearBtn.setStyle("-fx-background-color: #271419; -fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 12;");
        netClearBtn.setOnAction(e -> handleClearNetworkWithConfirmation());
        setTooltip(netClearBtn, "Remove all devices and links to start building a custom network from scratch");

        Region barSpacer = new Region();
        HBox.setHgrow(barSpacer, javafx.scene.layout.Priority.ALWAYS);

        Label quickTip = new Label("💡 Drag nodes to rearrange • Save exports complete node telemetry and mesh links");
        quickTip.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");

        Button netHomeBtn = new Button("🏠 Dashboard");
        netHomeBtn.getStyleClass().add("btn-ghost");
        netHomeBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(netHomeBtn, "Return to main Emergency Command Dashboard");

        networkActionsBar.getChildren().addAll(netBarLabel, netSaveBtn, netLoadBtn, netResetBtn, netClearBtn, barSpacer, quickTip, netHomeBtn);
        view.getChildren().add(networkActionsBar);

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
        HBox controlStrip = new HBox(10);
        controlStrip.setAlignment(Pos.CENTER_LEFT);
        controlStrip.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 10px 14px; -fx-border-color: #192742; -fx-border-radius: 8px;");

        Label selectLbl = new Label("Selected Node:");
        selectLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-font-weight: 700;");

        selectedDeviceComboBox = createDeviceComboBox();
        selectedDeviceComboBox.setPrefWidth(220);
        selectedDeviceComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            updateToggleState(newV);
            if (newV != null && topologyPane != null) {
                topologyPane.selectDevice(newV);
            }
        });
        setTooltip(selectedDeviceComboBox, "Select a device to view status, toggle power, recharge, rename, or delete");

        dynamicToggleBtn = new Button("Toggle Online / Offline");
        dynamicToggleBtn.getStyleClass().add("btn-rose");
        dynamicToggleBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10;");
        dynamicToggleBtn.setOnAction(e -> handleToggleStatus());
        setTooltip(dynamicToggleBtn, "Toggle the operational state (Online/Offline) to test node failure handling");

        rechargeBtn = new Button("⚡ Recharge to 100%");
        rechargeBtn.getStyleClass().add("btn-emerald");
        rechargeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10;");
        rechargeBtn.setOnAction(e -> handleRecharge());
        setTooltip(rechargeBtn, "Recharge this node's battery back to 100% (ACTIVE)");

        Button renameBtn = new Button("✏️ Rename");
        renameBtn.getStyleClass().add("btn-ghost");
        renameBtn.setStyle("-fx-font-size: 11px; -fx-padding: 6 10;");
        renameBtn.setOnAction(e -> handleRenameSelectedDevice());
        setTooltip(renameBtn, "Rename the selected device node");

        Button deleteBtn = new Button("🗑️ Delete Node");
        deleteBtn.setStyle("-fx-background-color: #3b1116; -fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-background-radius: 6px; -fx-cursor: hand; -fx-padding: 6 10; -fx-border-color: #dc2626; -fx-border-radius: 6px;");
        deleteBtn.setOnAction(e -> handleDeleteSelectedDevice());
        setTooltip(deleteBtn, "Remove the selected device and sever all its connecting mesh links");

        selectedDeviceStatusLabel = new Label("");
        selectedDeviceStatusLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");

        controlStrip.getChildren().addAll(selectLbl, selectedDeviceComboBox, dynamicToggleBtn, rechargeBtn, renameBtn, deleteBtn, selectedDeviceStatusLabel);
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
        setTooltip(addDeviceNameField, "Enter a unique name or label for the device");

        addDeviceTypeSelect = new ComboBox<>();
        addDeviceTypeSelect.getItems().addAll("Student Phone", "Security Station", "Medical Station");
        addDeviceTypeSelect.setValue("Student Phone");
        setTooltip(addDeviceTypeSelect, "Select device classification: Student Phone, Security Station, or Medical Center");

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
        setTooltip(addBtn, "Register and provision this device into the network graph");

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
        setTooltip(linkDeviceAComboBox, "Select the first node for the bidirectional wireless link");
        linkDeviceBComboBox = createDeviceComboBox();
        setTooltip(linkDeviceBComboBox, "Select the second node for the bidirectional wireless link");

        Label linkHelpLabel = new Label("🔗 Links allow emergency alerts to hop across nodes. Alerts route across the shortest path of connected links.");
        linkHelpLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-line-spacing: 1px;");
        linkHelpLabel.setWrapText(true);

        Button linkBtn = new Button("🔗 Connect Wireless Link");
        linkBtn.getStyleClass().add("btn-blue");
        linkBtn.setOnAction(e -> handleConnectDevices());
        setTooltip(linkBtn, "Establish a wireless mesh communication channel between the two nodes");

        Button disconnectBtn = new Button("✂️ Disconnect Link");
        disconnectBtn.getStyleClass().add("btn-reset");
        disconnectBtn.setOnAction(e -> handleDisconnectDevices());
        setTooltip(disconnectBtn, "Sever the bidirectional wireless mesh link between the two selected nodes");

        HBox linkActionsBox = new HBox(8, linkBtn, disconnectBtn);
        linkActionsBox.setAlignment(Pos.CENTER_LEFT);

        connectFeedbackLabel = new Label("");
        connectFeedbackLabel.getStyleClass().add("feedback-msg");

        linkGrid.add(createFormLabel("Source Node:"), 0, 0);
        linkGrid.add(linkDeviceAComboBox, 1, 0);
        linkGrid.add(createFormLabel("Connect With:"), 0, 1);
        linkGrid.add(linkDeviceBComboBox, 1, 1);
        linkGrid.add(linkHelpLabel, 1, 2);
        linkGrid.add(linkActionsBox, 1, 3);
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
                "Select route endpoints, message payload, and priority to trigger shortest-path BFS mesh propagation"));

        // Top Action Bar with Return to Dashboard and View Topology
        HBox dispatchActionBar = new HBox(10);
        dispatchActionBar.setAlignment(Pos.CENTER_LEFT);
        dispatchActionBar.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 8px 14px; -fx-border-color: #192742; -fx-border-radius: 8px;");

        Button dispatchHomeBtn = new Button("🏠 Return to Dashboard");
        dispatchHomeBtn.getStyleClass().add("btn-ghost");
        dispatchHomeBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(dispatchHomeBtn, "Return to main Emergency Command Dashboard");

        Button dispatchToNetBtn = new Button("🗺️ View Network Topology");
        dispatchToNetBtn.getStyleClass().add("btn-cyan");
        dispatchToNetBtn.setOnAction(e -> {
            showView(networkScrollPane, navNetworkBtn);
            if (networkTabPane != null) {
                networkTabPane.getSelectionModel().select(topologyTab);
            }
        });
        setTooltip(dispatchToNetBtn, "Inspect the live node topology and mesh link layout");

        Region dispSpacer = new Region();
        HBox.setHgrow(dispSpacer, javafx.scene.layout.Priority.ALWAYS);

        Label dispTip = new Label("💡 BFS routes messages through shortest available path of operational nodes");
        dispTip.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-style: italic;");

        dispatchActionBar.getChildren().addAll(dispatchHomeBtn, dispatchToNetBtn, dispSpacer, dispTip);
        view.getChildren().add(dispatchActionBar);

        // Contextual Guidance Banner
        dispatchGuidanceBanner = new HBox(8);
        dispatchGuidanceBanner.setAlignment(Pos.CENTER_LEFT);
        dispatchGuidanceBanner.getStyleClass().add("guide-banner");
        dispatchGuidanceLabel = new Label("💡 Select an Origin Sender and Target Recipient, enter an emergency message, and click 'Send Emergency'.");
        dispatchGuidanceLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 12px;");
        dispatchGuidanceBanner.getChildren().add(dispatchGuidanceLabel);
        view.getChildren().add(dispatchGuidanceBanner);

        // Dispatch Console Card
        VBox dispatchCard = createCard("Emergency Dispatch Console (Step 3)",
                "Select origin sender, destination target, priority level, and distress alert message");

        GridPane dispatchGrid = new GridPane();
        dispatchGrid.setHgap(14);
        dispatchGrid.setVgap(12);

        senderComboBox = createDeviceComboBox();
        senderComboBox.setPromptText("-- Select Origin Sender --");
        senderComboBox.valueProperty().addListener((obs, oldV, newV) -> {
            if (settingsManager.getSettings().isAutoSelectRecipient() && newV != null) {
                if (recipientComboBox.getValue() == null || recipientComboBox.getValue().equals(newV)) {
                    for (CommunicationDevice dev : graph.getAllDevices()) {
                        if (!dev.equals(newV) && dev.isAvailable()) {
                            recipientComboBox.setValue(dev);
                            break;
                        }
                    }
                }
            }
            updateDispatchGuidance();
        });

        recipientComboBox = createDeviceComboBox();
        recipientComboBox.setPromptText("-- Select Target Recipient --");
        setTooltip(recipientComboBox, "Select target destination node to receive the message");
        recipientComboBox.valueProperty().addListener((obs, oldV, newV) -> updateDispatchGuidance());

        priorityComboBox = new ComboBox<>();
        priorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        priorityComboBox.setValue(settingsManager.getSettings().getDefaultPriority());
        priorityComboBox.setPrefWidth(280);
        setTooltip(priorityComboBox, "Message priority level (Low, Normal, High, or Critical)");

        priorityDescLabel = new Label("🔵 Normal Priority: Standard emergency message relay.");
        priorityDescLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        priorityDescLabel.setWrapText(true);
        priorityDescLabel.setMaxWidth(280);

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
        messageTextField.setPromptText("Enter emergency alert or status details (e.g., Medical assistance needed)...");
        messageTextField.setPrefWidth(340);
        setTooltip(messageTextField, "Details of the emergency situation or distress report (Required)");

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
        Label quickLbl = new Label("1-Click Presets:");
        quickLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px; -fx-font-weight: 700;");

        Button presetMed = new Button("🏥 Medical Aid");
        presetMed.getStyleClass().add("btn-preset");
        presetMed.setOnAction(e -> {
            messageTextField.setText("URGENT: Medical assistance requested at Quad Block 4");
            priorityComboBox.setValue(Priority.HIGH);
            clearDispatchValidationError();
        });
        setTooltip(presetMed, "Fill with high-priority medical aid distress alert");

        Button presetSec = new Button("⚠️ Hazard Alert");
        presetSec.getStyleClass().add("btn-preset");
        presetSec.setOnAction(e -> {
            messageTextField.setText("HAZARD: Structural blockage reported near Gate 2");
            priorityComboBox.setValue(Priority.NORMAL);
            clearDispatchValidationError();
        });
        setTooltip(presetSec, "Fill with normal-priority hazard alert");

        Button presetCrit = new Button("🚨 Evacuation SOS");
        presetCrit.getStyleClass().add("btn-preset");
        presetCrit.setOnAction(e -> {
            messageTextField.setText("CRITICAL: Severe hazard - immediate evacuation ordered");
            priorityComboBox.setValue(Priority.CRITICAL);
            clearDispatchValidationError();
        });
        setTooltip(presetCrit, "Fill with critical life-safety evacuation alert");

        Button presetPing = new Button("ℹ️ Node Heartbeat");
        presetPing.getStyleClass().add("btn-preset");
        presetPing.setOnAction(e -> {
            messageTextField.setText("STATUS: Node heartbeat and path liveness ping");
            priorityComboBox.setValue(Priority.LOW);
            clearDispatchValidationError();
        });
        setTooltip(presetPing, "Fill with low-priority heartbeat ping");

        presetRow.getChildren().addAll(quickLbl, presetMed, presetSec, presetCrit, presetPing);

        // Inline Validation Error Label
        dispatchInlineErrorLabel = new Label("");
        dispatchInlineErrorLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 700; -fx-background-color: #2b0d0d; -fx-padding: 6 12; -fx-background-radius: 6px; -fx-border-color: #dc2626; -fx-border-radius: 6px;");
        dispatchInlineErrorLabel.setWrapText(true);
        dispatchInlineErrorLabel.setVisible(false);
        dispatchInlineErrorLabel.setManaged(false);

        // Action Buttons Row: Prominent Send Emergency + Clear/Reset
        HBox actionBtnRow = new HBox(12);
        actionBtnRow.setAlignment(Pos.CENTER_LEFT);

        sendEmergencyBtn = new Button("🚨 Send Emergency");
        sendEmergencyBtn.getStyleClass().add("btn-dispatch");
        sendEmergencyBtn.setPrefHeight(38);
        sendEmergencyBtn.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-padding: 9 24;");
        HBox.setHgrow(sendEmergencyBtn, javafx.scene.layout.Priority.ALWAYS);
        sendEmergencyBtn.setMaxWidth(Double.MAX_VALUE);
        sendEmergencyBtn.setOnAction(e -> handleSendMessage());
        setTooltip(sendEmergencyBtn, "Execute Breadcrumb BFS shortest-path routing algorithm to transmit alert");

        clearDispatchBtn = new Button("🔄 Reset / Clear");
        clearDispatchBtn.getStyleClass().add("btn-reset");
        clearDispatchBtn.setPrefHeight(38);
        clearDispatchBtn.setOnAction(e -> handleResetDispatch());
        setTooltip(clearDispatchBtn, "Clear all fields, reset dropdowns, and clear route highlights to start a new dispatch");

        actionBtnRow.getChildren().addAll(sendEmergencyBtn, clearDispatchBtn);

        dispatchCard.getChildren().addAll(dispatchGrid, presetRow, dispatchInlineErrorLabel, actionBtnRow);

        // Outcome Card placed directly below Dispatch Card!
        outcomeCard = createOutcomeCard();

        view.getChildren().addAll(dispatchCard, outcomeCard);

        return view;
    }

    private VBox createOutcomeCard() {
        VBox card = createCard("Simulation Delivery Outcome", "BFS shortest-path discovery & multi-hop transmission analysis");
        card.getStyleClass().clear();
        card.getStyleClass().add("dash-card-highlight");

        HBox outcomeStatusRow = new HBox(10);
        outcomeStatusRow.setAlignment(Pos.CENTER_LEFT);

        Label outcomeTitle = new Label("DELIVERY STATUS:");
        outcomeTitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 11px;");

        resultBadge = new Label("WAITING FOR DISPATCH");
        resultBadge.getStyleClass().add("badge-waiting");
        resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #334155; -fx-border-radius: 4px;");
        setTooltip(resultBadge, "Indicates message delivery result: Delivered, Failed, or No Route Found");

        Region outcomeSpacer = new Region();
        HBox.setHgrow(outcomeSpacer, javafx.scene.layout.Priority.ALWAYS);

        replayFromOutcomeBtn = new Button("🎬 Watch Replay on Canvas");
        replayFromOutcomeBtn.setStyle("-fx-background-color: #0e223d; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold; -fx-border-color: #0284c7; -fx-border-radius: 5px; -fx-padding: 5 12; -fx-cursor: hand;");
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
        setTooltip(replayFromOutcomeBtn, "Watch animated visual replay of message propagation on the network canvas");

        viewTimelineFromOutcomeBtn = new Button("⏱️ View Timeline");
        viewTimelineFromOutcomeBtn.getStyleClass().add("btn-ghost");
        viewTimelineFromOutcomeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        viewTimelineFromOutcomeBtn.setOnAction(e -> showView(activityScrollPane, navActivityBtn));
        setTooltip(viewTimelineFromOutcomeBtn, "View chronological audit trail of simulation events in Activity History");

        outcomeStatusRow.getChildren().addAll(outcomeTitle, resultBadge, outcomeSpacer, replayFromOutcomeBtn, viewTimelineFromOutcomeBtn);

        // Metric Tiles Row
        HBox metricTilesRow = new HBox(10);
        metricTilesRow.setAlignment(Pos.CENTER_LEFT);

        outcomeStatusMetricLabel = new Label("Idle");
        VBox statusTile = createKpiTile("RESULT", outcomeStatusMetricLabel);

        outcomeHopsMetricLabel = new Label("--");
        VBox hopsTile = createKpiTile("HOPS", outcomeHopsMetricLabel);

        outcomeNodesMetricLabel = new Label("--");
        VBox nodesTile = createKpiTile("DEVICES TRAVERSED", outcomeNodesMetricLabel);

        outcomePriorityMetricLabel = new Label("--");
        VBox priorityTile = createKpiTile("PRIORITY", outcomePriorityMetricLabel);

        metricTilesRow.getChildren().addAll(statusTile, hopsTile, nodesTile, priorityTile);

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

        outcomeReadableRouteLabel = new Label("Route: (Awaiting dispatch)");
        outcomeReadableRouteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");
        outcomeReadableRouteLabel.setWrapText(true);

        routeSection.getChildren().addAll(routeHeader, routeScrollPane, outcomeReadableRouteLabel);

        resultExplanationLabel = new Label("Select sender, recipient, and message, then click 'Send Emergency' to simulate mesh propagation.");
        resultExplanationLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 2px;");
        resultExplanationLabel.setWrapText(true);

        // Collapsible Technical Diagnostics
        toggleTechDetailsBtn = new Button("🔍 [ Show Technical Route Details ]");
        toggleTechDetailsBtn.getStyleClass().add("btn-ghost");
        toggleTechDetailsBtn.setStyle("-fx-font-size: 10px; -fx-padding: 3 8;");
        setTooltip(toggleTechDetailsBtn, "Toggle technical diagnostics including hop counts, IDs, and battery impact");

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

        card.getChildren().addAll(outcomeStatusRow, metricTilesRow, routeSection, resultExplanationLabel, toggleTechDetailsBtn, technicalDetailsBox);
        return card;
    }

    private VBox createKpiTile(String title, Label valueLabel) {
        VBox box = new VBox(2);
        box.getStyleClass().add("kpi-tile");
        box.setPrefWidth(140);
        HBox.setHgrow(box, javafx.scene.layout.Priority.ALWAYS);

        Label titleLbl = new Label(title);
        titleLbl.getStyleClass().add("kpi-tile-title");

        valueLabel.getStyleClass().add("kpi-tile-value");

        box.getChildren().addAll(titleLbl, valueLabel);
        return box;
    }

    private VBox createDashboardOutcomeCard() {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: #0b1324; -fx-background-radius: 8px; -fx-padding: 12px 14px; -fx-border-color: #1a2942; -fx-border-radius: 8px;");

        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("LATEST DISPATCH RESULT:");
        title.setStyle("-fx-text-fill: #94a3b8; -fx-font-weight: 800; -fx-font-size: 11px;");

        dashboardOutcomeBadge = new Label("WAITING FOR DISPATCH");
        dashboardOutcomeBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");

        statusRow.getChildren().addAll(title, dashboardOutcomeBadge);

        dashboardOutcomeExplanation = new Label("No simulation executed yet. Use the Emergency Dispatch console to test mesh transmission.");
        dashboardOutcomeExplanation.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-line-spacing: 1px;");
        dashboardOutcomeExplanation.setWrapText(true);

        card.getChildren().addAll(statusRow, dashboardOutcomeExplanation);
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
    // 5. View 4: 📋 Emergency History & Reports Workspace
    // --------------------------------------------------------------------------
    private VBox createHistoryView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // 1. Header
        view.getChildren().add(createSectionHeader("Emergency Dispatch History & Reports",
                "Complete audit trail of simulated emergency transmissions with route tracking, performance KPIs, and multi-attribute filters"));

        // 2. Summary KPI Metric Cards (Total, Delivered, No Route, Failed)
        HBox kpiContainer = new HBox(12);
        kpiContainer.setAlignment(Pos.CENTER_LEFT);

        histTotalKpiLabel = new Label("0");
        VBox totalCard = createHistoryKpiCard("TOTAL DISPATCHES", histTotalKpiLabel, "Recorded this session", "#38bdf8");

        histDeliveredKpiLabel = new Label("0");
        histRateKpiLabel = new Label("0% Delivery Rate");
        VBox deliveredCard = createHistoryKpiCard("SUCCESSFUL DELIVERIES", histDeliveredKpiLabel, histRateKpiLabel, "#34d399");

        histNoRouteKpiLabel = new Label("0");
        VBox noRouteCard = createHistoryKpiCard("NO ROUTE FOUND", histNoRouteKpiLabel, "Disconnected mesh paths", "#f59e0b");

        histFailedKpiLabel = new Label("0");
        VBox failedCard = createHistoryKpiCard("DELIVERY FAILURES", histFailedKpiLabel, "Offline / depleted nodes", "#ef4444");

        HBox.setHgrow(totalCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(deliveredCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(noRouteCard, javafx.scene.layout.Priority.ALWAYS);
        HBox.setHgrow(failedCard, javafx.scene.layout.Priority.ALWAYS);

        kpiContainer.getChildren().addAll(totalCard, deliveredCard, noRouteCard, failedCard);
        view.getChildren().add(kpiContainer);

        // 3. Search and Filters Toolbar
        HBox filterBar = new HBox(10);
        filterBar.setAlignment(Pos.CENTER_LEFT);
        filterBar.getStyleClass().add("filter-bar");

        Label searchIcon = new Label("🔍");
        historySearchField = new TextField();
        historySearchField.setPromptText("Search message, ID, sender, recipient...");
        historySearchField.setPrefWidth(240);
        historySearchField.getStyleClass().add("text-field");

        Label filterLbl = new Label("Status:");
        filterLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");

        historyStatusFilter = new ComboBox<>();
        historyStatusFilter.getItems().addAll("All Statuses", "Delivered Only", "No Route Found", "Failed Only");
        historyStatusFilter.setValue("All Statuses");
        historyStatusFilter.setStyle("-fx-font-size: 11px;");

        Label devFilterLbl = new Label("Device:");
        devFilterLbl.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 700;");

        historyDeviceFilter = new ComboBox<>();
        historyDeviceFilter.getItems().add("All Devices");
        historyDeviceFilter.setValue("All Devices");
        historyDeviceFilter.setStyle("-fx-font-size: 11px;");
        historyDeviceFilter.setPrefWidth(150);

        Button resetFiltersBtn = new Button("🔄 Reset");
        resetFiltersBtn.getStyleClass().add("btn-ghost");
        resetFiltersBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 10;");
        resetFiltersBtn.setOnAction(e -> {
            historySearchField.clear();
            historyStatusFilter.setValue("All Statuses");
            historyDeviceFilter.setValue("All Devices");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button exportBtn = new Button("📄 Export Report");
        exportBtn.getStyleClass().add("btn-cyan");
        exportBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        exportBtn.setOnAction(e -> handleExportReport());

        Button clearHistoryBtn = new Button("🗑️ Clear History");
        clearHistoryBtn.getStyleClass().add("btn-reset");
        clearHistoryBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        clearHistoryBtn.setOnAction(e -> handleClearHistory());

        Button histHomeBtn = new Button("🏠 Dashboard");
        histHomeBtn.getStyleClass().add("btn-ghost");
        histHomeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        histHomeBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(histHomeBtn, "Return to main Emergency Command Dashboard");

        filterBar.getChildren().addAll(searchIcon, historySearchField, filterLbl, historyStatusFilter,
                devFilterLbl, historyDeviceFilter, resetFiltersBtn, spacer, exportBtn, clearHistoryBtn, histHomeBtn);
        view.getChildren().add(filterBar);

        // 4. Main Body: TableView on Left (65%) and Detail Inspector Card on Right (35%)
        HBox mainBody = new HBox(14);
        VBox.setVgrow(mainBody, javafx.scene.layout.Priority.ALWAYS);

        // A. Table Container
        VBox tableContainer = new VBox(8);
        HBox.setHgrow(tableContainer, javafx.scene.layout.Priority.ALWAYS);

        historyTableView = new TableView<>();
        historyTableView.getStyleClass().add("device-table");
        VBox.setVgrow(historyTableView, javafx.scene.layout.Priority.ALWAYS);
        historyTableView.setPrefHeight(450);

        // Setup FilteredList & SortedList
        filteredHistoryList = new FilteredList<>(historyManager.getRecords(), p -> true);
        sortedHistoryList = new SortedList<>(filteredHistoryList);
        sortedHistoryList.comparatorProperty().bind(historyTableView.comparatorProperty());
        historyTableView.setItems(sortedHistoryList);

        // Filter event listeners
        historySearchField.textProperty().addListener((obs, oldV, newV) -> applyHistoryFilters());
        historyStatusFilter.valueProperty().addListener((obs, oldV, newV) -> applyHistoryFilters());
        historyDeviceFilter.valueProperty().addListener((obs, oldV, newV) -> applyHistoryFilters());

        // Setup Columns
        TableColumn<SimulationRecord, String> timeCol = new TableColumn<>("Time");
        timeCol.setPrefWidth(75);
        timeCol.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
        timeCol.setStyle("-fx-alignment: center; -fx-font-family: monospace; -fx-text-fill: #94a3b8;");

        TableColumn<SimulationRecord, String> idCol = new TableColumn<>("ID");
        idCol.setPrefWidth(85);
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setStyle("-fx-font-family: monospace; -fx-font-weight: 700; -fx-text-fill: #38bdf8;");

        TableColumn<SimulationRecord, String> priorityCol = new TableColumn<>("Priority");
        priorityCol.setPrefWidth(85);
        priorityCol.setCellValueFactory(new PropertyValueFactory<>("priorityName"));
        priorityCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("CRITICAL".equalsIgnoreCase(item)) {
                        badge.getStyleClass().setAll("badge-priority-critical");
                    } else if ("HIGH".equalsIgnoreCase(item)) {
                        badge.getStyleClass().setAll("badge-priority-high");
                    } else {
                        badge.getStyleClass().setAll("badge-priority-normal");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<SimulationRecord, String> senderCol = new TableColumn<>("Sender");
        senderCol.setPrefWidth(125);
        senderCol.setCellValueFactory(new PropertyValueFactory<>("senderName"));
        senderCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SimulationRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                    String icon = "📱";
                    if (r != null && r.getMessage() != null && r.getMessage().getSender() != null) {
                        CommunicationDevice d = r.getMessage().getSender();
                        icon = (d instanceof SecurityStation) ? "🛡️" : (d instanceof MedicalStation) ? "🏥" : "📱";
                    }
                    setText(icon + " " + item);
                    setStyle("-fx-text-fill: #f1f5f9; -fx-font-weight: 600;");
                }
            }
        });

        TableColumn<SimulationRecord, String> recipientCol = new TableColumn<>("Recipient");
        recipientCol.setPrefWidth(125);
        recipientCol.setCellValueFactory(new PropertyValueFactory<>("recipientName"));
        recipientCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    SimulationRecord r = (getTableRow() != null) ? getTableRow().getItem() : null;
                    String icon = "🏥";
                    if (r != null && r.getMessage() != null && r.getMessage().getRecipient() != null) {
                        CommunicationDevice d = r.getMessage().getRecipient();
                        icon = (d instanceof SecurityStation) ? "🛡️" : (d instanceof MedicalStation) ? "🏥" : "📱";
                    }
                    setText(icon + " " + item);
                    setStyle("-fx-text-fill: #f1f5f9; -fx-font-weight: 600;");
                }
            }
        });

        TableColumn<SimulationRecord, String> statusCol = new TableColumn<>("Status");
        statusCol.setPrefWidth(120);
        statusCol.setCellValueFactory(new PropertyValueFactory<>("statusDisplay"));
        statusCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    Label badge = new Label(item);
                    if ("Delivered".equalsIgnoreCase(item)) {
                        badge.setText("✔ Delivered");
                        badge.getStyleClass().setAll("badge-delivered");
                    } else if ("No Route Found".equalsIgnoreCase(item)) {
                        badge.setText("⚠️ No Route");
                        badge.getStyleClass().setAll("badge-no-route");
                    } else {
                        badge.setText("✖ Failed");
                        badge.getStyleClass().setAll("badge-failed");
                    }
                    setGraphic(badge);
                    setAlignment(Pos.CENTER);
                }
            }
        });

        TableColumn<SimulationRecord, String> hopsCol = new TableColumn<>("Hops");
        hopsCol.setPrefWidth(65);
        hopsCol.setCellValueFactory(cell -> {
            SimulationRecord r = cell.getValue();
            return new SimpleStringProperty(r.isDelivered() ? String.valueOf(r.getHopCount()) : "—");
        });
        hopsCol.setStyle("-fx-alignment: center; -fx-text-fill: #22d3ee; -fx-font-weight: bold;");

        TableColumn<SimulationRecord, String> msgCol = new TableColumn<>("Message Content");
        msgCol.setPrefWidth(160);
        msgCol.setCellValueFactory(new PropertyValueFactory<>("messageContent"));
        msgCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                    setStyle("-fx-text-fill: #94a3b8;");
                }
            }
        });

        TableColumn<SimulationRecord, String> routeCol = new TableColumn<>("Route Path");
        routeCol.setPrefWidth(180);
        routeCol.setCellValueFactory(new PropertyValueFactory<>("routePath"));
        routeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                    setStyle("");
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                    if (item.contains("None")) {
                        setStyle("-fx-text-fill: #fca5a5; -fx-font-style: italic;");
                    } else {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px;");
                    }
                }
            }
        });

        historyTableView.getColumns().add(timeCol);
        historyTableView.getColumns().add(idCol);
        historyTableView.getColumns().add(priorityCol);
        historyTableView.getColumns().add(senderCol);
        historyTableView.getColumns().add(recipientCol);
        historyTableView.getColumns().add(statusCol);
        historyTableView.getColumns().add(hopsCol);
        historyTableView.getColumns().add(msgCol);
        historyTableView.getColumns().add(routeCol);

        // Empty state placeholder
        historyTableView.setPlaceholder(createHistoryEmptyState());

        // Table selection listener
        historyTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) {
                showHistoryRecordDetails(newV);
            } else {
                resetHistoryDetailPanel();
            }
        });

        tableContainer.getChildren().add(historyTableView);

        // B. Detail Inspector Card on Right
        historyDetailCard = createHistoryDetailInspector();
        historyDetailCard.setPrefWidth(380);
        historyDetailCard.setMinWidth(340);

        mainBody.getChildren().addAll(tableContainer, historyDetailCard);
        view.getChildren().add(mainBody);

        // Initialize KPIs
        updateHistorySummaryKpis();

        return view;
    }

    private VBox createHistoryKpiCard(String title, Label numberLabel, Object subtitle, String accentColor) {
        VBox card = new VBox(4);
        card.getStyleClass().add("kpi-card");

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("kpi-title");

        numberLabel.getStyleClass().add("kpi-number");
        numberLabel.setStyle("-fx-text-fill: " + accentColor + ";");

        javafx.scene.Node subNode;
        if (subtitle instanceof Label lbl) {
            lbl.getStyleClass().add("kpi-subtext");
            subNode = lbl;
        } else {
            Label lbl = new Label(String.valueOf(subtitle));
            lbl.getStyleClass().add("kpi-subtext");
            subNode = lbl;
        }

        card.getChildren().addAll(titleLabel, numberLabel, subNode);
        return card;
    }

    private VBox createHistoryEmptyState() {
        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(30));
        box.getStyleClass().add("history-empty-state");

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 38px;");

        Label title = new Label("No Emergency Dispatches Recorded Yet");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: 800; -fx-text-fill: #f1f5f9;");

        Label subtitle = new Label("Dispatch an alert from the Emergency Dispatch console to simulate\npacket routing and record real-time telemetry here.");
        subtitle.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 12px; -fx-text-alignment: center;");

        Button goDispatchBtn = new Button("🚨 Go to Emergency Dispatch");
        goDispatchBtn.getStyleClass().add("btn-danger");
        goDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));

        box.getChildren().addAll(icon, title, subtitle, goDispatchBtn);
        return box;
    }

    private VBox createHistoryDetailInspector() {
        VBox card = new VBox(12);
        card.getStyleClass().add("history-detail-card");

        // Title Header
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        Label headerIcon = new Label("📋");
        headerIcon.setStyle("-fx-font-size: 16px;");
        Label headerTitle = new Label("Dispatch Details");
        headerTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 800; -fx-text-fill: #f8fafc;");
        Region sp = new Region();
        HBox.setHgrow(sp, javafx.scene.layout.Priority.ALWAYS);
        historyDetailTimeBadge = new Label("");
        historyDetailTimeBadge.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        header.getChildren().addAll(headerIcon, headerTitle, sp, historyDetailTimeBadge);
        card.getChildren().add(header);

        // ID & Status row
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        historyDetailIdLabel = new Label("No Record Selected");
        historyDetailIdLabel.setStyle("-fx-font-family: monospace; -fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");
        historyDetailStatusBadge = new Label("STANDBY");
        historyDetailStatusBadge.getStyleClass().setAll("badge-waiting");
        historyDetailPriorityBadge = new Label("NORMAL");
        historyDetailPriorityBadge.getStyleClass().setAll("badge-priority-normal");
        badgeRow.getChildren().addAll(historyDetailIdLabel, historyDetailStatusBadge, historyDetailPriorityBadge);
        card.getChildren().add(badgeRow);

        // Endpoints Grid: Sender -> Recipient
        GridPane endpointsGrid = new GridPane();
        endpointsGrid.setHgap(10);
        endpointsGrid.setVgap(6);
        endpointsGrid.setStyle("-fx-background-color: #070c17; -fx-background-radius: 8px; -fx-padding: 10px; -fx-border-color: #16243f; -fx-border-radius: 8px;");

        Label fromTag = new Label("ORIGIN / SENDER:");
        fromTag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        historyDetailSenderLabel = new Label("—");
        historyDetailSenderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #f1f5f9;");

        Label toTag = new Label("DESTINATION / RECIPIENT:");
        toTag.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        historyDetailRecipientLabel = new Label("—");
        historyDetailRecipientLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #f1f5f9;");

        endpointsGrid.add(fromTag, 0, 0);
        endpointsGrid.add(historyDetailSenderLabel, 0, 1);
        endpointsGrid.add(toTag, 1, 0);
        endpointsGrid.add(historyDetailRecipientLabel, 1, 1);
        card.getChildren().add(endpointsGrid);

        // Emergency Message Payload Box
        VBox msgBox = new VBox(4);
        Label msgTitle = new Label("MESSAGE PAYLOAD:");
        msgTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        historyDetailMessageContent = new Label("Select a record from the table to view its transmitted content.");
        historyDetailMessageContent.setWrapText(true);
        historyDetailMessageContent.setStyle("-fx-background-color: #070c17; -fx-text-fill: #f8fafc; -fx-font-size: 12px; -fx-font-weight: 600; -fx-padding: 10px; -fx-background-radius: 6px; -fx-border-color: #0284c7; -fx-border-width: 0 0 0 3px;");
        msgBox.getChildren().addAll(msgTitle, historyDetailMessageContent);
        card.getChildren().add(msgBox);

        // Route Sequence Path
        VBox routeBox = new VBox(6);
        Label routeTitle = new Label("DEVICE ROUTE & HOPS:");
        routeTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        historyDetailRouteHBox = new HBox(6);
        historyDetailRouteHBox.setAlignment(Pos.CENTER_LEFT);
        Label emptyRouteLbl = new Label("No route available");
        emptyRouteLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        historyDetailRouteHBox.getChildren().add(emptyRouteLbl);
        routeBox.getChildren().addAll(routeTitle, historyDetailRouteHBox);
        card.getChildren().add(routeBox);

        // Diagnostic Explanation
        VBox diagBox = new VBox(4);
        Label diagTitle = new Label("SIMULATION DIAGNOSTIC:");
        diagTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #64748b;");
        historyDetailExplanationLabel = new Label("Awaiting record selection.");
        historyDetailExplanationLabel.setWrapText(true);
        historyDetailExplanationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");
        diagBox.getChildren().addAll(diagTitle, historyDetailExplanationLabel);
        card.getChildren().add(diagBox);

        // Action Buttons: Replay on Canvas & Inspect Timeline
        HBox actionsRow = new HBox(8);
        historyReplayBtn = new Button("🎬 Canvas Replay");
        historyReplayBtn.getStyleClass().add("btn-cyan");
        historyReplayBtn.setDisable(true);
        historyReplayBtn.setOnAction(e -> {
            SimulationRecord sel = historyTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showView(networkScrollPane, navNetworkBtn);
                if (networkTabPane != null) {
                    networkTabPane.getSelectionModel().select(topologyTab);
                }
                if (topologyPane != null) {
                    topologyPane.loadReplay(sel);
                    topologyPane.playReplay();
                }
            }
        });

        historyTimelineBtn = new Button("⏱️ View Timeline");
        historyTimelineBtn.getStyleClass().add("btn-ghost");
        historyTimelineBtn.setDisable(true);
        historyTimelineBtn.setOnAction(e -> {
            SimulationRecord sel = historyTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                showView(activityScrollPane, navActivityBtn);
                if (historyComboBox != null) {
                    historyComboBox.setValue(sel);
                }
            }
        });

        Button copyBtn = new Button("📋 Copy");
        copyBtn.getStyleClass().add("btn-ghost");
        copyBtn.setOnAction(e -> {
            SimulationRecord sel = historyTableView.getSelectionModel().getSelectedItem();
            if (sel != null) {
                javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(String.format("[%s @ %s] Priority: %s | Status: %s | Route: %s | Message: \"%s\"",
                        sel.getId(), sel.getTimestamp(), sel.getPriorityName(), sel.getStatusDisplay(), sel.getRoutePath(), sel.getMessageContent()));
                clipboard.setContent(content);
                showAlert("Copied to Clipboard", "Simulation dispatch summary was copied to the clipboard.");
            }
        });

        actionsRow.getChildren().addAll(historyReplayBtn, historyTimelineBtn, copyBtn);
        card.getChildren().add(actionsRow);

        return card;
    }

    private void showHistoryRecordDetails(SimulationRecord record) {
        if (record == null) {
            resetHistoryDetailPanel();
            return;
        }

        historyDetailIdLabel.setText(record.getId());
        historyDetailTimeBadge.setText("Time: " + record.getTimestamp());

        // Status badge
        SimulationResult.DeliveryOutcome outcome = record.getOutcome();
        if (outcome == SimulationResult.DeliveryOutcome.DELIVERED) {
            int hops = record.getHopCount();
            historyDetailStatusBadge.setText("✔ DELIVERED (" + hops + " " + (hops == 1 ? "HOP" : "HOPS") + ")");
            historyDetailStatusBadge.getStyleClass().setAll("badge-delivered");
        } else if (outcome == SimulationResult.DeliveryOutcome.NO_ROUTE_FOUND) {
            historyDetailStatusBadge.setText("⚠️ NO ROUTE FOUND");
            historyDetailStatusBadge.getStyleClass().setAll("badge-no-route");
        } else {
            historyDetailStatusBadge.setText("✖ DELIVERY FAILED");
            historyDetailStatusBadge.getStyleClass().setAll("badge-failed");
        }

        // Priority badge
        String prio = record.getPriorityName();
        historyDetailPriorityBadge.setText(prio);
        if ("CRITICAL".equalsIgnoreCase(prio)) {
            historyDetailPriorityBadge.getStyleClass().setAll("badge-priority-critical");
        } else if ("HIGH".equalsIgnoreCase(prio)) {
            historyDetailPriorityBadge.getStyleClass().setAll("badge-priority-high");
        } else {
            historyDetailPriorityBadge.getStyleClass().setAll("badge-priority-normal");
        }

        // Endpoints
        CommunicationDevice sender = record.getMessage() != null ? record.getMessage().getSender() : null;
        CommunicationDevice recipient = record.getMessage() != null ? record.getMessage().getRecipient() : null;

        String senderIcon = (sender instanceof SecurityStation) ? "🛡️" : (sender instanceof MedicalStation) ? "🏥" : "📱";
        String recipIcon = (recipient instanceof SecurityStation) ? "🛡️" : (recipient instanceof MedicalStation) ? "🏥" : "📱";

        historyDetailSenderLabel.setText(sender != null ? String.format("%s %s (%.0f%%)", senderIcon, sender.getName(), sender.getBatteryLevel()) : "—");
        historyDetailRecipientLabel.setText(recipient != null ? String.format("%s %s (%.0f%%)", recipIcon, recipient.getName(), recipient.getBatteryLevel()) : "—");

        // Message
        historyDetailMessageContent.setText("\"" + record.getMessageContent() + "\"");

        // Route chips
        historyDetailRouteHBox.getChildren().clear();
        List<CommunicationDevice> route = record.getResult() != null ? record.getResult().getRoute() : Collections.emptyList();
        if (route != null && !route.isEmpty()) {
            for (int i = 0; i < route.size(); i++) {
                CommunicationDevice dev = route.get(i);
                String icon = (dev instanceof SecurityStation) ? "🛡️" : (dev instanceof MedicalStation) ? "🏥" : "📱";
                Label chip = new Label(icon + " " + dev.getName());
                chip.setStyle("-fx-background-color: #0c213d; -fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700; -fx-padding: 3 8; -fx-background-radius: 4px; -fx-border-color: #0284c7; -fx-border-radius: 4px;");
                historyDetailRouteHBox.getChildren().add(chip);

                if (i < route.size() - 1) {
                    Label arrow = new Label("──▶");
                    arrow.setStyle("-fx-text-fill: #22d3ee; -fx-font-weight: 900; -fx-font-size: 11px;");
                    historyDetailRouteHBox.getChildren().add(arrow);
                }
            }
        } else {
            Label noRouteLbl = new Label("⛔ No Route Traversed");
            noRouteLbl.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 3 8; -fx-background-radius: 4px;");
            historyDetailRouteHBox.getChildren().add(noRouteLbl);
        }

        // Explanation
        historyDetailExplanationLabel.setText(record.getResult() != null ? record.getResult().getExplanation() : "No diagnostic information recorded.");

        // Actions
        historyReplayBtn.setDisable(!record.isDelivered());
        historyTimelineBtn.setDisable(record.getTimeline() == null);
    }

    private void resetHistoryDetailPanel() {
        if (historyDetailIdLabel != null) historyDetailIdLabel.setText("No Record Selected");
        if (historyDetailTimeBadge != null) historyDetailTimeBadge.setText("");
        if (historyDetailStatusBadge != null) {
            historyDetailStatusBadge.setText("STANDBY");
            historyDetailStatusBadge.getStyleClass().setAll("badge-waiting");
        }
        if (historyDetailPriorityBadge != null) {
            historyDetailPriorityBadge.setText("NORMAL");
            historyDetailPriorityBadge.getStyleClass().setAll("badge-priority-normal");
        }
        if (historyDetailSenderLabel != null) historyDetailSenderLabel.setText("—");
        if (historyDetailRecipientLabel != null) historyDetailRecipientLabel.setText("—");
        if (historyDetailMessageContent != null) historyDetailMessageContent.setText("Select a record from the table to view its transmitted content.");
        if (historyDetailRouteHBox != null) {
            historyDetailRouteHBox.getChildren().clear();
            Label emptyRouteLbl = new Label("No route available");
            emptyRouteLbl.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
            historyDetailRouteHBox.getChildren().add(emptyRouteLbl);
        }
        if (historyDetailExplanationLabel != null) historyDetailExplanationLabel.setText("Awaiting record selection.");
        if (historyReplayBtn != null) historyReplayBtn.setDisable(true);
        if (historyTimelineBtn != null) historyTimelineBtn.setDisable(true);
    }

    private void applyHistoryFilters() {
        if (filteredHistoryList == null) return;
        String query = historySearchField != null ? historySearchField.getText() : null;
        String status = historyStatusFilter != null ? historyStatusFilter.getValue() : null;
        String device = historyDeviceFilter != null ? historyDeviceFilter.getValue() : null;

        filteredHistoryList.setPredicate(record -> historyManager.matches(record, query, status, device));
    }

    private void updateHistorySummaryKpis() {
        if (histTotalKpiLabel == null) return;
        int total = historyManager.getTotalCount();
        int delivered = historyManager.getDeliveredCount();
        int noRoute = historyManager.getNoRouteCount();
        int failed = historyManager.getFailedCount();
        double rate = historyManager.getDeliveryRate();

        histTotalKpiLabel.setText(String.valueOf(total));
        histDeliveredKpiLabel.setText(String.valueOf(delivered));
        histNoRouteKpiLabel.setText(String.valueOf(noRoute));
        histFailedKpiLabel.setText(String.valueOf(failed));
        if (histRateKpiLabel != null) {
            histRateKpiLabel.setText(String.format("%.1f%% Delivery Rate", rate));
        }
    }

    private void handleClearHistory() {
        if (historyManager.isEmpty()) {
            showAlert("History Already Empty", "There are no dispatch records to clear.");
            return;
        }

        if (settingsManager.getSettings().isConfirmClearHistory()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.initOwner(primaryStage);
            alert.setTitle("Clear Emergency Dispatch History");
            alert.setHeaderText("Clear All " + historyManager.getTotalCount() + " Dispatch Records?");
            alert.setContentText("This will permanently remove all emergency dispatch history from the current session.\n\nAre you sure you want to proceed?");
            applyDialogStyles(alert);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
        }

        historyManager.clear();
        if (historyComboBox != null) {
            historyComboBox.setValue(null);
        }
        currentTimelineEvents.clear();
        resetHistoryDetailPanel();
        updateHistorySummaryKpis();
        log("HISTORY", "Emergency dispatch history was cleared by user.");
    }

    private void handleExportReport() {
        String report = historyManager.generateSummaryReport();

        TextArea reportArea = new TextArea(report);
        reportArea.setEditable(false);
        reportArea.setWrapText(false);
        reportArea.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; -fx-background-color: #070c17; -fx-text-fill: #38bdf8;");
        reportArea.setPrefSize(680, 420);

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Emergency Dispatch Summary Report");
        alert.setHeaderText("Session Dispatch Report (" + historyManager.getTotalCount() + " Total Records)");

        ButtonType copyButton = new ButtonType("📋 Copy to Clipboard", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().setAll(copyButton, ButtonType.CLOSE);

        alert.getDialogPane().setContent(reportArea);
        alert.getDialogPane().setStyle("-fx-background-color: #0b1324;");

        Optional<ButtonType> res = alert.showAndWait();
        if (res.isPresent() && res.get() == copyButton) {
            javafx.scene.input.Clipboard clipboard = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putString(report);
            clipboard.setContent(content);
            log("REPORT", "Emergency dispatch summary report exported to clipboard.");
        }
    }

    // --------------------------------------------------------------------------
    // 6. View 5: ⏱️ Event Timeline & Activity Logs Workspace
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

        Region actSpacer = new Region();
        HBox.setHgrow(actSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button actHomeBtn = new Button("🏠 Dashboard");
        actHomeBtn.getStyleClass().add("btn-ghost");
        actHomeBtn.setStyle("-fx-font-size: 11px; -fx-padding: 5 12;");
        actHomeBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(actHomeBtn, "Return to main Emergency Command Dashboard");

        historyBar.getChildren().addAll(historyLbl, historyComboBox, replayHistoryBtn, timelineSummaryLabel, actSpacer, actHomeBtn);
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
        CommunicationDevice sender = senderComboBox != null ? senderComboBox.getValue() : null;
        CommunicationDevice recipient = recipientComboBox != null ? recipientComboBox.getValue() : null;
        Priority priority = priorityComboBox != null ? priorityComboBox.getValue() : Priority.NORMAL;
        String content = messageTextField != null ? messageTextField.getText() : "";

        // 1. Validate required fields and device operational states
        if (sender == null && recipient == null) {
            showDispatchValidationError("Incomplete Selection", "Please select both an Origin Sender node and a Target Recipient node from the dropdowns.");
            return;
        }
        if (sender == null) {
            showDispatchValidationError("Missing Sender", "Please select an Origin Sender device from the dropdown.");
            return;
        }
        if (recipient == null) {
            showDispatchValidationError("Missing Recipient", "Please select a Target Recipient device from the dropdown.");
            return;
        }
        if (sender.equals(recipient)) {
            showDispatchValidationError("Invalid Route", "Origin Sender and Target Recipient cannot be the same device. Please select two distinct nodes.");
            return;
        }
        if (content == null || content.trim().isEmpty()) {
            showDispatchValidationError("Missing Emergency Message", "Please enter an emergency alert message or select one of the 1-click presets.");
            return;
        }
        if (sender.getStatus() == DeviceStatus.OFFLINE) {
            showDispatchValidationError("Sender Offline", String.format("Sender node '%s' is currently OFFLINE and cannot transmit messages. Bring it online in 'Network & Devices' or choose an active node.", sender.getName()));
            return;
        }
        if (sender.getBatteryLevel() <= 0.0) {
            showDispatchValidationError("Sender Depleted", String.format("Sender node '%s' has 0%% battery. Recharge the node in 'Network & Devices' before transmitting.", sender.getName()));
            return;
        }
        if (recipient.getStatus() == DeviceStatus.OFFLINE) {
            showDispatchValidationError("Recipient Offline", String.format("Target node '%s' is currently OFFLINE. Delivery cannot be completed until the device is brought online.", recipient.getName()));
            return;
        }
        if (recipient.getBatteryLevel() <= 0.0) {
            showDispatchValidationError("Recipient Depleted", String.format("Target node '%s' has 0%% battery and cannot acknowledge receipt.", recipient.getName()));
            return;
        }
        if (graph.getDeviceCount() < 2) {
            showDispatchValidationError("Insufficient Nodes", "At least 2 devices must be registered in the network to simulate message dispatch.");
            return;
        }
        if (graph.getTotalLinkCount() / 2 <= 0) {
            showDispatchValidationError("Mesh Disconnected", "Registered devices have 0 active wireless links between them. Connect devices in 'Network & Devices' to establish communication paths.");
            return;
        }

        // Clear any previous validation errors
        clearDispatchValidationError();

        String msgId = "MSG-" + (messageCounter++);
        totalMessagesDispatched++;

        if (topologyPane != null) {
            topologyPane.clearRouteHighlight();
        }

        EmergencyMessage message = new EmergencyMessage(msgId, sender, recipient, content.trim(), priority != null ? priority : Priority.NORMAL);
        log("DISPATCH", String.format("[%s] Initiated from '%s' to '%s' | Priority: %s | Message: \"%s\"",
                msgId, sender.getName(), recipient.getName(), message.getPriority(), message.getContent()));

        // Run simulation using existing SimulationEngine backend
        SimulationResult result = engine.send(message);

        // Advance tutorial step if user is currently on DISPATCH_ALERT step
        if (tutorialManager.getCurrentStep() == TutorialStep.DISPATCH_ALERT) {
            tutorialManager.nextStep();
            updateTutorialBanner();
        }

        // Update UI outcome showcases (both in Dispatch Console and Dashboard)
        updateDeliveryOutcomeUI(message, result);

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
            replayFromOutcomeBtn.setDisable(!result.delivered());
        }

        if (topologyPane != null) {
            topologyPane.loadReplay(record);
        }

        refreshUI();
    }

    private void updateDeliveryOutcomeUI(EmergencyMessage message, SimulationResult result) {
        String msgId = message.getId();
        Priority priority = message.getPriority();
        CommunicationDevice recipient = message.getRecipient();

        if (result.delivered()) {
            successfulDeliveries++;
            int hops = result.getHopCount();
            int nodesCount = result.route().size();
            String routeStr = result.route().stream().map(CommunicationDevice::getName).collect(Collectors.joining(" -> "));

            // 1. Result Badge
            resultBadge.setText("✔ DELIVERED (" + hops + " " + (hops == 1 ? "HOP" : "HOPS") + ")");
            resultBadge.getStyleClass().setAll("badge-delivered");
            resultBadge.setStyle("-fx-background-color: #064e3b; -fx-text-fill: #34d399; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #059669; -fx-border-radius: 4px;");

            // 2. Metric Tiles
            if (outcomeStatusMetricLabel != null) {
                outcomeStatusMetricLabel.setText("Delivered");
                outcomeStatusMetricLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 13px; -fx-font-weight: 800;");
            }
            if (outcomeHopsMetricLabel != null) {
                outcomeHopsMetricLabel.setText(hops + (hops == 1 ? " Hop" : " Hops"));
            }
            if (outcomeNodesMetricLabel != null) {
                outcomeNodesMetricLabel.setText(nodesCount + " Devices");
            }
            if (outcomePriorityMetricLabel != null) {
                outcomePriorityMetricLabel.setText(priority.name());
            }

            // 3. Readable route summary & Breadcrumbs
            if (outcomeReadableRouteLabel != null) {
                outcomeReadableRouteLabel.setText("Route: " + routeStr);
                outcomeReadableRouteLabel.setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: 700;");
            }
            renderHopRoute(result.route());

            // 4. Topology Highlighting
            if (topologyPane != null) {
                topologyPane.highlightRoute(result.route());
            }

            // 5. Explanations
            double currentDrain = settingsManager.getSettings().getBatteryDrainPerHop();
            resultExplanationLabel.setText(result.explanation() + String.format("\n⚡ Battery Impact: -%.1f%% deducted from all %d devices along the transmission path.", currentDrain, nodesCount));

            // 6. Guidance Banner
            if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-success");
                dispatchGuidanceLabel.setText(String.format("✔ Alert [%s] delivered successfully to '%s' via %d hop(s)!", msgId, recipient.getName(), hops));
            }

            log("SUCCESS", String.format("[%s] Successfully delivered via %d hops: %s", msgId, hops, routeStr));

            technicalDetailsContent.setText(String.format(
                    "ALGORITHM: Breadcrumb BFS Shortest-Path Discovery\n" +
                    "MESSAGE ID: %s | PRIORITY: %s\n" +
                    "CALCULATED ROUTE (%d hops):\n  %s\n" +
                    "BATTERY IMPACT: -%.1f%% deducted per hop (%d nodes drained)\n" +
                    "STATUS: Delivered successfully to destination.",
                    msgId, priority, hops, routeStr, currentDrain, nodesCount));

        } else if (result.isNoRouteFound()) {
            // NO ROUTE FOUND
            resultBadge.setText("⚠️ NO ROUTE FOUND");
            resultBadge.getStyleClass().setAll("badge-no-route");
            resultBadge.setStyle("-fx-background-color: #451a03; -fx-text-fill: #fbbf24; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #d97706; -fx-border-radius: 4px;");

            if (outcomeStatusMetricLabel != null) {
                outcomeStatusMetricLabel.setText("No Route Found");
                outcomeStatusMetricLabel.setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 13px; -fx-font-weight: 800;");
            }
            if (outcomeHopsMetricLabel != null) {
                outcomeHopsMetricLabel.setText("0 Hops");
            }
            if (outcomeNodesMetricLabel != null) {
                outcomeNodesMetricLabel.setText("0 Devices");
            }
            if (outcomePriorityMetricLabel != null) {
                outcomePriorityMetricLabel.setText(priority.name());
            }

            if (outcomeReadableRouteLabel != null) {
                outcomeReadableRouteLabel.setText("Route: None (Path severed or disconnected)");
                outcomeReadableRouteLabel.setStyle("-fx-text-fill: #fca5a5; -fx-font-size: 11px; -fx-font-weight: 700;");
            }
            renderHopRoute(Collections.emptyList());

            if (topologyPane != null) {
                topologyPane.clearRouteHighlight();
            }

            resultExplanationLabel.setText(result.explanation() + "\n💡 Fix: Verify intermediate nodes are online, have sufficient battery (>0%), and can forward " + priority + " priority.");

            if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
                dispatchGuidanceLabel.setText("⚠️ No Route Found: BFS search completed without finding a viable path between endpoints.");
            }

            log("NO ROUTE", String.format("[%s] No route found. Reason: %s", msgId, result.explanation()));

            technicalDetailsContent.setText(String.format(
                    "ALGORITHM: Breadcrumb BFS Shortest-Path Discovery\n" +
                    "MESSAGE ID: %s | PRIORITY: %s\n" +
                    "FAILURE DIAGNOSTIC: %s\n" +
                    "STATUS: BFS search completed without finding a viable route.",
                    msgId, priority, result.explanation()));

        } else {
            // FAILED (Node offline, depleted, etc.)
            resultBadge.setText("✖ DELIVERY FAILED");
            resultBadge.getStyleClass().setAll("badge-failed");
            resultBadge.setStyle("-fx-background-color: #450a0a; -fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #dc2626; -fx-border-radius: 4px;");

            if (outcomeStatusMetricLabel != null) {
                outcomeStatusMetricLabel.setText("Failed");
                outcomeStatusMetricLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 13px; -fx-font-weight: 800;");
            }
            if (outcomeHopsMetricLabel != null) {
                outcomeHopsMetricLabel.setText("0 Hops");
            }
            if (outcomeNodesMetricLabel != null) {
                outcomeNodesMetricLabel.setText("0 Devices");
            }
            if (outcomePriorityMetricLabel != null) {
                outcomePriorityMetricLabel.setText(priority.name());
            }

            if (outcomeReadableRouteLabel != null) {
                outcomeReadableRouteLabel.setText("Route: None (Transmission aborted)");
                outcomeReadableRouteLabel.setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: 700;");
            }
            renderHopRoute(Collections.emptyList());

            if (topologyPane != null) {
                topologyPane.clearRouteHighlight();
            }

            resultExplanationLabel.setText(result.explanation() + "\n💡 Fix: Verify sender and recipient devices are online and recharged.");

            if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
                dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
                dispatchGuidanceLabel.setText("✖ Delivery Failed: " + result.explanation());
            }

            log("FAILED", String.format("[%s] Delivery failed. Reason: %s", msgId, result.explanation()));

            technicalDetailsContent.setText(String.format(
                    "ALGORITHM: Breadcrumb BFS Shortest-Path Discovery\n" +
                    "MESSAGE ID: %s | PRIORITY: %s\n" +
                    "FAILURE DIAGNOSTIC: %s\n" +
                    "STATUS: Transmission failed before routing.",
                    msgId, priority, result.explanation()));
        }

        // Update dashboard snapshot labels
        if (dashboardOutcomeBadge != null) {
            dashboardOutcomeBadge.setText(resultBadge.getText());
            dashboardOutcomeBadge.setStyle(resultBadge.getStyle());
        }
        if (dashboardOutcomeExplanation != null) {
            dashboardOutcomeExplanation.setText(result.explanation());
        }
    }

    private void handleResetDispatch() {
        if (messageTextField != null) {
            messageTextField.clear();
        }
        if (senderComboBox != null) {
            senderComboBox.setValue(null);
        }
        if (recipientComboBox != null) {
            recipientComboBox.setValue(null);
        }
        if (priorityComboBox != null) {
            priorityComboBox.setValue(Priority.NORMAL);
        }
        if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
            dispatchGuidanceBanner.getStyleClass().setAll("guide-banner");
            dispatchGuidanceLabel.setText("💡 Select an Origin Sender and Target Recipient, enter an emergency message, and click 'Send Emergency'.");
        }
        clearDispatchValidationError();

        resetOutcomeDisplay();

        if (topologyPane != null) {
            topologyPane.clearRouteHighlight();
        }

        log("DISPATCH", "Emergency dispatch console reset to initial state.");
    }

    private void resetOutcomeDisplay() {
        if (resultBadge != null) {
            resultBadge.setText("WAITING FOR DISPATCH");
            resultBadge.getStyleClass().setAll("badge-waiting");
            resultBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px; -fx-border-color: #334155; -fx-border-radius: 4px;");
        }
        if (outcomeStatusMetricLabel != null) {
            outcomeStatusMetricLabel.setText("Idle");
            outcomeStatusMetricLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 13px; -fx-font-weight: 800;");
        }
        if (outcomeHopsMetricLabel != null) {
            outcomeHopsMetricLabel.setText("--");
        }
        if (outcomeNodesMetricLabel != null) {
            outcomeNodesMetricLabel.setText("--");
        }
        if (outcomePriorityMetricLabel != null) {
            outcomePriorityMetricLabel.setText("--");
        }
        if (outcomeReadableRouteLabel != null) {
            outcomeReadableRouteLabel.setText("Route: (Awaiting dispatch)");
            outcomeReadableRouteLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-style: italic;");
        }
        if (resultExplanationLabel != null) {
            resultExplanationLabel.setText("Select sender, recipient, and message, then click 'Send Emergency' to simulate mesh propagation.");
        }
        if (resultRouteHBox != null) {
            renderEmptyRoute();
        }
        if (replayFromOutcomeBtn != null) {
            replayFromOutcomeBtn.setDisable(true);
        }
        if (technicalDetailsContent != null) {
            technicalDetailsContent.setText("No simulation executed yet.");
        }
        if (dashboardOutcomeBadge != null) {
            dashboardOutcomeBadge.setText("WAITING FOR DISPATCH");
            dashboardOutcomeBadge.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px; -fx-font-weight: 800; -fx-padding: 4 10; -fx-background-radius: 4px;");
        }
        if (dashboardOutcomeExplanation != null) {
            dashboardOutcomeExplanation.setText("Ready for simulation dispatch.");
        }
    }

    private void showDispatchValidationError(String title, String message) {
        if (dispatchGuidanceBanner != null && dispatchGuidanceLabel != null) {
            dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
            dispatchGuidanceLabel.setText("⚠️ " + title + ": " + message);
        }
        if (dispatchInlineErrorLabel != null) {
            dispatchInlineErrorLabel.setText("⚠️ " + title + ": " + message);
            dispatchInlineErrorLabel.setVisible(true);
            dispatchInlineErrorLabel.setManaged(true);
        }
        log("VALIDATION", "Dispatch blocked: " + message);
        showAlert("Cannot Send Emergency Alert", title + "\n\n" + message);
    }

    private void clearDispatchValidationError() {
        if (dispatchInlineErrorLabel != null) {
            dispatchInlineErrorLabel.setText("");
            dispatchInlineErrorLabel.setVisible(false);
            dispatchInlineErrorLabel.setManaged(false);
        }
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

        // If on tutorial ADD_DEVICE step, provide helpful guidance
        if (tutorialManager.getCurrentStep() == TutorialStep.ADD_DEVICE && tutorialHintLabel != null) {
            if (graph.getDeviceCount() >= 2) {
                tutorialHintLabel.setText("✔ Registered " + graph.getDeviceCount() + " nodes! Click 'Next Step ➡' to establish wireless mesh links.");
            }
        }

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

            // If on tutorial CONNECT_DEVICES step, provide helpful guidance
            if (tutorialManager.getCurrentStep() == TutorialStep.CONNECT_DEVICES && tutorialHintLabel != null) {
                if (graph.getTotalLinkCount() / 2 >= 1) {
                    tutorialHintLabel.setText("✔ Wireless link established! Click 'Next Step ➡' to dispatch an emergency alert.");
                }
            }

            refreshUI();
            log("LINK", String.format("Established mesh link: '%s' <───> '%s'", devA.getName(), devB.getName()));
        } else {
            connectFeedbackLabel.setText("ℹ️ Devices '" + devA.getName() + "' and '" + devB.getName() + "' are already directly connected.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #38bdf8;");
        }
    }

    private void handleRenameSelectedDevice() {
        CommunicationDevice dev = selectedDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Device Selected", "Please select a device from the dropdown to rename.");
            return;
        }

        TextInputDialog dialog = new TextInputDialog(dev.getName());
        dialog.initOwner(primaryStage);
        dialog.setTitle("Edit Device Name");
        dialog.setHeaderText("Rename Node: " + dev.getName() + " [" + dev.getId() + "]");
        dialog.setContentText("Enter new name for device:");
        applyDialogStyles(dialog);

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            String oldName = dev.getName();
            String newName = result.get().trim();
            dev.setName(newName);
            log("NODE", String.format("Renamed device [%s] from '%s' to '%s'", dev.getId(), oldName, newName));
            refreshUI();
            selectedDeviceComboBox.setValue(dev);
            selectedDeviceStatusLabel.setText("✔ Renamed to '" + newName + "'.");
            selectedDeviceStatusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px;");
        }
    }

    private void handleDeleteSelectedDevice() {
        CommunicationDevice dev = selectedDeviceComboBox.getValue();
        if (dev == null) {
            showAlert("No Device Selected", "Please select a device from the dropdown to delete.");
            return;
        }

        boolean requireConfirm = settingsManager.getSettings().isConfirmClearNetwork();
        if (requireConfirm) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(primaryStage);
            confirm.setTitle("Confirm Delete Device");
            confirm.setHeaderText("Delete Node '" + dev.getName() + "' [" + dev.getId() + "]?");
            confirm.setContentText("This will permanently remove the device and sever all connecting wireless links.\n\nAre you sure you want to proceed?");
            ButtonType delBtnType = new ButtonType("Delete Node", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(delBtnType, cancelBtnType);
            applyDialogStyles(confirm);

            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isEmpty() || choice.get() != delBtnType) {
                return;
            }
        }

        String name = dev.getName();
        String id = dev.getId();
        boolean removed = graph.removeDevice(dev);
        if (removed) {
            log("NODE", String.format("Deleted device node: '%s' [%s] and severed all connected mesh links.", name, id));
            selectedDeviceComboBox.setValue(null);
            refreshUI();
            selectedDeviceStatusLabel.setText("✔ Node '" + name + "' deleted.");
            selectedDeviceStatusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-size: 11px;");
        } else {
            showAlert("Delete Failed", "Could not remove device: Device not found in network graph.");
        }
    }

    private void handleDisconnectDevices() {
        CommunicationDevice devA = linkDeviceAComboBox.getValue();
        CommunicationDevice devB = linkDeviceBComboBox.getValue();

        if (devA == null || devB == null) {
            connectFeedbackLabel.setText("⚠️ Please select both devices to disconnect their mesh link.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        if (devA.equals(devB)) {
            connectFeedbackLabel.setText("⚠️ Please select two distinct connected devices.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f87171;");
            return;
        }

        if (!graph.areConnected(devA, devB)) {
            connectFeedbackLabel.setText("ℹ️ Devices '" + devA.getName() + "' and '" + devB.getName() + "' are not directly connected.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f59e0b;");
            return;
        }

        boolean disconnected = graph.disconnect(devA, devB);
        if (disconnected) {
            connectFeedbackLabel.setText("✔ Severed wireless link: '" + devA.getName() + "' <─/─> '" + devB.getName() + "'");
            connectFeedbackLabel.setStyle("-fx-text-fill: #34d399;");
            refreshUI();
            log("LINK", String.format("Severed wireless link: '%s' <─/─> '%s'", devA.getName(), devB.getName()));
        } else {
            connectFeedbackLabel.setText("⚠️ Failed to disconnect link.");
            connectFeedbackLabel.setStyle("-fx-text-fill: #f87171;");
        }
    }

    private void handleSaveNetwork() {
        if (graph.getDeviceCount() == 0) {
            Alert confirmEmpty = new Alert(Alert.AlertType.CONFIRMATION);
            confirmEmpty.initOwner(primaryStage);
            confirmEmpty.setTitle("Save Empty Network");
            confirmEmpty.setHeaderText("Current Network Has No Registered Devices");
            confirmEmpty.setContentText("The simulation graph is currently empty (0 devices).\n\n" +
                    "Do you still want to export an empty network configuration template file?");
            ButtonType saveEmptyBtn = new ButtonType("Save Empty File", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmEmpty.getButtonTypes().setAll(saveEmptyBtn, cancelBtn);
            applyDialogStyles(confirmEmpty);
            Optional<ButtonType> choice = confirmEmpty.showAndWait();
            if (choice.isEmpty() || choice.get() != saveEmptyBtn) {
                return;
            }
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save ResQMesh Network Topology");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Configuration Files (*.json)", "*.json"));
        fileChooser.setInitialFileName("resqmesh-network.json");

        File file = fileChooser.showSaveDialog(primaryStage);
        if (file != null) {
            try {
                NetworkConfigManager.saveToFile(graph, file, "ResQMesh Simulation Topology");
                log("CONFIG", String.format("Saved network configuration (%d nodes, %d links) to '%s'",
                        graph.getDeviceCount(), graph.getTotalLinkCount() / 2, file.getName()));
                showSuccessAlert("Network Saved Successfully",
                        String.format("Successfully saved network configuration:\n\n" +
                                        "• File: %s\n" +
                                        "• Registered Devices: %d\n" +
                                        "• Active Mesh Connections: %d\n" +
                                        "• Destination Path:\n%s",
                                file.getName(), graph.getDeviceCount(), graph.getTotalLinkCount() / 2, file.getAbsolutePath()));
            } catch (Exception ex) {
                log("ERROR", "Failed to save configuration: " + ex.getMessage());
                showErrorAlert("Save Failed", "Could not write configuration to disk:\n\n" + ex.getMessage());
            }
        }
    }

    private void handleLoadNetwork() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Load ResQMesh Network Topology");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Configuration Files (*.json)", "*.json"));

        File file = fileChooser.showOpenDialog(primaryStage);
        if (file == null) {
            // User cancelled file selection - return safely
            return;
        }

        // Ask for confirmation before replacing the current network
        if (graph.getDeviceCount() > 0 && settingsManager.getSettings().isConfirmNetworkReplacement()) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.initOwner(primaryStage);
            confirmAlert.setTitle("Confirm Load Network");
            confirmAlert.setHeaderText("Replace Current Network Configuration?");
            confirmAlert.setContentText(String.format(
                    "Loading '%s' will replace your active network configuration (%d device%s, %d active connection%s).\n\n" +
                    "All unsaved changes to the current network will be permanently lost.\n\n" +
                    "Do you want to proceed and load the selected configuration?",
                    file.getName(),
                    graph.getDeviceCount(), graph.getDeviceCount() == 1 ? "" : "s",
                    graph.getTotalLinkCount() / 2, (graph.getTotalLinkCount() / 2) == 1 ? "" : "s"));

            ButtonType replaceBtn = new ButtonType("Replace Network", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel (Keep Current)", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(replaceBtn, cancelBtn);
            applyDialogStyles(confirmAlert);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != replaceBtn) {
                log("CONFIG", "Load network cancelled by user. Active network preserved.");
                return;
            }
        }

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
            updateDeviceIdCounterAfterLoad();

            refreshUI();

            if (!graph.getAllDevices().isEmpty()) {
                List<CommunicationDevice> list = new ArrayList<>(graph.getAllDevices());
                CommunicationDevice first = list.get(0);
                selectedDeviceComboBox.setValue(first);
                senderComboBox.setValue(first);
                if (list.size() > 1) {
                    recipientComboBox.setValue(list.get(1));
                    linkDeviceAComboBox.setValue(list.get(0));
                    linkDeviceBComboBox.setValue(list.get(1));
                } else {
                    recipientComboBox.setValue(null);
                    linkDeviceAComboBox.setValue(first);
                    linkDeviceBComboBox.setValue(null);
                }
                if (topologyPane != null) {
                    topologyPane.selectDevice(first);
                }
            } else {
                selectedDeviceComboBox.setValue(null);
                senderComboBox.setValue(null);
                recipientComboBox.setValue(null);
                linkDeviceAComboBox.setValue(null);
                linkDeviceBComboBox.setValue(null);
                if (topologyPane != null) {
                    topologyPane.selectDevice(null);
                }
            }

            int devCount = graph.getDeviceCount();
            int linkCount = graph.getTotalLinkCount() / 2;
            long phoneCount = graph.getAllDevices().stream().filter(d -> d instanceof StudentPhone).count();
            long secCount = graph.getAllDevices().stream().filter(d -> d instanceof SecurityStation).count();
            long medCount = graph.getAllDevices().stream().filter(d -> d instanceof MedicalStation).count();

            log("CONFIG", String.format("Loaded network topology from '%s': %d nodes, %d links.",
                    file.getName(), devCount, linkCount));
            showSuccessAlert("Network Loaded Successfully",
                    String.format("Successfully loaded configuration '%s':\n\n" +
                                    "• Total Devices: %d (📱 %d Phones, 🛡️ %d Security, 🏥 %d Medical)\n" +
                                    "• Active Mesh Connections: %d Links\n\n" +
                                    "Topology visualization, device telemetry, and dispatch controls have been refreshed.",
                            file.getName(), devCount, phoneCount, secCount, medCount, linkCount));
        } catch (ConfigurationException ex) {
            log("ERROR", "Invalid configuration file: " + ex.getMessage());
            showErrorAlert("Configuration Validation Error",
                    "The selected file is not a valid ResQMesh configuration:\n\n" + ex.getMessage() +
                    "\n\nPlease ensure the file contains valid JSON with matching device IDs and connections.");
        } catch (Exception ex) {
            log("ERROR", "Failed to load configuration file: " + ex.getMessage());
            showErrorAlert("Load Failed",
                    "An error occurred while reading the configuration file:\n\n" + ex.getMessage());
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
        loadSampleNetwork(true);
    }

    private void loadSampleNetwork(boolean confirmIfNotEmpty) {
        if (confirmIfNotEmpty && settingsManager.getSettings().isConfirmNetworkReplacement() && tutorialManager.requiresConfirmationToLoadSample(graph.getDeviceCount(), graph.getTotalLinkCount() / 2)) {
            Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmAlert.initOwner(primaryStage);
            confirmAlert.setTitle("Replace Current Network?");
            confirmAlert.setHeaderText("Load Sample Campus Network");
            confirmAlert.setContentText(String.format(
                    "Your current network contains %d registered device(s) and %d active connection(s).\n\n" +
                    "Loading the sample network will replace your existing topology.\n\n" +
                    "Do you want to proceed and load the sample network?",
                    graph.getDeviceCount(), graph.getTotalLinkCount() / 2));

            ButtonType replaceBtn = new ButtonType("Replace & Load Sample", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtn = new ButtonType("Cancel (Keep Current)", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirmAlert.getButtonTypes().setAll(replaceBtn, cancelBtn);
            applyDialogStyles(confirmAlert);

            Optional<ButtonType> result = confirmAlert.showAndWait();
            if (result.isEmpty() || result.get() != replaceBtn) {
                log("CONFIG", "Sample network loading cancelled by user. Existing network preserved.");
                return;
            }
        }

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
        Set<CommunicationDevice> allDevices = graph.getAllDevices();
        deviceObservableList.setAll(allDevices);

        // Invalidate stale selections in combo boxes if the selected item was removed
        if (selectedDeviceComboBox != null && selectedDeviceComboBox.getValue() != null && !allDevices.contains(selectedDeviceComboBox.getValue())) {
            selectedDeviceComboBox.setValue(null);
            updateToggleState(null);
        }
        if (senderComboBox != null && senderComboBox.getValue() != null && !allDevices.contains(senderComboBox.getValue())) {
            senderComboBox.setValue(null);
        }
        if (recipientComboBox != null && recipientComboBox.getValue() != null && !allDevices.contains(recipientComboBox.getValue())) {
            recipientComboBox.setValue(null);
        }
        if (linkDeviceAComboBox != null && linkDeviceAComboBox.getValue() != null && !allDevices.contains(linkDeviceAComboBox.getValue())) {
            linkDeviceAComboBox.setValue(null);
        }
        if (linkDeviceBComboBox != null && linkDeviceBComboBox.getValue() != null && !allDevices.contains(linkDeviceBComboBox.getValue())) {
            linkDeviceBComboBox.setValue(null);
        }

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
        updateDispatchGuidance();

        // Update Step 12 Tutorial Banner State
        updateTutorialBanner();

        // Sync Emergency History device filter options
        if (historyDeviceFilter != null) {
            String currentVal = historyDeviceFilter.getValue();
            java.util.Set<String> devNames = new java.util.LinkedHashSet<>();
            devNames.add("All Devices");
            if (graph != null) {
                for (CommunicationDevice dev : graph.getAllDevices()) {
                    devNames.add(dev.getName());
                }
            }
            if (historyManager != null) {
                for (SimulationRecord rec : historyManager.getRecords()) {
                    if (rec.getSenderName() != null) devNames.add(rec.getSenderName());
                    if (rec.getRecipientName() != null) devNames.add(rec.getRecipientName());
                }
            }
            List<String> list = new ArrayList<>(devNames);
            if (!historyDeviceFilter.getItems().equals(list)) {
                historyDeviceFilter.getItems().setAll(list);
                if (currentVal != null && list.contains(currentVal)) {
                    historyDeviceFilter.setValue(currentVal);
                } else {
                    historyDeviceFilter.setValue("All Devices");
                }
            }
        }

        // Sync history summary KPI tiles
        updateHistorySummaryKpis();
    }

    private void updateDispatchGuidance() {
        if (dispatchGuidanceBanner == null || dispatchGuidanceLabel == null) return;
        CommunicationDevice sender = senderComboBox != null ? senderComboBox.getValue() : null;
        CommunicationDevice recipient = recipientComboBox != null ? recipientComboBox.getValue() : null;
        int devCount = graph != null ? graph.getDeviceCount() : 0;
        int linkCount = graph != null ? graph.getTotalLinkCount() / 2 : 0;

        String msg = tutorialManager.getDispatchValidationMessage(sender, recipient, devCount, linkCount);
        if (msg != null) {
            dispatchGuidanceBanner.getStyleClass().setAll("guide-banner-warning");
            dispatchGuidanceLabel.setText("⚠️ " + msg);
        } else {
            dispatchGuidanceBanner.getStyleClass().setAll("guide-banner");
            dispatchGuidanceLabel.setText(String.format("💡 Ready to Dispatch: Shortest active path will be computed using Breadcrumb BFS. Participating nodes consume -%.1f%% battery per hop.", settingsManager.getSettings().getBatteryDrainPerHop()));
        }
    }

    private void setTooltip(javafx.scene.Node node, String text) {
        if (node != null && text != null && !text.isEmpty()) {
            Tooltip tt = new Tooltip(text);
            tt.setShowDelay(Duration.millis(300));
            Tooltip.install(node, tt);
        }
    }

    private VBox createTutorialBanner() {
        VBox banner = new VBox(8);
        banner.getStyleClass().add("tutorial-banner");
        banner.setPadding(new Insets(10, 16, 10, 16));

        HBox topRow = new HBox(10);
        topRow.setAlignment(Pos.CENTER_LEFT);

        tutorialBadgeLabel = new Label("TUTORIAL");
        tutorialBadgeLabel.getStyleClass().add("tutorial-badge");

        tutorialTitleLabel = new Label("Interactive Onboarding Guide");
        tutorialTitleLabel.getStyleClass().add("tutorial-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        tutorialPrevBtn = new Button("⬅ Previous");
        tutorialPrevBtn.getStyleClass().add("tutorial-btn");
        tutorialPrevBtn.setOnAction(e -> {
            tutorialManager.previousStep();
            applyTutorialStep();
        });
        setTooltip(tutorialPrevBtn, "Return to previous onboarding step");

        tutorialNextBtn = new Button("Next Step ➡");
        tutorialNextBtn.getStyleClass().add("tutorial-btn-primary");
        tutorialNextBtn.setOnAction(e -> {
            tutorialManager.nextStep();
            applyTutorialStep();
        });
        setTooltip(tutorialNextBtn, "Advance to the next onboarding tutorial step");

        tutorialRestartBtn = new Button("↺ Restart");
        tutorialRestartBtn.getStyleClass().add("tutorial-btn");
        tutorialRestartBtn.setOnAction(e -> {
            tutorialManager.restartTutorial();
            applyTutorialStep();
        });
        setTooltip(tutorialRestartBtn, "Restart tutorial from the beginning");

        tutorialSkipBtn = new Button("✕ Skip Tutorial");
        tutorialSkipBtn.getStyleClass().add("tutorial-btn");
        tutorialSkipBtn.setOnAction(e -> {
            tutorialManager.skipTutorial();
            updateTutorialBanner();
        });
        setTooltip(tutorialSkipBtn, "Dismiss tutorial guidance at any time");

        topRow.getChildren().addAll(tutorialBadgeLabel, tutorialTitleLabel, spacer, tutorialPrevBtn, tutorialNextBtn, tutorialRestartBtn, tutorialSkipBtn);

        tutorialDescLabel = new Label("");
        tutorialDescLabel.getStyleClass().add("tutorial-desc");
        tutorialDescLabel.setWrapText(true);

        tutorialHintLabel = new Label("");
        tutorialHintLabel.getStyleClass().add("tutorial-hint");
        tutorialHintLabel.setWrapText(true);

        banner.getChildren().addAll(topRow, tutorialDescLabel, tutorialHintLabel);
        return banner;
    }

    private void applyTutorialStep() {
        TutorialStep step = tutorialManager.getCurrentStep();
        switch (step) {
            case WELCOME:
            case ADD_DEVICE:
                showView(networkScrollPane, navNetworkBtn);
                if (networkTabPane != null && topologyTab != null) {
                    networkTabPane.getSelectionModel().select(topologyTab);
                }
                break;
            case CONNECT_DEVICES:
                showView(networkScrollPane, navNetworkBtn);
                break;
            case DISPATCH_ALERT:
                showView(dispatchScrollPane, navDispatchBtn);
                break;
            case REVIEW_OUTCOME:
                showView(dashboardScrollPane, navDashboardBtn);
                break;
            case COMPLETED:
            case SKIPPED:
                break;
        }
        updateTutorialBanner();
    }

    private void updateTutorialBanner() {
        if (tutorialBanner == null) return;
        boolean active = tutorialManager.isActive();
        tutorialBanner.setVisible(active);
        tutorialBanner.setManaged(active);
        if (!active) return;

        TutorialStep step = tutorialManager.getCurrentStep();
        int num = step.getStepNumber();
        if (num == 0) {
            tutorialBadgeLabel.setText("WELCOME");
        } else if (num >= 1 && num <= 4) {
            tutorialBadgeLabel.setText(String.format("STEP %d OF 4", num));
        } else {
            tutorialBadgeLabel.setText("STATUS");
        }

        tutorialTitleLabel.setText(step.getTitle());
        tutorialDescLabel.setText(tutorialManager.getStepInstruction());
        tutorialHintLabel.setText("👉 Action: " + tutorialManager.getActionHint());

        tutorialPrevBtn.setDisable(step == TutorialStep.WELCOME || step == TutorialStep.ADD_DEVICE);

        if (step == TutorialStep.COMPLETED) {
            tutorialNextBtn.setText("Finish ✔");
            tutorialNextBtn.setOnAction(e -> {
                tutorialManager.skipTutorial();
                updateTutorialBanner();
            });
        } else {
            tutorialNextBtn.setText("Next Step ➡");
            tutorialNextBtn.setOnAction(e -> {
                tutorialManager.nextStep();
                applyTutorialStep();
            });
        }
    }

    private void showWelcomeDialog() {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Welcome to ResQMesh");
        if (primaryStage != null) {
            dialog.initOwner(primaryStage);
        }

        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("welcome-modal");
        pane.setStyle("-fx-background-color: #080e1c; -fx-border-color: #0284c7; -fx-border-width: 1.5px; -fx-background-radius: 12px; -fx-border-radius: 12px;");

        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            pane.getStylesheets().add(cssResource.toExternalForm());
        }

        VBox content = new VBox(16);
        content.setPrefWidth(580);
        content.setPadding(new Insets(10, 10, 10, 10));

        // Header
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🛡️");
        icon.setStyle("-fx-font-size: 30px;");

        VBox titleBox = new VBox(3);
        Label title = new Label("Welcome to ResQMesh");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: 900; -fx-text-fill: #22d3ee;");
        Label subtitle = new Label("Offline Emergency Disaster Communication & Multi-Hop BFS Simulator");
        subtitle.setStyle("-fx-font-size: 12px; -fx-text-fill: #94a3b8;");
        titleBox.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(icon, titleBox);

        // Introduction
        Label desc = new Label(
                "When natural disasters or grid failures disable cellular towers and internet infrastructure, " +
                "ResQMesh establishes a resilient, decentralized peer-to-peer mesh network.\n\n" +
                "• Student Phones, Security Posts, and Medical Centers collaborate dynamically.\n" +
                "• Emergency distress alerts find the shortest path with lowest latency using Breadth-First Search (BFS).\n" +
                "• Each wireless hop consumes battery energy (-2.0%), simulating real-world hardware constraints.\n\n" +
                "Choose an option below to get started:"
        );
        desc.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 12px; -fx-line-spacing: 3px;");
        desc.setWrapText(true);

        // Options
        VBox options = new VBox(10);

        Button startTutBtn = new Button("🎓 Start Interactive Tutorial (Step-by-Step Guide)");
        startTutBtn.setMaxWidth(Double.MAX_VALUE);
        startTutBtn.getStyleClass().add("btn-cyan");
        startTutBtn.setOnAction(e -> {
            dialog.close();
            tutorialManager.startFromStep1();
            applyTutorialStep();
        });

        Button loadSampleBtn = new Button("📦 Load Sample Campus Network (Ready to Dispatch)");
        loadSampleBtn.setMaxWidth(Double.MAX_VALUE);
        loadSampleBtn.getStyleClass().add("btn-blue");
        loadSampleBtn.setOnAction(e -> {
            dialog.close();
            loadSampleNetwork(false);
            showView(dashboardScrollPane, navDashboardBtn);
        });

        Button newSimBtn = new Button("✨ New Blank Simulation (Build Network From Scratch)");
        newSimBtn.setMaxWidth(Double.MAX_VALUE);
        newSimBtn.getStyleClass().add("btn-ghost");
        newSimBtn.setOnAction(e -> {
            dialog.close();
            graph.clear();
            refreshUI();
            showView(networkScrollPane, navNetworkBtn);
        });

        Button guideBtn = new Button("❓ Open Quick Reference & Troubleshooting Guide");
        guideBtn.setMaxWidth(Double.MAX_VALUE);
        guideBtn.getStyleClass().add("btn-ghost");
        guideBtn.setOnAction(e -> {
            dialog.close();
            showView(helpScrollPane, navHelpBtn);
        });

        options.getChildren().addAll(startTutBtn, loadSampleBtn, newSimBtn, guideBtn);
        content.getChildren().addAll(header, desc, options);

        pane.setContent(content);
        pane.getButtonTypes().add(ButtonType.CLOSE);

        Button closeBtn = (Button) pane.lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setText("Dismiss & Explore");
            closeBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #94a3b8; -fx-font-size: 11px;");
        }

        dialog.showAndWait();
    }

    // --------------------------------------------------------------------------
    // 6. View 6: 📖 Quick User Guide Workspace
    // --------------------------------------------------------------------------
    private VBox createHelpView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Quick User Guide & System Reference",
                "Complete step-by-step instructions, device specifications, relay policies, and error recovery"));

        // Top Action Bar with prominent Return to Dashboard button
        HBox topActionBar = new HBox(12);
        topActionBar.setAlignment(Pos.CENTER_LEFT);
        topActionBar.getStyleClass().add("guide-action-bar");

        Button returnHomeTop = new Button("🏠 Return to Dashboard (Main Screen)");
        returnHomeTop.getStyleClass().add("btn-cyan");
        returnHomeTop.setStyle("-fx-font-weight: bold; -fx-padding: 7 14;");
        returnHomeTop.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(returnHomeTop, "Return to high-level Emergency Command Dashboard");

        Button toDispatchBtn = new Button("🚨 Go to Emergency Dispatch");
        toDispatchBtn.getStyleClass().add("btn-dispatch");
        toDispatchBtn.setStyle("-fx-padding: 7 14;");
        toDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));

        Button toNetworkBtn = new Button("🗺️ Network & Devices");
        toNetworkBtn.getStyleClass().add("btn-ghost");
        toNetworkBtn.setStyle("-fx-padding: 7 14;");
        toNetworkBtn.setOnAction(e -> showView(networkScrollPane, navNetworkBtn));

        Button toHistoryBtn = new Button("📋 Emergency History");
        toHistoryBtn.getStyleClass().add("btn-ghost");
        toHistoryBtn.setStyle("-fx-padding: 7 14;");
        toHistoryBtn.setOnAction(e -> showView(historyScrollPane, navHistoryBtn));

        Button toSettingsBtn = new Button("⚙️ Settings");
        toSettingsBtn.getStyleClass().add("btn-ghost");
        toSettingsBtn.setStyle("-fx-padding: 7 14;");
        toSettingsBtn.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));

        Region topSpacer = new Region();
        HBox.setHgrow(topSpacer, javafx.scene.layout.Priority.ALWAYS);

        Button tutBtn = new Button("🎓 Interactive Tutorial");
        tutBtn.getStyleClass().add("btn-blue");
        tutBtn.setStyle("-fx-padding: 7 14;");
        tutBtn.setOnAction(e -> {
            tutorialManager.startFromStep1();
            applyTutorialStep();
        });

        topActionBar.getChildren().addAll(returnHomeTop, toDispatchBtn, toNetworkBtn, toHistoryBtn, toSettingsBtn, topSpacer, tutBtn);
        view.getChildren().add(topActionBar);

        // Section 1: Basic Workflow (6 Detailed Steps)
        VBox workflowCard = createCard("🚀 Basic Operational Workflow (6 Simple Steps)",
                "Follow these core steps to build the mesh, manage device power, transmit alerts, and review routing");

        VBox stepsBox = new VBox(10);
        stepsBox.getChildren().addAll(
                createGuideStepItem(1, "Adding Devices",
                        "Go to 'Network & Devices' workspace -> 'Add Virtual Device' form. Specify a unique ID (e.g., DEV-4) and descriptive name, select the device role (Student Phone, Security Post, or Medical Center), and click 'Add Device to Network'. The new node will appear on the interactive canvas."),
                createGuideStepItem(2, "Connecting Devices",
                        "In 'Network & Devices' -> 'Connect Nodes' form, select Device A and Device B from the dropdowns and click 'Connect Wireless Link'. This creates a bidirectional wireless link between the two nodes. Devices can only route packets across connected links."),
                createGuideStepItem(3, "Setting Online / Offline Status",
                        "In 'Network & Devices', click on any device on the visual canvas or table. Click 'Take OFFLINE' or 'Bring ONLINE' to toggle its operational status. You can also simulate battery drain or click 'Recharge to 100%' to restore depleted devices."),
                createGuideStepItem(4, "Sending an Emergency Message",
                        "Go to 'Emergency Dispatch'. Select the 'Origin Sender', 'Target Recipient', and desired 'Priority Level' (LOW, NORMAL, HIGH, or CRITICAL). Type emergency details and click 'Dispatch Emergency Alert'."),
                createGuideStepItem(5, "Viewing the Route & Step-by-Step Replay",
                        "Upon transmission, the Breadth-First Search (BFS) shortest path lights up in radiant neon green on the visual topology canvas with hop numbers. Use the Replay Controller (▶ Play, ⏸ Pause, ⏭ Step) to watch the message packet animate along the path."),
                createGuideStepItem(6, "Checking Emergency History",
                        "Switch to 'Emergency History' to inspect the complete session audit log. Filter by status, search by device name, view detailed hop sequences, check delivery metrics, or click 'Export Report' to copy a summary report.")
        );
        workflowCard.getChildren().add(stepsBox);
        view.getChildren().add(workflowCard);

        // Section 2: What Happens When a Device is Offline or No Route is Available
        VBox diagnosticCard = createCard("⚠️ What Happens When a Device is Offline or No Route is Available?",
                "Detailed mechanics and recovery solutions for offline nodes and unreachable destinations");

        VBox diagBox = new VBox(10);
        diagBox.getChildren().addAll(
                createHelpItem("🔴 What Happens When a Device is OFFLINE?",
                        "• Unavailable Node: An offline device (or one with 0% battery) cannot transmit, receive, or relay messages.\n" +
                        "• Sender/Recipient Offline: If the chosen sender or recipient is offline, dispatch fails immediately with a descriptive error notification.\n" +
                        "• Intermediate Relay Offline: When an intermediate node is offline, the BFS pathfinding algorithm treats it as non-existent and automatically attempts to discover an alternate path through other active nodes.\n" +
                        "• Severed Path: If all redundant routes around the offline node are broken, the transmission fails.\n" +
                        "• Solution: Select the offline device in 'Network & Devices' and click 'Bring ONLINE' or 'Recharge to 100%'."),
                createHelpItem("❌ What Happens When NO ROUTE is Available?",
                        "• Graph Partition: If there is no continuous chain of wireless links connecting sender and recipient, BFS determines that the destination is unreachable.\n" +
                        "• Priority Authorization Restriction: Student Phones cannot relay CRITICAL life-safety alerts. If the only available physical path between sender and recipient traverses a Student Phone, the alert cannot be forwarded.\n" +
                        "• Simulation Outcome: Status is marked as FAILED, an explanation detailing the partition or relay block is displayed, and the failure is logged to Emergency History.\n" +
                        "• Solution: Add missing wireless links between disconnected clusters in 'Network & Devices', or reduce message priority from CRITICAL to HIGH if routing through handhelds.")
        );
        diagnosticCard.getChildren().add(diagBox);
        view.getChildren().add(diagnosticCard);

        // Section 3: Reference Cards (Device Types, Priorities, Power)
        HBox refRow = new HBox(16);
        HBox.setHgrow(refRow, javafx.scene.layout.Priority.ALWAYS);

        // Device Roles
        VBox devCard = createCard("📱 Device Classifications & Relay Rules", "Capabilities and relay authorization policies");
        devCard.setPrefWidth(580);
        HBox.setHgrow(devCard, javafx.scene.layout.Priority.ALWAYS);
        VBox devBox = new VBox(8);
        devBox.getChildren().addAll(
                createHelpItem("📱 Student Phone (Handheld)",
                        "Mobile battery-operated handheld node. Relays LOW, NORMAL, and HIGH priority alerts. Blocked from relaying CRITICAL life-safety alerts."),
                createHelpItem("🛡️ Security Station (Fixed Post)",
                        "Stationary security post with backup power. Fully authorized to relay all alerts, including CRITICAL life-safety messages."),
                createHelpItem("🏥 Medical Center (Triage Center)",
                        "Fixed high-capacity healthcare facility. Priority destination for medical aid alerts and full-priority backbone relay.")
        );
        devCard.getChildren().add(devBox);

        // Priority Levels & Power Rules
        VBox prioCard = createCard("🚨 Priorities & Battery Consumption", "Traffic handling urgency and energy constraints");
        prioCard.setPrefWidth(580);
        HBox.setHgrow(prioCard, javafx.scene.layout.Priority.ALWAYS);
        VBox prioBox = new VBox(8);
        prioBox.getChildren().addAll(
                createHelpItem("🟢 LOW / 🔵 NORMAL Priority",
                        "Routine check-ins (LOW) and standard situational awareness traffic (NORMAL). Forwarded across all operational nodes."),
                createHelpItem("🟠 HIGH / 🔴 CRITICAL Priority",
                        "Urgent aid (HIGH, relayed by all) and immediate life-safety alerts (CRITICAL, restricted to Security and Medical stations)."),
                createHelpItem("⚡ Battery Drain Constraints",
                        "Participating in a transmission consumes battery per hop (configurable in Settings, default 2.0%). At ≤ 20%, nodes enter LOW_BATTERY. At 0%, nodes shut down completely until recharged.")
        );
        prioCard.getChildren().add(prioBox);

        refRow.getChildren().addAll(devCard, prioCard);
        view.getChildren().add(refRow);

        // Bottom Action Bar with Return to Dashboard
        HBox bottomBar = new HBox(12);
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.getStyleClass().add("guide-action-bar");

        Button returnHomeBottom = new Button("🏠 Return to Dashboard (Main Screen)");
        returnHomeBottom.getStyleClass().add("btn-cyan");
        returnHomeBottom.setStyle("-fx-font-weight: bold; -fx-padding: 8 16;");
        returnHomeBottom.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(returnHomeBottom, "Return to main Emergency Command Dashboard");

        Button bottomDispatchBtn = new Button("🚀 Launch Emergency Dispatch");
        bottomDispatchBtn.getStyleClass().add("btn-dispatch");
        bottomDispatchBtn.setStyle("-fx-padding: 8 16;");
        bottomDispatchBtn.setOnAction(e -> showView(dispatchScrollPane, navDispatchBtn));

        Button bottomSettingsBtn = new Button("⚙️ Open Settings");
        bottomSettingsBtn.getStyleClass().add("btn-ghost");
        bottomSettingsBtn.setStyle("-fx-padding: 8 16;");
        bottomSettingsBtn.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));

        bottomBar.getChildren().addAll(returnHomeBottom, bottomDispatchBtn, bottomSettingsBtn);
        view.getChildren().add(bottomBar);

        return view;
    }

    private VBox createGuideStepItem(int stepNumber, String title, String description) {
        VBox box = new VBox(4);
        box.getStyleClass().add("guide-step-card");

        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);

        Label numBadge = new Label("STEP " + stepNumber);
        numBadge.getStyleClass().add("guide-step-num");

        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        header.getChildren().addAll(numBadge, titleLabel);

        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1; -fx-line-spacing: 2px;");
        descLabel.setWrapText(true);

        box.getChildren().addAll(header, descLabel);
        return box;
    }

    // --------------------------------------------------------------------------
    // 7. View 7: ⚙️ Settings Workspace
    // --------------------------------------------------------------------------
    private VBox createSettingsView() {
        VBox view = new VBox(16);
        view.setPadding(new Insets(18, 22, 22, 22));

        // Header
        view.getChildren().add(createSectionHeader("Application & Simulation Settings",
                "Configure simulation engine parameters, safety confirmation prompts, and user interface preferences"));

        // Feedback Notice Banner
        settingsFeedbackLabel = new Label();
        settingsFeedbackLabel.setMaxWidth(Double.MAX_VALUE);
        settingsFeedbackLabel.setVisible(false);
        settingsFeedbackLabel.setManaged(false);
        view.getChildren().add(settingsFeedbackLabel);

        AppSettings current = settingsManager.getSettings();

        // Cards Row
        HBox deck = new HBox(16);
        HBox.setHgrow(deck, javafx.scene.layout.Priority.ALWAYS);

        // Left Column: Simulation Physics
        VBox simCard = createCard("⚙️ Simulation Engine Parameters",
                "Parameters governing message propagation and energy consumption");
        simCard.setPrefWidth(580);
        HBox.setHgrow(simCard, javafx.scene.layout.Priority.ALWAYS);

        GridPane simGrid = new GridPane();
        simGrid.setHgap(14);
        simGrid.setVgap(12);

        // 1. Battery Drain Per Hop
        Label drainLbl = createFormLabel("Battery Drain per Hop (%):");
        setTooltip(drainLbl, "Percentage of battery charge consumed by each node along a message route");
        SpinnerValueFactory.DoubleSpinnerValueFactory drainFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 10.0, current.getBatteryDrainPerHop(), 0.5);
        batteryDrainSpinner = new Spinner<>(drainFactory);
        batteryDrainSpinner.setEditable(true);
        batteryDrainSpinner.setPrefWidth(160);
        batteryDrainSpinner.getStyleClass().add("settings-spinner");

        Label drainDesc = new Label("Charge deducted from each device participating in an emergency delivery (Default: 2.0%).");
        drainDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        drainDesc.setWrapText(true);

        simGrid.add(drainLbl, 0, 0);
        simGrid.add(batteryDrainSpinner, 1, 0);
        simGrid.add(drainDesc, 0, 1, 2, 1);

        // 2. Default Dispatch Priority
        Label prioLbl = createFormLabel("Default Dispatch Priority:");
        setTooltip(prioLbl, "Initial priority preselected in the Emergency Dispatch console");
        defaultPriorityComboBox = new ComboBox<>();
        defaultPriorityComboBox.getItems().addAll(Priority.LOW, Priority.NORMAL, Priority.HIGH, Priority.CRITICAL);
        defaultPriorityComboBox.setValue(current.getDefaultPriority());
        defaultPriorityComboBox.setPrefWidth(180);

        Label prioDesc = new Label("Preselected priority level when opening Emergency Dispatch (Default: NORMAL).");
        prioDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        prioDesc.setWrapText(true);

        simGrid.add(prioLbl, 0, 2);
        simGrid.add(defaultPriorityComboBox, 1, 2);
        simGrid.add(prioDesc, 0, 3, 2, 1);

        // 3. Topology Replay Speed
        Label speedLbl = createFormLabel("Visual Replay Speed:");
        setTooltip(speedLbl, "Controls the packet traversal animation speed on the visual canvas");
        replaySpeedComboBox = new ComboBox<>();
        replaySpeedComboBox.getItems().addAll("0.5x (Slow Motion)", "1.0x (Normal)", "1.5x (Fast)", "2.0x (Double Speed)");
        replaySpeedComboBox.setValue(formatReplaySpeed(current.getReplaySpeed()));
        replaySpeedComboBox.setPrefWidth(180);

        Label speedDesc = new Label("Playback animation rate for the packet indicator on the network topology canvas.");
        speedDesc.setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
        speedDesc.setWrapText(true);

        simGrid.add(speedLbl, 0, 4);
        simGrid.add(replaySpeedComboBox, 1, 4);
        simGrid.add(speedDesc, 0, 5, 2, 1);

        simCard.getChildren().add(simGrid);

        // Right Column: Safety Confirmations & UI Preferences
        VBox safetyCard = createCard("🛡️ Safety Confirmations & User Interface",
                "Prevent accidental loss of topologies and customize guidance presentation");
        safetyCard.setPrefWidth(580);
        HBox.setHgrow(safetyCard, javafx.scene.layout.Priority.ALWAYS);

        VBox safetyBox = new VBox(10);

        Label confirmTitle = new Label("DATA SAFETY & CONFIRMATION PROMPTS");
        confirmTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase;");

        confirmNetworkReplacementCheck = new CheckBox("Confirm before replacing active network (JSON load & sample reset)");
        confirmNetworkReplacementCheck.setSelected(current.isConfirmNetworkReplacement());
        confirmNetworkReplacementCheck.getStyleClass().add("settings-check-box");
        setTooltip(confirmNetworkReplacementCheck, "Prompt confirmation before replacing current network nodes");

        confirmClearHistoryCheck = new CheckBox("Confirm before clearing emergency dispatch history");
        confirmClearHistoryCheck.setSelected(current.isConfirmClearHistory());
        confirmClearHistoryCheck.getStyleClass().add("settings-check-box");
        setTooltip(confirmClearHistoryCheck, "Prompt confirmation before permanently wiping session dispatch history");

        confirmClearNetworkCheck = new CheckBox("Confirm before clearing entire network graph");
        confirmClearNetworkCheck.setSelected(current.isConfirmClearNetwork());
        confirmClearNetworkCheck.getStyleClass().add("settings-check-box");
        setTooltip(confirmClearNetworkCheck, "Prompt confirmation before removing all nodes and links");

        Label uiTitle = new Label("USER INTERFACE & GUIDANCE PREFERENCES");
        uiTitle.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase; -fx-padding: 8 0 0 0;");

        showTutorialBannerCheck = new CheckBox("Display top interactive onboarding tutorial banner");
        showTutorialBannerCheck.setSelected(current.isShowTutorialBanner());
        showTutorialBannerCheck.getStyleClass().add("settings-check-box");
        setTooltip(showTutorialBannerCheck, "Show or hide the guided onboarding banner at the top of the application");

        autoSelectRecipientCheck = new CheckBox("Auto-suggest valid recipient upon selecting sender in Dispatch");
        autoSelectRecipientCheck.setSelected(current.isAutoSelectRecipient());
        autoSelectRecipientCheck.getStyleClass().add("settings-check-box");
        setTooltip(autoSelectRecipientCheck, "Automatically chooses an operational recipient device when an origin is picked");

        safetyBox.getChildren().addAll(
                confirmTitle,
                confirmNetworkReplacementCheck,
                confirmClearHistoryCheck,
                confirmClearNetworkCheck,
                uiTitle,
                showTutorialBannerCheck,
                autoSelectRecipientCheck
        );
        safetyCard.getChildren().add(safetyBox);

        deck.getChildren().addAll(simCard, safetyCard);
        view.getChildren().add(deck);

        // Action Buttons Bar
        HBox actionStrip = new HBox(12);
        actionStrip.setAlignment(Pos.CENTER_LEFT);
        actionStrip.getStyleClass().add("guide-action-bar");

        Button saveBtn = new Button("💾 Save Settings");
        saveBtn.getStyleClass().add("btn-cyan");
        saveBtn.setStyle("-fx-font-weight: bold; -fx-padding: 8 18;");
        saveBtn.setOnAction(e -> handleSaveSettings());
        setTooltip(saveBtn, "Apply changes to the simulation engine and persist to resqmesh-settings.json");

        Button resetBtn = new Button("↺ Reset to Defaults");
        resetBtn.getStyleClass().add("btn-reset");
        resetBtn.setStyle("-fx-padding: 8 16;");
        resetBtn.setOnAction(e -> handleResetSettingsWithConfirmation());
        setTooltip(resetBtn, "Restore all settings to standard factory defaults with confirmation");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button returnHomeBtn = new Button("🏠 Return to Dashboard");
        returnHomeBtn.getStyleClass().add("btn-ghost");
        returnHomeBtn.setStyle("-fx-padding: 8 16;");
        returnHomeBtn.setOnAction(e -> showView(dashboardScrollPane, navDashboardBtn));
        setTooltip(returnHomeBtn, "Return to main Dashboard screen");

        actionStrip.getChildren().addAll(saveBtn, resetBtn, spacer, returnHomeBtn);
        view.getChildren().add(actionStrip);

        return view;
    }

    private void handleSaveSettings() {
        try {
            AppSettings s = settingsManager.getSettings();
            if (batteryDrainSpinner != null && batteryDrainSpinner.getValue() != null) {
                s.setBatteryDrainPerHop(batteryDrainSpinner.getValue());
            }
            if (defaultPriorityComboBox != null && defaultPriorityComboBox.getValue() != null) {
                s.setDefaultPriority(defaultPriorityComboBox.getValue());
            }
            if (replaySpeedComboBox != null && replaySpeedComboBox.getValue() != null) {
                s.setReplaySpeed(parseReplaySpeed(replaySpeedComboBox.getValue()));
            }
            if (confirmNetworkReplacementCheck != null) {
                s.setConfirmNetworkReplacement(confirmNetworkReplacementCheck.isSelected());
            }
            if (confirmClearHistoryCheck != null) {
                s.setConfirmClearHistory(confirmClearHistoryCheck.isSelected());
            }
            if (confirmClearNetworkCheck != null) {
                s.setConfirmClearNetwork(confirmClearNetworkCheck.isSelected());
            }
            if (showTutorialBannerCheck != null) {
                s.setShowTutorialBanner(showTutorialBannerCheck.isSelected());
            }
            if (autoSelectRecipientCheck != null) {
                s.setAutoSelectRecipient(autoSelectRecipientCheck.isSelected());
            }

            settingsManager.saveSettings(s);
            applySettingsToApp(s);

            showSettingsFeedback("✅ Settings saved successfully! Parameters applied to simulation engine and persisted to disk.", true);
            log("SETTINGS", String.format("Settings saved: BatteryDrain=%.1f%%, DefaultPriority=%s, ReplaySpeed=%.1fx",
                    s.getBatteryDrainPerHop(), s.getDefaultPriority(), s.getReplaySpeed()));
        } catch (Exception ex) {
            showSettingsFeedback("❌ Failed to save settings: " + ex.getMessage(), false);
            log("ERROR", "Failed to save application settings: " + ex.getMessage());
        }
    }

    private void handleResetSettingsWithConfirmation() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.initOwner(primaryStage);
        confirm.setTitle("Reset Settings to Defaults");
        confirm.setHeaderText("Restore All Settings to Factory Defaults?");
        confirm.setContentText(
                "Are you sure you want to reset all settings to their default values?\n\n" +
                "• Battery Drain: 2.0% per hop\n" +
                "• Default Priority: NORMAL\n" +
                "• Replay Speed: 1.0x (Normal)\n" +
                "• All Safety Confirmations: Enabled\n" +
                "• Tutorial Banner: Visible\n\n" +
                "This will immediately persist defaults to resqmesh-settings.json.");
        ButtonType resetType = new ButtonType("Reset to Defaults", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(resetType, cancelType);
        applyDialogStyles(confirm);

        Optional<ButtonType> choice = confirm.showAndWait();
        if (choice.isPresent() && choice.get() == resetType) {
            try {
                AppSettings defs = settingsManager.resetToDefaults();
                populateSettingsControls(defs);
                applySettingsToApp(defs);
                showSettingsFeedback("↺ Settings have been reset to factory defaults and saved.", true);
                log("SETTINGS", "Application settings reset to default values.");
            } catch (Exception ex) {
                showSettingsFeedback("❌ Failed to reset settings: " + ex.getMessage(), false);
            }
        }
    }

    private void populateSettingsControls(AppSettings s) {
        if (s == null) return;
        if (batteryDrainSpinner != null) {
            batteryDrainSpinner.getValueFactory().setValue(s.getBatteryDrainPerHop());
        }
        if (defaultPriorityComboBox != null) {
            defaultPriorityComboBox.setValue(s.getDefaultPriority());
        }
        if (replaySpeedComboBox != null) {
            replaySpeedComboBox.setValue(formatReplaySpeed(s.getReplaySpeed()));
        }
        if (confirmNetworkReplacementCheck != null) {
            confirmNetworkReplacementCheck.setSelected(s.isConfirmNetworkReplacement());
        }
        if (confirmClearHistoryCheck != null) {
            confirmClearHistoryCheck.setSelected(s.isConfirmClearHistory());
        }
        if (confirmClearNetworkCheck != null) {
            confirmClearNetworkCheck.setSelected(s.isConfirmClearNetwork());
        }
        if (showTutorialBannerCheck != null) {
            showTutorialBannerCheck.setSelected(s.isShowTutorialBanner());
        }
        if (autoSelectRecipientCheck != null) {
            autoSelectRecipientCheck.setSelected(s.isAutoSelectRecipient());
        }
    }

    private void applySettingsToApp(AppSettings s) {
        if (s == null) return;
        if (engine != null) {
            engine.setBatteryCostPerTransmission(s.getBatteryDrainPerHop());
        }
        if (topologyPane != null) {
            topologyPane.setReplaySpeed(s.getReplaySpeed());
        }
        if (energyRuleLabel != null) {
            energyRuleLabel.setText(String.format("Energy Drain: -%.1f%% / hop", s.getBatteryDrainPerHop()));
        }
        if (tutorialBanner != null) {
            tutorialBanner.setVisible(s.isShowTutorialBanner());
            tutorialBanner.setManaged(s.isShowTutorialBanner());
        }
    }

    private void showSettingsFeedback(String message, boolean success) {
        if (settingsFeedbackLabel != null) {
            settingsFeedbackLabel.setText(message);
            settingsFeedbackLabel.setVisible(true);
            settingsFeedbackLabel.setManaged(true);
            if (success) {
                settingsFeedbackLabel.setStyle(
                        "-fx-background-color: #064e3b; -fx-text-fill: #6ee7b7; -fx-font-size: 12px; -fx-font-weight: 700; " +
                        "-fx-padding: 10px 14px; -fx-background-radius: 6px; -fx-border-color: #059669; -fx-border-radius: 6px;");
            } else {
                settingsFeedbackLabel.setStyle(
                        "-fx-background-color: #450a0a; -fx-text-fill: #fca5a5; -fx-font-size: 12px; -fx-font-weight: 700; " +
                        "-fx-padding: 10px 14px; -fx-background-radius: 6px; -fx-border-color: #b91c1c; -fx-border-radius: 6px;");
            }
        }
    }

    private String formatReplaySpeed(double speed) {
        if (Math.abs(speed - 0.5) < 0.05) return "0.5x (Slow Motion)";
        if (Math.abs(speed - 1.5) < 0.05) return "1.5x (Fast)";
        if (Math.abs(speed - 2.0) < 0.05) return "2.0x (Double Speed)";
        return "1.0x (Normal)";
    }

    private double parseReplaySpeed(String text) {
        if (text != null) {
            if (text.startsWith("0.5x")) return 0.5;
            if (text.startsWith("1.5x")) return 1.5;
            if (text.startsWith("2.0x")) return 2.0;
        }
        return 1.0;
    }

    private VBox createHelpItem(String title, String body) {
        VBox box = new VBox(3);
        box.setStyle("-fx-background-color: #0b1324; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-border-color: #192742; -fx-border-radius: 6px;");

        Label t = new Label(title);
        t.setStyle("-fx-font-size: 12px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        Label b = new Label(body);
        b.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-line-spacing: 2px;");
        b.setWrapText(true);

        box.getChildren().addAll(t, b);
        return box;
    }

    private ComboBox<CommunicationDevice> createDeviceComboBox() {
        ComboBox<CommunicationDevice> box = new ComboBox<>(deviceObservableList);
        box.setPromptText("-- Select Device --");
        box.setConverter(new StringConverter<>() {
            @Override
            public String toString(CommunicationDevice d) {
                if (d == null) return "-- Select Device --";
                String icon = (d instanceof SecurityStation) ? "🛡️" : (d instanceof MedicalStation) ? "🏥" : "📱";
                String state = (d.getStatus() == DeviceStatus.OFFLINE) ? "🔴 OFFLINE" : String.format("⚡ %.0f%%", d.getBatteryLevel());
                return icon + " " + d.getName() + " (" + state + ")";
            }

            @Override
            public CommunicationDevice fromString(String string) {
                return null;
            }
        });

        box.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(CommunicationDevice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = (item instanceof SecurityStation) ? "🛡️" : (item instanceof MedicalStation) ? "🏥" : "📱";
                    String role = (item instanceof SecurityStation) ? "Security" : (item instanceof MedicalStation) ? "Medical" : "Phone";
                    if (item.getStatus() == DeviceStatus.OFFLINE) {
                        setText(icon + " " + item.getName() + " [" + role + " - 🔴 OFFLINE]");
                        setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px;");
                    } else if (item.getBatteryLevel() <= CommunicationDevice.LOW_BATTERY_THRESHOLD) {
                        setText(String.format("%s %s [%s - ⚠️ %.0f%%]", icon, item.getName(), role, item.getBatteryLevel()));
                        setStyle("-fx-text-fill: #fbbf24; -fx-font-size: 11px;");
                    } else {
                        setText(String.format("%s %s [%s - ⚡ %.0f%%]", icon, item.getName(), role, item.getBatteryLevel()));
                        setStyle("-fx-text-fill: #e2e8f0; -fx-font-size: 11px;");
                    }
                }
            }
        });

        box.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(CommunicationDevice item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("-- Select Device --");
                    setStyle("-fx-text-fill: #64748b; -fx-font-size: 11px;");
                } else {
                    String icon = (item instanceof SecurityStation) ? "🛡️" : (item instanceof MedicalStation) ? "🏥" : "📱";
                    String state = (item.getStatus() == DeviceStatus.OFFLINE) ? "🔴 OFFLINE" : String.format("⚡ %.0f%%", item.getBatteryLevel());
                    setText(icon + " " + item.getName() + " (" + state + ")");
                    if (item.getStatus() == DeviceStatus.OFFLINE) {
                        setStyle("-fx-text-fill: #f87171; -fx-font-size: 11px; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #38bdf8; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                }
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.getStyleClass().add("top-menu-bar");

        // --- File Menu ---
        Menu fileMenu = new Menu("File");

        MenuItem saveItem = new MenuItem("💾 Save Network Configuration...");
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.CONTROL_DOWN));
        saveItem.setOnAction(e -> handleSaveNetwork());

        MenuItem loadItem = new MenuItem("📂 Load Network Configuration...");
        loadItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN));
        loadItem.setOnAction(e -> handleLoadNetwork());

        MenuItem newSimItem = new MenuItem("✨ New Simulation");
        newSimItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.CONTROL_DOWN));
        newSimItem.setOnAction(e -> handleNewSimulation());

        MenuItem resetSampleItem = new MenuItem("↺ Reset Sample Mesh");
        resetSampleItem.setOnAction(e -> loadSampleNetwork());

        MenuItem clearNetworkItem = new MenuItem("✕ Clear Network Graph");
        clearNetworkItem.setOnAction(e -> handleClearNetworkWithConfirmation());

        MenuItem settingsItem = new MenuItem("⚙️ Settings...");
        settingsItem.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));

        MenuItem exitItem = new MenuItem("🚪 Exit");
        exitItem.setOnAction(e -> {
            if (primaryStage != null) {
                primaryStage.close();
            }
        });

        fileMenu.getItems().addAll(
                saveItem,
                loadItem,
                new SeparatorMenuItem(),
                newSimItem,
                resetSampleItem,
                clearNetworkItem,
                new SeparatorMenuItem(),
                settingsItem,
                new SeparatorMenuItem(),
                exitItem
        );

        // --- Network Menu ---
        Menu networkMenu = new Menu("Network");

        MenuItem viewNetItem = new MenuItem("🗺️ Go to Network & Devices");
        viewNetItem.setOnAction(e -> showView(networkScrollPane, navNetworkBtn));

        MenuItem saveNetItem = new MenuItem("💾 Save Current Topology");
        saveNetItem.setOnAction(e -> handleSaveNetwork());

        MenuItem loadNetItem = new MenuItem("📂 Load Topology from File");
        loadNetItem.setOnAction(e -> handleLoadNetwork());

        MenuItem rechargeAllItem = new MenuItem("⚡ Recharge All Nodes to 100%");
        rechargeAllItem.setOnAction(e -> handleRechargeAllNodes());

        networkMenu.getItems().addAll(
                viewNetItem,
                new SeparatorMenuItem(),
                saveNetItem,
                loadNetItem,
                new SeparatorMenuItem(),
                rechargeAllItem
        );

        // --- Settings Menu ---
        Menu settingsMenu = new Menu("Settings");

        MenuItem prefItem = new MenuItem("⚙️ Preferences & Simulation Settings...");
        prefItem.setAccelerator(new KeyCodeCombination(KeyCode.COMMA, KeyCombination.CONTROL_DOWN));
        prefItem.setOnAction(e -> showView(settingsScrollPane, navSettingsBtn));

        MenuItem resetSettingsItem = new MenuItem("↺ Reset Settings to Defaults...");
        resetSettingsItem.setOnAction(e -> handleResetSettingsWithConfirmation());

        settingsMenu.getItems().addAll(
                prefItem,
                new SeparatorMenuItem(),
                resetSettingsItem
        );

        // --- Help Menu ---
        Menu helpMenu = new Menu("Help");

        MenuItem welcomeItem = new MenuItem("👋 Welcome Overview");
        welcomeItem.setOnAction(e -> showWelcomeDialog());

        MenuItem tutorialItem = new MenuItem("🎓 Interactive Tutorial");
        tutorialItem.setOnAction(e -> {
            tutorialManager.startFromStep1();
            applyTutorialStep();
        });

        MenuItem guideItem = new MenuItem("📖 Quick User Guide");
        guideItem.setOnAction(e -> showView(helpScrollPane, navHelpBtn));

        MenuItem aboutItem = new MenuItem("ℹ️ About ResQMesh");
        aboutItem.setOnAction(e -> showAboutDialog());

        helpMenu.getItems().addAll(
                welcomeItem,
                tutorialItem,
                guideItem,
                new SeparatorMenuItem(),
                aboutItem
        );

        menuBar.getMenus().addAll(fileMenu, networkMenu, settingsMenu, helpMenu);
        return menuBar;
    }

    private void handleClearNetworkWithConfirmation() {
        if (graph.getDeviceCount() == 0) {
            showAlert("Network Empty", "The network is already empty.");
            return;
        }

        if (settingsManager.getSettings().isConfirmClearNetwork()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.initOwner(primaryStage);
            confirm.setTitle("Clear Network");
            confirm.setHeaderText("Remove All Devices and Links?");
            confirm.setContentText(String.format(
                    "Are you sure you want to completely clear the network graph?\n\n" +
                    "This will remove all %d devices and %d connections.\nAny unsaved changes will be lost.",
                    graph.getDeviceCount(), graph.getTotalLinkCount() / 2));
            ButtonType clearBtnType = new ButtonType("Clear Network", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            confirm.getButtonTypes().setAll(clearBtnType, cancelBtnType);
            applyDialogStyles(confirm);

            Optional<ButtonType> choice = confirm.showAndWait();
            if (choice.isEmpty() || choice.get() != clearBtnType) {
                return;
            }
        }

        graph.clear();
        if (topologyPane != null) {
            topologyPane.resetReplay();
            topologyPane.clearRouteHighlight();
        }
        refreshUI();
        selectedDeviceComboBox.setValue(null);
        senderComboBox.setValue(null);
        recipientComboBox.setValue(null);
        linkDeviceAComboBox.setValue(null);
        linkDeviceBComboBox.setValue(null);
        log("TOPOLOGY", "Network graph cleared. All devices and links removed.");
    }

    private void handleRechargeAllNodes() {
        for (CommunicationDevice dev : graph.getAllDevices()) {
            dev.recharge(100.0);
        }
        refreshUI();
        if (selectedDeviceComboBox.getValue() != null) {
            updateToggleState(selectedDeviceComboBox.getValue());
        }
        log("BATTERY", "Recharged all registered nodes to 100% battery (ACTIVE).");
        showSuccessAlert("Batteries Recharged", "All " + graph.getDeviceCount() + " devices have been restored to 100% battery (ACTIVE).");
    }

    private void updateDeviceIdCounterAfterLoad() {
        int maxId = 0;
        for (CommunicationDevice dev : graph.getAllDevices()) {
            String id = dev.getId();
            if (id != null) {
                String digits = id.replaceAll("\\D+", "");
                if (!digits.isEmpty()) {
                    try {
                        int num = Integer.parseInt(digits);
                        if (num > maxId) {
                            maxId = num;
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        deviceIdCounter = Math.max(deviceIdCounter, maxId + 1);
    }

    private void showAboutDialog() {
        Alert about = new Alert(Alert.AlertType.INFORMATION);
        about.initOwner(primaryStage);
        about.setTitle("About ResQMesh");
        about.setHeaderText("ResQMesh – Emergency Communication Dashboard");
        about.setContentText(
                "Version 1.0 (Simulation Edition)\n\n" +
                "ResQMesh is an offline emergency communication simulator modeling\n" +
                "decentralized peer-to-peer mesh networking for disaster relief.\n\n" +
                "Key Capabilities:\n" +
                "• Breadcrumb Breadth-First Search (BFS) shortest-path discovery\n" +
                "• Priority relay policies (Low, Normal, High, Critical)\n" +
                "• Battery energy constraints (-2.0% drain / hop)\n" +
                "• Save and Load network topologies in JSON format\n" +
                "• Non-destructive simulation replay and interactive onboarding\n"
        );
        applyDialogStyles(about);
        about.showAndWait();
    }

    private void applyDialogStyles(Dialog<?> dialog) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStyleClass().add("themed-dialog");
        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            pane.getStylesheets().add(cssResource.toExternalForm());
        }
    }

    private void showSuccessAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("Success");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    private void showErrorAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initOwner(primaryStage);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(primaryStage);
        alert.setTitle("ResQMesh Notice");
        alert.setHeaderText(header);
        alert.setContentText(content);
        applyDialogStyles(alert);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
