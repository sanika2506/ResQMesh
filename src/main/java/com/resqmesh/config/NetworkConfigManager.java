package com.resqmesh.config;

import com.google.gson.*;
import com.resqmesh.model.*;
import com.resqmesh.routing.NetworkGraph;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service responsible for persisting and restoring ResQMesh network topologies to/from JSON.
 * 
 * Features:
 * - Full serialization of devices, types, battery levels, operational states, coordinates, and active mesh links.
 * - Strict schema validation: detects malformed JSON, duplicate device IDs, unknown endpoints, and self-connections.
 * - Clean graph restoration: wipes stale connections and devices before repopulating the target graph.
 */
public class NetworkConfigManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String SCHEMA_VERSION = "1.0";

    /**
     * Serializes the current NetworkGraph into a formatted JSON string.
     *
     * @param graph      the active network graph to serialize
     * @param configName human-readable scenario name
     * @return pretty-printed JSON string
     */
    public static String exportToJson(NetworkGraph graph, String configName) {
        if (graph == null) {
            throw new IllegalArgumentException("NetworkGraph cannot be null.");
        }

        String name = (configName != null && !configName.trim().isEmpty())
                ? configName.trim() : "ResQMesh Topology";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        NetworkConfigDTO dto = new NetworkConfigDTO(name, SCHEMA_VERSION, timestamp);

        // 1. Export Devices
        for (CommunicationDevice dev : graph.getAllDevices()) {
            String typeStr;
            if (dev instanceof SecurityStation) {
                typeStr = "SECURITY_STATION";
            } else if (dev instanceof MedicalStation) {
                typeStr = "MEDICAL_STATION";
            } else {
                typeStr = "STUDENT_PHONE";
            }

            NetworkConfigDTO.LocationDTO locDTO = null;
            if (dev.getLocation() != null) {
                locDTO = new NetworkConfigDTO.LocationDTO(dev.getLocation().getX(), dev.getLocation().getY());
            }

            NetworkConfigDTO.DeviceDTO devDTO = new NetworkConfigDTO.DeviceDTO(
                    dev.getId(),
                    dev.getName(),
                    typeStr,
                    dev.getBatteryLevel(),
                    dev.getStatus().name(),
                    locDTO
            );
            dto.getDevices().add(devDTO);
        }

        // 2. Export Unique Bidirectional Connections
        Set<String> processedPairs = new HashSet<>();
        for (CommunicationDevice devA : graph.getAllDevices()) {
            for (CommunicationLink link : graph.getLinks(devA)) {
                CommunicationDevice devB = link.getDestination();
                String key1 = devA.getId() + "<->" + devB.getId();
                String key2 = devB.getId() + "<->" + devA.getId();

                if (!processedPairs.contains(key1) && !processedPairs.contains(key2)) {
                    processedPairs.add(key1);
                    processedPairs.add(key2);
                    dto.getConnections().add(new NetworkConfigDTO.ConnectionDTO(devA.getId(), devB.getId()));
                }
            }
        }

        return GSON.toJson(dto);
    }

    /**
     * Saves the network graph topology to a JSON file.
     *
     * @param graph      the active network graph
     * @param file       destination file
     * @param configName human-readable scenario name
     * @throws IOException if writing to disk fails
     */
    public static void saveToFile(NetworkGraph graph, File file, String configName) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Target file cannot be null.");
        }
        String json = exportToJson(graph, configName);
        Files.writeString(file.toPath(), json, StandardCharsets.UTF_8);
    }

    /**
     * Parses and validates JSON content without modifying any active graph.
     *
     * @param json the JSON string to validate
     * @return validated NetworkConfigDTO
     * @throws ConfigurationException if validation fails
     */
    public static NetworkConfigDTO parseAndValidate(String json) throws ConfigurationException {
        if (json == null || json.trim().isEmpty()) {
            throw new ConfigurationException("Configuration JSON content is empty.");
        }

        JsonElement rootElement;
        try {
            rootElement = JsonParser.parseString(json);
        } catch (JsonSyntaxException e) {
            throw new ConfigurationException("Malformed JSON syntax: " + e.getMessage(), e);
        }

        if (rootElement == null || !rootElement.isJsonObject()) {
            throw new ConfigurationException("Failed to parse configuration: root must be a JSON object.");
        }

        JsonObject rootObj = rootElement.getAsJsonObject();
        if (!rootObj.has("devices")) {
            throw new ConfigurationException("Configuration validation failed: 'devices' array is missing from JSON.");
        }

        if (!rootObj.get("devices").isJsonArray()) {
            throw new ConfigurationException("Configuration validation failed: 'devices' must be a JSON array.");
        }

        NetworkConfigDTO dto;
        try {
            dto = GSON.fromJson(rootElement, NetworkConfigDTO.class);
        } catch (JsonSyntaxException e) {
            throw new ConfigurationException("Malformed JSON syntax: " + e.getMessage(), e);
        }

        if (dto == null) {
            throw new ConfigurationException("Failed to parse configuration: root object is null.");
        }

        // Validate devices
        Set<String> deviceIds = new HashSet<>();
        for (NetworkConfigDTO.DeviceDTO dev : dto.getDevices()) {
            if (dev.getId() == null || dev.getId().trim().isEmpty()) {
                throw new ConfigurationException("Invalid device entry: 'id' field is required and cannot be blank.");
            }

            String id = dev.getId().trim();
            if (deviceIds.contains(id.toLowerCase())) {
                throw new ConfigurationException("Duplicate device ID detected: '" + id + "'. All device IDs must be unique.");
            }
            deviceIds.add(id.toLowerCase());

            if (dev.getName() == null || dev.getName().trim().isEmpty()) {
                dev.setName("Device " + id);
            }

            // Clamp battery
            double battery = Math.max(0.0, Math.min(100.0, dev.getBatteryLevel()));
            dev.setBatteryLevel(battery);
        }

        // Validate connections
        if (dto.getConnections() != null) {
            for (NetworkConfigDTO.ConnectionDTO conn : dto.getConnections()) {
                if (conn.getNodeA() == null || conn.getNodeA().trim().isEmpty() ||
                    conn.getNodeB() == null || conn.getNodeB().trim().isEmpty()) {
                    throw new ConfigurationException("Invalid connection entry: 'nodeA' and 'nodeB' must both be specified.");
                }

                String idA = conn.getNodeA().trim();
                String idB = conn.getNodeB().trim();

                if (idA.equalsIgnoreCase(idB)) {
                    throw new ConfigurationException("Invalid connection: device '" + idA + "' cannot be connected to itself.");
                }

                if (!deviceIds.contains(idA.toLowerCase())) {
                    throw new ConfigurationException("Connection references unknown device ID '" + idA + "'.");
                }
                if (!deviceIds.contains(idB.toLowerCase())) {
                    throw new ConfigurationException("Connection references unknown device ID '" + idB + "'.");
                }
            }
        }

        return dto;
    }

    /**
     * Loads a configuration from a JSON string and restores the target NetworkGraph.
     * All existing devices and links in targetGraph are cleared first to guarantee no stale state remains.
     *
     * @param json        the JSON string to load
     * @param targetGraph the NetworkGraph instance to populate
     * @throws ConfigurationException if validation fails
     */
    public static void loadFromJson(String json, NetworkGraph targetGraph) throws ConfigurationException {
        if (targetGraph == null) {
            throw new IllegalArgumentException("Target NetworkGraph cannot be null.");
        }

        NetworkConfigDTO dto = parseAndValidate(json);

        // Wipe clean before restoration (Requirement 7: prevent retaining old connections)
        targetGraph.clear();

        Map<String, CommunicationDevice> createdDevices = new HashMap<>();

        // 1. Instantiate devices
        for (NetworkConfigDTO.DeviceDTO devDTO : dto.getDevices()) {
            String id = devDTO.getId().trim();
            String name = devDTO.getName().trim();
            double battery = devDTO.getBatteryLevel();

            Location loc = new Location(10, 10);
            if (devDTO.getLocation() != null) {
                loc = new Location(devDTO.getLocation().getX(), devDTO.getLocation().getY());
            }

            CommunicationDevice device;
            String type = (devDTO.getType() != null) ? devDTO.getType().toUpperCase() : "STUDENT_PHONE";
            if (type.contains("SECURITY")) {
                device = new SecurityStation(id, name, loc, battery);
            } else if (type.contains("MEDICAL")) {
                device = new MedicalStation(id, name, loc, battery);
            } else {
                device = new StudentPhone(id, name, loc, battery);
            }

            // Restore operational status
            if (battery <= 0.0) {
                device.setStatus(DeviceStatus.OFFLINE);
            } else if (devDTO.getStatus() != null) {
                String st = devDTO.getStatus().toUpperCase();
                if (st.contains("OFFLINE")) {
                    device.setStatus(DeviceStatus.OFFLINE);
                } else if (st.contains("LOW")) {
                    device.setStatus(DeviceStatus.LOW_BATTERY);
                } else {
                    device.setStatus(DeviceStatus.ACTIVE);
                }
            }

            targetGraph.addDevice(device);
            createdDevices.put(id.toLowerCase(), device);
        }

        // 2. Establish connections
        if (dto.getConnections() != null) {
            for (NetworkConfigDTO.ConnectionDTO conn : dto.getConnections()) {
                CommunicationDevice devA = createdDevices.get(conn.getNodeA().trim().toLowerCase());
                CommunicationDevice devB = createdDevices.get(conn.getNodeB().trim().toLowerCase());

                if (devA != null && devB != null) {
                    targetGraph.connect(devA, devB);
                }
            }
        }
    }

    /**
     * Reads a JSON file from disk and restores the target NetworkGraph.
     *
     * @param file        the file to read
     * @param targetGraph the NetworkGraph to populate
     * @throws IOException            if disk read fails
     * @throws ConfigurationException if validation fails
     */
    public static void loadFromFile(File file, NetworkGraph targetGraph) throws IOException, ConfigurationException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null.");
        }
        if (!file.exists() || !file.isFile()) {
            throw new FileNotFoundException("Configuration file not found: " + file.getAbsolutePath());
        }

        String json = Files.readString(file.toPath(), StandardCharsets.UTF_8);
        if (json.trim().isEmpty()) {
            throw new ConfigurationException("Configuration file is empty: " + file.getName());
        }
        loadFromJson(json, targetGraph);
    }
}
