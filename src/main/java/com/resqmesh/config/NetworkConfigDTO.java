package com.resqmesh.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Transfer Object (DTO) modeling the serialized JSON structure of a ResQMesh network configuration.
 */
public class NetworkConfigDTO {

    private String name;
    private String version;
    private String savedAt;
    private List<DeviceDTO> devices = new ArrayList<>();
    private List<ConnectionDTO> connections = new ArrayList<>();

    public NetworkConfigDTO() {}

    public NetworkConfigDTO(String name, String version, String savedAt) {
        this.name = name;
        this.version = version;
        this.savedAt = savedAt;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getSavedAt() { return savedAt; }
    public void setSavedAt(String savedAt) { this.savedAt = savedAt; }

    public List<DeviceDTO> getDevices() { return devices; }
    public void setDevices(List<DeviceDTO> devices) { this.devices = devices; }

    public List<ConnectionDTO> getConnections() { return connections; }
    public void setConnections(List<ConnectionDTO> connections) { this.connections = connections; }

    /**
     * DTO representing an individual device node.
     */
    public static class DeviceDTO {
        private String id;
        private String name;
        private String type; // STUDENT_PHONE, SECURITY_STATION, MEDICAL_STATION
        private double batteryLevel;
        private String status; // ACTIVE, LOW_BATTERY, OFFLINE
        private LocationDTO location;

        public DeviceDTO() {}

        public DeviceDTO(String id, String name, String type, double batteryLevel, String status, LocationDTO location) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.batteryLevel = batteryLevel;
            this.status = status;
            this.location = location;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public double getBatteryLevel() { return batteryLevel; }
        public void setBatteryLevel(double batteryLevel) { this.batteryLevel = batteryLevel; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public LocationDTO getLocation() { return location; }
        public void setLocation(LocationDTO location) { this.location = location; }
    }

    /**
     * DTO representing spatial coordinates.
     */
    public static class LocationDTO {
        private double x;
        private double y;

        public LocationDTO() {}

        public LocationDTO(double x, double y) {
            this.x = x;
            this.y = y;
        }

        public double getX() { return x; }
        public void setX(double x) { this.x = x; }

        public double getY() { return y; }
        public void setY(double y) { this.y = y; }
    }

    /**
     * DTO representing a bidirectional connection between two device nodes.
     */
    public static class ConnectionDTO {
        private String nodeA;
        private String nodeB;

        public ConnectionDTO() {}

        public ConnectionDTO(String nodeA, String nodeB) {
            this.nodeA = nodeA;
            this.nodeB = nodeB;
        }

        public String getNodeA() { return nodeA; }
        public void setNodeA(String nodeA) { this.nodeA = nodeA; }

        public String getNodeB() { return nodeB; }
        public void setNodeB(String nodeB) { this.nodeB = nodeB; }
    }
}
