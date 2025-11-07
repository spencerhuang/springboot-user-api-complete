package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private MetricsService metricsService;

    /**
     * Get user by ID (no caching - should be real-time)
     */
    @Timed(value = "user.service.get.time", description = "Time taken to get user by ID from service")
    public Optional<User> getUserById(Long id) {
        logger.debug("Fetching user with ID: {}", id);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Optional<User> result = userRepository.findById(id);
            return result;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Get user by username (no caching - should be real-time)
     */
    @Timed(value = "user.service.get.by.username.time", description = "Time taken to get user by username from service")
    public Optional<User> getUserByUsername(String username) {
        logger.debug("Fetching user with username: {}", username);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Optional<User> result = userRepository.findByUsername(username);
            return result;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Get user by email (no caching - should be real-time)
     */
    @Timed(value = "user.service.get.by.email.time", description = "Time taken to get user by email from service")
    public Optional<User> getUserByEmail(String email) {
        logger.debug("Fetching user with email: {}", email);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Optional<User> result = userRepository.findByEmail(email);
            return result;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Get all users with pagination (no caching - dynamic content)
     */
    @Timed(value = "user.service.list.time", description = "Time taken to list users from service")
    public Page<User> getAllUsers(Pageable pageable) {
        logger.debug("Fetching users with pagination: {}", pageable);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Page<User> result = userRepository.findAll(pageable);
            metricsService.recordBusinessMetric("users_listed", result.getTotalElements());
            return result;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Search users by query (no caching - dynamic content)
     */
    @Timed(value = "user.service.search.time", description = "Time taken to search users from service")
    public Page<User> searchUsers(String query, Pageable pageable) {
        logger.debug("Searching users with query: {}", query);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Page<User> result = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                query, query, pageable);
            metricsService.recordBusinessMetric("users_searched", result.getTotalElements(), "query", query);
            return result;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Create new user (no caching - dynamic operation)
     */
    @Timed(value = "user.service.create.time", description = "Time taken to create user from service")
    public User createUser(User user) {
        logger.debug("Creating new user: {}", user.getUsername());
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            // Check if user with same username or email already exists
            if (userRepository.existsByUsername(user.getUsername())) {
                throw new RuntimeException("Username already exists: " + user.getUsername());
            }
            
            if (userRepository.existsByEmail(user.getEmail())) {
                throw new RuntimeException("Email already exists: " + user.getEmail());
            }
            
            User savedUser = userRepository.save(user);
            logger.info("Created user: {} with ID: {}", savedUser.getUsername(), savedUser.getId());
            
            metricsService.recordBusinessMetric("user_created", 1.0, "username", savedUser.getUsername());
            
            // Update user counts in metrics
            long totalUsers = userRepository.count();
            metricsService.setTotalUsers((int) totalUsers);
            
            return savedUser;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Update existing user (no caching - dynamic operation)
     */
    @Timed(value = "user.service.update.time", description = "Time taken to update user from service")
    public User updateUser(Long id, User userDetails) {
        logger.debug("Updating user with ID: {}", id);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Optional<User> existingUser = userRepository.findById(id);
            if (existingUser.isPresent()) {
                User user = existingUser.get();
                user.setUsername(userDetails.getUsername());
                user.setEmail(userDetails.getEmail());
                user.setFullName(userDetails.getFullName());
                user.setPhoneNumber(userDetails.getPhoneNumber());
                user.setActive(userDetails.getActive());
                
                User updatedUser = userRepository.save(user);
                logger.info("Updated user: {} with ID: {}", updatedUser.getUsername(), updatedUser.getId());
                
                metricsService.recordBusinessMetric("user_updated", 1.0, "username", updatedUser.getUsername());
                
                return updatedUser;
            } else {
                throw new RuntimeException("User not found with ID: " + id);
            }
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Delete user (no caching - dynamic operation)
     */
    @Timed(value = "user.service.delete.time", description = "Time taken to delete user from service")
    public void deleteUser(Long id) {
        logger.debug("Deleting user with ID: {}", id);
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            Optional<User> existingUser = userRepository.findById(id);
            if (existingUser.isPresent()) {
                String username = existingUser.get().getUsername();
                userRepository.deleteById(id);
                logger.info("Deleted user: {} with ID: {}", username, id);
                
                metricsService.recordBusinessMetric("user_deleted", 1.0, "username", username);
                
                // Update user counts in metrics
                long totalUsers = userRepository.count();
                metricsService.setTotalUsers((int) totalUsers);
            } else {
                throw new RuntimeException("User not found with ID: " + id);
            }
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Get total user count (cached - static content)
     */
    @Cacheable(value = "users", key = "'count'")
    @Timed(value = "user.service.count.time", description = "Time taken to count users from service")
    public long getUserCount() {
        logger.debug("Fetching total user count");
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            long count = userRepository.count();
            metricsService.recordBusinessMetric("user_count_retrieved", count);
            return count;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Get active user count (cached - static content)
     */
    @Cacheable(value = "users", key = "'active_count'")
    @Timed(value = "user.service.active.count.time", description = "Time taken to count active users from service")
    public long getActiveUserCount() {
        logger.debug("Fetching active user count");
        
        Timer.Sample timer = metricsService.startDatabaseQueryTimer();
        try {
            // This would need a custom query in the repository
            long count = userRepository.count(); // Simplified for demo
            metricsService.recordBusinessMetric("active_user_count_retrieved", count);
            return count;
        } finally {
            metricsService.stopDatabaseQueryTimer(timer);
        }
    }

    /**
     * Clear all caches manually
     */
    public void clearAllCaches() {
        logger.info("Clearing all caches");
        metricsService.recordBusinessMetric("cache_cleared", 1.0, "cache_type", "all");
        // This would typically be done through CacheManager
        // For demo purposes, we'll just log it
    }
}
