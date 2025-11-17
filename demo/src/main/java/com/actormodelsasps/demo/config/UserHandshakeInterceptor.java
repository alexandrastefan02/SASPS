package com.actormodelsasps.demo.config;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.security.Principal;
import java.util.Map;

/**
 * Interceptor to extract username from handshake and set it as Principal
 * This allows Spring to properly route user-specific messages
 */
public class UserHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) throws Exception {
        // Extract username from query parameter if provided
        String query = request.getURI().getQuery();
        if (query != null && query.contains("username=")) {
            String username = extractUsername(query);
            if (username != null) {
                attributes.put("username", username);
                System.out.println("👤 Handshake: User " + username + " connecting");
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                              WebSocketHandler wsHandler, Exception exception) {
        // Nothing to do after handshake
    }

    private String extractUsername(String query) {
        for (String param : query.split("&")) {
            String[] keyValue = param.split("=");
            if (keyValue.length == 2 && "username".equals(keyValue[0])) {
                return keyValue[1];
            }
        }
        return null;
    }
}
