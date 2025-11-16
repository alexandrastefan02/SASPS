package com.actormodelsasps.demo.model;

import java.time.LocalDateTime;

/**
 * Message model representing a chat message
 * 
 * This class will be:
 * - Serialized to JSON when sent over WebSocket
 * - Deserialized from JSON when received
 * - Stored in message history (thread-safe collection)
 */
public class Message {
    
    private String content;           // The actual message text
    private String sender;            // Username of who sent it
    private String recipient;         // Username of who should receive it (for 1-to-1)
    private LocalDateTime timestamp;   // When it was sent
    private MessageType type;          // Type of message (PRIVATE, JOIN, LEAVE, etc.)
    private String conversationId;     // Unique identifier for the conversation
    
    /**
     * Message types for different events
     */
    public enum MessageType {
        PRIVATE,    // Private 1-to-1 message
        JOIN,       // User joined notification
        LEAVE,      // User left notification
        TYPING,     // Typing indicator
        DELIVERY_RECEIPT  // Message delivery confirmation
    }
    
    // Default constructor (required for JSON deserialization)
    public Message() {
    }
    
    // Constructor with parameters
    public Message(String content, String sender, MessageType type) {
        this.content = content;
        this.sender = sender;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
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
    
    public String getRecipient() {
        return recipient;
    }
    
    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }
    
    public String getConversationId() {
        return conversationId;
    }
    
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
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
    
    @Override
    public String toString() {
        return "Message{" +
                "content='" + content + '\'' +
                ", sender='" + sender + '\'' +
                ", timestamp=" + timestamp +
                ", type=" + type +
                '}';
    }
}
