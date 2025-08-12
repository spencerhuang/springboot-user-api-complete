package com.example.demo.service;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MetricsService metricsService;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private User testUser2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("john_doe");
        testUser.setEmail("john.doe@example.com");
        testUser.setFullName("John Doe");
        testUser.setPhoneNumber("+1-555-123-4567");
        testUser.setActive(true);

        testUser2 = new User();
        testUser2.setId(2L);
        testUser2.setUsername("jane_smith");
        testUser2.setEmail("jane.smith@example.com");
        testUser2.setFullName("Jane Smith");
        testUser2.setPhoneNumber("+1-555-234-5678");
        testUser2.setActive(true);
    }

    @Test
    void testGetUserById_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserById(1L);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getId(), result.get().getId());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findById(1L);
    }

    @Test
    void testGetUserById_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When
        Optional<User> result = userService.getUserById(999L);

        // Then
        assertFalse(result.isPresent());
        verify(userRepository).findById(999L);
    }

    @Test
    void testGetUserByUsername_Success() {
        // Given
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByUsername("john_doe");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getUsername(), result.get().getUsername());
        verify(userRepository).findByUsername("john_doe");
    }

    @Test
    void testGetUserByEmail_Success() {
        // Given
        when(userRepository.findByEmail("john.doe@example.com")).thenReturn(Optional.of(testUser));

        // When
        Optional<User> result = userService.getUserByEmail("john.doe@example.com");

        // Then
        assertTrue(result.isPresent());
        assertEquals(testUser.getEmail(), result.get().getEmail());
        verify(userRepository).findByEmail("john.doe@example.com");
    }

    @Test
    void testGetAllUsers_Success() {
        // Given
        List<User> users = Arrays.asList(testUser, testUser2);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findAll(pageable)).thenReturn(page);

        // When
        Page<User> result = userService.getAllUsers(pageable);

        // Then
        assertNotNull(result);
        assertEquals(2, result.getContent().size());
        assertEquals(2, result.getTotalElements());
        verify(userRepository).findAll(pageable);
    }

    @Test
    void testSearchUsers_Success() {
        // Given
        String query = "john";
        List<User> users = Arrays.asList(testUser);
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> page = new PageImpl<>(users, pageable, users.size());
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            eq(query), eq(query), eq(pageable))).thenReturn(page);

        // When
        Page<User> result = userService.searchUsers(query, pageable);

        // Then
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(testUser.getUsername(), result.getContent().get(0).getUsername());
        verify(userRepository).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(query, query, pageable);
    }

    @Test
    void testCreateUser_Success() {
        // Given
        User newUser = new User();
        newUser.setUsername("new_user");
        newUser.setEmail("new.user@example.com");
        newUser.setFullName("New User");
        newUser.setActive(true);

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("new.user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        // When
        User result = userService.createUser(newUser);

        // Then
        assertNotNull(result);
        assertEquals("new_user", result.getUsername());
        assertEquals("new.user@example.com", result.getEmail());
        verify(userRepository).existsByUsername("new_user");
        verify(userRepository).existsByEmail("new.user@example.com");
        verify(userRepository).save(newUser);
    }

    @Test
    void testCreateUser_UsernameAlreadyExists() {
        // Given
        User newUser = new User();
        newUser.setUsername("john_doe");
        newUser.setEmail("new.user@example.com");

        when(userRepository.existsByUsername("john_doe")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(newUser);
        });

        assertEquals("Username already exists: john_doe", exception.getMessage());
        verify(userRepository).existsByUsername("john_doe");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testCreateUser_EmailAlreadyExists() {
        // Given
        User newUser = new User();
        newUser.setUsername("new_user");
        newUser.setEmail("john.doe@example.com");

        when(userRepository.existsByUsername("new_user")).thenReturn(false);
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.createUser(newUser);
        });

        assertEquals("Email already exists: john.doe@example.com", exception.getMessage());
        verify(userRepository).existsByUsername("new_user");
        verify(userRepository).existsByEmail("john.doe@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testUpdateUser_Success() {
        // Given
        User updateData = new User();
        updateData.setUsername("john_updated");
        updateData.setEmail("john.updated@example.com");
        updateData.setFullName("John Updated");
        updateData.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        User result = userService.updateUser(1L, updateData);

        // Then
        assertNotNull(result);
        verify(userRepository).findById(1L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testUpdateUser_NotFound() {
        // Given
        User updateData = new User();
        updateData.setUsername("updated_user");

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.updateUser(999L, updateData);
        });

        assertEquals("User not found with ID: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testDeleteUser_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        doNothing().when(userRepository).deleteById(1L);

        // When
        userService.deleteUser(1L);

        // Then
        verify(userRepository).findById(1L);
        verify(userRepository).deleteById(1L);
    }

    @Test
    void testDeleteUser_NotFound() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(999L);
        });

        assertEquals("User not found with ID: 999", exception.getMessage());
        verify(userRepository).findById(999L);
        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void testGetUserCount_Success() {
        // Given
        when(userRepository.count()).thenReturn(2L);

        // When
        long result = userService.getUserCount();

        // Then
        assertEquals(2L, result);
        verify(userRepository).count();
    }

    @Test
    void testGetActiveUserCount_Success() {
        // Given
        when(userRepository.count()).thenReturn(2L);

        // When
        long result = userService.getActiveUserCount();

        // Then
        assertEquals(2L, result);
        verify(userRepository).count();
    }

    @Test
    void testClearAllCaches_Success() {
        // When
        userService.clearAllCaches();

        // Then
        // This method just logs, so we just verify it doesn't throw an exception
        assertDoesNotThrow(() -> userService.clearAllCaches());
    }
}
