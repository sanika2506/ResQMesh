package com.resqmesh.ui;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.CommunicationLink;
import com.resqmesh.model.DeviceStatus;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.SecurityStation;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.simulation.timeline.ReplayController;
import com.resqmesh.simulation.timeline.ReplayState;
import com.resqmesh.simulation.timeline.SimulationEvent;
import com.resqmesh.simulation.timeline.SimulationEventType;
import com.resqmesh.simulation.timeline.SimulationRecord;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

import java.util.*;
import java.util.function.Consumer;

/**
 * Interactive Visual Network Topology Pane for ResQMesh.
 * Step 8: Visual Network Graph Rendering & Route Highlighting.
 * Step 11: Emergency Simulation Replay & Event Timeline Animation.
 * 
 * Features:
 * - Renders all registered devices as interactive draggable nodes.
 * - Renders all active bidirectional communication links.
 * - Distinct visual styles for ONLINE (green/cyan glow) vs OFFLINE (crimson/dimmed).
 * - Real-time BFS route illumination with glowing links and hop sequence numbers.
 * - Message packet indicator gliding between nodes along BFS routes with Play, Pause, Resume, Reset, and Speed controls.
 * - On-node click inspector displaying device telemetry.
 * - Mouse drag-and-drop node repositioning with dynamic link tracking.
 */
public class NetworkTopologyPane extends StackPane {

    private final NetworkGraph graph;
    private final Pane canvasPane;
    private final Pane linkLayer;
    private final Pane nodeLayer;
    private final Pane replayLayer;
    private final VBox inspectorCard;

    // Node and Link tracking
    private final Map<CommunicationDevice, NodeVisual> deviceNodeMap = new HashMap<>();
    private final List<LinkVisual> linkVisuals = new ArrayList<>();
    private final Map<CommunicationDevice, Point2D> manualPositions = new HashMap<>();

    // State
    private CommunicationDevice selectedDevice;
    private List<CommunicationDevice> activeRoute = Collections.emptyList();
    private Consumer<CommunicationDevice> deviceSelectionListener;

    // Step 11: Replay Engine & Packet Indicator
    private final MessageIndicatorVisual messageIndicator;
    private final ReplayController replayController = new ReplayController();
    private SimulationRecord activeReplayRecord;
    private Timeline activeAnimationTimeline;
    private double currentReplaySpeed = 1.0;

    // Replay HUD Elements
    private HBox replayBar;
    private Label replayStatusLabel;
    private Button playReplayBtn;
    private Button pauseReplayBtn;
    private Button resetReplayBtn;
    private ComboBox<String> speedComboBox;

    // Inspector HUD elements
    private Label inspectorTitleLabel;
    private Label inspectorIdLabel;
    private Label inspectorTypeLabel;
    private Label inspectorBatteryLabel;
    private Label inspectorStatusLabel;
    private Label inspectorPeersLabel;

    public static class Point2D {
        public double x;
        public double y;

        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    public NetworkTopologyPane(NetworkGraph graph) {
        this.graph = graph;

        // Container Styling
        setStyle("-fx-background-color: #070c17; -fx-background-radius: 10px; -fx-border-color: #192742; -fx-border-radius: 10px;");
        setPrefHeight(340);
        setMinHeight(280);

        // Canvas pane for layers
        canvasPane = new Pane();
        linkLayer = new Pane();
        nodeLayer = new Pane();
        replayLayer = new Pane();
        linkLayer.setPickOnBounds(false);
        nodeLayer.setPickOnBounds(false);
        replayLayer.setPickOnBounds(false);

        // Message packet indicator sits on replayLayer above nodes
        messageIndicator = new MessageIndicatorVisual();
        replayLayer.getChildren().add(messageIndicator);

        canvasPane.getChildren().addAll(linkLayer, nodeLayer, replayLayer);

        // Mini HUD Inspector Card (top-right overlay)
        inspectorCard = createInspectorCard();
        StackPane.setAlignment(inspectorCard, Pos.TOP_RIGHT);
        StackPane.setMargin(inspectorCard, new Insets(12));

        // Legend Badge (top-left overlay)
        HBox legendBox = createLegendBox();
        StackPane.setAlignment(legendBox, Pos.TOP_LEFT);
        StackPane.setMargin(legendBox, new Insets(12));

        // Replay HUD Controls (bottom-center overlay)
        replayBar = createReplayBar();
        StackPane.setAlignment(replayBar, Pos.BOTTOM_CENTER);
        StackPane.setMargin(replayBar, new Insets(12));

        getChildren().addAll(canvasPane, legendBox, inspectorCard, replayBar);

        // Reposition nodes smoothly when pane resizes
        widthProperty().addListener((obs, oldW, newW) -> relayoutNodes());
        heightProperty().addListener((obs, oldH, newH) -> relayoutNodes());
    }

    public void setOnDeviceSelected(Consumer<CommunicationDevice> listener) {
        this.deviceSelectionListener = listener;
    }

    /**
     * Completely refreshes the visual network graph based on NetworkGraph state.
     */
    public void refresh() {
        deviceNodeMap.clear();
        linkVisuals.clear();
        linkLayer.getChildren().clear();
        nodeLayer.getChildren().clear();
        replayLayer.getChildren().clear();
        replayLayer.getChildren().add(messageIndicator);

        List<CommunicationDevice> devices = new ArrayList<>(graph.getAllDevices());
        if (devices.isEmpty()) {
            inspectorCard.setVisible(false);
            messageIndicator.setVisible(false);
            return;
        }

        // 1. Calculate positions for devices
        calculateInitialPositions(devices);

        // 2. Create Node Visuals
        for (CommunicationDevice dev : devices) {
            Point2D pt = manualPositions.get(dev);
            NodeVisual visual = new NodeVisual(dev, pt.x, pt.y);
            deviceNodeMap.put(dev, visual);
            nodeLayer.getChildren().add(visual);

            // Interaction
            visual.setOnMouseClicked(e -> selectDevice(dev));
            setupDragAndDrop(visual, dev);
        }

        // 3. Create Link Visuals (prevent duplicates since graph is bidirectional)
        Set<String> renderedPairs = new HashSet<>();
        for (CommunicationDevice dev : devices) {
            for (CommunicationLink link : graph.getLinks(dev)) {
                CommunicationDevice dest = link.getDestination();
                String keyA = dev.getId() + "->" + dest.getId();
                String keyB = dest.getId() + "->" + dev.getId();

                if (!renderedPairs.contains(keyA) && !renderedPairs.contains(keyB)) {
                    renderedPairs.add(keyA);
                    renderedPairs.add(keyB);

                    NodeVisual srcVis = deviceNodeMap.get(dev);
                    NodeVisual dstVis = deviceNodeMap.get(dest);

                    if (srcVis != null && dstVis != null) {
                        LinkVisual linkVis = new LinkVisual(dev, dest, srcVis, dstVis);
                        linkVisuals.add(linkVis);
                        linkLayer.getChildren().add(linkVis.getLine());
                    }
                }
            }
        }

        // Reapply active selections or route highlights
        if (selectedDevice != null && graph.getAllDevices().contains(selectedDevice)) {
            selectDevice(selectedDevice);
        } else if (!devices.isEmpty()) {
            selectDevice(devices.get(0));
        }

        if (activeRoute != null && !activeRoute.isEmpty()) {
            if (graph.getAllDevices().containsAll(activeRoute)) {
                highlightRoute(activeRoute);
            } else {
                clearRouteHighlight();
            }
        }
    }

    /**
     * Highlights the discovered BFS route on nodes and links.
     */
    public void highlightRoute(List<CommunicationDevice> route) {
        this.activeRoute = (route != null) ? new ArrayList<>(route) : Collections.emptyList();

        // 1. Reset all links to default
        for (LinkVisual lv : linkVisuals) {
            lv.setHighlighted(false);
        }

        // 2. Reset all nodes
        for (NodeVisual nv : deviceNodeMap.values()) {
            nv.setRouteHopIndex(-1);
        }

        if (activeRoute.isEmpty()) return;

        // 3. Highlight route nodes with hop numbers
        for (int i = 0; i < activeRoute.size(); i++) {
            CommunicationDevice dev = activeRoute.get(i);
            NodeVisual nv = deviceNodeMap.get(dev);
            if (nv != null) {
                nv.setRouteHopIndex(i + 1);
            }
        }

        // 4. Highlight transmission links between sequential hops
        for (int i = 0; i < activeRoute.size() - 1; i++) {
            CommunicationDevice a = activeRoute.get(i);
            CommunicationDevice b = activeRoute.get(i + 1);

            for (LinkVisual lv : linkVisuals) {
                if (lv.connects(a, b)) {
                    lv.setHighlighted(true);
                }
            }
        }
    }

    public void clearRouteHighlight() {
        this.activeRoute = Collections.emptyList();
        for (LinkVisual lv : linkVisuals) {
            lv.setHighlighted(false);
        }
        for (NodeVisual nv : deviceNodeMap.values()) {
            nv.setRouteHopIndex(-1);
        }
    }

    // --------------------------------------------------------------------------
    // Replay Engine & Visual Timeline Animation (Step 11)
    // --------------------------------------------------------------------------
    public void loadReplay(SimulationRecord record) {
        this.activeReplayRecord = record;
        if (activeAnimationTimeline != null) {
            activeAnimationTimeline.stop();
        }

        if (record == null) {
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("No simulation loaded");
            }
            messageIndicator.setVisible(false);
            return;
        }

        replayController.loadTimeline(record.getTimeline());
        clearRouteHighlight();

        if (record.isDelivered() && !record.getResult().getRoute().isEmpty()) {
            CommunicationDevice startDev = record.getResult().getRoute().get(0);
            NodeVisual startVis = deviceNodeMap.get(startDev);
            if (startVis != null) {
                messageIndicator.setFailureMode(false);
                messageIndicator.setPosition(startVis.getX(), startVis.getY());
                messageIndicator.setVisible(true);
            }
            int hops = Math.max(0, record.getResult().getRoute().size() - 1);
            if (replayStatusLabel != null) {
                replayStatusLabel.setText(String.format("Loaded: %s (%d hop%s). Ready to play.",
                        record.getId(), hops, hops == 1 ? "" : "s"));
            }
        } else {
            CommunicationDevice sender = record.getMessage().getSender();
            NodeVisual senderVis = deviceNodeMap.get(sender);
            if (senderVis != null) {
                messageIndicator.setFailureMode(true);
                messageIndicator.setPosition(senderVis.getX(), senderVis.getY());
                messageIndicator.setVisible(true);
            }
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("Loaded: " + record.getId() + " [FAILED]. Ready to review.");
            }
        }
        if (playReplayBtn != null) {
            playReplayBtn.setText("▶ Play");
        }
    }

    public void playReplay() {
        if (activeReplayRecord == null) {
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("No simulation loaded to replay");
            }
            return;
        }

        if (activeAnimationTimeline != null && activeAnimationTimeline.getStatus() == Animation.Status.PAUSED) {
            activeAnimationTimeline.play();
            replayController.resume();
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("Resumed replay...");
            }
            if (playReplayBtn != null) {
                playReplayBtn.setText("▶ Playing");
            }
            return;
        }

        if (!activeReplayRecord.isDelivered()) {
            replayController.play();
            startFailureAnimation(activeReplayRecord);
            return;
        }

        List<CommunicationDevice> route = activeReplayRecord.getResult().getRoute();
        if (route == null || route.isEmpty()) {
            return;
        }

        replayController.play();
        startRouteAnimation(route);
    }

    private void startRouteAnimation(List<CommunicationDevice> route) {
        if (activeAnimationTimeline != null) {
            activeAnimationTimeline.stop();
        }

        clearRouteHighlight();
        messageIndicator.setFailureMode(false);

        NodeVisual startVis = deviceNodeMap.get(route.get(0));
        if (startVis == null) return;

        messageIndicator.setPosition(startVis.getX(), startVis.getY());
        messageIndicator.setVisible(true);
        startVis.setRouteHopIndex(1);

        activeAnimationTimeline = new Timeline();
        if (playReplayBtn != null) {
            playReplayBtn.setText("▶ Playing");
        }

        double baseHopDurationMs = 800.0;
        double currentDelayMs = 200.0;

        for (int i = 0; i < route.size() - 1; i++) {
            final int hopIndex = i;
            CommunicationDevice fromDev = route.get(hopIndex);
            CommunicationDevice toDev = route.get(hopIndex + 1);
            NodeVisual fromVis = deviceNodeMap.get(fromDev);
            NodeVisual toVis = deviceNodeMap.get(toDev);
            if (fromVis == null || toVis == null) continue;

            activeAnimationTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(currentDelayMs),
                    e -> {
                        for (LinkVisual lv : linkVisuals) {
                            if (lv.connects(fromDev, toDev)) {
                                lv.setHighlighted(true);
                            }
                        }
                        if (replayStatusLabel != null) {
                            replayStatusLabel.setText(String.format("Relaying: %s ──▶ %s (Hop %d/%d)",
                                    fromDev.getName(), toDev.getName(), hopIndex + 1, route.size() - 1));
                        }
                    }
            ));

            double nextDelayMs = currentDelayMs + baseHopDurationMs;

            activeAnimationTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(nextDelayMs),
                    new KeyValue(messageIndicator.layoutXProperty(), toVis.getX() - 14),
                    new KeyValue(messageIndicator.layoutYProperty(), toVis.getY() - 14)
            ));

            activeAnimationTimeline.getKeyFrames().add(new KeyFrame(
                    Duration.millis(nextDelayMs),
                    e -> {
                        toVis.setRouteHopIndex(hopIndex + 2);
                        replayController.stepNext();
                    }
            ));

            currentDelayMs = nextDelayMs + 200.0;
        }

        activeAnimationTimeline.getKeyFrames().add(new KeyFrame(
                Duration.millis(currentDelayMs),
                e -> {
                    if (replayStatusLabel != null) {
                        replayStatusLabel.setText("✔ Replay Complete: Delivered to " + route.get(route.size() - 1).getName());
                    }
                    if (playReplayBtn != null) {
                        playReplayBtn.setText("▶ Replay");
                    }
                }
        ));

        activeAnimationTimeline.setRate(currentReplaySpeed);
        activeAnimationTimeline.playFromStart();
    }

    private void startFailureAnimation(SimulationRecord record) {
        if (activeAnimationTimeline != null) {
            activeAnimationTimeline.stop();
        }
        clearRouteHighlight();

        CommunicationDevice sender = record.getMessage().getSender();
        NodeVisual senderVis = deviceNodeMap.get(sender);
        if (senderVis != null) {
            messageIndicator.setFailureMode(true);
            messageIndicator.setPosition(senderVis.getX(), senderVis.getY());
            messageIndicator.setVisible(true);
        }

        if (replayStatusLabel != null) {
            replayStatusLabel.setText("✖ FAILED: " + record.getResult().getExplanation());
        }

        activeAnimationTimeline = new Timeline(
                new KeyFrame(Duration.ZERO, new KeyValue(messageIndicator.scaleXProperty(), 1.0), new KeyValue(messageIndicator.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(250), new KeyValue(messageIndicator.scaleXProperty(), 1.6), new KeyValue(messageIndicator.scaleYProperty(), 1.6)),
                new KeyFrame(Duration.millis(500), new KeyValue(messageIndicator.scaleXProperty(), 1.0), new KeyValue(messageIndicator.scaleYProperty(), 1.0)),
                new KeyFrame(Duration.millis(750), new KeyValue(messageIndicator.scaleXProperty(), 1.6), new KeyValue(messageIndicator.scaleYProperty(), 1.6)),
                new KeyFrame(Duration.millis(1000), new KeyValue(messageIndicator.scaleXProperty(), 1.0), new KeyValue(messageIndicator.scaleYProperty(), 1.0))
        );
        activeAnimationTimeline.setRate(currentReplaySpeed);
        activeAnimationTimeline.playFromStart();
    }

    public void pauseReplay() {
        if (activeAnimationTimeline != null && activeAnimationTimeline.getStatus() == Animation.Status.RUNNING) {
            activeAnimationTimeline.pause();
            replayController.pause();
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("⏸ Replay Paused");
            }
            if (playReplayBtn != null) {
                playReplayBtn.setText("▶ Resume");
            }
        }
    }

    public void resumeReplay() {
        playReplay();
    }

    public void resetReplay() {
        if (activeAnimationTimeline != null) {
            activeAnimationTimeline.stop();
        }
        replayController.reset();
        clearRouteHighlight();

        if (activeReplayRecord != null) {
            if (activeReplayRecord.isDelivered() && !activeReplayRecord.getResult().getRoute().isEmpty()) {
                CommunicationDevice startDev = activeReplayRecord.getResult().getRoute().get(0);
                NodeVisual startVis = deviceNodeMap.get(startDev);
                if (startVis != null) {
                    messageIndicator.setFailureMode(false);
                    messageIndicator.setPosition(startVis.getX(), startVis.getY());
                    messageIndicator.setVisible(true);
                }
            } else {
                CommunicationDevice sender = activeReplayRecord.getMessage().getSender();
                NodeVisual senderVis = deviceNodeMap.get(sender);
                if (senderVis != null) {
                    messageIndicator.setFailureMode(true);
                    messageIndicator.setPosition(senderVis.getX(), senderVis.getY());
                    messageIndicator.setVisible(true);
                }
            }
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("Replay Reset: Ready to play");
            }
        } else {
            messageIndicator.setVisible(false);
            if (replayStatusLabel != null) {
                replayStatusLabel.setText("Ready");
            }
        }
        if (playReplayBtn != null) {
            playReplayBtn.setText("▶ Play");
        }
    }

    public void setReplaySpeed(double speed) {
        this.currentReplaySpeed = speed;
        replayController.setSpeed(speed);
        if (activeAnimationTimeline != null) {
            activeAnimationTimeline.setRate(speed);
        }
    }

    public MessageIndicatorVisual getMessageIndicator() {
        return messageIndicator;
    }

    public ReplayController getReplayController() {
        return replayController;
    }

    public SimulationRecord getActiveReplayRecord() {
        return activeReplayRecord;
    }

    public String getReplayStatusText() {
        return (replayStatusLabel != null) ? replayStatusLabel.getText() : "";
    }

    public double getReplaySpeed() {
        return currentReplaySpeed;
    }

    public Timeline getActiveAnimationTimeline() {
        return activeAnimationTimeline;
    }

    public List<CommunicationDevice> getActiveRoute() {
        return Collections.unmodifiableList(activeRoute);
    }

    public CommunicationDevice getSelectedDevice() {
        return selectedDevice;
    }

    public boolean isLinkHighlighted(CommunicationDevice a, CommunicationDevice b) {
        for (LinkVisual lv : linkVisuals) {
            if (lv.connects(a, b)) {
                return lv.isHighlighted();
            }
        }
        return false;
    }

    public int getNodeHopIndex(CommunicationDevice dev) {
        NodeVisual nv = deviceNodeMap.get(dev);
        return (nv != null) ? nv.getRouteHopIndex() : -1;
    }

    public NodeVisual getNodeVisual(CommunicationDevice dev) {
        return deviceNodeMap.get(dev);
    }

    public List<LinkVisual> getLinkVisuals() {
        return Collections.unmodifiableList(linkVisuals);
    }

    public Map<CommunicationDevice, NodeVisual> getDeviceNodeMap() {
        return Collections.unmodifiableMap(deviceNodeMap);
    }

    public boolean isInspectorVisible() {
        return inspectorCard != null && inspectorCard.isVisible();
    }

    public String getInspectorTitle() {
        return inspectorTitleLabel != null ? inspectorTitleLabel.getText() : "";
    }

    public String getInspectorId() {
        return inspectorIdLabel != null ? inspectorIdLabel.getText() : "";
    }

    public String getInspectorStatus() {
        return inspectorStatusLabel != null ? inspectorStatusLabel.getText() : "";
    }

    public String getInspectorBattery() {
        return inspectorBatteryLabel != null ? inspectorBatteryLabel.getText() : "";
    }

    public String getInspectorType() {
        return inspectorTypeLabel != null ? inspectorTypeLabel.getText() : "";
    }

    public String getInspectorPeers() {
        return inspectorPeersLabel != null ? inspectorPeersLabel.getText() : "";
    }

    /**
     * Programmatically selects a device and updates the visual halo and inspector.
     */
    public void selectDevice(CommunicationDevice dev) {
        this.selectedDevice = dev;

        for (Map.Entry<CommunicationDevice, NodeVisual> entry : deviceNodeMap.entrySet()) {
            entry.getValue().setSelected(entry.getKey().equals(dev));
        }

        updateInspector(dev);

        if (deviceSelectionListener != null && dev != null) {
            deviceSelectionListener.accept(dev);
        }
    }

    private void updateInspector(CommunicationDevice dev) {
        if (dev == null) {
            inspectorCard.setVisible(false);
            return;
        }

        inspectorCard.setVisible(true);
        inspectorTitleLabel.setText(dev.getName());
        inspectorIdLabel.setText("ID: " + dev.getId());

        String typeStr = (dev instanceof SecurityStation) ? "🛡️ Security Station" :
                         (dev instanceof MedicalStation) ? "🏥 Medical Station" : "📱 Student Phone";
        inspectorTypeLabel.setText(typeStr);

        inspectorBatteryLabel.setText(String.format("Battery: %.1f%%", dev.getBatteryLevel()));
        if (dev.getBatteryLevel() > 50.0) {
            inspectorBatteryLabel.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else if (dev.getBatteryLevel() >= 20.0) {
            inspectorBatteryLabel.setStyle("-fx-text-fill: #f59e0b; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            inspectorBatteryLabel.setStyle("-fx-text-fill: #f43f5e; -fx-font-weight: bold; -fx-font-size: 11px;");
        }

        if (dev.getStatus() == DeviceStatus.OFFLINE) {
            inspectorStatusLabel.setText("Status: 🔴 OFFLINE");
            inspectorStatusLabel.setStyle("-fx-text-fill: #f87171; -fx-font-weight: bold; -fx-font-size: 11px;");
        } else {
            inspectorStatusLabel.setText("Status: 🟢 ONLINE (" + dev.getStatus() + ")");
            inspectorStatusLabel.setStyle("-fx-text-fill: #34d399; -fx-font-weight: bold; -fx-font-size: 11px;");
        }

        int peerCount = graph.getLinks(dev).size();
        inspectorPeersLabel.setText("Connected Links: " + peerCount + " peer" + (peerCount == 1 ? "" : "s"));
    }

    private void calculateInitialPositions(List<CommunicationDevice> devices) {
        double w = getWidth() > 0 ? getWidth() : 680;
        double h = getHeight() > 0 ? getHeight() : 340;

        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radiusX = Math.max(120, (w / 2.0) - 100);
        double radiusY = Math.max(90, (h / 2.0) - 70);

        int count = devices.size();
        for (int i = 0; i < count; i++) {
            CommunicationDevice dev = devices.get(i);
            if (!manualPositions.containsKey(dev)) {
                double angle = (2.0 * Math.PI * i) / count - (Math.PI / 2.0);
                double x = centerX + radiusX * Math.cos(angle);
                double y = centerY + radiusY * Math.sin(angle);
                manualPositions.put(dev, new Point2D(x, y));
            }
        }
    }

    private void relayoutNodes() {
        double w = getWidth();
        double h = getHeight();
        if (w <= 0 || h <= 0) return;

        double centerX = w / 2.0;
        double centerY = h / 2.0;
        double radiusX = Math.max(120, (w / 2.0) - 110);
        double radiusY = Math.max(80, (h / 2.0) - 70);

        List<CommunicationDevice> devices = new ArrayList<>(graph.getAllDevices());
        int count = devices.size();

        for (int i = 0; i < count; i++) {
            CommunicationDevice dev = devices.get(i);
            NodeVisual visual = deviceNodeMap.get(dev);

            if (visual != null && !visual.isDragged()) {
                double angle = (2.0 * Math.PI * i) / count - (Math.PI / 2.0);
                double x = centerX + radiusX * Math.cos(angle);
                double y = centerY + radiusY * Math.sin(angle);
                visual.setX(x);
                visual.setY(y);
                manualPositions.put(dev, new Point2D(x, y));
            }
        }
    }

    private void setupDragAndDrop(NodeVisual visual, CommunicationDevice dev) {
        visual.setOnMouseEntered(e -> setCursor(Cursor.HAND));
        visual.setOnMouseExited(e -> setCursor(Cursor.DEFAULT));

        final Delta dragDelta = new Delta();
        visual.setOnMousePressed(e -> {
            dragDelta.x = visual.getX() - e.getSceneX();
            dragDelta.y = visual.getY() - e.getSceneY();
            visual.setDragged(true);
            selectDevice(dev);
        });

        visual.setOnMouseDragged(e -> {
            double newX = Math.max(40, Math.min(getWidth() - 40, e.getSceneX() + dragDelta.x));
            double newY = Math.max(40, Math.min(getHeight() - 40, e.getSceneY() + dragDelta.y));
            visual.setX(newX);
            visual.setY(newY);
            manualPositions.put(dev, new Point2D(newX, newY));
        });

        visual.setOnMouseReleased(e -> visual.setDragged(false));
    }

    private static class Delta {
        double x, y;
    }

    // --------------------------------------------------------------------------
    // Inspector & Legend Overlays
    // --------------------------------------------------------------------------
    private VBox createInspectorCard() {
        VBox card = new VBox(4);
        card.setStyle("-fx-background-color: rgba(10, 16, 29, 0.90); -fx-background-radius: 8px; -fx-border-color: #1e3a5f; -fx-border-radius: 8px; -fx-padding: 10px 14px;");
        card.setMaxWidth(200);
        card.setMouseTransparent(true);

        Label header = new Label("NODE INSPECTOR");
        header.setStyle("-fx-font-size: 9px; -fx-font-weight: 800; -fx-text-fill: #38bdf8; -fx-text-transform: uppercase;");

        inspectorTitleLabel = new Label("Device Name");
        inspectorTitleLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        inspectorIdLabel = new Label("DEV-1");
        inspectorIdLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        inspectorTypeLabel = new Label("Student Phone");
        inspectorTypeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #cbd5e1;");

        inspectorBatteryLabel = new Label("Battery: 100%");
        inspectorStatusLabel = new Label("Status: ONLINE");
        inspectorPeersLabel = new Label("Connected Links: 0");
        inspectorPeersLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

        card.getChildren().addAll(header, inspectorTitleLabel, inspectorIdLabel, inspectorTypeLabel, inspectorBatteryLabel, inspectorStatusLabel, inspectorPeersLabel);
        return card;
    }

    private HBox createLegendBox() {
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setStyle("-fx-background-color: rgba(10, 16, 29, 0.85); -fx-background-radius: 6px; -fx-border-color: #16243f; -fx-border-radius: 6px; -fx-padding: 5px 10px;");
        legend.setMouseTransparent(true);

        Label legOnline = new Label("🟢 Online Node");
        legOnline.setStyle("-fx-font-size: 10px; -fx-text-fill: #34d399; -fx-font-weight: bold;");

        Label legOffline = new Label("🔴 Offline Node");
        legOffline.setStyle("-fx-font-size: 10px; -fx-text-fill: #f87171; -fx-font-weight: bold;");

        Label legRoute = new Label("⚡ Active BFS Path");
        legRoute.setStyle("-fx-font-size: 10px; -fx-text-fill: #22d3ee; -fx-font-weight: bold;");

        legend.getChildren().addAll(legOnline, legOffline, legRoute);
        return legend;
    }

    private HBox createReplayBar() {
        HBox bar = new HBox(8);
        bar.setAlignment(Pos.CENTER);
        bar.setStyle("-fx-background-color: rgba(10, 16, 29, 0.94); -fx-background-radius: 8px; -fx-border-color: #1e3a5f; -fx-border-radius: 8px; -fx-padding: 6px 14px; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 8, 0, 0, 2);");

        Label title = new Label("🎬 REPLAY:");
        title.setStyle("-fx-font-size: 10px; -fx-font-weight: 800; -fx-text-fill: #38bdf8;");

        replayStatusLabel = new Label("Ready (No simulation loaded)");
        replayStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-weight: 600;");
        replayStatusLabel.setMaxWidth(260);

        playReplayBtn = new Button("▶ Play");
        playReplayBtn.setStyle("-fx-background-color: #0284c7; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 5px; -fx-cursor: hand;");
        playReplayBtn.setOnAction(e -> playReplay());

        pauseReplayBtn = new Button("⏸ Pause");
        pauseReplayBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 5px; -fx-cursor: hand;");
        pauseReplayBtn.setOnAction(e -> pauseReplay());

        resetReplayBtn = new Button("↺ Reset");
        resetReplayBtn.setStyle("-fx-background-color: #1e293b; -fx-text-fill: #cbd5e1; -fx-font-size: 11px; -fx-font-weight: bold; -fx-padding: 4 10; -fx-background-radius: 5px; -fx-cursor: hand;");
        resetReplayBtn.setOnAction(e -> resetReplay());

        speedComboBox = new ComboBox<>();
        speedComboBox.getItems().addAll("0.5x", "1.0x", "1.5x", "2.0x");
        speedComboBox.setValue("1.0x");
        speedComboBox.setStyle("-fx-font-size: 10px; -fx-background-color: #0f172a; -fx-text-fill: #38bdf8; -fx-border-color: #1e293b; -fx-border-radius: 4px;");
        speedComboBox.setOnAction(e -> {
            String val = speedComboBox.getValue();
            double speed = 1.0;
            if ("0.5x".equals(val)) speed = 0.5;
            else if ("1.5x".equals(val)) speed = 1.5;
            else if ("2.0x".equals(val)) speed = 2.0;
            setReplaySpeed(speed);
        });

        bar.getChildren().addAll(title, replayStatusLabel, playReplayBtn, pauseReplayBtn, resetReplayBtn, speedComboBox);
        return bar;
    }

    // --------------------------------------------------------------------------
    // Visual Node Component (Circle + Icon + Label + Halo)
    // --------------------------------------------------------------------------
    public static class NodeVisual extends StackPane {
        private final CommunicationDevice device;
        private final DoubleProperty xProperty = new SimpleDoubleProperty();
        private final DoubleProperty yProperty = new SimpleDoubleProperty();

        private final Circle baseCircle;
        private final Circle haloCircle;
        private final Label iconLabel;
        private final Label nameLabel;
        private final Label hopBadge;

        private boolean dragged = false;
        private int routeHopIndex = -1;
        private boolean selected = false;

        public NodeVisual(CommunicationDevice device, double x, double y) {
            this.device = device;
            setX(x);
            setY(y);

            // Halo Ring (for selection or route highlight)
            haloCircle = new Circle(28);
            haloCircle.setFill(Color.TRANSPARENT);
            haloCircle.setStroke(Color.TRANSPARENT);
            haloCircle.setStrokeWidth(2.5);

            // Base Node Circle
            baseCircle = new Circle(22);
            updateNodeStyle();

            // Device Icon
            String icon = (device instanceof SecurityStation) ? "🛡️" : (device instanceof MedicalStation) ? "🏥" : "📱";
            iconLabel = new Label(icon);
            iconLabel.setStyle("-fx-font-size: 16px;");

            // Hop sequence badge (shown when node is on active BFS path)
            hopBadge = new Label("");
            hopBadge.setStyle("-fx-background-color: #06b6d4; -fx-text-fill: #000000; -fx-font-size: 9px; -fx-font-weight: 900; -fx-padding: 1 5; -fx-background-radius: 8px;");
            hopBadge.setVisible(false);
            StackPane.setAlignment(hopBadge, Pos.TOP_RIGHT);

            StackPane circleStack = new StackPane(haloCircle, baseCircle, iconLabel, hopBadge);

            // Bottom Name Label
            nameLabel = new Label(device.getName());
            nameLabel.setStyle("-fx-text-fill: #f1f5f9; -fx-font-size: 11px; -fx-font-weight: 600; -fx-effect: dropshadow(one-pass-box, black, 4, 0, 0, 1);");
            nameLabel.setMaxWidth(110);
            nameLabel.setWrapText(true);
            nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            nameLabel.setAlignment(Pos.CENTER);

            VBox layout = new VBox(3, circleStack, nameLabel);
            layout.setAlignment(Pos.CENTER);
            layout.setMaxWidth(110);

            getChildren().add(layout);

            Tooltip.install(this, new Tooltip(device.getName() + " [" + device.getId() + "]\n" +
                    "Battery: " + String.format("%.1f%%", device.getBatteryLevel()) + "\n" +
                    "Status: " + device.getStatus()));
        }

        public void updateNodeStyle() {
            boolean isOffline = (device.getStatus() == DeviceStatus.OFFLINE);
            if (isOffline) {
                baseCircle.setFill(Color.web("#3f1218"));
                baseCircle.setStroke(Color.web("#e11d48"));
                baseCircle.setStrokeWidth(2.0);
                baseCircle.getStrokeDashArray().setAll(4.0, 4.0);
                setOpacity(0.70);
            } else {
                baseCircle.setFill(Color.web("#0e223d"));
                baseCircle.setStroke(Color.web("#10b981"));
                baseCircle.setStrokeWidth(2.0);
                baseCircle.getStrokeDashArray().clear();
                setOpacity(1.0);
            }
        }

        public void setSelected(boolean selected) {
            this.selected = selected;
            if (selected) {
                haloCircle.setStroke(Color.web("#38bdf8"));
                haloCircle.setEffect(new DropShadow(14, Color.web("#06b6d4")));
            } else if (routeHopIndex <= 0) {
                haloCircle.setStroke(Color.TRANSPARENT);
                haloCircle.setEffect(null);
            }
        }

        public void setRouteHopIndex(int hopIndex) {
            this.routeHopIndex = hopIndex;
            if (hopIndex > 0) {
                hopBadge.setText("#" + hopIndex);
                hopBadge.setVisible(true);
                haloCircle.setStroke(Color.web("#06b6d4"));
                haloCircle.setEffect(new DropShadow(16, Color.web("#22d3ee")));
            } else {
                hopBadge.setVisible(false);
                if (selected) {
                    haloCircle.setStroke(Color.web("#38bdf8"));
                    haloCircle.setEffect(new DropShadow(14, Color.web("#06b6d4")));
                } else {
                    haloCircle.setStroke(Color.TRANSPARENT);
                    haloCircle.setEffect(null);
                }
            }
        }

        public double getX() { return xProperty.get(); }
        public void setX(double x) {
            xProperty.set(x);
            setLayoutX(x - 55);
        }
        public DoubleProperty xProperty() { return xProperty; }

        public double getY() { return yProperty.get(); }
        public void setY(double y) {
            yProperty.set(y);
            setLayoutY(y - 30);
        }
        public DoubleProperty yProperty() { return yProperty; }

        public boolean isDragged() { return dragged; }
        public void setDragged(boolean dragged) { this.dragged = dragged; }

        public int getRouteHopIndex() { return routeHopIndex; }
        public boolean isSelected() { return selected; }
        public CommunicationDevice getDevice() { return device; }
        public Circle getBaseCircle() { return baseCircle; }
        public Circle getHaloCircle() { return haloCircle; }
        public Label getHopBadge() { return hopBadge; }
        public Label getNameLabel() { return nameLabel; }
        public Label getIconLabel() { return iconLabel; }
    }

    // --------------------------------------------------------------------------
    // Visual Link Component (Dynamic Line between Node Centers)
    // --------------------------------------------------------------------------
    public static class LinkVisual {
        private final CommunicationDevice devA;
        private final CommunicationDevice devB;
        private final Line line;
        private boolean highlighted = false;

        public LinkVisual(CommunicationDevice devA, CommunicationDevice devB, NodeVisual visA, NodeVisual visB) {
            this.devA = devA;
            this.devB = devB;

            line = new Line();
            line.startXProperty().bind(visA.xProperty());
            line.startYProperty().bind(visA.yProperty());
            line.endXProperty().bind(visB.xProperty());
            line.endYProperty().bind(visB.yProperty());

            setHighlighted(false);
        }

        public Line getLine() { return line; }

        public boolean connects(CommunicationDevice a, CommunicationDevice b) {
            return (devA.equals(a) && devB.equals(b)) || (devA.equals(b) && devB.equals(a));
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
            if (highlighted) {
                line.setStroke(Color.web("#22d3ee"));
                line.setStrokeWidth(3.5);
                line.getStrokeDashArray().clear();
                line.setEffect(new DropShadow(10, Color.web("#06b6d4")));
            } else if (!devA.isAvailable() || !devB.isAvailable()) {
                line.setStroke(Color.web("#2d1822"));
                line.setStrokeWidth(1.2);
                line.getStrokeDashArray().setAll(4.0, 4.0);
                line.setEffect(null);
            } else {
                line.setStroke(Color.web("#1e2d47"));
                line.setStrokeWidth(1.8);
                line.getStrokeDashArray().clear();
                line.setEffect(null);
            }
        }

        public boolean isHighlighted() { return highlighted; }
        public CommunicationDevice getDevA() { return devA; }
        public CommunicationDevice getDevB() { return devB; }
    }

    // --------------------------------------------------------------------------
    // Animated Message Packet Indicator (Step 11)
    // --------------------------------------------------------------------------
    public static class MessageIndicatorVisual extends StackPane {
        private final Circle outerGlow;
        private final Circle coreCircle;
        private final Label iconLabel;
        private boolean failureMode = false;

        public MessageIndicatorVisual() {
            outerGlow = new Circle(14);
            outerGlow.setFill(Color.web("#06b6d4", 0.35));

            coreCircle = new Circle(10);
            coreCircle.setFill(Color.web("#06b6d4"));
            coreCircle.setStroke(Color.web("#22d3ee"));
            coreCircle.setStrokeWidth(1.5);
            coreCircle.setEffect(new DropShadow(12, Color.web("#22d3ee")));

            iconLabel = new Label("📨");
            iconLabel.setStyle("-fx-font-size: 10px;");

            getChildren().addAll(outerGlow, coreCircle, iconLabel);
            setMouseTransparent(true);
            setVisible(false);
        }

        public void setFailureMode(boolean failure) {
            this.failureMode = failure;
            if (failure) {
                outerGlow.setFill(Color.web("#f43f5e", 0.40));
                coreCircle.setFill(Color.web("#e11d48"));
                coreCircle.setStroke(Color.web("#fecdd3"));
                coreCircle.setEffect(new DropShadow(14, Color.web("#f43f5e")));
                iconLabel.setText("✖");
            } else {
                outerGlow.setFill(Color.web("#06b6d4", 0.35));
                coreCircle.setFill(Color.web("#06b6d4"));
                coreCircle.setStroke(Color.web("#22d3ee"));
                coreCircle.setEffect(new DropShadow(12, Color.web("#22d3ee")));
                iconLabel.setText("📨");
            }
        }

        public boolean isFailureMode() {
            return failureMode;
        }

        public void setPosition(double x, double y) {
            setLayoutX(x - 14);
            setLayoutY(y - 14);
        }
    }
}
