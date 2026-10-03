package com.resqmesh.routing;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.CommunicationLink;
import com.resqmesh.model.Priority;

import java.util.*;

/**
 * Discovers the route with the fewest hops between two devices using Breadth-First Search (BFS).
 * Demonstrates:
 * - BFS Algorithm: Explores graph nodes level-by-level using a FIFO Queue to guarantee shortest path (in hop count).
 * - Visited Set: Prevents revisiting nodes, terminating infinite loops caused by mesh cycles.
 * - Parent Map: Records predecessors during traversal to reconstruct the optimal route once the destination is reached.
 * - Dynamic Filtering: Bypasses broken links, dead batteries, offline devices, and devices that cannot forward the given message priority.
 */
public class ShortestPathStrategy implements RoutingStrategy {

    /**
     * Finds the shortest hop route from source to target respecting network conditions and forwarding policies.
     *
     * @param source   originating device
     * @param target   destination device
     * @param graph    active network graph topology
     * @param priority urgency of the message being transmitted
     * @return ordered List of devices [source, ..., target], or an empty list if no valid route exists
     */
    @Override
    public List<CommunicationDevice> findRoute(CommunicationDevice source,
                                                CommunicationDevice target,
                                                NetworkGraph graph,
                                                Priority priority) {
        // Basic validation
        if (source == null || target == null || graph == null || priority == null) {
            return Collections.emptyList();
        }

        // Verify devices are registered in the graph
        if (!graph.containsDevice(source) || !graph.containsDevice(target)) {
            return Collections.emptyList();
        }

        // Both endpoints must be currently operational
        if (!source.isAvailable() || !target.isAvailable()) {
            return Collections.emptyList();
        }

        // Edge case: Source and destination are the same device
        if (source.equals(target)) {
            return Collections.singletonList(source);
        }

        // 1. Queue to explore devices level-by-level (FIFO order)
        Queue<CommunicationDevice> queue = new ArrayDeque<>();

        // 2. Set to track visited devices and avoid cycles
        Set<CommunicationDevice> visited = new HashSet<>();

        // 3. Map to remember the predecessor device for path reconstruction (Child -> Parent)
        Map<CommunicationDevice, CommunicationDevice> previousMap = new HashMap<>();

        // Initialize BFS with the source device
        queue.add(source);
        visited.add(source);
        previousMap.put(source, null);

        boolean destinationFound = false;

        while (!queue.isEmpty()) {
            CommunicationDevice current = queue.poll();

            // Reached the destination
            if (current.equals(target)) {
                destinationFound = true;
                break;
            }

            // Inspect all outgoing links from current device
            for (CommunicationLink link : graph.getLinks(current)) {
                // Ignore inactive connections, broken links, or depleted devices
                if (!link.usable()) {
                    continue;
                }

                CommunicationDevice neighbor = link.getDestination();

                // Skip if already visited
                if (visited.contains(neighbor)) {
                    continue;
                }

                // If neighbor is an intermediate relay (not the final destination),
                // it MUST satisfy the device forwarding rules for this message priority
                if (!neighbor.equals(target) && !neighbor.canForward(priority)) {
                    continue;
                }

                // Mark visited, record parent for route reconstruction, and enqueue
                visited.add(neighbor);
                previousMap.put(neighbor, current);
                queue.add(neighbor);
            }
        }

        // If BFS finished without reaching target, no valid route exists
        if (!destinationFound) {
            return Collections.emptyList();
        }

        // 4. Reconstruct path by backtracking from target to source
        return reconstructRoute(target, previousMap);
    }

    /**
     * Backtracks from the destination device back to the source using the parent map,
     * then reverses the list to produce the correct forward traversal order.
     */
    private List<CommunicationDevice> reconstructRoute(CommunicationDevice target,
                                                        Map<CommunicationDevice, CommunicationDevice> previousMap) {
        List<CommunicationDevice> route = new ArrayList<>();
        CommunicationDevice current = target;

        while (current != null) {
            route.add(current);
            current = previousMap.get(current);
        }

        // Backtracking produces [target, hopN, ..., source]; reverse to get [source, ..., target]
        Collections.reverse(route);
        return Collections.unmodifiableList(route);
    }
}
