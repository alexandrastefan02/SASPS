package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Message entity
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    /**
     * Find all messages for a specific team, ordered by timestamp
     */
    List<Message> findByTeamIdOrderByTimestampAsc(Long teamId);
    
    /**
     * Find undelivered messages for a specific team
     */
    List<Message> findByTeamIdAndDeliveredFalseOrderByTimestampAsc(Long teamId);
    
    /**
     * Find recent messages for a team (last N messages)
     */
    @Query("SELECT m FROM Message m WHERE m.teamId = :teamId ORDER BY m.timestamp DESC LIMIT :limit")
    List<Message> findRecentMessagesByTeamId(@Param("teamId") Long teamId, @Param("limit") int limit);
    
    /**
     * Count undelivered messages for a team
     */
    long countByTeamIdAndDeliveredFalse(Long teamId);
}
