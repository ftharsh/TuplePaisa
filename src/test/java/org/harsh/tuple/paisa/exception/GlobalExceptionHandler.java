package org.harsh.tuple.paisa.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.harsh.tuple.paisa.dto.ErrorResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;
    private LocalDateTime testTime;
    private Map<String, Object> errorDetails;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
        testTime = LocalDateTime.now();
        errorDetails = new HashMap<>();
        errorDetails.put("detail", "Test detail");
    }

    @Test
    void handleUserAlreadyExistsException() {
        String message = "User already exists";
        String code = "ERR_USER_EXISTS";
        UserAlreadyExistsException exception = new UserAlreadyExistsException(message, code);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserAlreadyExistsException(exception);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(message, response.getBody().getMessage());
    }

    @Test
    void handleJsonProcessingException() {
        String errorMessage = "Invalid JSON";
        JsonProcessingException exception = new JsonProcessingException(errorMessage) {};

        ResponseEntity<String> response = exceptionHandler.handleJsonProcessingException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains(errorMessage));
    }

    @Test
    void handleUserNotFoundException() {
        String errorMessage = "User not found";
        UserNotFoundException exception = new UserNotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUserNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleInvalidLoginException() {
        String errorMessage = "Invalid username or password";
        InvalidLoginException exception = new InvalidLoginException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidLoginException(exception);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleInvalidTransactionAmountException() {
        double invalidAmount = 0.0;
        InvalidTransactionAmountException exception = new InvalidTransactionAmountException(invalidAmount);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidTransactionAmountException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleWalletNotFoundException() {
        String errorMessage = "Wallet not found";
        WalletNotFoundException exception = new WalletNotFoundException(errorMessage);

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleWalletNotFoundException(exception);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(errorMessage, response.getBody().getMessage());
    }

    @Test
    void handleInsufficientBalanceException() {
        double requiredAmount = 100.0;
        double currentBalance = 50.0;
        InsufficientBalanceException exception = new InsufficientBalanceException(
                "Insufficient balance",
                requiredAmount,
                currentBalance
        );

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInsufficientBalanceException(exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    @Test
    void handleGenericException() {
        Exception exception = new RuntimeException("Unexpected error");

        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(exception);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Internal Server Error", response.getBody().getMessage());
    }
}