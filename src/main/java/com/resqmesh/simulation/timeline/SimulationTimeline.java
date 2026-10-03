package com.resqmesh.simulation.timeline;

import com.resqmesh.model.CommunicationDevice;
import com.resqmesh.model.EmergencyMessage;
import com.resqmesh.simulation.SimulationResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Encapsulates the ordered sequence of simulation events generated during
 * an emergency message transmission attempt.
 *
 * Provides factory methods to construct timelines faithfully from SimulationResult
 * without mutating or re-running the underlying graph or BFS algorithms.
 */
public class SimulationTimeline {

    public static final long DISCOVERY_DELAY_MS = 200;
    public static final long HOP_DELAY_MS = 600;

    private final String messageId;
    private final CommunicationDevice sender;
    private final CommunicationDevice recipient;
    private final boolean delivered;
    private final List<SimulationEvent> events;
    private final List<CommunicationDevice> route;

    public SimulationTimeline(String messageId,
                              CommunicationDevice sender,
                              CommunicationDevice recipient,
                              boolean delivered,
                              List<SimulationEvent> events,
                              List<CommunicationDevice> route) {
        this.messageId = messageId;
        this.sender = sender;
        this.recipient = recipient;
        this.delivered = delivered;
        this.events = (events != null) ? Collections.unmodifiableList(new ArrayList<>(events)) : Collections.emptyList();
        this.route = (route != null) ? Collections.unmodifiableList(new ArrayList<>(route)) : Collections.emptyList();
    }

    /**
     * Builds a chronological SimulationTimeline faithfully from an EmergencyMessage and SimulationResult.
     *
     * @param message the message transmitted
     * @param result  the outcome returned by SimulationEngine
     * @return non-null, immutable SimulationTimeline
     */
    public static SimulationTimeline fromSimulation(EmergencyMessage message, SimulationResult result) {
        if (message == null || result == null) {
            throw new IllegalArgumentException("EmergencyMessage and SimulationResult cannot be null.");
        }

        List<SimulationEvent> list = new ArrayList<>();
        int seq = 1;
        long elapsed = 0;

        CommunicationDevice sender = message.getSender();
        CommunicationDevice recipient = message.getRecipient();
        List<CommunicationDevice> route = result.getRoute();
        int totalHops = Math.max(0, route.size() - 1);

        // 1. Message Created Event
        list.add(new SimulationEvent(
                seq++,
                elapsed,
                SimulationEventType.MESSAGE_CREATED,
                sender,
                recipient,
                String.format("Emergency message [%s] created by '%s' with priority %s. Payload: \"%s\"",
                        message.getId(), sender.getName(), message.getPriority(), message.getContent()),
                0,
                totalHops
        ));

        if (result.isDelivered()) {
            // 2. Route Discovered Event
            elapsed += DISCOVERY_DELAY_MS;
            String pathStr = route.stream().map(CommunicationDevice::getName).collect(Collectors.joining(" ──▶ "));
            list.add(new SimulationEvent(
                    seq++,
                    elapsed,
                    SimulationEventType.ROUTE_DISCOVERED,
                    sender,
                    recipient,
                    String.format("BFS shortest path discovered in %d hop(s) [%d nodes]: %s",
                            totalHops, route.size(), pathStr),
                    0,
                    totalHops
            ));

            // 3. Sequential Hop Relays
            for (int i = 0; i < route.size() - 1; i++) {
                CommunicationDevice from = route.get(i);
                CommunicationDevice to = route.get(i + 1);
                elapsed += HOP_DELAY_MS;

                list.add(new SimulationEvent(
                        seq++,
                        elapsed,
                        SimulationEventType.HOP_FORWARDING,
                        from,
                        to,
                        String.format("Hop %d/%d: Packet forwarded from '%s' to '%s' (-2.0%% battery consumed).",
                                (i + 1), totalHops, from.getName(), to.getName()),
                        (i + 1),
                        totalHops
                ));
            }

            // 4. Message Delivered Event
            elapsed += (HOP_DELAY_MS / 2);
            list.add(new SimulationEvent(
                    seq++,
                    elapsed,
                    SimulationEventType.MESSAGE_DELIVERED,
                    recipient,
                    null,
                    String.format("Message [%s] successfully delivered and acknowledged by target recipient '%s'.",
                            message.getId(), recipient.getName()),
                    totalHops,
                    totalHops
            ));
        } else {
            // Failure Event: No forwarding hops are simulated
            elapsed += DISCOVERY_DELAY_MS;
            list.add(new SimulationEvent(
                    seq++,
                    elapsed,
                    SimulationEventType.DELIVERY_FAILED,
                    sender,
                    recipient,
                    String.format("Delivery failed: %s", result.getExplanation()),
                    0,
                    0
            ));
        }

        return new SimulationTimeline(message.getId(), sender, recipient, result.isDelivered(), list, route);
    }

    public String getMessageId() {
        return messageId;
    }

    public CommunicationDevice getSender() {
        return sender;
    }

    public CommunicationDevice getRecipient() {
        return recipient;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public boolean isEmpty() {
        return events.isEmpty();
    }

    public List<SimulationEvent> getEvents() {
        return events;
    }

    public List<CommunicationDevice> getRoute() {
        return route;
    }

    public int size() {
        return events.size();
    }

    public SimulationEvent getEvent(int index) {
        return events.get(index);
    }

    public long getTotalDurationMs() {
        if (events.isEmpty()) return 0;
        return events.get(events.size() - 1).getElapsedMs();
    }
}
