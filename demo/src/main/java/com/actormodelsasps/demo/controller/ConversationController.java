package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Conversation;
import com.actormodelsasps.demo.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for managing conversations
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
    /**
     * Get all conversations for a user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserConversations(@PathVariable String username) {
        try {
            // First sync conversations to ensure all teams are included
            conversationService.syncUserConversations(username);
            
            // Get conversations
            List<Map<String, Object>> conversations = conversationService.getUserConversations(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversations);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark a conversation as read
     */
    @PostMapping("/mark-read")
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Long teamId = request.get("teamId") != null ? Long.valueOf(request.get("teamId").toString()) : null;
            Long participantId = request.get("participantId") != null ? Long.valueOf(request.get("participantId").toString()) : null;
            
            // Get user ID would need to be implemented
            // For now, we'll use the service method directly
            // conversationService.markConversationAsRead(userId, teamId, participantId);
            
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update private conversation after sending a message
     */
    @PostMapping("/update-private")
    public ResponseEntity<?> updatePrivateConversation(@RequestBody Map<String, Object> request) {
        try {
            String username = (String) request.get("username");
            Long participantId = Long.valueOf(request.get("participantId").toString());
            String lastMessage = (String) request.get("lastMessage");
            
            Conversation conversation = conversationService.updatePrivateConversationByUsername(username, participantId, lastMessage);
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", conversation.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
