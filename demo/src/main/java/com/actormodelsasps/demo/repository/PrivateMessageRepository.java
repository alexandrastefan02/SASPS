package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.PrivateMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for PrivateMessage entity
 */
@Repository
public interface PrivateMessageRepository extends JpaRepository<PrivateMessage, Long> {
    
    /**
     * Find all messages between two users, ordered by timestamp
     */
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.senderId = :userId1 AND pm.receiverId = :userId2) OR " +
           "(pm.senderId = :userId2 AND pm.receiverId = :userId1) " +
           "ORDER BY pm.timestamp ASC")
    List<PrivateMessage> findMessagesBetweenUsers(
        @Param("userId1") Long userId1, 
        @Param("userId2") Long userId2
    );
    
    /**
     * Find unread messages for a user
     */
    List<PrivateMessage> findByReceiverIdAndReadFalseOrderByTimestampAsc(Long receiverId);
    
    /**
     * Count unread messages for a user from a specific sender
     */
    long countByReceiverIdAndSenderIdAndReadFalse(Long receiverId, Long senderId);
    
    /**
     * Find recent message between two users (for conversation list)
     */
    @Query("SELECT pm FROM PrivateMessage pm WHERE " +
           "(pm.senderId = :userId1 AND pm.receiverId = :userId2) OR " +
           "(pm.senderId = :userId2 AND pm.receiverId = :userId1) " +
           "ORDER BY pm.timestamp DESC LIMIT 1")
    PrivateMessage findLastMessageBetweenUsers(
        @Param("userId1") Long userId1, 
        @Param("userId2") Long userId2
    );
}
