package com.actormodelsasps.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket Configuration
 * 
 * This class sets up:
 * 1. WebSocket endpoint - where clients connect (ws://localhost:8080/ws)
 * 2. Message broker - handles pub/sub messaging
 * 3. Application destination prefix - routes messages to controllers
 * 
 * Flow:
 * - Client connects to: ws://localhost:8080/ws
 * - Client subscribes to: /topic/messages (receives broadcasts)
 * - Client sends to: /app/chat.send (routed to @MessageMapping)
 */
@Configuration
@EnableWebSocketMessageBroker  // Enable WebSocket message handling backed by message broker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    /**
     * Register STOMP endpoints
     * 
     * STOMP = Simple Text Oriented Messaging Protocol
     * It's a frame-based protocol that works over WebSocket
     * 
     * SockJS = Fallback options for browsers that don't support WebSocket
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")          // WebSocket endpoint
                .setAllowedOriginPatterns("*") // Allow all origins (for development)
                .withSockJS();                 // Enable SockJS fallback options
        
        System.out.println("✅ WebSocket endpoint registered: ws://localhost:8080/ws");
        System.out.println("✅ SockJS fallback enabled for browser compatibility");
    }
    
    /**
     * Configure message broker
     * 
     * Message broker handles routing for private messaging:
     * - /queue/* : Private queues for user-specific messages (1-to-1)
     * - /topic/* : Broadcast queues (for system notifications, if needed)
     * - /app/*   : Routes to @MessageMapping in controllers
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple in-memory message broker for both queues and topics
        // /queue/* for private 1-to-1 messages
        // /topic/* for system-wide notifications (if needed)
        registry.enableSimpleBroker("/queue", "/topic");
        
        // Messages sent to /app/* will be routed to @MessageMapping methods
        registry.setApplicationDestinationPrefixes("/app");
        
        // Set user destination prefix for private messaging
        registry.setUserDestinationPrefix("/user");
        
        System.out.println("✅ Message broker configured:");
        System.out.println("   - Private queues: /queue/*");
        System.out.println("   - Broadcast topics: /topic/*");
        System.out.println("   - Application: /app/*");
        System.out.println("   - User destinations: /user/*");
    }
}
