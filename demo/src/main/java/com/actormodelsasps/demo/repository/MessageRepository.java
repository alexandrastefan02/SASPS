package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Message;
import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity
 */
@Repository
public interface MessageRepository extends CosmosRepository<Message, String> {
    
    /**
     * Find all messages for a specific team, ordered by timestamp
     */
    @Query("SELECT * FROM c WHERE c.teamId = @teamId ORDER BY c.timestamp ASC")
    List<Message> findByTeamIdOrderByTimestamp(@Param("teamId") String teamId);
    
    /**
     * Find undelivered messages for a specific team
     */
    @Query("SELECT * FROM c WHERE c.teamId = @teamId AND c.delivered = false ORDER BY c.timestamp ASC")
    List<Message> findUndeliveredByTeamId(@Param("teamId") String teamId);
    
    /**
     * Count undelivered messages for a team
     */
    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.teamId = @teamId AND c.delivered = false")
    long countUndeliveredByTeamId(@Param("teamId") String teamId);
    
    /**
     * Find private messages between two users (both directions)
     * Private messages use teamId = 'private' as partition key
     */
    @Query("SELECT * FROM c WHERE c.teamId = 'private' AND ((c.sender = @userId1 AND c.receiverId = @userId2) OR (c.sender = @userId2 AND c.receiverId = @userId1)) ORDER BY c.timestamp ASC")
    List<Message> findPrivateMessagesBetweenUsers(@Param("userId1") String userId1, @Param("userId2") String userId2);
}
