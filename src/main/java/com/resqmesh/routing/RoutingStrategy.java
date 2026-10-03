package com.resqmesh.routing;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.Priority;

import java.util.List;

/**
 * Strategy interface defining the contract for discovering communication routes
 * across the ResQMesh network topology.
 * Demonstrates:
 * - Strategy Pattern: Decouples routing algorithms from the network graph and devices,
 *   allowing different algorithms (BFS, Dijkstra, A*, etc.) to be swapped easily.
 */
public interface RoutingStrategy {

    /**
     * Finds an ordered route of communication devices from the source to the target.
     *
     * @param source   the originating device
     * @param target   the destination device
     * @param graph    the network graph topology
     * @param priority the priority level of the message being routed
     * @return an ordered List of CommunicationDevice objects representing the path
     *         [source, ..., target], or an empty list if no route exists
     */
    List<CommunicationDevice> findRoute(CommunicationDevice source,
                                        CommunicationDevice target,
                                        NetworkGraph graph,
                                        Priority priority);
}
