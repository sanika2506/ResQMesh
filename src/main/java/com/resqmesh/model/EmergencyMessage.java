package com.resqmesh.model;

import java.util.Objects;

/**
 * Represents an emergency message packet transmitted across the ResQMesh network.
 * Stores sender, recipient, text content, priority urgency, status, and creation timestamp.
 */
public class EmergencyMessage {

    private final String id;
    private final CommunicationDevice sender;
    private final CommunicationDevice recipient;
    private final String content;
    private final Priority priority;
    private MessageStatus status;
    private final long timestamp;

    /**
     * Constructs a new EmergencyMessage with CREATED status.
     *
     * @param id        unique message identifier (non-empty)
     * @param sender    originating device (non-null)
     * @param recipient intended target device (non-null)
     * @param content   text message payload (non-null)
     * @param priority  urgency level (non-null)
     */
    public EmergencyMessage(String id,
                            CommunicationDevice sender,
                            CommunicationDevice recipient,
                            String content,
                            Priority priority) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("Message ID cannot be null or empty.");
        }
        if (sender == null) {
            throw new IllegalArgumentException("Sender device cannot be null.");
        }
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient device cannot be null.");
        }
        if (content == null) {
            throw new IllegalArgumentException("Message content cannot be null.");
        }
        if (priority == null) {
            throw new IllegalArgumentException("Message priority cannot be null.");
        }

        this.id = id.trim();
        this.sender = sender;
        this.recipient = recipient;
        this.content = content.trim();
        this.priority = priority;
        this.status = MessageStatus.CREATED;
        this.timestamp = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public CommunicationDevice getSender() {
        return sender;
    }

    public CommunicationDevice getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public Priority getPriority() {
        return priority;
    }

    public MessageStatus getStatus() {
        return status;
    }

    public void setStatus(MessageStatus status) {
        if (status == null) {
            throw new IllegalArgumentException("Message status cannot be null.");
        }
        this.status = status;
    }

    public long getTimestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EmergencyMessage that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.format("EmergencyMessage[id='%s', from='%s', to='%s', priority=%s, status=%s, text='%s']",
                id, sender.getName(), recipient.getName(), priority, status, content);
    }
}
