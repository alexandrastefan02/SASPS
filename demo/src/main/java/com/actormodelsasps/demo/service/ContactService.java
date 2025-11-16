package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.Contact;
import com.actormodelsasps.demo.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing user contacts
 * 
 * For now, this provides hardcoded contacts for each user.
 * In a full implementation, this would be based on user relationships in the database.
 */
@Service
public class ContactService {
    
    @Autowired
    private UserService userService;
    
    // Hardcoded contact lists for demo purposes
    private final Map<String, List<String>> userContacts = new HashMap<>();
    
    public ContactService() {
        // Initialize hardcoded contacts
        initializeHardcodedContacts();
    }
    
    /**
     * Initialize hardcoded contact relationships
     */
    private void initializeHardcodedContacts() {
        // Alice's contacts
        List<String> aliceContacts = List.of("bob", "charlie", "diana");
        userContacts.put("alice", aliceContacts);
        
        // Bob's contacts  
        List<String> bobContacts = List.of("alice", "charlie", "eve");
        userContacts.put("bob", bobContacts);
        
        // Charlie's contacts
        List<String> charlieContacts = List.of("alice", "bob", "diana", "eve");
        userContacts.put("charlie", charlieContacts);
        
        // Diana's contacts
        List<String> dianaContacts = List.of("alice", "charlie");
        userContacts.put("diana", dianaContacts);
        
        // Eve's contacts
        List<String> eveContacts = List.of("bob", "charlie");
        userContacts.put("eve", eveContacts);
        
        System.out.println("✅ Hardcoded contacts initialized");
    }
    
    /**
     * Get contacts for a specific user
     */
    public List<Contact> getContactsForUser(String username) {
        List<Contact> contacts = new ArrayList<>();
        List<String> contactUsernames = userContacts.getOrDefault(username.toLowerCase(), new ArrayList<>());
        
        // Convert usernames to Contact objects with online status
        for (String contactUsername : contactUsernames) {
            // Check if contact user exists and get their online status
            boolean isOnline = userService.findByUsername(contactUsername)
                    .map(User::isOnline)
                    .orElse(false);
            
            // Create contact with display name (capitalize first letter)
            String displayName = capitalizeFirstLetter(contactUsername);
            Contact contact = new Contact(contactUsername, displayName, isOnline);
            contacts.add(contact);
        }
        
        System.out.println("📇 Retrieved " + contacts.size() + " contacts for user: " + username);
        return contacts;
    }
    
    /**
     * Check if two users are contacts (can message each other)
     */
    public boolean areUsersContacts(String user1, String user2) {
        List<String> user1Contacts = userContacts.getOrDefault(user1.toLowerCase(), new ArrayList<>());
        return user1Contacts.contains(user2.toLowerCase());
    }
    
    /**
     * Add a contact relationship (for future use)
     */
    public void addContact(String username, String contactUsername) {
        userContacts.computeIfAbsent(username.toLowerCase(), k -> new ArrayList<>())
                   .add(contactUsername.toLowerCase());
        
        // Add reverse relationship
        userContacts.computeIfAbsent(contactUsername.toLowerCase(), k -> new ArrayList<>())
                   .add(username.toLowerCase());
        
        System.out.println("✅ Added contact relationship: " + username + " <-> " + contactUsername);
    }
    
    /**
     * Utility method to capitalize first letter
     */
    private String capitalizeFirstLetter(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
    
    /**
     * Get all hardcoded contact relationships (for debugging)
     */
    public Map<String, List<String>> getAllContactRelationships() {
        return new HashMap<>(userContacts);
    }
}