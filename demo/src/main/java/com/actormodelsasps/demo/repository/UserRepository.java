package com.actormodelsasps.demo.repository;

import com.actormodelsasps.demo.model.User;
import com.azure.spring.data.cosmos.repository.CosmosRepository;
import com.azure.spring.data.cosmos.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for User entity operations
 */
@Repository
public interface UserRepository extends CosmosRepository<User, String> {
    
    /**
     * Find user by username (for authentication)
     * Using explicit query since username is the partition key
     * Returns a list because Cosmos DB @Query returns collections
     */
    @Query("SELECT * FROM c WHERE c.username = @username")
    List<User> findByUsernameQuery(@Param("username") String username);
    
    /**
     * Helper method for compatibility - delegates to findByUsernameQuery
     */
    default Optional<User> findByUsername(String username) {
        List<User> users = findByUsernameQuery(username);
        return users.isEmpty() ? Optional.empty() : Optional.of(users.get(0));
    }
    
    /**
     * Check if username already exists (for registration validation)
     * Using explicit query since username is the partition key
     * Just query and check if list is non-empty
     */
    default boolean existsByUsername(String username) {
        List<User> users = findByUsernameQuery(username);
        return !users.isEmpty();
    }
    
    /**
     * Find all online users
     */
    List<User> findByOnline(boolean online);
    
    /**
     * Search users by username (case-insensitive partial match)
     */
    @Query("SELECT * FROM c WHERE CONTAINS(LOWER(c.username), LOWER(@username))")
    List<User> findByUsernameContaining(@Param("username") String username);
}