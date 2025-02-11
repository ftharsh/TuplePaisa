package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.exception.InvalidLoginException;
import org.harsh.tuple.paisa.exception.UserAlreadyExistsException;
import org.harsh.tuple.paisa.exception.UserNotFoundException;
import org.harsh.tuple.paisa.model.User;
import org.harsh.tuple.paisa.model.Wallet;
import org.harsh.tuple.paisa.repository.UserRepository;
import org.harsh.tuple.paisa.repository.WalletRepository;
import org.harsh.tuple.paisa.util.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@RequiredArgsConstructor
@Service
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // ?Register User
    public User registerUser(User user) {
        log.info("Registering user with Username: {}", user.getUsername());

        userRepository.findByUsername(user.getUsername())
                .ifPresent(_ -> {
                    log.error("User with Username {} already exists.", user.getUsername());
                    throw new UserAlreadyExistsException("username", user.getUsername());
                });

        userRepository.findByEmail(user.getEmail())
                .ifPresent(_ -> {
                    log.error("User with Email {} already exists.", user.getEmail());
                    throw new UserAlreadyExistsException("email", user.getEmail());
                });


        user.setPassword(passwordEncoder.encode(user.getPassword()));



        User registeredUser = userRepository.save(user);
        log.info("User registered successfully, initializing Wallet Creation: {}", registeredUser.getUsername());

        Wallet wallet =  Wallet.builder()
                .userId(registeredUser.getId())
                .balance(0.0)
                .build();


        walletRepository.save(wallet);
        log.info("Wallet registered successfully: {}", wallet.getBalance());

        return registeredUser;
    }

    // ?Login User
    public String loginUser(String username, String password) {
        log.info("User login attempt for Username: {}", username);

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.error("Invalid login attempt for Username: {}", username);
                    return new InvalidLoginException(username);
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.error("Invalid password attempt for Username: {}", username);
            throw new InvalidLoginException(username);
        }

        log.info("User logged in successfully: {}", user.getUsername());
        return jwtUtil.generateToken(username);
    }

    public Optional<User> findUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    //?Delete User
    public void deleteUser(String id) {
        log.debug("Attempting to find user with id: {}", id);

        User user = userRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("User not found with id: {}", id);
                    return new UserNotFoundException(id);
                });

        userRepository.deleteById(id);
        walletRepository.deleteByUserId(user.getId());
        log.debug("User and associated wallet deleted successfully for id: {}", id);
    }

    public Map<String, Set<String>> getSuggestions(String query){
        String regexQuery = ".*"+query+".*";
        List<String> results = userRepository.findUsernamesByQuery(regexQuery);

        ObjectMapper objectMapper = new ObjectMapper();
        Set<String> usernames = new HashSet<>();

        for (String result : results) {
            try {
                // Parse the JSON string to extract the username
                JsonNode jsonNode = objectMapper.readTree(result);
                String username = jsonNode.get("username").asText();
                usernames.add(username);
            } catch (Exception e) {
                // Handle parsing errors
                e.printStackTrace();
            }
        }

        // Return the usernames as a JSON object
        Map<String, Set<String>> response = new HashMap<>();
        response.put("usernames", usernames);
        return response;
    }
}








