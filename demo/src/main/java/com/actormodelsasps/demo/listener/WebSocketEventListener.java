package com.actormodelsasps.demo.listener;

import com.actormodelsasps.demo.service.PrivateMessageService;
import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocketEventListener - Handles WebSocket connection lifecycle events for private messaging
 * 
 * THREAD-BASED BEHAVIOR:
 * - These event handlers run on threads from the thread pool
 * - Can be called concurrently when multiple clients connect/disconnect
 * - Manages user online status and private messaging sessions
 * 
 * Events:
 * 1. SessionConnectedEvent  - When WebSocket handshake completes
 * 2. SessionDisconnectEvent - When WebSocket connection closes
 */
@Component
public class WebSocketEventListener {
    
    @Autowired
    private PrivateMessageService privateMessageService;
    
    @Autowired
    private UserService userService;
    
    // Store session-to-username mapping for cleanup
    private final Map<String, String> sessionToUsername = new ConcurrentHashMap<>();
    
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
     * Handle WebSocket disconnections for private messaging
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
        
        // Find username associated with this session
        String username = sessionToUsername.get(sessionId);
        
        if (username != null) {
            System.out.println("   User: " + username);
            
            // Remove from session mapping
            sessionToUsername.remove(sessionId);
            
            // Remove user session from private messaging service
            privateMessageService.removeUserSession(username);
            
            // Set user as offline in database
            userService.setUserOnline(username, false);
            
            System.out.println("   User set offline: ✅");
            
            // Get current active users for debugging
            System.out.println("   Active users: " + privateMessageService.getActiveUsers().size());
        } else {
            System.out.println("   User not found (session may not have been registered)");
        }
        
        System.out.println("═════════════════════════════════════════\n");
    }
    
    /**
     * Register username for a session (called from PrivateMessageController)
     */
    public void registerSession(String sessionId, String username) {
        sessionToUsername.put(sessionId, username);
        System.out.println("🔗 Session registered: " + username + " -> " + sessionId);
    }
}
