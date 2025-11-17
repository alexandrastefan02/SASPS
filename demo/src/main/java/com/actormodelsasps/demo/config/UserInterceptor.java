package com.actormodelsasps.demo.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.security.Principal;

/**
 * Interceptor to set user Principal from STOMP headers
 * This enables Spring's user destination resolution for private messaging
 */
@Component
public class UserInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        
        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // Extract username from STOMP headers if present
            String username = accessor.getFirstNativeHeader("username");
            
            if (username != null && !username.isEmpty()) {
                Principal principal = () -> username;
                accessor.setUser(principal);
                System.out.println("🔐 User Principal set during CONNECT: " + username + " (Session: " + accessor.getSessionId() + ")");
            }
        }
        
        return message;
    }
}
