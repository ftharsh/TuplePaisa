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

import java.util.*;

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

    @Test
    public void testLoginUser_SuccessfulLogin() {
        // Arrange
        String username = "testUser";
        String password = "correctPassword";
        User mockUser = User.builder()
                .username(username)
                .password(new BCryptPasswordEncoder().encode(password)).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // Act
        String token = userService.loginUser(username, password);

        // Assert
        assertNull(token);
        verify(userRepository, times(1)).findByUsername(username);
    }

    @Test
    public void testLoginUser_InvalidPassword() {
        // Arrange
        String username = "testUser";
        String correctPassword = "correctPassword";
        String wrongPassword = "wrongPassword";
        User mockUser = User.builder()
        .username(username)
                .password(new BCryptPasswordEncoder().encode(correctPassword)).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(mockUser));

        // Act & Assert
        InvalidLoginException exception = assertThrows(InvalidLoginException.class, () -> {
            userService.loginUser(username, wrongPassword);
        });

        // Verify the ErrorResponse details
        assertNotNull(exception.getErrorResponse());
        assertEquals("Invalid username or password", exception.getErrorResponse().getMessage());
        assertEquals("ERR_INVALID_LOGIN", exception.getErrorResponse().getErrorCode());
        assertNotNull(exception.getErrorResponse().getTimestamp());

        Map<String, Object> details = exception.getErrorResponse().getDetails();
        assertNotNull(details);
        assertEquals(username, details.get("username"));

        // Verify interactions
        verify(userRepository, times(1)).findByUsername(username);
    }
    @Test
    public void testDeleteUser_SuccessfulDeletion() {
        // Arrange
        String userId = "123";
        User mockUser = User.builder()
                .id(userId).build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(mockUser));

        // Act
        userService.deleteUser(userId);

        // Assert
        verify(userRepository, times(1)).deleteById(userId);
        verify(walletRepository, times(1)).deleteByUserId(userId);
    }

    @Test
    public void testGetSuggestions_ValidJson() throws Exception {
        // Arrange
        String query = "test";
        List<String> mockResults = Arrays.asList(
                "{\"username\":\"user1\"}",
                "{\"username\":\"user2\"}"
        );

        when(userRepository.findUsernamesByQuery(".*" + query + ".*")).thenReturn(mockResults);

        // Act
        Map<String, Set<String>> response = userService.getSuggestions(query);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("usernames"));
        assertEquals(2, response.get("usernames").size());
        assertTrue(response.get("usernames").contains("user1"));
        assertTrue(response.get("usernames").contains("user2"));
        verify(userRepository, times(1)).findUsernamesByQuery(".*" + query + ".*");
    }

    @Test
    public void testGetSuggestions_InvalidJson() {
        // Arrange
        String query = "test";
        List<String> mockResults = Arrays.asList(
                "{\"username\":\"user1\"}",
                "{invalidJson}"
        );

        when(userRepository.findUsernamesByQuery(".*" + query + ".*")).thenReturn(mockResults);

        // Act
        Map<String, Set<String>> response = userService.getSuggestions(query);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("usernames"));
        assertEquals(1, response.get("usernames").size());
        assertTrue(response.get("usernames").contains("user1"));
        verify(userRepository, times(1)).findUsernamesByQuery(".*" + query + ".*");
    }

    @Test
    public void testGetSuggestions_NoResults() {
        // Arrange
        String query = "test";
        List<String> mockResults = Collections.emptyList();

        when(userRepository.findUsernamesByQuery(".*" + query + ".*")).thenReturn(mockResults);

        // Act
        Map<String, Set<String>> response = userService.getSuggestions(query);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("usernames"));
        assertTrue(response.get("usernames").isEmpty());
        verify(userRepository, times(1)).findUsernamesByQuery(".*" + query + ".*");
    }
}
