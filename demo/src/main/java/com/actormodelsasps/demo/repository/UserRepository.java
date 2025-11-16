package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * Find user by username (for authentication)
     */
    Optional<User> findByUsername(String username);
    
    /**
     * Check if username already exists (for registration validation)
     */
    boolean existsByUsername(String username);
    
    /**
     * Find all online users
     */
    List<User> findByOnlineTrue();
    
    /**
     * Update user's online status
     */
    @Modifying
    @Query("UPDATE User u SET u.online = :online, u.lastSeen = :lastSeen WHERE u.username = :username")
    void updateUserOnlineStatus(@Param("username") String username, 
                               @Param("online") boolean online, 
                               @Param("lastSeen") LocalDateTime lastSeen);
}