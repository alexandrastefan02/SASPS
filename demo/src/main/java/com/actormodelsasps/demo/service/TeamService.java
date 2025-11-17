package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.repository.TeamRepository;
import com.actormodelsasps.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for team management
 */
@Service
public class TeamService {
    
    @Autowired
    private TeamRepository teamRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    /**
     * Create a new team
     */
    @Transactional
    public Team createTeam(String teamName, String creatorUsername) {
        // Check if team name already exists
        if (teamRepository.existsByName(teamName)) {
            throw new RuntimeException("Team name already exists: " + teamName);
        }
        
        // Find the creator user
        User creator = userRepository.findByUsername(creatorUsername)
                .orElseThrow(() -> new RuntimeException("User not found: " + creatorUsername));
        
        // Create team
        Team team = new Team(teamName, creator.getId());
        
        // Add creator as first member
        team.addMember(creator);
        
        // Save team
        Team savedTeam = teamRepository.save(team);
        
        System.out.println("✅ Team created: " + teamName + " by " + creatorUsername);
        return savedTeam;
    }
    
    /**
     * Join an existing team
     */
    @Transactional
    public Team joinTeam(String teamName, String username) {
        // Find team
        Team team = teamRepository.findByName(teamName)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));
        
        // Find user
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Check if user is already a member
        if (team.getMembers().contains(user)) {
            System.out.println("ℹ️ User " + username + " is already a member of team " + teamName);
            return team;
        }
        
        // Add user to team
        team.addMember(user);
        Team savedTeam = teamRepository.save(team);
        
        System.out.println("✅ User " + username + " joined team: " + teamName);
        return savedTeam;
    }
    
    /**
     * Get all teams a user is a member of
     */
    public List<Team> getUserTeams(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        return teamRepository.findTeamsByUserId(user.getId());
    }
    
    /**
     * Get team by name
     */
    public Optional<Team> getTeamByName(String teamName) {
        return teamRepository.findByName(teamName);
    }
    
    /**
     * Get team by ID
     */
    public Optional<Team> getTeamById(Long teamId) {
        return teamRepository.findById(teamId);
    }
    
    /**
     * Get all members of a team
     */
    public List<User> getTeamMembers(Long teamId) {
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + teamId));
        
        return team.getMembers().stream()
                .collect(Collectors.toList());
    }
    
    /**
     * Get all members of a team by team name
     */
    public List<User> getTeamMembersByName(String teamName) {
        Team team = teamRepository.findByName(teamName)
                .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));
        
        return team.getMembers().stream()
                .collect(Collectors.toList());
    }
    
    /**
     * Check if user is a member of a team
     */
    @Transactional(readOnly = true)
    public boolean isUserMemberOfTeam(String username, Long teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        // Use query to avoid lazy loading issues
        return teamRepository.isUserMemberOfTeam(teamId, user.getId());
    }
    
    /**
     * Leave a team
     */
    @Transactional
    public void leaveTeam(String username, Long teamId) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found: " + username));
        
        Team team = teamRepository.findById(teamId)
                .orElseThrow(() -> new RuntimeException("Team not found with ID: " + teamId));
        
        team.removeMember(user);
        teamRepository.save(team);
        
        System.out.println("👋 User " + username + " left team: " + team.getName());
    }
    
    /**
     * Get all teams (for admin/debugging)
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAll();
    }
}
