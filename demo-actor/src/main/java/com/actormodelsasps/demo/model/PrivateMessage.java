package com.actormodelsasps.demo.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * PrivateMessage entity representing a 1-on-1 direct message between two users
 */
@Container(containerName = "privateMessages", autoCreateContainer = false)
public class PrivateMessage {
    
    @Id
    private String id;
    
    private String content;
    
    @PartitionKey
    private String senderId;
    
    private String receiverId;
    
    private LocalDateTime timestamp;
    
    private boolean read = false;
    
    private boolean delivered = false;
    
    public PrivateMessage() {
        this.timestamp = LocalDateTime.now();
    }
    
    public PrivateMessage(String content, String senderId, String receiverId) {
        this();
        this.content = content;
        this.senderId = senderId;
        this.receiverId = receiverId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getSenderId() {
        return senderId;
    }
    
    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
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
