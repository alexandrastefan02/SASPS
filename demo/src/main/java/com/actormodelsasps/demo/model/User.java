package com.actormodelsasps.demo.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;

/**
 * User entity representing registered users
 * 
 * Each user has:
 * - Unique username (used for login and display)
 * - Password (will be encoded)
 * - Registration timestamp
 * - Online status for messaging
 */
@Container(containerName = "users", autoCreateContainer = false)
public class User {
    
    @Id
    private String id;
    
    @PartitionKey
    private String username;
    
    private String password;  // Will be encoded by BCrypt
    
    private LocalDateTime createdAt;
    
    private LocalDateTime lastSeen;
    
    private boolean online = false;
    
    // Store team IDs instead of Team objects to avoid circular references
    private List<String> teamIds = new ArrayList<>();
    
    // Default constructor
    public User() {
        this.createdAt = LocalDateTime.now();
        this.lastSeen = LocalDateTime.now();
    }
    
    // Constructor
    public User(String username, String password) {
        this();
        this.username = username;
        this.password = password;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getLastSeen() {
        return lastSeen;
    }
    
    public void setLastSeen(LocalDateTime lastSeen) {
        this.lastSeen = lastSeen;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
        if (online) {
            this.lastSeen = LocalDateTime.now();
        }
    }
    
    public List<String> getTeamIds() {
        return teamIds;
    }
    
    public void setTeamIds(List<String> teamIds) {
        this.teamIds = teamIds;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(username, user.username);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(username);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", online=" + online +
                ", lastSeen=" + lastSeen +
                '}';
    }
}