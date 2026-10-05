package com.resqmesh.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;
import com.resqmesh.routing.ShortestPathStrategy;
import com.resqmesh.simulation.SimulationEngine;
import com.resqmesh.simulation.SimulationResult;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight browser-accessible web server for the ResQMesh simulator.
 * Built using the standard JDK com.sun.net.httpserver.HttpServer with zero web framework bloat.
 * Reuses the existing core Java OOP model, BFS routing algorithm, and SimulationEngine.
 */
public class ResQMeshWebServer {

    public static final int DEFAULT_PORT = 8080;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final int port;
    private HttpServer server;
    private final NetworkGraph graph;
    private final SimulationEngine engine;
    private final AtomicInteger messageCounter = new AtomicInteger(1);

    public ResQMeshWebServer() {
        this(DEFAULT_PORT);
    }

    public ResQMeshWebServer(int port) {
        this.port = port;
        this.graph = new NetworkGraph();
        this.engine = new SimulationEngine(graph, new ShortestPathStrategy());
        initDefaultNetwork();
    }

    /**
     * Initializes the default campus disaster emergency mesh topology.
     */
    public synchronized void initDefaultNetwork() {
        graph.clear();

        // 1. Create devices
        StudentPhone alice = new StudentPhone("DEV-1", "Alice's Phone", new Location(100, 200), 100.0);
        StudentPhone bob = new StudentPhone("DEV-2", "Bob's Phone", new Location(260, 140), 100.0);
        SecurityStation security = new SecurityStation("SEC-1", "Security Post", new Location(420, 200), 100.0);
        MedicalStation medical = new MedicalStation("MED-1", "Medical Center", new Location(580, 200), 100.0);
        StudentPhone charlie = new StudentPhone("DEV-3", "Charlie's Phone", new Location(260, 290), 100.0);
        StudentPhone david = new StudentPhone("DEV-4", "David's Phone (Isolated)", new Location(480, 330), 100.0);

        // 2. Register devices in graph
        graph.addDevice(alice);
        graph.addDevice(bob);
        graph.addDevice(security);
        graph.addDevice(medical);
        graph.addDevice(charlie);
        graph.addDevice(david); // Notice: David is isolated to test disconnected failure routing

        // 3. Connect active mesh links
        graph.connect(alice, bob);
        graph.connect(bob, security);
        graph.connect(security, medical);
        graph.connect(alice, charlie);
        graph.connect(charlie, security);
    }

    /**
     * Starts the HTTP web server.
     */
    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Register HTTP handlers
        server.createContext("/", new StaticPageHandler());
        server.createContext("/api/network", new NetworkHandler());
        server.createContext("/api/dispatch", new DispatchHandler());
        server.createContext("/api/reset", new ResetHandler());

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();

        System.out.println("=========================================================");
        System.out.println("    ResQMesh Web Server successfully started!           ");
        System.out.println("    URL: http://localhost:" + port + "/                 ");
        System.out.println("=========================================================");
    }

    /**
     * Stops the HTTP web server.
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("ResQMesh Web Server stopped.");
        }
    }

    // -------------------------------------------------------------------------
    // HTTP Handlers
    // -------------------------------------------------------------------------

    /**
     * Serves index.html at root path "/".
     */
    private class StaticPageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "text/plain", "Method Not Allowed".getBytes(StandardCharsets.UTF_8));
                return;
            }

            byte[] content = loadIndexHtml();
            sendResponse(exchange, 200, "text/html; charset=UTF-8", content);
        }

        private byte[] loadIndexHtml() {
            // 1. Try loading from classpath
            try (InputStream is = ResQMeshWebServer.class.getResourceAsStream("/web/index.html")) {
                if (is != null) {
                    return is.readAllBytes();
                }
            } catch (Exception ignored) {
            }

            // 2. Fallback to direct filesystem path
            try {
                Path directPath = Path.of("src", "main", "resources", "web", "index.html");
                if (Files.exists(directPath)) {
                    return Files.readAllBytes(directPath);
                }
            } catch (Exception ignored) {
            }

            // 3. Fallback inline HTML if file not found
            String fallback = "<!DOCTYPE html><html><body><h1>ResQMesh Simulator</h1><p>index.html not found in classpath.</p></body></html>";
            return fallback.getBytes(StandardCharsets.UTF_8);
        }
    }

    /**
     * Returns the current network topology as JSON at GET /api/network.
     */
    private class NetworkHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String jsonResponse;
            synchronized (ResQMeshWebServer.this) {
                jsonResponse = buildNetworkJson();
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Handles emergency message dispatch at POST /api/dispatch.
     */
    private class DispatchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }

            try {
                String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                JsonObject json = JsonParser.parseString(requestBody).getAsJsonObject();

                String senderId = json.has("senderId") ? json.get("senderId").getAsString() : "";
                String recipientId = json.has("recipientId") ? json.get("recipientId").getAsString() : "";
                String priorityStr = json.has("priority") ? json.get("priority").getAsString() : "NORMAL";
                String content = json.has("content") ? json.get("content").getAsString() : "Emergency Alert";

                Priority priority;
                try {
                    priority = Priority.valueOf(priorityStr.toUpperCase().trim());
                } catch (Exception e) {
                    priority = Priority.NORMAL;
                }

                String responseJson;
                synchronized (ResQMeshWebServer.this) {
                    CommunicationDevice sender = graph.getDeviceById(senderId);
                    CommunicationDevice recipient = graph.getDeviceById(recipientId);

                    if (sender == null || recipient == null) {
                        Map<String, Object> errorMap = new HashMap<>();
                        errorMap.put("success", false);
                        errorMap.put("delivered", false);
                        errorMap.put("explanation", "Invalid sender or recipient identifier.");
                        responseJson = GSON.toJson(errorMap);
                    } else {
                        EmergencyMessage msg = new EmergencyMessage(
                                "MSG-" + messageCounter.getAndIncrement(),
                                sender,
                                recipient,
                                content,
                                priority
                        );

                        // Execute core simulation engine (reuses BFS and battery consumption)
                        SimulationResult result = engine.send(msg);

                        Map<String, Object> resp = new LinkedHashMap<>();
                        resp.put("success", true);
                        resp.put("delivered", result.delivered());
                        resp.put("explanation", result.explanation());
                        resp.put("hopCount", result.getHopCount());
                        resp.put("messageStatus", msg.getStatus().name());

                        List<Map<String, Object>> routeList = new ArrayList<>();
                        for (CommunicationDevice dev : result.route()) {
                            Map<String, Object> devMap = new LinkedHashMap<>();
                            devMap.put("id", dev.getId());
                            devMap.put("name", dev.getName());
                            devMap.put("type", dev.getClass().getSimpleName());
                            devMap.put("batteryLevel", dev.getBatteryLevel());
                            devMap.put("status", dev.getStatus().name());
                            routeList.add(devMap);
                        }
                        resp.put("route", routeList);
                        resp.put("network", buildNetworkDataMap());

                        responseJson = GSON.toJson(resp);
                    }
                }

                sendResponse(exchange, 200, "application/json; charset=UTF-8", responseJson.getBytes(StandardCharsets.UTF_8));
            } catch (Exception e) {
                e.printStackTrace();
                String err = GSON.toJson(Map.of("success", false, "error", e.getMessage()));
                sendResponse(exchange, 400, "application/json; charset=UTF-8", err.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Resets the simulation network to default values at POST /api/reset.
     */
    private class ResetHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            addCorsHeaders(exchange);
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method Not Allowed\"}".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String jsonResponse;
            synchronized (ResQMeshWebServer.this) {
                initDefaultNetwork();
                jsonResponse = buildNetworkJson();
            }

            sendResponse(exchange, 200, "application/json; charset=UTF-8", jsonResponse.getBytes(StandardCharsets.UTF_8));
        }
    }

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------

    private void addCorsHeaders(HttpExchange exchange) {
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, byte[] data) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, data.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    private String buildNetworkJson() {
        return GSON.toJson(buildNetworkDataMap());
    }

    private Map<String, Object> buildNetworkDataMap() {
        Map<String, Object> data = new LinkedHashMap<>();

        // 1. Devices list
        List<Map<String, Object>> deviceList = new ArrayList<>();
        for (CommunicationDevice dev : graph.getAllDevices()) {
            Map<String, Object> devMap = new LinkedHashMap<>();
            devMap.put("id", dev.getId());
            devMap.put("name", dev.getName());
            devMap.put("type", dev.getClass().getSimpleName());
            devMap.put("batteryLevel", dev.getBatteryLevel());
            devMap.put("status", dev.getStatus().name());
            devMap.put("isAvailable", dev.isAvailable());

            if (dev.getLocation() != null) {
                Map<String, Object> loc = new LinkedHashMap<>();
                loc.put("x", dev.getLocation().getX());
                loc.put("y", dev.getLocation().getY());
                devMap.put("location", loc);
            }
            deviceList.add(devMap);
        }
        data.put("devices", deviceList);

        // 2. Unique undirected links
        Set<String> processedPairs = new HashSet<>();
        List<Map<String, Object>> linkList = new ArrayList<>();
        for (CommunicationDevice devA : graph.getAllDevices()) {
            for (CommunicationLink link : graph.getLinks(devA)) {
                CommunicationDevice devB = link.getDestination();
                String key1 = devA.getId() + "--" + devB.getId();
                String key2 = devB.getId() + "--" + devA.getId();

                if (!processedPairs.contains(key1) && !processedPairs.contains(key2)) {
                    processedPairs.add(key1);
                    processedPairs.add(key2);

                    Map<String, Object> linkMap = new LinkedHashMap<>();
                    linkMap.put("sourceId", devA.getId());
                    linkMap.put("targetId", devB.getId());
                    linkMap.put("active", link.isActive());
                    linkMap.put("usable", link.usable());
                    linkList.add(linkMap);
                }
            }
        }
        data.put("links", linkList);

        return data;
    }

    // -------------------------------------------------------------------------
    // Main Entry Point
    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            ResQMeshWebServer server = new ResQMeshWebServer(port);
            server.start();

            // Keep main thread alive
            Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        } catch (IOException e) {
            System.err.println("Failed to start ResQMesh Web Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
