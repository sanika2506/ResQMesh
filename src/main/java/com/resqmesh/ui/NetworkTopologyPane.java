package com.resqmesh.ui;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.CommunicationLink;
import com.resqmesh.model.DeviceStatus;
import com.resqmesh.model.MedicalStation;
import com.resqmesh.model.SecurityStation;
import com.resqmesh.model.StudentPhone;
import com.resqmesh.routing.NetworkGraph;

import javafx.animation.FadeTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
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
 * 
 * Features:
 * - Renders all registered devices as interactive draggable nodes.
 * - Renders all active bidirectional communication links.
 * - Distinct visual styles for ONLINE (green/cyan glow) vs OFFLINE (crimson/dimmed).
 * - Real-time BFS route illumination with glowing links and hop sequence numbers.
 * - On-node click inspector displaying device telemetry.
 * - Mouse drag-and-drop node repositioning with dynamic link tracking.
 */
public class NetworkTopologyPane extends StackPane {

    private final NetworkGraph graph;
    private final Pane canvasPane;
    private final Pane linkLayer;
    private final Pane nodeLayer;
    private final VBox inspectorCard;

    // Node and Link tracking
    private final Map<CommunicationDevice, NodeVisual> deviceNodeMap = new HashMap<>();
    private final List<LinkVisual> linkVisuals = new ArrayList<>();
    private final Map<CommunicationDevice, Point2D> manualPositions = new HashMap<>();

    // State
    private CommunicationDevice selectedDevice;
    private List<CommunicationDevice> activeRoute = Collections.emptyList();
    private Consumer<CommunicationDevice> deviceSelectionListener;

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
        linkLayer.setPickOnBounds(false);
        nodeLayer.setPickOnBounds(false);

        canvasPane.getChildren().addAll(linkLayer, nodeLayer);

        // Mini HUD Inspector Card (top-right overlay)
        inspectorCard = createInspectorCard();
        StackPane.setAlignment(inspectorCard, Pos.TOP_RIGHT);
        StackPane.setMargin(inspectorCard, new Insets(12));

        // Legend Badge (top-left overlay)
        HBox legendBox = createLegendBox();
        StackPane.setAlignment(legendBox, Pos.TOP_LEFT);
        StackPane.setMargin(legendBox, new Insets(12));

        getChildren().addAll(canvasPane, legendBox, inspectorCard);

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

        List<CommunicationDevice> devices = new ArrayList<>(graph.getAllDevices());
        if (devices.isEmpty()) {
            inspectorCard.setVisible(false);
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
            highlightRoute(activeRoute);
        }
    }

    /**
     * Highlights the discovered BFS route on nodes and links.
     */
    public void highlightRoute(List<CommunicationDevice> route) {
        this.activeRoute = (route != null) ? route : Collections.emptyList();

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

        Label legOnline = new Label("🟢 Online Node");
        legOnline.setStyle("-fx-font-size: 10px; -fx-text-fill: #34d399; -fx-font-weight: bold;");

        Label legOffline = new Label("🔴 Offline Node");
        legOffline.setStyle("-fx-font-size: 10px; -fx-text-fill: #f87171; -fx-font-weight: bold;");

        Label legRoute = new Label("⚡ Active BFS Path");
        legRoute.setStyle("-fx-font-size: 10px; -fx-text-fill: #22d3ee; -fx-font-weight: bold;");

        legend.getChildren().addAll(legOnline, legOffline, legRoute);
        return legend;
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

            VBox layout = new VBox(3, circleStack, nameLabel);
            layout.setAlignment(Pos.CENTER);

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
                setOpacity(0.70);
            } else {
                baseCircle.setFill(Color.web("#0e223d"));
                baseCircle.setStroke(Color.web("#10b981"));
                baseCircle.setStrokeWidth(2.0);
                setOpacity(1.0);
            }
        }

        public void setSelected(boolean selected) {
            if (selected) {
                haloCircle.setStroke(Color.web("#38bdf8"));
                haloCircle.setEffect(new DropShadow(14, Color.web("#06b6d4")));
            } else if (!hopBadge.isVisible()) {
                haloCircle.setStroke(Color.TRANSPARENT);
                haloCircle.setEffect(null);
            }
        }

        public void setRouteHopIndex(int hopIndex) {
            if (hopIndex > 0) {
                hopBadge.setText("#" + hopIndex);
                hopBadge.setVisible(true);
                haloCircle.setStroke(Color.web("#06b6d4"));
                haloCircle.setEffect(new DropShadow(16, Color.web("#22d3ee")));
            } else {
                hopBadge.setVisible(false);
                haloCircle.setStroke(Color.TRANSPARENT);
                haloCircle.setEffect(null);
            }
        }

        public double getX() { return xProperty.get(); }
        public void setX(double x) {
            xProperty.set(x);
            setLayoutX(x - 40);
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
    }

    // --------------------------------------------------------------------------
    // Visual Link Component (Dynamic Line between Node Centers)
    // --------------------------------------------------------------------------
    public static class LinkVisual {
        private final CommunicationDevice devA;
        private final CommunicationDevice devB;
        private final Line line;

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
            if (highlighted) {
                line.setStroke(Color.web("#22d3ee"));
                line.setStrokeWidth(3.5);
                line.setEffect(new DropShadow(10, Color.web("#06b6d4")));
            } else {
                line.setStroke(Color.web("#1e2d47"));
                line.setStrokeWidth(1.8);
                line.setEffect(null);
            }
        }
    }
}
