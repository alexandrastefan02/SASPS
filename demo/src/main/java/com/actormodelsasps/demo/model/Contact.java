package com.actormodelsasps.demo.model;

/**
 * Contact represents a user's contact for 1-to-1 messaging
 * 
 * For now, this is a simple POJO that represents contacts.
 * In a full implementation, this could be a many-to-many relationship
 * between users, but we'll keep it simple with hardcoded contacts.
 */
public class Contact {
    
    private String username;
    private String displayName;
    private boolean online;
    private int unreadCount;
    
    // Default constructor
    public Contact() {
    }
    
    // Constructor
    public Contact(String username, String displayName) {
        this.username = username;
        this.displayName = displayName;
        this.online = false;
        this.unreadCount = 0;
    }
    
    public Contact(String username, String displayName, boolean online) {
        this.username = username;
        this.displayName = displayName;
        this.online = online;
        this.unreadCount = 0;
    }
    
    // Getters and Setters
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public int getUnreadCount() {
        return unreadCount;
    }
    
    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
    
    public void incrementUnreadCount() {
        this.unreadCount++;
    }
    
    public void resetUnreadCount() {
        this.unreadCount = 0;
    }
    
    @Override
    public String toString() {
        return "Contact{" +
                "username='" + username + '\'' +
                ", displayName='" + displayName + '\'' +
                ", online=" + online +
                ", unreadCount=" + unreadCount +
                '}';
    }
}