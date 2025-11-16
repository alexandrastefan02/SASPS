package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling private 1-to-1 messaging
 * 
 * This replaces the pub/sub broadcast approach with direct user-to-user messaging.
 * Messages are sent directly to specific users rather than broadcast to all.
 */
@Service
public class PrivateMessageService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private ContactService contactService;
    
    // Store active user sessions (username -> sessionId)
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    // Store message history per conversation (conversationId -> List<Message>)
    private final Map<String, List<Message>> conversationHistory = new ConcurrentHashMap<>();
    
    // Store typing indicators (conversationId -> Set<username>)
    private final Map<String, Set<String>> typingIndicators = new ConcurrentHashMap<>();
    
    /**
     * Register user session for private messaging
     */
    public void registerUserSession(String username, String sessionId) {
        userSessions.put(username, sessionId);
        System.out.println("📱 User session registered: " + username + " -> " + sessionId);
    }
    
    /**
     * Remove user session
     */
    public void removeUserSession(String username) {
        String sessionId = userSessions.remove(username);
        System.out.println("📱 User session removed: " + username + " (was: " + sessionId + ")");
    }
    
    /**
     * Send private message between two users
     */
    public boolean sendPrivateMessage(String sender, String recipient, String content) {
        try {
            // Check if users are contacts
            if (!contactService.areUsersContacts(sender, recipient)) {
                System.out.println("❌ Users are not contacts: " + sender + " -> " + recipient);
                return false;
            }
            
            // Check if recipient is online
            String recipientSessionId = userSessions.get(recipient);
            if (recipientSessionId == null) {
                System.out.println("📵 Recipient is offline: " + recipient);
                // In a full implementation, you might queue the message for later delivery
                return false;
            }
            
            // Create message
            Message message = new Message();
            message.setSender(sender);
            message.setRecipient(recipient);
            message.setContent(content);
            message.setType(Message.MessageType.PRIVATE);
            message.setTimestamp(LocalDateTime.now());
            
            // Generate conversation ID (consistent regardless of who sends first)
            String conversationId = generateConversationId(sender, recipient);
            message.setConversationId(conversationId);
            
            // Store in conversation history
            conversationHistory.computeIfAbsent(conversationId, k -> new ArrayList<>()).add(message);
            
            // Send to recipient via their private queue
            String recipientQueue = "/queue/messages/" + recipient;
            messagingTemplate.convertAndSend(recipientQueue, message);
            
            // Also send confirmation to sender (optional)
            String senderQueue = "/queue/messages/" + sender;
            messagingTemplate.convertAndSend(senderQueue, message);
            
            System.out.println("💬 Private message sent: " + sender + " -> " + recipient + ": \"" + content + "\"");
            return true;
            
        } catch (Exception e) {
            System.err.println("❌ Error sending private message: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get conversation history between two users
     */
    public List<Message> getConversationHistory(String user1, String user2, int limit) {
        String conversationId = generateConversationId(user1, user2);
        List<Message> messages = conversationHistory.getOrDefault(conversationId, new ArrayList<>());
        
        // Return last 'limit' messages
        int fromIndex = Math.max(0, messages.size() - limit);
        List<Message> recentMessages = messages.subList(fromIndex, messages.size());
        
        System.out.println("📜 Retrieved " + recentMessages.size() + " messages for conversation: " + conversationId);
        return new ArrayList<>(recentMessages);
    }
    
    /**
     * Handle typing indicator
     */
    public void handleTypingIndicator(String sender, String recipient, boolean isTyping) {
        String conversationId = generateConversationId(sender, recipient);
        
        Set<String> typingUsers = typingIndicators.computeIfAbsent(conversationId, k -> new HashSet<>());
        
        if (isTyping) {
            typingUsers.add(sender);
        } else {
            typingUsers.remove(sender);
        }
        
        // Send typing indicator to recipient
        String recipientQueue = "/queue/typing/" + recipient;
        Map<String, Object> typingData = Map.of(
            "sender", sender,
            "isTyping", isTyping,
            "conversationId", conversationId
        );
        
        messagingTemplate.convertAndSend(recipientQueue, typingData);
        
        System.out.println("⌨️ Typing indicator: " + sender + " -> " + recipient + " (typing: " + isTyping + ")");
    }
    
    /**
     * Generate consistent conversation ID for two users
     */
    private String generateConversationId(String user1, String user2) {
        // Sort usernames to ensure consistent ID regardless of order
        String[] users = {user1.toLowerCase(), user2.toLowerCase()};
        Arrays.sort(users);
        return users[0] + "_" + users[1];
    }
    
    /**
     * Get active users (for debugging)
     */
    public Set<String> getActiveUsers() {
        return new HashSet<>(userSessions.keySet());
    }
    
    /**
     * Get conversation statistics (for debugging)
     */
    public Map<String, Integer> getConversationStats() {
        Map<String, Integer> stats = new HashMap<>();
        for (Map.Entry<String, List<Message>> entry : conversationHistory.entrySet()) {
            stats.put(entry.getKey(), entry.getValue().size());
        }
        return stats;
    }
    
    /**
     * Check if user is online
     */
    public boolean isUserOnline(String username) {
        return userSessions.containsKey(username);
    }
}