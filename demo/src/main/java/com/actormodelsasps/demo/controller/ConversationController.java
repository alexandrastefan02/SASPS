package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Conversation;
import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.repository.MessageRepository;
import com.actormodelsasps.demo.repository.UserRepository;
import com.actormodelsasps.demo.repository.TeamRepository;
import com.actormodelsasps.demo.service.ConversationService;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * REST Controller for managing conversations
 * Builds conversations dynamically from messages (no conversations container needed)
 */
@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {
    
    @Autowired
    private ConversationService conversationService;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    /**
     * Get all conversations for a user
     * Dynamically builds conversations from messages in the database
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserConversations(@PathVariable String username) {
        try {
            User currentUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
            
            List<Map<String, Object>> conversations = new ArrayList<>();
            
            // Get all private messages involving this user
            List<Message> allMessages = new ArrayList<>();
            messageRepository.findAll().forEach(allMessages::add);
            Map<String, Message> latestPrivateMessages = new HashMap<>();
            
            for (Message msg : allMessages) {
                if (msg.getType() == Message.MessageType.PRIVATE && msg.getTeamId().equals("private")) {
                    String senderId = msg.getSender();
                    String receiverId = msg.getReceiverId();
                    
                    // Check if this message involves the current user
                    String otherUserId = null;
                    if (senderId.equals(currentUser.getId())) {
                        otherUserId = receiverId;
                    } else if (receiverId.equals(currentUser.getId())) {
                        otherUserId = senderId;
                    }
                    
                    if (otherUserId != null) {
                        // Keep only the latest message for each conversation
                        Message existing = latestPrivateMessages.get(otherUserId);
                        if (existing == null || msg.getTimestamp().isAfter(existing.getTimestamp())) {
                            latestPrivateMessages.put(otherUserId, msg);
                        }
                    }
                }
            }
            
            // Build conversation objects from latest private messages
            for (Map.Entry<String, Message> entry : latestPrivateMessages.entrySet()) {
                String otherUserId = entry.getKey();
                Message latestMsg = entry.getValue();
                
                Optional<User> otherUserOpt = userRepository.findById(otherUserId);
                if (otherUserOpt.isPresent()) {
                    User otherUser = otherUserOpt.get();
                    
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("id", "private-" + currentUser.getId() + "-" + otherUserId);
                    conv.put("type", "PRIVATE");
                    conv.put("participantId", otherUserId);
                    conv.put("participantUsername", otherUser.getUsername());
                    conv.put("participantOnline", otherUser.isOnline());
                    conv.put("lastMessage", latestMsg.getContent());
                    conv.put("lastMessageTime", latestMsg.getTimestamp().toString());
                    conv.put("unreadCount", 0); // Could calculate this if needed
                    
                    conversations.add(conv);
                }
            }
            
            // Add team conversations
            List<String> userTeamIds = currentUser.getTeamIds();
            if (userTeamIds != null && !userTeamIds.isEmpty()) {
                for (String teamId : userTeamIds) {
                    Optional<Team> teamOpt = teamRepository.findById(teamId);
                    if (teamOpt.isPresent()) {
                        Team team = teamOpt.get();
                        
                        // Find the latest message in this team
                        Message latestTeamMessage = null;
                        for (Message msg : allMessages) {
                            if (msg.getTeamId() != null && msg.getTeamId().equals(teamId)) {
                                if (latestTeamMessage == null || msg.getTimestamp().isAfter(latestTeamMessage.getTimestamp())) {
                                    latestTeamMessage = msg;
                                }
                            }
                        }
                        
                        // Build team conversation object
                        Map<String, Object> conv = new HashMap<>();
                        conv.put("id", "team-" + teamId);
                        conv.put("type", "TEAM");
                        conv.put("teamId", teamId);
                        conv.put("teamName", team.getName());
                        conv.put("memberCount", team.getMemberIds().size());
                        
                        if (latestTeamMessage != null) {
                            conv.put("lastMessage", latestTeamMessage.getContent());
                            conv.put("lastMessageTime", latestTeamMessage.getTimestamp().toString());
                        } else {
                            conv.put("lastMessage", "No messages yet");
                            conv.put("lastMessageTime", team.getCreatedAt().toString());
                        }
                        conv.put("unreadCount", 0);
                        
                        conversations.add(conv);
                    }
                }
            }
            
            // Sort by last message time (newest first)
            conversations.sort((a, b) -> {
                String timeA = (String) a.get("lastMessageTime");
                String timeB = (String) b.get("lastMessageTime");
                return timeB.compareTo(timeA);
            });
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("conversations", conversations);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Mark a conversation as read
     * DISABLED: Conversations container not available
     */
    @PostMapping("/mark-read")
    public ResponseEntity<?> markAsRead(@RequestBody Map<String, Object> request) {
        try {
            // No-op since conversations container doesn't exist
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Update private conversation after sending a message
     * DISABLED: Conversations container not available
     */
    @PostMapping("/update-private")
    public ResponseEntity<?> updatePrivateConversation(@RequestBody Map<String, Object> request) {
        try {
            // No-op since conversations container doesn't exist
            // Return a mock conversation ID
            return ResponseEntity.ok(Map.of(
                "success", true,
                "conversationId", "mock-conversation-id"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
