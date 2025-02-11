package org.harsh.tuple.paisa.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Getter
@EqualsAndHashCode(callSuper = true)
public class UserNotFoundException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public UserNotFoundException(String userId) {
        Map<String, Object> details = new HashMap<>();
        details.put("user_id", userId);
        this.errorResponse = new ErrorResponse("User not found", "ERR_USER_NOT_FOUND", LocalDateTime.now(), details);
    }

}


