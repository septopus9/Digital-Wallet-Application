package com.rs.payments.wallet.service.impl;

import com.rs.payments.wallet.exception.DuplicateResourceException;
import com.rs.payments.wallet.model.User;
import com.rs.payments.wallet.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User user; // added here
    @BeforeEach
    void setUp() {
        user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
    }

    @Test
    @DisplayName("Should create user successfully")
    void shouldCreateUser() {

        UUID id = UUID.randomUUID();
        User savedUser = new User(id, "testuser", "test@example.com", null);
        when(userRepository.save(user)).thenReturn(savedUser);

        // When
        User result = userService.createUser(user);

        // Then
        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("testuser", result.getUsername());
        verify(userRepository, times(1)).save(user);
    }

    // ── NEW test 1 ────────────────────────────────────────────────────────


    @Test
    @DisplayName("Should throw DuplicateResourceException when username already exists")
    void shouldThrowWhenUsernameIsDuplicate() {
        // username exists in DB → returns true
        when(userRepository.existsByUsername("testuser")).thenReturn(true);

        // calling createUser should throw DuplicateResourceException
        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username already taken");

        // save must NEVER be called — we stopped before reaching it
        verify(userRepository, never()).save(any());
    }

    // ── NEW test 2 ────────────────────────────────────────────────────────
    @Test
    @DisplayName("Should throw DuplicateResourceException when email already exists")
    void shouldThrowWhenEmailIsDuplicate() {
        // username is fine, but email exists
        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(user))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already registered");

        verify(userRepository, never()).save(any());
    }
}
