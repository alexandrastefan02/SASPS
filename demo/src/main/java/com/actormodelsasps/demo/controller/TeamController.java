package com.actormodelsasps.demo.controller;

import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller for team management
 */
@RestController
@RequestMapping("/api/teams")
@CrossOrigin(origins = "*")
public class TeamController {
    
    @Autowired
    private TeamService teamService;
    
    /**
     * Create a new team
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTeam(@RequestBody CreateTeamRequest request) {
        try {
            if (request.getTeamName() == null || request.getTeamName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Team name is required"));
            }
            
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            Team team = teamService.createTeam(request.getTeamName().trim(), request.getUsername().trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Team created successfully");
            response.put("team", Map.of(
                "id", team.getId(),
                "name", team.getName(),
                "createdAt", team.getCreatedAt().toString(),
                "memberCount", team.getMembers().size()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create team"));
        }
    }
    
    /**
     * Join an existing team
     */
    @PostMapping("/join")
    public ResponseEntity<?> joinTeam(@RequestBody JoinTeamRequest request) {
        try {
            if (request.getTeamName() == null || request.getTeamName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Team name is required"));
            }
            
            if (request.getUsername() == null || request.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username is required"));
            }
            
            Team team = teamService.joinTeam(request.getTeamName().trim(), request.getUsername().trim());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Joined team successfully");
            response.put("team", Map.of(
                "id", team.getId(),
                "name", team.getName(),
                "createdAt", team.getCreatedAt().toString(),
                "memberCount", team.getMembers().size()
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to join team"));
        }
    }
    
    /**
     * Get all teams for a user
     */
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserTeams(@PathVariable String username) {
        try {
            List<Team> teams = teamService.getUserTeams(username);
            
            List<Map<String, Object>> teamList = teams.stream()
                .map(team -> Map.of(
                    "id", (Object) team.getId(),
                    "name", team.getName(),
                    "createdAt", team.getCreatedAt().toString(),
                    "memberCount", team.getMembers().size()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("teams", teamList));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve teams"));
        }
    }
    
    /**
     * Get all members of a team
     */
    @GetMapping("/{teamId}/members")
    public ResponseEntity<?> getTeamMembers(@PathVariable Long teamId) {
        try {
            List<User> members = teamService.getTeamMembers(teamId);
            
            List<Map<String, Object>> memberList = members.stream()
                .map(user -> Map.of(
                    "id", (Object) user.getId(),
                    "username", user.getUsername(),
                    "online", user.isOnline()
                ))
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of("members", memberList));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve team members"));
        }
    }
    
    /**
     * Get team by name
     */
    @GetMapping("/name/{teamName}")
    public ResponseEntity<?> getTeamByName(@PathVariable String teamName) {
        try {
            Team team = teamService.getTeamByName(teamName)
                    .orElseThrow(() -> new RuntimeException("Team not found: " + teamName));
            
            Map<String, Object> teamData = Map.of(
                "id", team.getId(),
                "name", team.getName(),
                "createdAt", team.getCreatedAt().toString(),
                "memberCount", team.getMembers().size()
            );
            
            return ResponseEntity.ok(Map.of("team", teamData));
            
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to retrieve team"));
        }
    }
    
    /**
     * Leave a team
     */
    @PostMapping("/leave")
    public ResponseEntity<?> leaveTeam(@RequestBody LeaveTeamRequest request) {
        try {
            teamService.leaveTeam(request.getUsername(), request.getTeamId());
            return ResponseEntity.ok(Map.of("success", true, "message", "Left team successfully"));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to leave team"));
        }
    }
    
    /**
     * Get messages for a specific team
     */
    @GetMapping("/{teamId}/messages")
    public ResponseEntity<?> getTeamMessages(@PathVariable Long teamId) {
        try {
            List<com.actormodelsasps.demo.model.Message> messages = teamService.getTeamMessages(teamId);
            
            List<Map<String, Object>> messageList = messages.stream()
                .map(msg -> {
                    Map<String, Object> messageMap = new HashMap<>();
                    messageMap.put("id", msg.getId());
                    messageMap.put("content", msg.getContent());
                    messageMap.put("sender", msg.getSender());
                    messageMap.put("teamId", msg.getTeamId());
                    messageMap.put("timestamp", msg.getTimestamp().toString());
                    messageMap.put("type", msg.getType() != null ? msg.getType().name() : "CHAT");
                    return messageMap;
                })
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "success", true,
                "messages", messageList
            ));
            
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                "success", false,
                "error", "Failed to load messages: " + e.getMessage()
            ));
        }
    }
    
    // Request DTOs
    public static class CreateTeamRequest {
        private String teamName;
        private String username;
        
        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
    
    public static class JoinTeamRequest {
        private String teamName;
        private String username;
        
        public String getTeamName() { return teamName; }
        public void setTeamName(String teamName) { this.teamName = teamName; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
    }
    
    public static class LeaveTeamRequest {
        private String username;
        private Long teamId;
        
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public Long getTeamId() { return teamId; }
        public void setTeamId(Long teamId) { this.teamId = teamId; }
    }
}
