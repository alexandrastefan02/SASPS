package com.actormodelsasps.demo.listener;

import com.actormodelsasps.demo.model.ChatUser;
import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.service.SessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.LocalDateTime;

/**
 * WebSocketEventListener - Handles WebSocket connection lifecycle events
 * 
 * THREAD-BASED BEHAVIOR:
 * - These event handlers run on threads from the thread pool
 * - Can be called concurrently when multiple clients connect/disconnect
 * - That's why SessionManager uses thread-safe collections!
 * 
 * Events:
 * 1. SessionConnectedEvent  - When WebSocket handshake completes
 * 2. SessionDisconnectEvent - When WebSocket connection closes
 */
@Component
public class WebSocketEventListener {
    
    @Autowired
    private SessionManager sessionManager;
    
    @Autowired
    private SimpMessageSendingOperations messagingTemplate;
    
    /**
     * Handle new WebSocket connections
     * 
     * Called after WebSocket handshake completes (HTTP 101 Switching Protocols)
     * 
     * THREAD: Runs on a thread from the pool (e.g., http-nio-8080-exec-7)
     */
    @EventListener
    public void handleWebSocketConnectListener(SessionConnectedEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        System.out.println("\n🔗 ══════════ NEW CONNECTION ══════════");
        System.out.println("   Thread: " + Thread.currentThread().getName());
        System.out.println("   Session ID: " + sessionId);
        System.out.println("   Status: WebSocket handshake completed ✅");
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Handle WebSocket disconnections
     * 
     * Called when:
     * - Client closes connection
     * - Connection times out
     * - Network error
     * 
     * THREAD: Runs on a thread from the pool
     * THREAD-SAFE: Multiple clients can disconnect simultaneously
     */
    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        
        System.out.println("\n🔌 ══════════ DISCONNECTION ══════════");
        System.out.println("   Thread: " + Thread.currentThread().getName());
        System.out.println("   Session ID: " + sessionId);
        
        // Remove user from session manager
        // ⚠️ CRITICAL SECTION: Multiple threads might call this simultaneously!
        // SessionManager.removeUser() uses ConcurrentHashMap which is thread-safe
        ChatUser disconnectedUser = sessionManager.removeUser(sessionId);
        
        if (disconnectedUser != null) {
            String username = disconnectedUser.getUsername();
            System.out.println("   User: " + username);
            
            // Create leave notification
            Message leaveMessage = new Message();
            leaveMessage.setType(Message.MessageType.LEAVE);
            leaveMessage.setSender(username);
            leaveMessage.setContent(username + " left the chat.");
            leaveMessage.setTimestamp(LocalDateTime.now());
            
            // Store leave message
            sessionManager.addMessage(leaveMessage);
            
            // Broadcast leave notification to all remaining clients
            messagingTemplate.convertAndSend("/topic/messages", leaveMessage);
            
            System.out.println("   Notification sent: ✅");
            
            // Print updated statistics
            sessionManager.printStats();
        } else {
            System.out.println("   User not found (may have already left)");
        }
        
        System.out.println("═════════════════════════════════════════\n");
    }
}
