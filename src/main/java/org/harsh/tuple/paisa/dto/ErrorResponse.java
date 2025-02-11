package org.harsh.tuple.paisa.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class ErrorResponse {
    private final String message;
    private final String errorCode;
    private final LocalDateTime timestamp;
    private final Map<String, Object> details;
}

