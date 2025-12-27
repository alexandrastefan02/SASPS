package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.Team;
import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Team entity
 */
@Repository
public interface TeamRepository extends CosmosRepository<Team, String> {
    
    /**
     * Find team by exact name
     */
    @Query("SELECT * FROM c WHERE c.name = @name")
    List<Team> findByNameQuery(@Param("name") String name);
    
    /**
     * Find team by name (returns Optional)
     */
    default Optional<Team> findByName(String name) {
        List<Team> teams = findByNameQuery(name);
        return teams.isEmpty() ? Optional.empty() : Optional.of(teams.get(0));
    }
    
    /**
     * Check if team name exists
     */
    default boolean existsByName(String name) {
        List<Team> teams = findByNameQuery(name);
        return !teams.isEmpty();
    }
    
    /**
     * Find all teams that a user is a member of
     */
    @Query("SELECT * FROM c WHERE ARRAY_CONTAINS(c.memberIds, @userId)")
    List<Team> findTeamsByUserId(@Param("userId") String userId);
    
    /**
     * Find teams created by a specific user
     */
    List<Team> findByOwnerId(String ownerId);
}
