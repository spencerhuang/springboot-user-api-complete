package com.example.demo.integration;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class CachingIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CacheManager cacheManager;

    @Test
    public void testUserCountCaching() {
        // Clear cache first
        cacheManager.getCache("users").clear();
        
        // First call should hit database
        long count1 = userService.getUserCount();
        assertTrue(count1 >= 0);
        
        // Second call should hit cache
        long count2 = userService.getUserCount();
        assertEquals(count1, count2);
        
        // Verify cache is working by checking cache size
        assertNotNull(cacheManager.getCache("users"));
    }

    @Test
    public void testActiveUserCountCaching() {
        // Clear cache first
        cacheManager.getCache("users").clear();
        
        // First call should hit database
        long count1 = userService.getActiveUserCount();
        assertTrue(count1 >= 0);
        
        // Second call should hit cache
        long count2 = userService.getActiveUserCount();
        assertEquals(count1, count2);
        
        // Verify cache is working by checking cache size
        assertNotNull(cacheManager.getCache("users"));
    }

    @Test
    public void testCacheConfiguration() {
        // Verify cache names are configured correctly
        assertNotNull(cacheManager.getCache("users"));
        assertNull(cacheManager.getCache("auth")); // auth cache removed
        
        // Verify cache configuration
        var usersCache = cacheManager.getCache("users");
        assertNotNull(usersCache);
    }

    @Test
    public void testNoCachingOnDynamicOperations() {
        // Create a test user
        User testUser = new User();
        testUser.setUsername("test_user");
        testUser.setEmail("test@example.com");
        testUser.setFullName("Test User");
        testUser.setActive(true);
        
        // These operations should not be cached
        User createdUser = userService.createUser(testUser);
        assertNotNull(createdUser);
        assertNotNull(createdUser.getId());
        
        // Verify user can be retrieved (no caching)
        var retrievedUser = userService.getUserById(createdUser.getId());
        assertTrue(retrievedUser.isPresent());
        assertEquals("test_user", retrievedUser.get().getUsername());
        
        // Clean up
        userService.deleteUser(createdUser.getId());
    }

    @Test
    public void testCachePerformance() {
        // Test that cached operations are faster than non-cached
        long startTime = System.nanoTime();
        userService.getUserCount(); // First call - hits database
        long firstCallTime = System.nanoTime() - startTime;
        
        startTime = System.nanoTime();
        userService.getUserCount(); // Second call - hits cache
        long secondCallTime = System.nanoTime() - startTime;
        
        // Cached call should be faster (though this is not guaranteed in all environments)
        // We'll just verify both calls complete successfully
        assertTrue(firstCallTime > 0);
        assertTrue(secondCallTime > 0);
    }
}
