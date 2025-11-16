package com.actormodelsasps.demo.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * User entity representing registered users
 * 
 * Each user has:
 * - Unique username (used for login and display)
 * - Password (will be encoded)
 * - Registration timestamp
 * - Online status for messaging
 */
@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false, length = 50)
    private String username;
    
    @Column(nullable = false)
    private String password;  // Will be encoded by BCrypt
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
    
    @Column(name = "is_online")
    private boolean online = false;
    
    @ManyToMany(mappedBy = "members")
    private Set<Team> teams = new HashSet<>();
    
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
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
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
    
    public Set<Team> getTeams() {
        return teams;
    }
    
    public void setTeams(Set<Team> teams) {
        this.teams = teams;
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