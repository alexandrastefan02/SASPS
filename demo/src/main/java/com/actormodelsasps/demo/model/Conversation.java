package com.actormodelsasps.demo.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;

/**
 * Conversation entity representing a chat conversation (1-on-1 or team-based)
 * This serves as an overview/summary of a conversation
 */
@Container(containerName = "conversations", autoCreateContainer = false)
public class Conversation {
    
    @Id
    private String id;
    
    private ConversationType type;
    
    // For 1-on-1 conversations: stores the other user's ID
    private String participantUserId;
    
    // For team conversations: stores the team ID
    private String teamId;
    
    // The current user who owns this conversation view
    @PartitionKey
    private String userId;
    
    private String lastMessage;
    
    private LocalDateTime lastMessageTime;
    
    private int unreadCount = 0;
    
    private LocalDateTime createdAt;
    
    public enum ConversationType {
        PRIVATE,  // 1-on-1 conversation
        TEAM      // Team/group conversation
    }
    
    public Conversation() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Conversation(ConversationType type, String userId, String participantUserId, String teamId) {
        this();
        this.type = type;
        this.userId = userId;
        this.participantUserId = participantUserId;
        this.teamId = teamId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public ConversationType getType() {
        return type;
    }
    
    public void setType(ConversationType type) {
        this.type = type;
    }
    
    public String getParticipantUserId() {
        return participantUserId;
    }
    
    public void setParticipantUserId(String participantUserId) {
        this.participantUserId = participantUserId;
    }
    
    public String getTeamId() {
        return teamId;
    }
    
    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getLastMessage() {
        return lastMessage;
    }
    
    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }
    
    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }
    
    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
