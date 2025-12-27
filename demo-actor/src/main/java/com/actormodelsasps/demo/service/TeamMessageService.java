package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.MessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for handling team-based messaging
 * 
 * Features:
 * - Broadcast messages to all team members
 * - Store messages in database for offline users
 * - Deliver undelivered messages when users come online
 */
@Service
public class TeamMessageService {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private TeamService teamService;
    
    // Store active user sessions (username -> sessionId)
    private final Map<String, String> userSessions = new ConcurrentHashMap<>();
    
    // Store user's current team (username -> teamId)
    private final Map<String, String> userCurrentTeam = new ConcurrentHashMap<>();
    
    /**
     * Register user session
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
        userCurrentTeam.remove(username);
        System.out.println("📱 User session removed: " + username + " (was: " + sessionId + ")");
    }
    
    /**
     * Set user's current active team
     */
    public void setUserCurrentTeam(String username, String teamId) {
        userCurrentTeam.put(username, teamId);
        System.out.println("🏢 User " + username + " switched to team ID: " + teamId);
    }
    
    /**
     * Get user's current team
     */
    public String getUserCurrentTeam(String username) {
        return userCurrentTeam.get(username);
    }
    
    /**
     * Send message to team (broadcast to all members)
     */
    public Message sendTeamMessage(String sender, String teamId, String content) {
        try {
            // Verify user is member of team
            if (!teamService.isUserMemberOfTeam(sender, teamId)) {
                System.out.println("❌ User " + sender + " is not a member of team " + teamId);
                return null;
            }
            
            // Create and save message
            Message message = new Message();
            message.setId(java.util.UUID.randomUUID().toString()); // Generate UUID for Cosmos DB
            message.setSender(sender);
            message.setTeamId(teamId);
            message.setContent(content);
            message.setType(Message.MessageType.CHAT);
            message.setTimestamp(LocalDateTime.now());
            message.setDelivered(false);
            
            Message savedMessage = messageRepository.save(message);
            
            // Get all team members
            List<User> teamMembers = teamService.getTeamMembers(teamId);
            
            // Broadcast to all online members
            int deliveredCount = 0;
            for (User member : teamMembers) {
                String username = member.getUsername();
                
                // Send to online members
                if (userSessions.containsKey(username)) {
                    String destination = "/queue/team/" + teamId + "/messages";
                    messagingTemplate.convertAndSendToUser(username, destination, savedMessage);
                    deliveredCount++;
                    System.out.println("   📤 Sent to online member: " + username);
                }
            }
            
            // Mark as delivered if at least one person received it
            if (deliveredCount > 0) {
                savedMessage.setDelivered(true);
                messageRepository.save(savedMessage);
            }
            
            System.out.println("💬 Team message sent: " + sender + " -> Team " + teamId);
            System.out.println("   Delivered to " + deliveredCount + "/" + teamMembers.size() + " members");
            
            return savedMessage;
            
        } catch (Exception e) {
            System.err.println("❌ Error sending team message: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Deliver undelivered messages to user when they come online
     */
    public void deliverPendingMessages(String username, String teamId) {
        try {
            // Check if user is online
            if (!userSessions.containsKey(username)) {
                return;
            }
            
            // Get undelivered messages for this team
            List<Message> undeliveredMessages = messageRepository
                    .findUndeliveredByTeamId(teamId);
            
            if (undeliveredMessages.isEmpty()) {
                System.out.println("📭 No pending messages for " + username + " in team " + teamId);
                return;
            }
            
            // Send each message to the user
            String destination = "/queue/team/" + teamId + "/messages";
            for (Message message : undeliveredMessages) {
                messagingTemplate.convertAndSendToUser(username, destination, message);
            }
            
            System.out.println("📬 Delivered " + undeliveredMessages.size() + " pending messages to " + username);
            
        } catch (Exception e) {
            System.err.println("❌ Error delivering pending messages: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Get conversation history for a team
     */
    public List<Message> getTeamHistory(String teamId, int limit) {
        // Get all messages and sort by timestamp
        List<Message> messages = messageRepository.findByTeamIdOrderByTimestamp(teamId);
        
        if (limit > 0 && messages.size() > limit) {
            // Return only the most recent messages
            return messages.subList(Math.max(0, messages.size() - limit), messages.size());
        }
        
        return messages;
    }
    
    /**
     * Check if user is online
     */
    public boolean isUserOnline(String username) {
        return userSessions.containsKey(username);
    }
    
    /**
     * Get all online users
     */
    public Set<String> getOnlineUsers() {
        return new HashSet<>(userSessions.keySet());
    }
    
    /**
     * Get online users in a specific team
     */
    public List<String> getOnlineTeamMembers(String teamId) {
        List<User> teamMembers = teamService.getTeamMembers(teamId);
        
        return teamMembers.stream()
                .map(User::getUsername)
                .filter(userSessions::containsKey)
                .toList();
    }
    
    /**
     * Broadcast system message (user joined/left team)
     */
    public void sendSystemMessage(String teamId, String content, Message.MessageType type) {
        Message message = new Message();
        message.setId(java.util.UUID.randomUUID().toString()); // Generate UUID for Cosmos DB
        message.setSender("System");
        message.setTeamId(teamId);
        message.setContent(content);
        message.setType(type);
        message.setTimestamp(LocalDateTime.now());
        message.setDelivered(true);
        
        Message savedMessage = messageRepository.save(message);
        
        // Broadcast to all online team members
        List<User> teamMembers = teamService.getTeamMembers(teamId);
        for (User member : teamMembers) {
            if (userSessions.containsKey(member.getUsername())) {
                String destination = "/queue/team/" + teamId + "/messages";
                messagingTemplate.convertAndSendToUser(member.getUsername(), destination, savedMessage);
            }
        }
        
        System.out.println("📢 System message sent to team " + teamId + ": " + content);
    }
}
