package com.example.demo.service;

import com.example.demo.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private AuthService authService;

    private static final String TEST_USERNAME = "john_doe";
    private static final String TEST_TOKEN = "eyJhbGciOiJIUzI1NiJ9.test.token";

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(jwtUtil, metricsService);
    }

    @Test
    void testValidateToken_Success() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(true);

        // When
        boolean result = authService.validateToken(TEST_TOKEN);

        // Then
        assertTrue(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).validateToken(TEST_TOKEN);
    }

    @Test
    void testValidateToken_Failure() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(TEST_TOKEN)).thenReturn(false);

        // When
        boolean result = authService.validateToken(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil).validateToken(TEST_TOKEN);
    }

    @Test
    void testValidateToken_Exception() {
        // Given
        when(jwtUtil.extractUsername(TEST_TOKEN)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        boolean result = authService.validateToken(TEST_TOKEN);

        // Then
        assertFalse(result);
        verify(jwtUtil).extractUsername(TEST_TOKEN);
        verify(jwtUtil, never()).validateToken(anyString());
    }

    @Test
    void testAuthenticateUser_Success() {
        // Given
        when(jwtUtil.generateToken(TEST_USERNAME)).thenReturn(TEST_TOKEN);

        // When
        Map<String, Object> result = authService.authenticateUser(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_TOKEN, result.get("token"));
        assertEquals(TEST_USERNAME, result.get("username"));
        assertEquals("Authentication successful", result.get("message"));
        assertEquals("Bearer", result.get("tokenType"));
        assertEquals("1 hour", result.get("expiresIn"));
        verify(jwtUtil).generateToken(TEST_USERNAME);
    }

    @Test
    void testAuthenticateUser_EmptyUsername() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.authenticateUser("");
        });

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testAuthenticateUser_NullUsername() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.authenticateUser(null);
        });

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testAuthenticateUser_WhitespaceUsername() {
        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.authenticateUser("   ");
        });

        assertEquals("Username cannot be empty", exception.getMessage());
        verify(jwtUtil, never()).generateToken(anyString());
    }

    @Test
    void testGetUserSession_Success() {
        // When
        Map<String, Object> result = authService.getUserSession(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.get("username"));
        assertTrue((Boolean) result.get("active"));
        assertNotNull(result.get("lastAccess"));
    }

    @Test
    void testLogoutUser_Success() {
        // When
        authService.logoutUser(TEST_USERNAME);

        // Then
        // The @CacheEvict annotation will handle cache clearing
        // We just verify the method executes without exception
        assertDoesNotThrow(() -> authService.logoutUser(TEST_USERNAME));
    }

    @Test
    void testClearAllAuthCaches_Success() {
        // When
        authService.clearAllAuthCaches();

        // Then
        // The @CacheEvict annotation will handle cache clearing
        // We just verify the method executes without exception
        assertDoesNotThrow(() -> authService.clearAllAuthCaches());
    }

    @Test
    void testRefreshUserSession_Success() {
        // When
        Map<String, Object> result = authService.refreshUserSession(TEST_USERNAME);

        // Then
        assertNotNull(result);
        assertEquals(TEST_USERNAME, result.get("username"));
        assertTrue((Boolean) result.get("active"));
        assertNotNull(result.get("lastAccess"));
        assertTrue((Boolean) result.get("refreshed"));
    }

    @Test
    void testIsUserAuthenticated_ValidUsername() {
        // When
        boolean result = authService.isUserAuthenticated(TEST_USERNAME);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsUserAuthenticated_EmptyUsername() {
        // When
        boolean result = authService.isUserAuthenticated("");

        // Then
        assertFalse(result);
    }

    @Test
    void testIsUserAuthenticated_NullUsername() {
        // When
        boolean result = authService.isUserAuthenticated(null);

        // Then
        assertFalse(result);
    }

    @Test
    void testIsUserAuthenticated_WhitespaceUsername() {
        // When
        boolean result = authService.isUserAuthenticated("   ");

        // Then
        assertFalse(result);
    }

    @Test
    void testValidateToken_WithValidToken() {
        // Given
        String validToken = "valid.jwt.token";
        when(jwtUtil.extractUsername(validToken)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(validToken)).thenReturn(true);

        // When
        boolean result = authService.validateToken(validToken);

        // Then
        assertTrue(result);
        verify(jwtUtil).extractUsername(validToken);
        verify(jwtUtil).validateToken(validToken);
    }

    @Test
    void testValidateToken_WithInvalidToken() {
        // Given
        String invalidToken = "invalid.jwt.token";
        when(jwtUtil.extractUsername(invalidToken)).thenReturn(TEST_USERNAME);
        when(jwtUtil.validateToken(invalidToken)).thenReturn(false);

        // When
        boolean result = authService.validateToken(invalidToken);

        // Then
        assertFalse(result);
        verify(jwtUtil).extractUsername(invalidToken);
        verify(jwtUtil).validateToken(invalidToken);
    }

    @Test
    void testValidateToken_WithException() {
        // Given
        String problematicToken = "problematic.jwt.token";
        when(jwtUtil.extractUsername(problematicToken)).thenThrow(new RuntimeException("Token parsing error"));

        // When
        boolean result = authService.validateToken(problematicToken);

        // Then
        assertFalse(result);
        verify(jwtUtil).extractUsername(problematicToken);
        verify(jwtUtil, never()).validateToken(anyString());
    }
}
