package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.service.TeamMessageService;
import com.actormodelsasps.demo.service.TeamService;
import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * WebSocket controller for team-based messaging
 * 
 * Handles:
 * - User registration for team messaging
 * - Sending messages to team (broadcast to all members)
 * - Joining/switching teams
 * - Delivering offline messages
 */
@Controller
public class TeamMessageController {
    
    @Autowired
    private TeamMessageService teamMessageService;
    
    @Autowired
    private TeamService teamService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Register user for team messaging
     * 
     * Route: /app/team.register
     */
    @MessageMapping("/team.register")
    public void registerUser(@Payload Map<String, String> payload,
                            SimpMessageHeaderAccessor headerAccessor) {
        
        String username = payload.get("username");
        String sessionId = headerAccessor.getSessionId();
        
        if (username == null || username.trim().isEmpty()) {
            System.err.println("❌ Registration failed: No username provided");
            return;
        }
        
        System.out.println("\n👤 ══════════ USER CONNECTING ══════════");
        System.out.println("   Username: " + username);
        System.out.println("   Session ID: " + sessionId);
        System.out.println("   Thread: " + Thread.currentThread().getName());
        
        // CRITICAL: Set user principal for Spring's user destination resolution
        headerAccessor.setUser(() -> username);
        System.out.println("👤 User Principal set for session: " + username);
        
        // Register user session
        teamMessageService.registerUserSession(username, sessionId);
        
        // Set user as online in database
        userService.setUserOnline(username, true);
        
        System.out.println("   ✅ User registered for team messaging");
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Join a team and receive pending messages
     * 
     * Route: /app/team.join
     */
    @MessageMapping("/team.join")
    public void joinTeam(@Payload Map<String, Object> payload,
                        SimpMessageHeaderAccessor headerAccessor) {
        
        String username = (String) payload.get("username");
        Long teamId = Long.valueOf(payload.get("teamId").toString());
        
        if (username == null || teamId == null) {
            System.err.println("❌ Join team failed: Missing username or teamId");
            return;
        }
        
        System.out.println("\n🏢 ══════════ USER JOINING TEAM ══════════");
        System.out.println("   Username: " + username);
        System.out.println("   Team ID: " + teamId);
        
        // Verify user is member of team
        if (!teamService.isUserMemberOfTeam(username, teamId)) {
            System.err.println("❌ User " + username + " is not a member of team " + teamId);
            return;
        }
        
        // Set user's current team
        teamMessageService.setUserCurrentTeam(username, teamId);
        
        // Deliver any pending/undelivered messages
        teamMessageService.deliverPendingMessages(username, teamId);
        
        // Send system message that user joined
        teamMessageService.sendSystemMessage(teamId, username + " joined the team", Message.MessageType.JOIN);
        
        System.out.println("   ✅ User joined team successfully");
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Send message to team
     * 
     * Route: /app/team.send
     */
    @MessageMapping("/team.send")
    public void sendTeamMessage(@Payload Map<String, Object> payload,
                               SimpMessageHeaderAccessor headerAccessor) {
        
        String sender = (String) payload.get("sender");
        Long teamId = Long.valueOf(payload.get("teamId").toString());
        String content = (String) payload.get("content");
        
        System.out.println("\n💬 ══════════ TEAM MESSAGE ══════════");
        System.out.println("   From: " + sender);
        System.out.println("   Team ID: " + teamId);
        System.out.println("   Content: " + content);
        System.out.println("   Session: " + headerAccessor.getSessionId());
        System.out.println("   Thread: " + Thread.currentThread().getName());
        
        // Validate
        if (sender == null || teamId == null || content == null) {
            System.err.println("❌ Invalid message: missing sender, teamId, or content");
            return;
        }
        
        // Send message to team (broadcasts to all online members, saves for offline)
        Message message = teamMessageService.sendTeamMessage(sender, teamId, content);
        
        if (message != null) {
            System.out.println("   ✅ Message sent successfully (ID: " + message.getId() + ")");
        } else {
            System.out.println("   ❌ Message delivery failed");
        }
        
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Get team conversation history
     * 
     * Route: /app/team.history
     */
    @MessageMapping("/team.history")
    public void getTeamHistory(@Payload Map<String, Object> payload,
                              SimpMessageHeaderAccessor headerAccessor) {
        
        Long teamId = Long.valueOf(payload.get("teamId").toString());
        int limit = payload.containsKey("limit") ? 
                    Integer.parseInt(payload.get("limit").toString()) : 50;
        
        System.out.println("📜 History request for team " + teamId + " (limit: " + limit + ")");
        
        // Get conversation history
        List<Message> history = teamMessageService.getTeamHistory(teamId, limit);
        
        System.out.println("📜 Retrieved " + history.size() + " messages for team " + teamId);
        
        // Note: In a full implementation, you'd send this back to the requester
        // For now, messages will be delivered when they join the team
    }
    
    /**
     * Leave team
     * 
     * Route: /app/team.leave
     */
    @MessageMapping("/team.leave")
    public void leaveTeam(@Payload Map<String, Object> payload,
                         SimpMessageHeaderAccessor headerAccessor) {
        
        String username = (String) payload.get("username");
        Long teamId = Long.valueOf(payload.get("teamId").toString());
        
        if (username == null || teamId == null) {
            return;
        }
        
        System.out.println("👋 User " + username + " leaving team " + teamId);
        
        // Send system message
        teamMessageService.sendSystemMessage(teamId, username + " left the team", Message.MessageType.LEAVE);
        
        // Clear user's current team
        if (teamId.equals(teamMessageService.getUserCurrentTeam(username))) {
            teamMessageService.setUserCurrentTeam(username, null);
        }
    }
}
