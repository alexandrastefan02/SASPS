package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.service.ContactService;
import com.actormodelsasps.demo.service.PrivateMessageService;
import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.Map;

/**
 * Controller for private 1-to-1 messaging
 * 
 * This replaces the broadcast-based ChatController with direct private messaging.
 * Each user receives messages in their private queue: /queue/messages/{username}
 */
@Controller
public class PrivateMessageController {
    
    @Autowired
    private PrivateMessageService privateMessageService;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private ContactService contactService;
    
    @Autowired
    private com.actormodelsasps.demo.listener.WebSocketEventListener webSocketEventListener;
    
    /**
     * Handle user registration for private messaging
     * 
     * When a user connects, register their session for private messaging
     */
    @MessageMapping("/private.register")
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
        
        // Register user session for private messaging
        privateMessageService.registerUserSession(username, sessionId);
        
        // Register session with event listener for cleanup
        webSocketEventListener.registerSession(sessionId, username);
        
        // Set user as online in database
        userService.setUserOnline(username, true);
        
        System.out.println("   ✅ User registered for private messaging");
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Handle private message sending
     * 
     * Route: /app/private.send
     * Message goes directly to recipient's private queue
     */
    @MessageMapping("/private.send")
    public void sendPrivateMessage(@Payload Message message, 
                                  SimpMessageHeaderAccessor headerAccessor) {
        
        String sender = message.getSender();
        String recipient = message.getRecipient();
        String content = message.getContent();
        
        System.out.println("\n💌 ══════════ PRIVATE MESSAGE ══════════");
        System.out.println("   From: " + sender);
        System.out.println("   To: " + recipient);
        System.out.println("   Content: " + content);
        System.out.println("   Session: " + headerAccessor.getSessionId());
        System.out.println("   Thread: " + Thread.currentThread().getName());
        
        // Validate sender and recipient
        if (sender == null || recipient == null || content == null) {
            System.err.println("❌ Invalid message: missing sender, recipient, or content");
            return;
        }
        
        // Send the private message
        boolean sent = privateMessageService.sendPrivateMessage(sender, recipient, content);
        
        if (sent) {
            System.out.println("   ✅ Message delivered successfully");
        } else {
            System.out.println("   ❌ Message delivery failed");
        }
        
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Handle conversation history request
     * 
     * Route: /app/private.history
     * Returns recent messages between two users
     */
    @MessageMapping("/private.history")
    public void getConversationHistory(@Payload Map<String, String> request,
                                      SimpMessageHeaderAccessor headerAccessor) {
        
        String requester = request.get("requester");
        String otherUser = request.get("otherUser");
        int limit = Integer.parseInt(request.getOrDefault("limit", "50"));
        
        System.out.println("📜 History request: " + requester + " <-> " + otherUser + " (limit: " + limit + ")");
        
        if (requester == null || otherUser == null) {
            System.err.println("❌ Invalid history request: missing requester or otherUser");
            return;
        }
        
        // Get conversation history
        List<Message> history = privateMessageService.getConversationHistory(requester, otherUser, limit);
        
        // Send history back to requester's private queue
        // Note: In a full implementation, you'd use SimpMessagingTemplate here
        System.out.println("📜 Retrieved " + history.size() + " messages for " + requester);
    }
    
    /**
     * Handle typing indicators
     * 
     * Route: /app/private.typing
     * Notifies recipient when someone is typing
     */
    @MessageMapping("/private.typing")
    public void handleTypingIndicator(@Payload Map<String, Object> payload,
                                     SimpMessageHeaderAccessor headerAccessor) {
        
        String sender = (String) payload.get("sender");
        String recipient = (String) payload.get("recipient");
        Boolean isTyping = (Boolean) payload.get("isTyping");
        
        if (sender == null || recipient == null || isTyping == null) {
            System.err.println("❌ Invalid typing indicator: missing data");
            return;
        }
        
        // Handle typing indicator
        privateMessageService.handleTypingIndicator(sender, recipient, isTyping);
    }
}