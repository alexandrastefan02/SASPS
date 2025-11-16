package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.service.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;

/**
 * ChatController - Handles WebSocket chat messages
 * 
 * THREAD-BASED ARCHITECTURE:
 * 
 * When a client sends a message:
 * 1. Message arrives at WebSocket endpoint
 * 2. Tomcat's thread pool assigns a thread (e.g., Thread-12)
 * 3. That thread calls the @MessageMapping method
 * 4. Multiple threads can execute these methods SIMULTANEOUSLY
 * 5. That's why we use thread-safe collections in SessionManager!
 * 
 * Message Flow:
 * Client → /app/chat.send → handleChatMessage() → /topic/messages → All subscribers
 */
@Controller
public class ChatController {
    
    @Autowired
    private SessionManager sessionManager;
    
    /**
     * Handle incoming chat messages
     * 
     * @MessageMapping("/chat.send")  - Listens for messages sent to /app/chat.send
     * @SendTo("/topic/messages")      - Broadcasts result to all subscribed to /topic/messages
     * 
     * THREAD BEHAVIOR:
     * - This method runs on a thread from Tomcat's thread pool
     * - Multiple threads can execute this SIMULTANEOUSLY for different messages
     * - That's why sessionManager uses thread-safe collections!
     * 
     * Example with 3 concurrent messages:
     * T=0ms: Client 1 sends → [Thread-5] processes
     * T=2ms: Client 2 sends → [Thread-12] processes (parallel!)
     * T=5ms: Client 3 sends → [Thread-18] processes (parallel!)
     */
    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public Message handleChatMessage(Message message, 
                                    SimpMessageHeaderAccessor headerAccessor) {
        
        // Log which thread is processing this message
        String threadName = Thread.currentThread().getName();
        System.out.println("\n📨 ══════════ MESSAGE RECEIVED ══════════");
        System.out.println("   Thread: " + threadName);
        System.out.println("   From: " + message.getSender());
        System.out.println("   Content: " + message.getContent());
        
        // Set server timestamp
        message.setTimestamp(LocalDateTime.now());
        
        // Store in message history
        // ⚠️ CRITICAL SECTION: Multiple threads might call this simultaneously!
        // SessionManager.addMessage() uses CopyOnWriteArrayList which is thread-safe
        sessionManager.addMessage(message);
        
        System.out.println("   Stored: ✅");
        System.out.println("   Broadcasting to all clients...");
        System.out.println("═════════════════════════════════════════\n");
        
        // Return message - Spring will broadcast it to /topic/messages
        // All clients subscribed to /topic/messages will receive this
        return message;
    }
    
    /**
     * Handle user registration
     * 
     * Called when a new user joins the chat
     */
    @MessageMapping("/chat.register")
    @SendTo("/topic/messages")
    public Message registerUser(Message message, 
                               SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        String username = message.getSender();
        
        System.out.println("\n🆕 ══════════ USER JOINING ══════════");
        System.out.println("   Thread: " + Thread.currentThread().getName());
        System.out.println("   Username: " + username);
        System.out.println("   Session: " + sessionId);
        
        // Add user to session manager
        sessionManager.addUser(sessionId, username);
        
        // Create join notification message
        Message joinMessage = new Message();
        joinMessage.setType(Message.MessageType.JOIN);
        joinMessage.setSender(username);
        joinMessage.setContent(username + " joined the chat!");
        joinMessage.setTimestamp(LocalDateTime.now());
        
        // Store join message
        sessionManager.addMessage(joinMessage);
        
        // Print statistics
        sessionManager.printStats();
        
        System.out.println("═════════════════════════════════════════\n");
        
        // Broadcast join notification
        return joinMessage;
    }
    
    /**
     * Get message history
     * 
     * Clients can request recent messages
     */
    @MessageMapping("/chat.history")
    @SendTo("/topic/history")
    public java.util.List<Message> getHistory() {
        System.out.println("📜 History requested by " + Thread.currentThread().getName());
        return sessionManager.getRecentMessages(50);
    }
}
