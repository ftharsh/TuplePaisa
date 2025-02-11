package org.harsh.tuple.paisa.exception;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.harsh.tuple.paisa.dto.ErrorResponse;

import java.time.LocalDateTime;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Getter
public class InvalidTransactionAmountException extends RuntimeException {

    private final ErrorResponse errorResponse;

    public InvalidTransactionAmountException(double amount) {
        this.errorResponse = new ErrorResponse(
                "Transaction amount must be positive",
                "ERR_INVALID_AMOUNT",
                LocalDateTime.now(),
                Map.of("amount", amount)
        );
    }
}

