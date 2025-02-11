package org.harsh.tuple.paisa.exception;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
public class WalletNotFoundException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public WalletNotFoundException(String userId) {
        Map<String, Object> details = new HashMap<>();
        details.put("userId", userId);
        this.errorResponse = new ErrorResponse(
                "Wallet not found",
                "ERR_WALLET_NOT_FOUND",
                LocalDateTime.now(),
                Map.of("user_id", userId)
        );
    }
}
