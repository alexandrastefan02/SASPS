package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for user authentication and management
 */
@Service
public class UserService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    /**
     * Register a new user
     */
    public User registerUser(String username, String password) {
        // Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists: " + username);
        }
        
        // Create new user with encoded password
        User user = new User();
        user.setId(java.util.UUID.randomUUID().toString()); // Generate UUID for Cosmos DB
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setCreatedAt(LocalDateTime.now());
        user.setLastSeen(LocalDateTime.now());
        user.setOnline(false);
        
        User savedUser = userRepository.save(user);
        System.out.println("✅ New user registered: " + username);
        return savedUser;
    }
    
    /**
     * Authenticate user with username and password
     */
    public boolean authenticateUser(String username, String password) {
        // Use findByUsernameQuery which properly handles Cosmos DB query results as List
        List<User> users = userRepository.findByUsernameQuery(username);
        
        if (!users.isEmpty()) {
            User user = users.get(0);
            boolean matches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("🔐 Authentication attempt for " + username + ": " + (matches ? "SUCCESS" : "FAILED"));
            return matches;
        }
        
        System.out.println("❌ User not found: " + username);
        return false;
    }
    
    /**
     * Get user by username
     */
    public Optional<User> findByUsername(String username) {
        // Use the default method in repository which handles the List conversion
        return userRepository.findByUsername(username);
    }
    
    /**
     * Set user online status
     */
    public void setUserOnline(String username, boolean online) {
        // Use findByUsernameQuery which properly handles Cosmos DB query results as List
        List<User> users = userRepository.findByUsernameQuery(username);
        if (!users.isEmpty()) {
            User user = users.get(0);
            user.setOnline(online);
            user.setLastSeen(LocalDateTime.now());
            userRepository.save(user);
            
            System.out.println("👤 User " + username + " is now " + (online ? "ONLINE" : "OFFLINE"));
        }
    }
    
    /**
     * Get all online users
     */
    public List<User> getOnlineUsers() {
        return userRepository.findByOnline(true);
    }
    
    /**
     * Get all users (for contact list - in real app this would be filtered by relationships)
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        userRepository.findAll().forEach(users::add);
        return users;
    }
    
    /**
     * Search users by username (case-insensitive partial match)
     */
    public List<User> searchUsers(String query) {
        return userRepository.findByUsernameContaining(query.toLowerCase());
    }
    
    /**
     * Get user by username (returns null if not found)
     */
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }
    
    /**
     * Get current authenticated user from session context
     * Note: In a real application, this would get the user from SecurityContext
     */
    public User getCurrentUser() {
        // For now, return null - this should be implemented with proper session management
        // The controller will handle authentication via username parameter
        return null;
    }
}