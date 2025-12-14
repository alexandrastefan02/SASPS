package com.actormodelsasps.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

/**
 * WebSocket configuration for real-time messaging
 * 
 * This configuration sets up:
 * 1. A STOMP endpoint at /ws for clients to connect
 * 2. A message broker for broadcasting messages
 * 3. Application destination prefix for message handling
 * 4. User Principal authentication from connection headers
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // Enable a simple memory-based message broker to carry messages back to clients
        // on destinations prefixed with "/topic" and "/queue"
        // Note: /user is NOT included here because it's handled by UserDestinationMessageHandler
        config.enableSimpleBroker("/topic", "/queue");
        
        // Designate the "/app" prefix for messages bound for @MessageMapping-annotated methods
        config.setApplicationDestinationPrefixes("/app");
        
        // Enable user-specific destinations
        config.setUserDestinationPrefix("/user");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register the "/ws" endpoint for WebSocket connections
        // withSockJS() enables SockJS fallback options
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // Extract username from STOMP CONNECT headers
                    String username = accessor.getFirstNativeHeader("username");
                    if (username != null && !username.isEmpty()) {
                        Principal user = () -> username;
                        accessor.setUser(user);
                        System.out.println("🔐 User Principal set at CONNECT: " + username + " (session: " + accessor.getSessionId() + ")");
                    } else {
                        System.out.println("⚠️ No username in CONNECT headers for session: " + accessor.getSessionId());
                    }
                }
                
                return message;
            }
        });
    }
}
