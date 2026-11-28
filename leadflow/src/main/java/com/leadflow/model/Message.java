package com.leadflow.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private int messageId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id", nullable = false)
    private Lead lead;

    @Column(name = "sender_role", nullable = false, length = 20)
    private String senderRole; // CUSTOMER, EMPLOYEE, OWNER, SYSTEM

    @Column(name = "sender_name", nullable = false, length = 100)
    private String senderName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /** Default constructor required by Hibernate. */
    public Message() {}

    /**
     * Constructs a conversation message attached to a lead.
     *
     * @param lead       associated Lead entity
     * @param senderRole role identifier (CUSTOMER, EMPLOYEE, OWNER, SYSTEM)
     * @param senderName display name of sender
     * @param content    message body
     */
    public Message(Lead lead, String senderRole, String senderName, String content) {
        this.lead = lead;
        this.senderRole = senderRole;
        this.senderName = senderName;
        this.content = content;
        this.sentAt = LocalDateTime.now();
    }

    public int getMessageId() { return messageId; }
    public void setMessageId(int messageId) { this.messageId = messageId; }
    public Lead getLead() { return lead; }
    public void setLead(Lead lead) { this.lead = lead; }
    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
}