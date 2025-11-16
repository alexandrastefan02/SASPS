package com.actormodelsasps.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Message entity representing a chat message in a team
 * 
 * Messages are persisted in the database so they can be:
 * - Delivered to offline users when they come online
 * - Retrieved as conversation history
 * - Stored permanently
 */
@Entity
@Table(name = "messages")
public class Message {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;           // The actual message text
    
    @Column(nullable = false)
    private String sender;            // Username of who sent it
    
    @Column(name = "team_id", nullable = false)
    private Long teamId;              // Which team this message belongs to
    
    @Column(nullable = false)
    private LocalDateTime timestamp;   // When it was sent
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageType type;          // Type of message
    
    @Column(name = "is_delivered")
    private boolean delivered = false; // Has this been delivered to all recipients?
    
    /**
     * Message types for different events
     */
    public enum MessageType {
        CHAT,       // Regular chat message
        JOIN,       // User joined team notification
        LEAVE,      // User left team notification
        SYSTEM      // System message
    }
    
    // Default constructor (required for JPA)
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor with parameters
    public Message(String content, String sender, Long teamId, MessageType type) {
        this();
        this.content = content;
        this.sender = sender;
        this.teamId = teamId;
        this.type = type;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public Long getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public boolean isDelivered() {
        return delivered;
    }
    
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", teamId=" + teamId +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", delivered=" + delivered +
                '}';
    }
}
