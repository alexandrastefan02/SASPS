package com.actormodelsasps.demo.model;

import java.time.LocalDateTime;

/**
 * ChatUser model representing a connected user
 * 
 * Tracks:
 * - Username
 * - Session ID
 * - Connection time
 */
public class ChatUser {
    
    private String username;
    private String sessionId;
    private LocalDateTime connectedAt;
    
    // Default constructor
    public ChatUser() {
    }
    
    // Constructor
    public ChatUser(String username, String sessionId) {
        this.username = username;
        this.sessionId = sessionId;
        this.connectedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public LocalDateTime getConnectedAt() {
        return connectedAt;
    }
    
    public void setConnectedAt(LocalDateTime connectedAt) {
        this.connectedAt = connectedAt;
    }
    
    @Override
    public String toString() {
        return "ChatUser{" +
                "username='" + username + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", connectedAt=" + connectedAt +
                '}';
    }
}
