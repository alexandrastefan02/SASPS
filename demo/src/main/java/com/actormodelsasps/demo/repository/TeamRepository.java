package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Team;
import com.actormodelsasps.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Team entity
 */
@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    
    /**
     * Find team by exact name
     */
    Optional<Team> findByName(String name);
    
    /**
     * Check if team name exists
     */
    boolean existsByName(String name);
    
    /**
     * Find all teams that a user is a member of
     */
    @Query("SELECT t FROM Team t JOIN t.members m WHERE m.id = :userId")
    List<Team> findTeamsByUserId(@Param("userId") Long userId);
    
    /**
     * Find teams created by a specific user
     */
    List<Team> findByOwnerId(Long ownerId);
}
