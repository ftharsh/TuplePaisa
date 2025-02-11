package org.harsh.tuple.paisa.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Collections;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.error("UserAlreadyExistsException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getErrorResponse());
    }

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<String> handleJsonProcessingException(JsonProcessingException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body("Invalid JSON format: " + ex.getMessage());
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException ex) {
        log.error("UserNotFoundException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getErrorResponse());
    }

    @ExceptionHandler(InvalidLoginException.class)
    public ResponseEntity<ErrorResponse> handleInvalidLoginException(InvalidLoginException ex) {
        log.error("InvalidLoginException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getErrorResponse());
    }

    @ExceptionHandler(InvalidTransactionAmountException.class)
    public ResponseEntity<ErrorResponse> handleInvalidTransactionAmountException(InvalidTransactionAmountException ex) {
        log.error("InvalidTransactionAmountException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getErrorResponse());
    }

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFoundException(WalletNotFoundException ex) {
        log.error("WalletNotFoundException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getErrorResponse());
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalanceException(InsufficientBalanceException ex) {
        log.error("InsufficientBalanceException: {}", ex.getErrorResponse().getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getErrorResponse());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        log.error("Unexpected exception: {}", ex.getMessage(), ex);
        ErrorResponse errorResponse = new ErrorResponse(
                "Internal Server Error",
                "ERR_INTERNAL_SERVER",
                LocalDateTime.now(),
                Collections.emptyMap()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

}


