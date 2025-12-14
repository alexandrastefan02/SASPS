package com.actormodelsasps.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * PrivateMessage entity representing a 1-on-1 direct message between two users
 */
@Entity
@Table(name = "private_messages")
public class PrivateMessage {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "sender_id", nullable = false)
    private Long senderId;
    
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;
    
    @Column(nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "is_read")
    private boolean read = false;
    
    @Column(name = "is_delivered")
    private boolean delivered = false;
    
    public PrivateMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public PrivateMessage(String content, Long senderId, Long receiverId) {
        this();
        this.content = content;
        this.senderId = senderId;
        this.receiverId = receiverId;
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
    
    public Long getSenderId() {
        return senderId;
    }
    
    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }
    
    public Long getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    public boolean isDelivered() {
        return delivered;
    }
    
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
    }
}
