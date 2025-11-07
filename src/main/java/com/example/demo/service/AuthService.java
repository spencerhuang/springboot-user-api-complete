package com.example.demo.service;

import com.example.demo.security.JwtUtil;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private MetricsService metricsService;

    /**
     * Validate JWT token (no caching - JWT has TTL)
     */
    @Timed(value = "auth.service.validate.time", description = "Time taken to validate token from service")
    public boolean validateToken(String token) {
        logger.debug("Validating JWT token");
        
        try {
            // Extract username and validate token
            String username = jwtUtil.extractUsername(token);
            boolean isValid = jwtUtil.validateToken(token);
            
            if (isValid) {
                logger.debug("Token validation successful for user: {}", username);
                metricsService.recordBusinessMetric("token_validated", 1.0, "username", username);
            } else {
                logger.debug("Token validation failed");
                metricsService.recordBusinessMetric("token_validation_failed", 1.0);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.warn("Token validation error: {}", e.getMessage());
            metricsService.recordBusinessMetric("token_validation_error", 1.0, "error", e.getClass().getSimpleName());
            return false;
        }
    }

    /**
     * Authenticate user (no caching - should be real-time)
     */
    @Timed(value = "auth.service.authenticate.time", description = "Time taken to authenticate user from service")
    public Map<String, Object> authenticateUser(String username) {
        logger.debug("Authenticating user: {}", username);
        
        try {
            // In a real application, you would validate credentials here
            // For demo purposes, we'll accept any non-empty username
            
            if (username == null || username.trim().isEmpty()) {
                metricsService.recordBusinessMetric("authentication_failed", 1.0, "reason", "empty_username");
                throw new RuntimeException("Username cannot be empty");
            }
            
            // Generate JWT token
            String token = jwtUtil.generateToken(username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("token", token);
            response.put("username", username);
            response.put("message", "Authentication successful");
            response.put("tokenType", "Bearer");
            response.put("expiresIn", "1 hour");
            
            logger.info("User authenticated successfully: {}", username);
            
            metricsService.recordBusinessMetric("user_authenticated", 1.0, "username", username);
            
            return response;
        } catch (RuntimeException e) {
            throw e;
        }
    }

    /**
     * Get user session info (no caching - should be real-time)
     */
    @Timed(value = "auth.service.session.time", description = "Time taken to get user session from service")
    public Map<String, Object> getUserSession(String username) {
        logger.debug("Getting session info for user: {}", username);
        
        Map<String, Object> session = new HashMap<>();
        session.put("username", username);
        session.put("lastAccess", System.currentTimeMillis());
        session.put("active", true);
        
        metricsService.recordBusinessMetric("session_retrieved", 1.0, "username", username);
        
        return session;
    }

    /**
     * Logout user (no caching needed)
     */
    @Timed(value = "auth.service.logout.time", description = "Time taken to logout user from service")
    public void logoutUser(String username) {
        logger.info("User logged out: {}", username);
        
        metricsService.recordBusinessMetric("user_logged_out", 1.0, "username", username);
    }

    /**
     * Clear all authentication caches (no caching, but keeping for consistency)
     */
    @Timed(value = "auth.service.clear.cache.time", description = "Time taken to clear auth cache from service")
    public void clearAllAuthCaches() {
        logger.info("Clearing all authentication caches");
        
        metricsService.recordBusinessMetric("auth_cache_cleared", 1.0, "cache_type", "all");
    }

    /**
     * Refresh user session (no caching - should be real-time)
     */
    @Timed(value = "auth.service.refresh.time", description = "Time taken to refresh user session from service")
    public Map<String, Object> refreshUserSession(String username) {
        logger.debug("Refreshing session for user: {}", username);
        
        // Create new session (no caching)
        Map<String, Object> newSession = new HashMap<>();
        newSession.put("username", username);
        newSession.put("lastAccess", System.currentTimeMillis());
        newSession.put("active", true);
        newSession.put("refreshed", true);
        
        metricsService.recordBusinessMetric("session_refreshed", 1.0, "username", username);
        
        return newSession;
    }

    /**
     * Check if user is authenticated (no caching - should be real-time)
     */
    @Timed(value = "auth.service.check.status.time", description = "Time taken to check user auth status from service")
    public boolean isUserAuthenticated(String username) {
        logger.debug("Checking authentication status for user: {}", username);
        
        // In a real application, you would check against a session store or database
        // For demo purposes, we'll return true if username is not empty
        boolean isAuthenticated = username != null && !username.trim().isEmpty();
        
        metricsService.recordBusinessMetric("auth_status_checked", 1.0, "username", username, "authenticated", String.valueOf(isAuthenticated));
        
        return isAuthenticated;
    }
}
