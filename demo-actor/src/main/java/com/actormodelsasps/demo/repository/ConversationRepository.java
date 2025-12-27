package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Conversation;
import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Conversation entity
 */
@Repository
public interface ConversationRepository extends CosmosRepository<Conversation, String> {
    
    /**
     * Find all conversations for a user, ordered by last message time
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId ORDER BY c.lastMessageTime DESC")
    List<Conversation> findByUserIdOrderByLastMessageTime(@Param("userId") String userId);
    
    /**
     * Find a private conversation between two users
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.type = 'PRIVATE' AND c.participantUserId = @participantUserId")
    Optional<Conversation> findPrivateConversation(
        @Param("userId") String userId,
        @Param("participantUserId") String participantUserId
    );
    
    /**
     * Find a team conversation for a user
     */
    @Query("SELECT * FROM c WHERE c.userId = @userId AND c.type = 'TEAM' AND c.teamId = @teamId")
    Optional<Conversation> findTeamConversation(
        @Param("userId") String userId,
        @Param("teamId") String teamId
    );
    
    /**
     * Count unread conversations for a user
     */
    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.userId = @userId AND c.unreadCount > 0")
    long countUnreadConversations(@Param("userId") String userId);
}
