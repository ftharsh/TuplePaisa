package org.harsh.tuple.paisa.controller;

import org.harsh.tuple.paisa.model.Cashback;
import org.harsh.tuple.paisa.service.CashbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CashbackControllerTest {

    @Mock
    private CashbackService cashbackService;

    @InjectMocks
    private CashbackController cashbackController;

    @Test
    void testGetCashbackHistory() {
        //!arrangement
        String userId = "Harsh123";
        Cashback cashback1 = Cashback.builder()
                .id("1")
                .userId(userId)
                .amount(123.456)
                .timestamp(LocalDateTime.now())
                .build();

        Cashback cashback2 = Cashback.builder()
                .id("1")
                .userId(userId)
                .amount(891.011)
                .timestamp(LocalDateTime.now())
                .build();


        List<Cashback> mockCashbacks = Arrays.asList(cashback1, cashback2);
        when(cashbackService.getCashbackHistory(userId)).thenReturn(mockCashbacks);

        // !action time
        ResponseEntity<?> response = cashbackController.getCashbackHistory(userId);

        //*assertion
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(mockCashbacks, response.getBody());
        verify(cashbackService, times(1)).getCashbackHistory(userId);
    }
}