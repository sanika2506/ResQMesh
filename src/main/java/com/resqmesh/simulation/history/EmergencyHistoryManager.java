package com.resqmesh.simulation.history;

import com.resqmesh.simulation.SimulationResult.DeliveryOutcome;
import com.resqmesh.simulation.timeline.SimulationRecord;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Service managing the session history and reporting of emergency dispatches.
 * Provides thread-safe list operations, multi-attribute filtering,
 * statistical metrics (success rates, total hops), and formatted report generation.
 */
public class EmergencyHistoryManager {

    private final ObservableList<SimulationRecord> records;

    public EmergencyHistoryManager() {
        this.records = FXCollections.observableArrayList();
    }

    public EmergencyHistoryManager(ObservableList<SimulationRecord> backingList) {
        this.records = Objects.requireNonNullElseGet(backingList, FXCollections::observableArrayList);
    }

    public ObservableList<SimulationRecord> getRecords() {
        return records;
    }

    public void addRecord(SimulationRecord record) {
        if (record != null) {
            records.add(0, record);
        }
    }

    public void clear() {
        records.clear();
    }

    public int size() {
        return records.size();
    }

    public boolean isEmpty() {
        return records.isEmpty();
    }

    public int getTotalCount() {
        return records.size();
    }

    public int getDeliveredCount() {
        return (int) records.stream().filter(SimulationRecord::isDelivered).count();
    }

    public int getFailedCount() {
        return (int) records.stream().filter(r -> r.getOutcome() == DeliveryOutcome.FAILED).count();
    }

    public int getNoRouteCount() {
        return (int) records.stream().filter(r -> r.getOutcome() == DeliveryOutcome.NO_ROUTE_FOUND).count();
    }

    public double getDeliveryRate() {
        if (records.isEmpty()) {
            return 0.0;
        }
        return (double) getDeliveredCount() / records.size() * 100.0;
    }

    public double getAverageHops() {
        List<SimulationRecord> delivered = records.stream()
                .filter(SimulationRecord::isDelivered)
                .toList();
        if (delivered.isEmpty()) {
            return 0.0;
        }
        double totalHops = delivered.stream().mapToInt(SimulationRecord::getHopCount).sum();
        return totalHops / delivered.size();
    }

    /**
     * Checks if a single record matches search and filter criteria.
     *
     * @param record        the simulation record to test
     * @param query         case-insensitive keyword matching ID, message, sender, recipient, or route
     * @param statusFilter  "ALL", "DELIVERED", "FAILED", "NO_ROUTE"
     * @param deviceFilter  "ALL", or specific device name/id
     * @return true if the record satisfies all active filters
     */
    public boolean matches(SimulationRecord record, String query, String statusFilter, String deviceFilter) {
        if (record == null) {
            return false;
        }

        // 1. Text search
        if (query != null && !query.trim().isEmpty()) {
            String q = query.trim().toLowerCase();
            boolean idMatch = record.getId() != null && record.getId().toLowerCase().contains(q);
            boolean senderMatch = record.getSenderName() != null && record.getSenderName().toLowerCase().contains(q);
            boolean recipientMatch = record.getRecipientName() != null && record.getRecipientName().toLowerCase().contains(q);
            boolean msgMatch = record.getMessageContent() != null && record.getMessageContent().toLowerCase().contains(q);
            boolean routeMatch = record.getRoutePath() != null && record.getRoutePath().toLowerCase().contains(q);
            if (!idMatch && !senderMatch && !recipientMatch && !msgMatch && !routeMatch) {
                return false;
            }
        }

        // 2. Status filter
        if (statusFilter != null && !statusFilter.equalsIgnoreCase("ALL") && !statusFilter.trim().isEmpty()) {
            String s = statusFilter.trim().toUpperCase();
            DeliveryOutcome outcome = record.getOutcome();
            if (s.contains("DELIVERED") && outcome != DeliveryOutcome.DELIVERED) {
                return false;
            } else if (s.contains("NO ROUTE") || s.contains("NO_ROUTE")) {
                if (outcome != DeliveryOutcome.NO_ROUTE_FOUND) {
                    return false;
                }
            } else if (s.contains("FAILED") && !s.contains("NO ROUTE")) {
                if (outcome != DeliveryOutcome.FAILED) {
                    return false;
                }
            }
        }

        // 3. Device filter (matches sender or recipient)
        if (deviceFilter != null && !deviceFilter.equalsIgnoreCase("ALL") && !deviceFilter.trim().isEmpty()) {
            String d = deviceFilter.trim().toLowerCase();
            boolean senderMatch = record.getSenderName() != null && record.getSenderName().toLowerCase().equals(d);
            boolean recipientMatch = record.getRecipientName() != null && record.getRecipientName().toLowerCase().equals(d);
            boolean senderIdMatch = record.getMessage() != null && record.getMessage().getSender() != null
                    && record.getMessage().getSender().getId().toLowerCase().equals(d);
            boolean recipientIdMatch = record.getMessage() != null && record.getMessage().getRecipient() != null
                    && record.getMessage().getRecipient().getId().toLowerCase().equals(d);
            if (!senderMatch && !recipientMatch && !senderIdMatch && !recipientIdMatch) {
                return false;
            }
        }

        return true;
    }

    public Predicate<SimulationRecord> createFilterPredicate(String query, String statusFilter, String deviceFilter) {
        return record -> matches(record, query, statusFilter, deviceFilter);
    }

    public List<SimulationRecord> filter(String query, String statusFilter, String deviceFilter) {
        Predicate<SimulationRecord> pred = createFilterPredicate(query, statusFilter, deviceFilter);
        return records.stream().filter(pred).collect(Collectors.toList());
    }

    /**
     * Generates a readable ASCII summary report suitable for clipboard export, logs, or reports.
     */
    public String generateSummaryReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================================================\n");
        sb.append("                   RESQMESH EMERGENCY DISPATCH REPORT                   \n");
        sb.append("========================================================================\n");
        sb.append(String.format("Total Dispatches : %d\n", getTotalCount()));
        sb.append(String.format("Delivered        : %d (%.1f%%)\n", getDeliveredCount(), getDeliveryRate()));
        sb.append(String.format("Failed           : %d\n", getFailedCount()));
        sb.append(String.format("No Route Found   : %d\n", getNoRouteCount()));
        sb.append(String.format("Average Hops     : %.2f\n", getAverageHops()));
        sb.append("------------------------------------------------------------------------\n");
        sb.append(String.format("%-10s %-9s %-12s %-16s %-16s %-6s %s\n",
                "TIME", "ID", "STATUS", "SENDER", "RECIPIENT", "HOPS", "MESSAGE"));
        sb.append("------------------------------------------------------------------------\n");
        for (SimulationRecord r : records) {
            String msg = r.getMessageContent();
            if (msg.length() > 25) {
                msg = msg.substring(0, 22) + "...";
            }
            sb.append(String.format("%-10s %-9s %-12s %-16s %-16s %-6d %s\n",
                    r.getTimestamp(),
                    r.getId(),
                    r.getStatusDisplay(),
                    r.getSenderName(),
                    r.getRecipientName(),
                    r.getHopCount(),
                    msg));
        }
        sb.append("========================================================================\n");
        return sb.toString();
    }
}
