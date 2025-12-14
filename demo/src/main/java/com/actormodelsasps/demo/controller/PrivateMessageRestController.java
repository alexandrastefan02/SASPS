package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.PrivateMessage;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.PrivateMessageRepository;
import com.actormodelsasps.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for private message operations
 */
@RestController
@RequestMapping("/api/private-messages")
@CrossOrigin(origins = "*")
public class PrivateMessageRestController {
    
    @Autowired
    private PrivateMessageRepository privateMessageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Get message history between current user and another user
     */
    @GetMapping("/history")
    public ResponseEntity<?> getMessageHistory(@RequestParam Long participantId,
                                               @RequestParam(required = false) String username) {
        try {
            // Get username from parameter (passed from frontend session)
            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username required"));
            }
            
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Get messages between the two users
            List<PrivateMessage> messages = privateMessageRepository.findMessagesBetweenUsers(
                currentUser.getId(), 
                participantId
            );
            
            // Convert to response format
            List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("id", msg.getId());
                    msgMap.put("content", msg.getContent());
                    msgMap.put("senderId", msg.getSenderId());
                    msgMap.put("receiverId", msg.getReceiverId());
                    msgMap.put("timestamp", msg.getTimestamp());
                    msgMap.put("read", msg.isRead());
                    msgMap.put("delivered", msg.isDelivered());
                    
                    // Add sender username for display
                    User sender = userRepository.findById(msg.getSenderId()).orElse(null);
                    if (sender != null) {
                        msgMap.put("sender", sender.getUsername());
                    }
                    
                    return msgMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("success", true, "messages", messageList));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
