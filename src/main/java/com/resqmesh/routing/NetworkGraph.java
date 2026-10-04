package com.resqmesh.routing;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.CommunicationLink;

import java.util.*;

/**
 * Represents the offline mesh topology using an Adjacency List graph structure.
 * Demonstrates:
 * - Graph Data Structure: Uses a Map of Devices to their outgoing CommunicationLinks.
 * - Encapsulation: Protects internal collections by returning unmodifiable views.
 * - Defensive Programming: Prevents duplicate links, self-loops, and null insertions.
 */
public class NetworkGraph {

    /**
     * Adjacency list representation: Maps each device node to a list of its outgoing communication links.
     * LinkedHashMap is used to maintain predictable insertion order.
     */
    private final Map<CommunicationDevice, List<CommunicationLink>> adjacencyList;

    /**
     * Initializes an empty network graph.
     */
    public NetworkGraph() {
        this.adjacencyList = new LinkedHashMap<>();
    }

    /**
     * Adds a device node to the network graph if it is not already present.
     *
     * @param device the CommunicationDevice to add (non-null)
     * @return true if the device was added; false if it was already present
     * @throws IllegalArgumentException if the device is null
     */
    public boolean addDevice(CommunicationDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("Cannot add a null device to the network graph.");
        }
        if (adjacencyList.containsKey(device)) {
            return false;
        }
        adjacencyList.put(device, new ArrayList<>());
        return true;
    }

    /**
     * Establishes a bidirectional (two-way) communication link between two devices.
     * If either device is not yet part of the graph, it is automatically registered.
     *
     * @param deviceA the first device
     * @param deviceB the second device
     * @return true if the connection was established; false if already connected
     * @throws IllegalArgumentException if either device is null, or if deviceA equals deviceB
     */
    public boolean connect(CommunicationDevice deviceA, CommunicationDevice deviceB) {
        if (deviceA == null || deviceB == null) {
            throw new IllegalArgumentException("Both devices must be non-null to establish a connection.");
        }
        if (deviceA.equals(deviceB)) {
            throw new IllegalArgumentException("Cannot connect a device to itself: " + deviceA.getId());
        }

        // Ensure both devices are registered in the graph
        addDevice(deviceA);
        addDevice(deviceB);

        // Check for existing connection to prevent duplicates
        if (hasConnection(deviceA, deviceB)) {
            return false; // Connection already exists
        }

        // Create forward (A -> B) and reverse (B -> A) links for bidirectional mesh communication
        CommunicationLink linkForward = new CommunicationLink(deviceA, deviceB);
        CommunicationLink linkReverse = new CommunicationLink(deviceB, deviceA);

        adjacencyList.get(deviceA).add(linkForward);
        adjacencyList.get(deviceB).add(linkReverse);

        return true;
    }

    /**
     * Checks whether an active directed link exists from deviceA to deviceB.
     *
     * @param deviceA the source device
     * @param deviceB the destination device
     * @return true if a link from A to B exists; false otherwise
     */
    public boolean hasConnection(CommunicationDevice deviceA, CommunicationDevice deviceB) {
        if (deviceA == null || deviceB == null || !adjacencyList.containsKey(deviceA)) {
            return false;
        }

        for (CommunicationLink link : adjacencyList.get(deviceA)) {
            if (link.getDestination().equals(deviceB)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether a bidirectional connection exists between deviceA and deviceB.
     *
     * @param deviceA the first device
     * @param deviceB the second device
     * @return true if both devices are directly connected; false otherwise
     */
    public boolean areConnected(CommunicationDevice deviceA, CommunicationDevice deviceB) {
        return hasConnection(deviceA, deviceB) && hasConnection(deviceB, deviceA);
    }

    /**
     * Retrieves an unmodifiable list of outgoing communication links from the specified device.
     *
     * @param device the source device
     * @return unmodifiable List of CommunicationLink objects (empty list if device has no links or not in graph)
     */
    public List<CommunicationLink> getLinks(CommunicationDevice device) {
        if (device == null || !adjacencyList.containsKey(device)) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(adjacencyList.get(device));
    }

    /**
     * Alias method for getLinks to provide clear, expressive naming.
     *
     * @param device the source device
     * @return unmodifiable List of CommunicationLink objects
     */
    public List<CommunicationLink> getLinksFrom(CommunicationDevice device) {
        return getLinks(device);
    }

    /**
     * Retrieves all devices currently registered in the network graph.
     *
     * @return unmodifiable Set of all CommunicationDevice nodes
     */
    public Set<CommunicationDevice> getAllDevices() {
        return Collections.unmodifiableSet(adjacencyList.keySet());
    }

    /**
     * Finds a device in the graph by its unique ID.
     *
     * @param deviceId the unique ID string to search for
     * @return the matching CommunicationDevice, or null if not found
     */
    public CommunicationDevice getDeviceById(String deviceId) {
        if (deviceId == null || deviceId.trim().isEmpty()) {
            return null;
        }
        for (CommunicationDevice device : adjacencyList.keySet()) {
            if (device.getId().equalsIgnoreCase(deviceId.trim())) {
                return device;
            }
        }
        return null;
    }

    /**
     * Returns the total count of device nodes currently in the network graph.
     *
     * @return total device count
     */
    public int getDeviceCount() {
        return adjacencyList.size();
    }

    /**
     * Returns the total count of directed communication links across all devices.
     *
     * @return total directed link count
     */
    public int getTotalLinkCount() {
        int count = 0;
        for (List<CommunicationLink> links : adjacencyList.values()) {
            count += links.size();
        }
        return count;
    }

    /**
     * Checks if the specified device exists in the graph.
     *
     * @param device the device to check
     * @return true if registered; false otherwise
     */
    public boolean containsDevice(CommunicationDevice device) {
        return device != null && adjacencyList.containsKey(device);
    }

    /**
     * Removes the bidirectional connection between two devices.
     *
     * @param deviceA the first device
     * @param deviceB the second device
     * @return true if any link was removed; false otherwise
     */
    public boolean disconnect(CommunicationDevice deviceA, CommunicationDevice deviceB) {
        if (deviceA == null || deviceB == null) {
            return false;
        }

        boolean removedA = false;
        boolean removedB = false;

        if (adjacencyList.containsKey(deviceA)) {
            removedA = adjacencyList.get(deviceA).removeIf(l -> l.getDestination().equals(deviceB));
        }
        if (adjacencyList.containsKey(deviceB)) {
            removedB = adjacencyList.get(deviceB).removeIf(l -> l.getDestination().equals(deviceA));
        }

        return removedA || removedB;
    }

    /**
     * Removes a device node and all its incoming and outgoing links from the graph.
     *
     * @param device the device to remove
     * @return true if device was found and removed; false otherwise
     */
    public boolean removeDevice(CommunicationDevice device) {
        if (device == null || !adjacencyList.containsKey(device)) {
            return false;
        }

        // 1. Remove device and its outgoing links
        adjacencyList.remove(device);

        // 2. Remove all incoming links from remaining devices
        for (List<CommunicationLink> links : adjacencyList.values()) {
            links.removeIf(link -> link.getDestination().equals(device));
        }

        return true;
    }

    /**
     * Removes a device by its unique ID.
     *
     * @param deviceId the ID of the device to remove
     * @return true if device was found and removed; false otherwise
     */
    public boolean removeDeviceById(String deviceId) {
        CommunicationDevice device = getDeviceById(deviceId);
        return device != null && removeDevice(device);
    }

    /**
     * Updates the unique ID of an existing device in the graph, ensuring map key consistency.
     *
     * @param device the device to update
     * @param newId  the new unique ID
     * @return true if updated successfully; false if new ID is duplicate or invalid
     * @throws IllegalArgumentException if device or newId is null/blank
     */
    public boolean updateDeviceId(CommunicationDevice device, String newId) {
        if (device == null) {
            throw new IllegalArgumentException("Device cannot be null.");
        }
        if (newId == null || newId.trim().isEmpty()) {
            throw new IllegalArgumentException("New device ID cannot be null or empty.");
        }
        newId = newId.trim();

        if (device.getId().equalsIgnoreCase(newId)) {
            return true; // No change needed
        }

        if (getDeviceById(newId) != null) {
            return false; // Duplicate ID exists
        }

        // Must re-key in adjacencyList since hashCode/equals depend on ID
        List<CommunicationLink> outgoing = adjacencyList.remove(device);
        if (outgoing == null) {
            return false; // Device not in graph
        }

        device.setId(newId);
        adjacencyList.put(device, outgoing);
        return true;
    }

    /**
     * Retrieves the direct connected neighbors of a device.
     *
     * @param device the device to query
     * @return unmodifiable list of connected devices
     */
    public List<CommunicationDevice> getNeighbors(CommunicationDevice device) {
        if (device == null || !adjacencyList.containsKey(device)) {
            return Collections.emptyList();
        }
        List<CommunicationDevice> neighbors = new ArrayList<>();
        for (CommunicationLink link : adjacencyList.get(device)) {
            neighbors.add(link.getDestination());
        }
        return Collections.unmodifiableList(neighbors);
    }

    /**
     * Returns the count of connected neighbors for the specified device.
     *
     * @param device the device to query
     * @return number of connected neighbors
     */
    public int getConnectionCount(CommunicationDevice device) {
        if (device == null || !adjacencyList.containsKey(device)) {
            return 0;
        }
        return adjacencyList.get(device).size();
    }

    /**
     * Clears all devices and links from the network graph.
     */
    public void clear() {
        adjacencyList.clear();
    }
}
