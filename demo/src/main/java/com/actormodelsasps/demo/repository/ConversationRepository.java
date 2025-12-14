package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entity
 */
@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    
    /**
     * Find all conversations for a user, ordered by last message time
     */
    List<Conversation> findByUserIdOrderByLastMessageTimeDesc(Long userId);
    
    /**
     * Find a private conversation between two users
     */
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId " +
           "AND c.type = 'PRIVATE' AND c.participantUserId = :participantUserId")
    Optional<Conversation> findPrivateConversation(
        @Param("userId") Long userId,
        @Param("participantUserId") Long participantUserId
    );
    
    /**
     * Find a team conversation for a user
     */
    @Query("SELECT c FROM Conversation c WHERE c.userId = :userId " +
           "AND c.type = 'TEAM' AND c.teamId = :teamId")
    Optional<Conversation> findTeamConversation(
        @Param("userId") Long userId,
        @Param("teamId") Long teamId
    );
    
    /**
     * Count unread conversations for a user
     */
    @Query("SELECT COUNT(c) FROM Conversation c WHERE c.userId = :userId AND c.unreadCount > 0")
    long countUnreadConversations(@Param("userId") Long userId);
}
