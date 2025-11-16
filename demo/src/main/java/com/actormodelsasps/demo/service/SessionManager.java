package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.ChatUser;
import com.actormodelsasps.demo.model.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SessionManager - Manages all WebSocket sessions and messages
 * 
 * THREAD-SAFE IMPLEMENTATION (Classic Thread Approach)
 * 
 * Uses thread-safe collections:
 * - ConcurrentHashMap: For user sessions (multiple threads can read/write)
 * - CopyOnWriteArrayList: For message history (optimized for many reads, few writes)
 * 
 * Why thread-safe?
 * Multiple threads from Tomcat's thread pool will access these collections
 * simultaneously when handling multiple WebSocket messages.
 */
@Service
public class SessionManager {
    
    // ═══════════════════════════════════════════════════════════
    // THREAD-SAFE COLLECTIONS
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Active users mapped by session ID
     * 
     * ConcurrentHashMap provides:
     * - Thread-safe put/get/remove operations
     * - No locking needed for these operations
     * - Allows concurrent reads from multiple threads
     */
    private final ConcurrentHashMap<String, ChatUser> activeUsers 
        = new ConcurrentHashMap<>();
    
    /**
     * Message history
     * 
     * CopyOnWriteArrayList provides:
     * - Thread-safe add/get operations
     * - Great for many reads, occasional writes
     * - Creates a new copy when adding (slightly expensive)
     * - All reads see consistent snapshot (no locks needed!)
     */
    private final CopyOnWriteArrayList<Message> messageHistory 
        = new CopyOnWriteArrayList<>();
    
    // ═══════════════════════════════════════════════════════════
    // USER MANAGEMENT
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Add a new user (called when WebSocket connects)
     * 
     * Thread-safe: ConcurrentHashMap handles synchronization
     */
    public void addUser(String sessionId, String username) {
        ChatUser user = new ChatUser(username, sessionId);
        activeUsers.put(sessionId, user);
        
        System.out.println("👤 [" + Thread.currentThread().getName() + 
                          "] User added: " + username + " (Session: " + sessionId + ")");
        System.out.println("📊 Active users: " + activeUsers.size());
    }
    
    /**
     * Remove a user (called when WebSocket disconnects)
     * 
     * Thread-safe: ConcurrentHashMap handles synchronization
     */
    public ChatUser removeUser(String sessionId) {
        ChatUser removedUser = activeUsers.remove(sessionId);
        
        if (removedUser != null) {
            System.out.println("👋 [" + Thread.currentThread().getName() + 
                              "] User removed: " + removedUser.getUsername());
            System.out.println("📊 Active users: " + activeUsers.size());
        }
        
        return removedUser;
    }
    
    /**
     * Get user by session ID
     */
    public ChatUser getUser(String sessionId) {
        return activeUsers.get(sessionId);
    }
    
    /**
     * Get all active users
     * 
     * Returns a new list to avoid concurrent modification issues
     */
    public List<ChatUser> getAllUsers() {
        return new ArrayList<>(activeUsers.values());
    }
    
    /**
     * Get count of active users
     */
    public int getActiveUserCount() {
        return activeUsers.size();
    }
    
    // ═══════════════════════════════════════════════════════════
    // MESSAGE MANAGEMENT
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Add message to history
     * 
     * Thread-safe: CopyOnWriteArrayList handles synchronization
     * 
     * How it works internally:
     * 1. Creates a copy of the array
     * 2. Adds the new message to the copy
     * 3. Atomically replaces the old array with the new one
     * 
     * This is expensive for writes but FAST for reads (no locks!)
     */
    public void addMessage(Message message) {
        messageHistory.add(message);
        
        System.out.println("💬 [" + Thread.currentThread().getName() + 
                          "] Message stored: " + message.getSender() + ": " + message.getContent());
        System.out.println("📊 Total messages: " + messageHistory.size());
    }
    
    /**
     * Get recent messages
     * 
     * Thread-safe: Reading from CopyOnWriteArrayList never blocks
     * 
     * @param count Number of recent messages to retrieve
     * @return List of recent messages
     */
    public List<Message> getRecentMessages(int count) {
        int size = messageHistory.size();
        int fromIndex = Math.max(0, size - count);
        
        // subList creates a view, so we copy to a new ArrayList
        return new ArrayList<>(messageHistory.subList(fromIndex, size));
    }
    
    /**
     * Get all messages
     */
    public List<Message> getAllMessages() {
        return new ArrayList<>(messageHistory);
    }
    
    /**
     * Get message count
     */
    public int getMessageCount() {
        return messageHistory.size();
    }
    
    /**
     * Clear all messages (for testing)
     */
    public void clearMessages() {
        messageHistory.clear();
        System.out.println("🗑️ Message history cleared");
    }
    
    // ═══════════════════════════════════════════════════════════
    // STATISTICS
    // ═══════════════════════════════════════════════════════════
    
    /**
     * Print current statistics
     */
    public void printStats() {
        System.out.println("\n📊 ═══════ SERVER STATISTICS ═══════");
        System.out.println("   Active Users: " + getActiveUserCount());
        System.out.println("   Total Messages: " + getMessageCount());
        System.out.println("   Online: ");
        for (ChatUser user : getAllUsers()) {
            System.out.println("      - " + user.getUsername());
        }
        System.out.println("═══════════════════════════════════════\n");
    }
}
