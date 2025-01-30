package org.harsh.tuple.paisa.service;

import org.harsh.tuple.paisa.exception.InvalidLoginException;
import org.harsh.tuple.paisa.exception.UserAlreadyExistsException;
import org.harsh.tuple.paisa.exception.UserNotFoundException;
import org.harsh.tuple.paisa.model.User;
import org.harsh.tuple.paisa.model.Wallet;
import org.harsh.tuple.paisa.repository.UserRepository;
import org.harsh.tuple.paisa.repository.WalletRepository;
import org.harsh.tuple.paisa.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testRegisterUser_Success() {
        // Arrange
        User user = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User registeredUser = userService.registerUser(user);

        // Assert
        assertEquals("testuser", registeredUser.getUsername());
        verify(userRepository).save(any(User.class));
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void testRegisterUser_UserAlreadyExists() {
        // Arrange
        User user = User.builder()
                .id("user123")
                .username("testuser")
                .email("test@example.com")
                .password("password")
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> userService.registerUser(user));
    }

    @Test
    void testLoginUser_Success() {
        // Arrange
        User user = User.builder()
                .id("user123")
                .username("testuser")
                .password(new BCryptPasswordEncoder().encode("password"))
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken(user.getUsername())).thenReturn("dummyToken");

        // Act
        String token = userService.loginUser(user.getUsername(), "password");

        // Assert
        assertEquals("dummyToken", token);
    }

    @Test
    void testLoginUser_InvalidCredentials() {
        // Arrange
        User user = User.builder()
                .id("user123")
                .username("testuser")
                .password(new BCryptPasswordEncoder().encode("password"))
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(InvalidLoginException.class, () -> userService.loginUser(user.getUsername(), "wrongpassword"));
    }

    @Test
    void testFindUserByUsername() {
        //! Arrange
        User user = User.builder()
                .id("user123")
                .username("testuser")
                .build();

        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));

        // *Act
        Optional<User> foundUser = userService.findUserByUsername(user.getUsername());

        // Assert
        assertTrue(foundUser.isPresent());
        assertEquals("testuser", foundUser.get().getUsername());
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        User user = User.builder()
                .id("user123")
                .build();

        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        // Act
        userService.deleteUser(user.getId());

        // Assert
        verify(userRepository).deleteById(user.getId());
        verify(walletRepository).deleteByUserId(user.getId());
    }

    @Test
    void testDeleteUser_UserNotFound() {
        // Arrange
        String userId = "user123";

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UserNotFoundException.class, () -> userService.deleteUser(userId));
    }
}
