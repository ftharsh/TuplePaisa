package org.harsh.tuple.paisa.exception;


import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
public class InsufficientBalanceException extends RuntimeException {
    private final ErrorResponse errorResponse;

    public InsufficientBalanceException(String userId, double availableBalance, double transferAmount) {
        this.errorResponse = new ErrorResponse(
                "Insufficient balance in wallet",
                "ERR_INSUFFICIENT_BALANCE",
                LocalDateTime.now(),
                Map.of(
                        "user_id", userId,
                        "available_balance", availableBalance,
                        "transfer_amount", transferAmount
                )
        );
    }
}