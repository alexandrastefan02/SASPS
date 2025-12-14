package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.MessageRepository;
import com.actormodelsasps.demo.repository.UserRepository;
import com.actormodelsasps.demo.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for private 1-on-1 messaging
 * 
 * Uses the 'messages' container with teamId='private' for private messages
 */
@Controller
public class PrivateMessageController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ConversationService conversationService;
    
    /**
     * Register user for private messaging
     */
    @MessageMapping("/private.register")
    public void registerForPrivateMessages(@Payload Map<String, String> payload,
                                          SimpMessageHeaderAccessor headerAccessor) {
        String username = payload.get("username");
        String sessionId = headerAccessor.getSessionId();
        
        // Store username in session attributes
        headerAccessor.getSessionAttributes().put("username", username);
        
        // CRITICAL: Set the user Principal so Spring can route messages correctly
        headerAccessor.setUser(() -> username);
        
        System.out.println("✅ User registered for private messaging: " + username + " (session: " + sessionId + ")");
        System.out.println("   User Principal set: " + headerAccessor.getUser());
    }
    
    /**
     * Handle private message sending
     */
    @MessageMapping("/private.send")
    public void sendPrivateMessage(@Payload Map<String, Object> payload,
                                  SimpMessageHeaderAccessor headerAccessor) {
        
        String senderUsername = (String) payload.get("username");
        String content = (String) payload.get("content");
        String receiverId = payload.get("receiverId").toString();
        
        System.out.println("\n📨 ══════════ PRIVATE MESSAGE ══════════");
        System.out.println("   From: " + senderUsername);
        System.out.println("   To (ID): " + receiverId);
        System.out.println("   Content: " + content);
        
        // Get sender
        User sender = userRepository.findByUsername(senderUsername)
            .orElseThrow(() -> new RuntimeException("Sender not found"));
        
        // Get receiver
        User receiver = userRepository.findById(receiverId)
            .orElseThrow(() -> new RuntimeException("Receiver not found"));
        
        // Save message to database using Message entity with teamId='private'
        Message message = new Message();
        message.setId(java.util.UUID.randomUUID().toString()); // Generate UUID for Cosmos DB
        message.setContent(content);
        message.setSender(sender.getId());
        message.setReceiverId(receiver.getId());
        message.setTeamId("private"); // Use 'private' as partition key for private messages
        message.setType(Message.MessageType.PRIVATE);
        message.setTimestamp(LocalDateTime.now());
        message.setDelivered(receiver.isOnline());
        message.setRead(false);
        messageRepository.save(message);
        
        System.out.println("   Saved to DB with ID: " + message.getId());
        
        // Prepare message response
        Map<String, Object> messageResponse = new HashMap<>();
        messageResponse.put("id", message.getId());
        messageResponse.put("content", message.getContent());
        messageResponse.put("senderId", sender.getId());
        messageResponse.put("sender", sender.getUsername());
        messageResponse.put("receiverId", receiver.getId());
        messageResponse.put("timestamp", message.getTimestamp());
        messageResponse.put("delivered", message.isDelivered());
        
        // Send to receiver
        System.out.println("   📤 Sending to receiver: " + receiver.getUsername() + " via /user/" + receiver.getUsername() + "/queue/private");
        messagingTemplate.convertAndSendToUser(
            receiver.getUsername(),
            "/queue/private",
            messageResponse
        );
        System.out.println("   ✅ Sent to receiver");
        
        // Send back to sender (for confirmation)
        System.out.println("   📤 Sending to sender: " + sender.getUsername() + " via /user/" + sender.getUsername() + "/queue/private");
        messagingTemplate.convertAndSendToUser(
            sender.getUsername(),
            "/queue/private",
            messageResponse
        );
        System.out.println("   ✅ Sent to sender");
        
        System.out.println("   Delivered to both users ✅");
        System.out.println("═════════════════════════════════════════\n");
        
        // Update conversations for both users
        conversationService.createOrUpdatePrivateConversation(sender.getId(), receiver.getId(), content);
        conversationService.createOrUpdatePrivateConversation(receiver.getId(), sender.getId(), content);
    }
}
