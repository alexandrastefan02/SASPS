package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.MessageRepository;
import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for private message operations
 * 
 * Uses the 'messages' container with teamId='private' for private messages
 */
@RestController
@RequestMapping("/api/private-messages")
@CrossOrigin(origins = "*")
public class PrivateMessageRestController {
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get message history between current user and another user
     * Uses messages container with teamId='private'
     */
    @GetMapping("/history")
    public ResponseEntity<?> getMessageHistory(@RequestParam String participantId,
                                               @RequestParam(required = false) String username) {
        try {
            // Get current user by username parameter
            if (username == null || username.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error", "Username parameter required"));
            }
            
            User currentUser = userService.getUserByUsername(username);
            if (currentUser == null) {
                return ResponseEntity.status(401).body(Map.of("error", "User not found: " + username));
            }
            
            // Get private messages between current user and participant
            List<Message> messages = messageRepository.findPrivateMessagesBetweenUsers(
                currentUser.getId(), participantId
            );
            
            // Convert to response format
            List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("id", msg.getId());
                    msgMap.put("content", msg.getContent());
                    msgMap.put("senderId", msg.getSender());
                    msgMap.put("receiverId", msg.getReceiverId());
                    msgMap.put("timestamp", msg.getTimestamp().toString());
                    msgMap.put("read", msg.isRead());
                    return msgMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("success", true, "messages", messageList));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
