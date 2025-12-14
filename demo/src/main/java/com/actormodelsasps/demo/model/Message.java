package com.actormodelsasps.demo.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * Message entity representing a chat message in a team
 * 
 * Messages are persisted in the database so they can be:
 * - Delivered to offline users when they come online
 * - Retrieved as conversation history
 * - Stored permanently
 */
@Container(containerName = "messages", autoCreateContainer = false)
public class Message {
    
    @Id
    private String id;
    
    private String content;           // The actual message text
    
    private String sender;            // Username of who sent it
    
    @PartitionKey
    private String teamId;            // Which team this message belongs to (or "private" for private messages)
    
    private String receiverId;        // For private messages: ID of the receiver (null for team messages)
    
    private LocalDateTime timestamp;   // When it was sent
    
    private MessageType type;          // Type of message
    
    private boolean delivered = false; // Has this been delivered to all recipients?
    
    private boolean read = false;      // For private messages: has it been read?
    
    /**
     * Message types for different events
     */
    public enum MessageType {
        CHAT,       // Regular chat message
        JOIN,       // User joined team notification
        LEAVE,      // User left team notification
        SYSTEM,     // System message
        PRIVATE     // Private 1-on-1 message
    }
    
    // Default constructor (required for JPA)
    public Message() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructor with parameters
    public Message(String content, String sender, String teamId, MessageType type) {
        this();
        this.content = content;
        this.sender = sender;
        this.teamId = teamId;
        this.type = type;
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
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getTeamId() {
        return teamId;
    }
    
    public void setTeamId(String teamId) {
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
    
    public String getReceiverId() {
        return receiverId;
    }
    
    public void setReceiverId(String receiverId) {
        this.receiverId = receiverId;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void setRead(boolean read) {
        this.read = read;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", teamId=" + teamId +
                ", receiverId='" + receiverId + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                ", delivered=" + delivered +
                ", read=" + read +
                '}';
    }
}
