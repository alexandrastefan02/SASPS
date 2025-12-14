package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for authentication and user management
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*") // Allow all origins for development
public class AuthController {
    
    @Autowired
    private UserService userService;
    
    /**
     * Register a new user
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            if (request.getPassword() == null || request.getPassword().length() < 3) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password must be at least 3 characters"));
            }
            
            // Register user
            User user = userService.registerUser(request.getUsername().trim(), request.getPassword());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Registration failed"));
        }
    }
    
    /**
     * Login user
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            // Validate input
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            if (request.getPassword() == null || request.getPassword().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }
            
            // Authenticate user
            boolean authenticated = userService.authenticateUser(request.getUsername().trim(), request.getPassword());
            
            if (authenticated) {
                // Set user as online
                userService.setUserOnline(request.getUsername().trim(), true);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Login successful");
                response.put("username", request.getUsername().trim());
                
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid username or password"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Login failed"));
        }
    }
    
    /**
     * Logout user
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            if (username != null && !username.trim().isEmpty()) {
                userService.setUserOnline(username.trim(), false);
            }
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Logged out successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Logout failed"));
        }
    }
    
    /**
     * Get online users
     */
    @GetMapping("/online-users")
    public ResponseEntity<?> getOnlineUsers() {
        try {
            List<User> onlineUsers = userService.getOnlineUsers();
            return ResponseEntity.ok(Map.of("onlineUsers", onlineUsers));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve online users"));
        }
    }
    
    /**
     * Search users by username
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String query) {
        try {
            if (query == null || query.trim().length() < 2) {
                return ResponseEntity.badRequest().body(Map.of("error", "Query must be at least 2 characters"));
            }
            
            List<User> users = userService.searchUsers(query.trim());
            
            // Convert to safe response format (without password)
            List<Map<String, Object>> userList = users.stream()
                .map(user -> {
                    Map<String, Object> userMap = new HashMap<>();
                    userMap.put("id", user.getId());
                    userMap.put("username", user.getUsername());
                    userMap.put("online", user.isOnline());
                    return userMap;
                })
                .toList();
            
            return ResponseEntity.ok(Map.of("success", true, "users", userList));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to search users"));
        }
    }
    
    /**
     * Get user by username
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserByUsername(@PathVariable String username) {
        try {
            User user = userService.getUserByUsername(username);
            
            if (user != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("id", user.getId());
                userMap.put("username", user.getUsername());
                userMap.put("online", user.isOnline());
                
                return ResponseEntity.ok(Map.of("success", true, "user", userMap));
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "User not found"));
            }
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve user"));
        }
    }
    
    // Request DTOs
    public static class RegisterRequest {
        private String username;
        private String password;
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
    
    public static class LoginRequest {
        private String username;
        private String password;
        
        // Getters and Setters
        public String getUsername() {
            return username;
        }
        
        public void setUsername(String username) {
            this.username = username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public void setPassword(String password) {
            this.password = password;
        }
    }
}