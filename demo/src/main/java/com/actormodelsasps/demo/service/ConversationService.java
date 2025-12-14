package com.actormodelsasps.demo.service;

import com.actormodelsasps.demo.model.Conversation;
import com.actormodelsasps.demo.model.Message;
import com.actormodelsasps.demo.model.PrivateMessage;
import com.actormodelsasps.demo.model.User;
import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.repository.ConversationRepository;
import com.actormodelsasps.demo.repository.MessageRepository;
import com.actormodelsasps.demo.repository.PrivateMessageRepository;
import com.actormodelsasps.demo.repository.UserRepository;
import com.actormodelsasps.demo.repository.TeamRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing conversations (both private and team-based)
 */
@Service
public class ConversationService {
    
    @Autowired
    private ConversationRepository conversationRepository;
    
    @Autowired
    private PrivateMessageRepository privateMessageRepository;
    
    @Autowired
    private MessageRepository messageRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private TeamRepository teamRepository;
    
    /**
     * Get all conversations for a user
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getUserConversations(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<Conversation> conversations = conversationRepository.findByUserIdOrderByLastMessageTimeDesc(user.getId());
        
        return conversations.stream()
            .map(conv -> buildConversationResponse(conv))
            .collect(Collectors.toList());
    }
    
    /**
     * Create or update a private conversation
     */
    @Transactional
    public Conversation createOrUpdatePrivateConversation(Long userId, Long participantUserId, String lastMessage) {
        Optional<Conversation> existingConv = conversationRepository.findPrivateConversation(userId, participantUserId);
        
        Conversation conversation;
        if (existingConv.isPresent()) {
            conversation = existingConv.get();
        } else {
            conversation = new Conversation(Conversation.ConversationType.PRIVATE, userId, participantUserId, null);
        }
        
        conversation.setLastMessage(lastMessage);
        conversation.setLastMessageTime(LocalDateTime.now());
        
        return conversationRepository.save(conversation);
    }
    
    /**
     * Create or update a team conversation
     */
    @Transactional
    public Conversation createOrUpdateTeamConversation(Long userId, Long teamId, String lastMessage) {
        Optional<Conversation> existingConv = conversationRepository.findTeamConversation(userId, teamId);
        
        Conversation conversation;
        if (existingConv.isPresent()) {
            conversation = existingConv.get();
        } else {
            conversation = new Conversation(Conversation.ConversationType.TEAM, userId, null, teamId);
        }
        
        conversation.setLastMessage(lastMessage);
        conversation.setLastMessageTime(LocalDateTime.now());
        
        return conversationRepository.save(conversation);
    }
    
    /**
     * Update unread count for a conversation
     */
    @Transactional
    public void updateUnreadCount(Long userId, Long teamId, Long participantUserId) {
        Optional<Conversation> convOpt;
        
        if (teamId != null) {
            convOpt = conversationRepository.findTeamConversation(userId, teamId);
        } else {
            convOpt = conversationRepository.findPrivateConversation(userId, participantUserId);
        }
        
        if (convOpt.isPresent()) {
            Conversation conversation = convOpt.get();
            
            int unreadCount;
            if (teamId != null) {
                // Count undelivered team messages
                unreadCount = (int) messageRepository.countByTeamIdAndDeliveredFalse(teamId);
            } else {
                // Count unread private messages
                unreadCount = (int) privateMessageRepository.countByReceiverIdAndSenderIdAndReadFalse(userId, participantUserId);
            }
            
            conversation.setUnreadCount(unreadCount);
            conversationRepository.save(conversation);
        }
    }
    
    /**
     * Mark conversation as read
     */
    @Transactional
    public void markConversationAsRead(Long userId, Long teamId, Long participantUserId) {
        Optional<Conversation> convOpt;
        
        if (teamId != null) {
            convOpt = conversationRepository.findTeamConversation(userId, teamId);
        } else {
            convOpt = conversationRepository.findPrivateConversation(userId, participantUserId);
        }
        
        if (convOpt.isPresent()) {
            Conversation conversation = convOpt.get();
            conversation.setUnreadCount(0);
            conversationRepository.save(conversation);
        }
    }
    
    /**
     * Build conversation response with user/team details
     */
    private Map<String, Object> buildConversationResponse(Conversation conv) {
        Map<String, Object> response = new HashMap<>();
        
        response.put("id", conv.getId());
        response.put("type", conv.getType().toString());
        response.put("lastMessage", conv.getLastMessage());
        response.put("lastMessageTime", conv.getLastMessageTime());
        response.put("unreadCount", conv.getUnreadCount());
        
        if (conv.getType() == Conversation.ConversationType.PRIVATE) {
            // Get participant details
            Optional<User> participant = userRepository.findById(conv.getParticipantUserId());
            if (participant.isPresent()) {
                response.put("participantId", participant.get().getId());
                response.put("participantUsername", participant.get().getUsername());
                response.put("participantOnline", participant.get().isOnline());
                response.put("name", participant.get().getUsername());
            }
        } else {
            // Get team details
            Optional<Team> team = teamRepository.findById(conv.getTeamId());
            if (team.isPresent()) {
                response.put("teamId", team.get().getId());
                response.put("teamName", team.get().getName());
                response.put("memberCount", team.get().getMembers().size());
                response.put("name", team.get().getName());
            }
        }
        
        return response;
    }
    
    /**
     * Sync conversations for a user (create conversation entries for their teams)
     */
    @Transactional
    public void syncUserConversations(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Create conversation entries for all teams the user is part of
        for (Team team : user.getTeams()) {
            Optional<Conversation> existing = conversationRepository.findTeamConversation(user.getId(), team.getId());
            
            if (existing.isEmpty()) {
                // Get last message for this team
                List<Message> messages = messageRepository.findByTeamIdOrderByTimestampAsc(team.getId());
                String lastMessage = messages.isEmpty() ? "No messages yet" : messages.get(messages.size() - 1).getContent();
                LocalDateTime lastMessageTime = messages.isEmpty() ? team.getCreatedAt() : messages.get(messages.size() - 1).getTimestamp();
                
                Conversation conversation = new Conversation(Conversation.ConversationType.TEAM, user.getId(), null, team.getId());
                conversation.setLastMessage(lastMessage);
                conversation.setLastMessageTime(lastMessageTime);
                conversation.setUnreadCount(0);
                
                conversationRepository.save(conversation);
            }
        }
    }
    
    /**
     * Update private conversation by username (helper method for controllers)
     */
    @Transactional
    public Conversation updatePrivateConversationByUsername(String username, Long participantId, String lastMessage) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        return createOrUpdatePrivateConversation(user.getId(), participantId, lastMessage);
    }
}
