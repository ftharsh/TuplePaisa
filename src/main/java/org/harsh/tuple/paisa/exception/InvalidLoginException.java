package org.harsh.tuple.paisa.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class InvalidLoginException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public InvalidLoginException(String username) {
        Map<String, Object> details = new HashMap<>();
        details.put("username", username);
        this.errorResponse = new ErrorResponse("Invalid username or password", "ERR_INVALID_LOGIN", LocalDateTime.now(), details);
    }

}

