package com.actormodelsasps.demo.model;

import com.azure.spring.data.cosmos.core.mapping.Container;
import com.azure.spring.data.cosmos.core.mapping.PartitionKey;
import org.springframework.data.annotation.Id;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Team entity representing a team/group chat room
 * 
 * Teams are created by users and other users can join them.
 * All members in a team can see and message each other.
 */
@Container(containerName = "teams", autoCreateContainer = false)
public class Team {
    
    @Id
    private String id;
    
    @PartitionKey
    private String name;
    
    private LocalDateTime createdAt;
    
    private String ownerId;  // User who created the team
    
    // Store member IDs instead of User objects
    private List<String> memberIds = new ArrayList<>();
    
    // Default constructor
    public Team() {
        this.createdAt = LocalDateTime.now();
    }
    
    // Constructor
    public Team(String name, String ownerId) {
        this();
        this.name = name;
        this.ownerId = ownerId;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }
    
    public List<String> getMemberIds() {
        return memberIds;
    }
    
    public void setMemberIds(List<String> memberIds) {
        this.memberIds = memberIds;
    }
    
    public void addMemberId(String userId) {
        if (!this.memberIds.contains(userId)) {
            this.memberIds.add(userId);
        }
    }
    
    public void removeMemberId(String userId) {
        this.memberIds.remove(userId);
    }
    
    @Override
    public String toString() {
        return "Team{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", ownerId=" + ownerId +
                ", memberCount=" + memberIds.size() +
                ", createdAt=" + createdAt +
                '}';
    }
}
