package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.PrivateMessage;
import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PrivateMessage entity
 */
@Repository
public interface PrivateMessageRepository extends CosmosRepository<PrivateMessage, String> {
    
    /**
     * Find all messages between two users, ordered by timestamp
     */
    @Query("SELECT * FROM c WHERE (c.senderId = @userId1 AND c.receiverId = @userId2) OR (c.senderId = @userId2 AND c.receiverId = @userId1) ORDER BY c.timestamp ASC")
    List<PrivateMessage> findMessagesBetweenUsers(
        @Param("userId1") String userId1, 
        @Param("userId2") String userId2
    );
    
    /**
     * Find unread messages for a user
     */
    @Query("SELECT * FROM c WHERE c.receiverId = @receiverId AND c.read = false ORDER BY c.timestamp ASC")
    List<PrivateMessage> findUnreadByReceiverId(@Param("receiverId") String receiverId);
    
    /**
     * Count unread messages for a user from a specific sender
     */
    @Query("SELECT VALUE COUNT(1) FROM c WHERE c.receiverId = @receiverId AND c.senderId = @senderId AND c.read = false")
    long countUnreadFromSender(@Param("receiverId") String receiverId, @Param("senderId") String senderId);
}
