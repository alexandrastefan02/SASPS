package com.actormodelsasps.demo.config;

import com.actormodelsasps.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Initialize demo data when the application starts
 * 
 * Creates hardcoded users that match the contact relationships
 */
@Component
public class DataInitializer implements ApplicationRunner {
    
    @Autowired
    private UserService userService;
    
    @Override
    public void run(ApplicationArguments args) throws Exception {
        System.out.println("\n🔧 ══════════ INITIALIZING DEMO DATA ══════════");
        
        // Create demo users (matching the hardcoded contacts)
        createUserIfNotExists("alice", "password123");
        createUserIfNotExists("bob", "password123");
        createUserIfNotExists("charlie", "password123");
        createUserIfNotExists("diana", "password123");
        createUserIfNotExists("eve", "password123");
        
        System.out.println("✅ Demo data initialized successfully");
        System.out.println("═════════════════════════════════════════\n");
    }
    
    private void createUserIfNotExists(String username, String password) {
        try {
            userService.registerUser(username, password);
            System.out.println("   Created user: " + username);
        } catch (RuntimeException e) {
            if (e.getMessage().contains("already exists")) {
                System.out.println("   User exists: " + username);
            } else {
                System.err.println("   Error creating user " + username + ": " + e.getMessage());
            }
        }
    }
}