package com.actormodelsasps.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Conversation entity representing a chat conversation (1-on-1 or team-based)
 * This serves as an overview/summary of a conversation
 */
@Entity
@Table(name = "conversations")
public class Conversation {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ConversationType type;
    
    // For 1-on-1 conversations: stores the other user's ID
    @Column(name = "participant_user_id")
    private Long participantUserId;
    
    // For team conversations: stores the team ID
    @Column(name = "team_id")
    private Long teamId;
    
    // The current user who owns this conversation view
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "last_message")
    private String lastMessage;
    
    @Column(name = "last_message_time")
    private LocalDateTime lastMessageTime;
    
    @Column(name = "unread_count")
    private int unreadCount = 0;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    public enum ConversationType {
        PRIVATE,  // 1-on-1 conversation
        TEAM      // Team/group conversation
    }
    
    public Conversation() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Conversation(ConversationType type, Long userId, Long participantUserId, Long teamId) {
        this();
        this.type = type;
        this.userId = userId;
        this.participantUserId = participantUserId;
        this.teamId = teamId;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public ConversationType getType() {
        return type;
    }
    
    public void setType(ConversationType type) {
        this.type = type;
    }
    
    public Long getParticipantUserId() {
        return participantUserId;
    }
    
    public void setParticipantUserId(Long participantUserId) {
        this.participantUserId = participantUserId;
    }
    
    public Long getTeamId() {
        return teamId;
    }
    
    public void setTeamId(Long teamId) {
        this.teamId = teamId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
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
