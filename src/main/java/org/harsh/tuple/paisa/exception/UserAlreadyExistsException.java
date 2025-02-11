package org.harsh.tuple.paisa.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
public class UserAlreadyExistsException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public UserAlreadyExistsException(String field, String value) {
        Map<String, Object> details = new HashMap<>();
        details.put(field, value);
        this.errorResponse = new ErrorResponse("User already exists", "ERR_USER_EXISTS", LocalDateTime.now(), details);
    }
}

