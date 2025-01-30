package org.harsh.tuple.paisa.service;

import org.harsh.tuple.paisa.exception.InvalidTransactionAmountException;
import org.harsh.tuple.paisa.exception.WalletNotFoundException;
import org.harsh.tuple.paisa.model.Cashback;
import org.harsh.tuple.paisa.model.Wallet;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.harsh.tuple.paisa.repository.WalletRepository;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CashbackServiceTest {

    @Mock
    private CashbackRepository cashbackRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private CashbackService cashbackService;

    private static String userId;
    private static Wallet wallet;

    @BeforeAll
    static void setUpConstants() {
        userId = "harsh123";
        wallet = Wallet.builder()
                .id("harsh'swallet22")
                .userId(userId)
                .balance(1000.0).build();
    }

    @BeforeEach
    void setUpMocks() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void resetWalletBalance() {
        wallet.setBalance(1000.0);
    }


    @Test
    @DisplayName("Should throw InvalidTransactionAmountException when recharge amount is zero")
    void applyCashback_ZeroAmount_ThrowsInvalidTransactionAmountException() {
        String userId = "harsh123";
        assertThrows(InvalidTransactionAmountException.class,
                () -> cashbackService.applyCashback(userId, 0));
    }

    @Test
    @DisplayName("Should throw InvalidTransactionAmountException when recharge amount is negative")
    void applyCashback_NegativeAmount_ThrowsInvalidTransactionAmountException() {
        String userId = "harsh123";
        assertThrows(InvalidTransactionAmountException.class,
                () -> cashbackService.applyCashback(userId, -100));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet doesn't exist")
    void applyCashback_WalletNotFound_ThrowsWalletNotFoundException() {
        String userId = "harsh123";
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class,
                () -> cashbackService.applyCashback(userId, 100));
    }

    @Test
    @DisplayName("Should successfully apply cashback and update wallet")
    void applyCashback_ValidAmount_UpdatesWalletAndSavesCashback() {
        String userId = "harsh123";
        double rechargeAmount = 100;
        double expectedCashback = 5;

        wallet.setBalance(1000);

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        cashbackService.applyCashback(userId, rechargeAmount);

        ArgumentCaptor<Cashback> cashbackCaptor = ArgumentCaptor.forClass(Cashback.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);

        verify(cashbackRepository).save(cashbackCaptor.capture());
        verify(walletRepository).save(walletCaptor.capture());

        Cashback savedCashback = cashbackCaptor.getValue();
        Wallet savedWallet = walletCaptor.getValue();

        assertEquals(userId, savedCashback.getUserId());
        assertEquals(expectedCashback, savedCashback.getAmount());
        assertEquals(1005, savedWallet.getBalance());
        assertNotNull(savedCashback.getTimestamp());
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when getting cashback history for non-existent wallet")
    void getCashbackHistory_WalletNotFound_ThrowsWalletNotFoundException() {
        String userId = "harsh123";
        when(walletRepository.existsByUserId(userId)).thenReturn(false);
        assertThrows(WalletNotFoundException.class,
                () -> cashbackService.getCashbackHistory(userId));
    }

    @Test
    @DisplayName("Should return cashback history for valid user")
    void getCashbackHistory_ValidUser_ReturnsCashbackList() {
        String userId = "harsh123";
        List<Cashback> expectedCashbacks = Arrays.asList(
                Cashback.builder().userId(userId).amount(5.0).timestamp(LocalDateTime.now()).build(),
                Cashback.builder().userId(userId).amount(10.0).timestamp(LocalDateTime.now()).build()
        );

        when(walletRepository.existsByUserId(userId)).thenReturn(true);
        when(cashbackRepository.findByUserId(userId)).thenReturn(expectedCashbacks);

        List<Cashback> actualCashbacks = cashbackService.getCashbackHistory(userId);

        assertEquals(expectedCashbacks, actualCashbacks);
        verify(cashbackRepository).findByUserId(userId);
    }
}
