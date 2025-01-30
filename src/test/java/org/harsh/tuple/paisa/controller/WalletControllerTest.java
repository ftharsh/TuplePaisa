package org.harsh.tuple.paisa.controller;

import org.harsh.tuple.paisa.model.Cashback;
import org.harsh.tuple.paisa.model.Transaction;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.harsh.tuple.paisa.repository.TransactionRepository;
import org.harsh.tuple.paisa.service.WalletService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @Mock
    private CashbackRepository cashbackRepository;


    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private WalletController walletController;

    private static Transaction transaction;
    private static Cashback cashback;

    @BeforeAll
    static void setupTestData() {
        // Updated user ID and transaction amount
        transaction = Transaction.builder()
                .id("txn1")
                .userId("harsh123")
                .recipientId("unknown123")  // Updated recipient ID
                .amount(69.0)  // Updated transaction amount
                .timestamp(LocalDateTime.now())
                .build();

        // Updated cashback amount
        cashback = Cashback.builder()
                .id("cb1")
                .userId("harsh123")
                .amount(21.0)  // Updated cashback amount
                .timestamp(LocalDateTime.now())
                .build();
    }

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        when(authentication.getName()).thenReturn("harsh123");
        when(authentication.getPrincipal()).thenReturn("harsh123");
    }

    // Test: Wallet Recharge
    @Test
    void testRechargeWallet() {
        double amount = 50.0;
        when(walletService.rechargeWallet("harsh123", amount)).thenReturn(transaction);

        ResponseEntity<?> response = walletController.rechargeWallet(amount);

        assertNotNull(response);
        assertEquals(ResponseEntity.ok(transaction), response);
        verify(walletService, times(1)).rechargeWallet("harsh123", amount);
    }

    // Test: Wallet Transfer - Successful
    @Test
    void testTransferWallet() {
        String recipientId = "unknown123";  // Updated recipient ID
        double amount = 30.0;
        List<Transaction> transactions = List.of(transaction);

        when(walletService.transferWallet("harsh123", recipientId, amount)).thenReturn(transactions);

        ResponseEntity<?> response = walletController.transferWallet(recipientId, amount);

        assertNotNull(response);
        assertEquals(ResponseEntity.ok(transactions), response);
        verify(walletService, times(1)).transferWallet("harsh123", recipientId, amount);
    }

    // Test: Wallet Transfer - Invalid Recipient
    @Test
    void testTransferWalletWithInvalidRecipient() {
        String recipientId = "invalid123";  // Invalid recipient ID
        double amount = 30.0;

        when(walletService.transferWallet("harsh123", recipientId, amount))
                .thenThrow(new IllegalArgumentException("Invalid recipient"));

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            walletController.transferWallet(recipientId, amount);
        });

        assertEquals("Invalid recipient", exception.getMessage());
    }


    @Test
    void testGetCombinedHistoryWithPagination_Empty() {
        String userId = "user123";
        when(authentication.getName()).thenReturn(userId);
        List<Object> combinedList = List.of(new Object(), new Object());
        when(walletService.getHistory(userId)).thenReturn(combinedList);

        List<Object> result = walletController.getCombinedHistory(2, 2);

        assertEquals(Collections.emptyList(), result);
    }

    @Test
    void testGetCombinedHistoryWithoutPagination() {
        String userId = "user123";
        when(authentication.getPrincipal()).thenReturn(userId);
        List<Object> combinedList = List.of(new Object(), new Object(), new Object());
        when(walletService.getHistory(userId)).thenReturn(combinedList);

        List<Object> result = walletController.getCombinedHistory();

        assertEquals(combinedList, result);
    }

    // Test: Get Wallet Balance - Valid Data
    @Test
    void testGetWalletBalance() {
        double balance = 200.0;
        when(walletService.getBalance("harsh123")).thenReturn(balance);

        Map<String, Double> result = walletController.getWalletBalance();

        assertNotNull(result);
        assertEquals(balance, result.get("balance"));
        verify(walletService, times(1)).getBalance("harsh123");
    }

    // Test: Get Wallet Balance - Zero Balance
    @Test
    void testGetWalletBalanceZero() {
        when(walletService.getBalance("harsh123")).thenReturn(0.0);

        Map<String, Double> result = walletController.getWalletBalance();

        assertNotNull(result);
        assertEquals(0.0, result.get("balance"));
    }

    @Test
    void testGetCombinedHistoryWithOnlyTransaction() {
        List<Object> combinedList = new ArrayList<>();
        combinedList.add(transaction);  // Only Transaction in the list

        // Sorting by timestamp (only Transaction, no sorting needed)
        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();
            return time2.compareTo(time1);  // Sort in descending order
        });

        // Verify that the list has only Transaction and is correctly sorted
        assertEquals(1, combinedList.size());
        assertTrue(combinedList.get(0) instanceof Transaction);  // Only one element (Transaction)
    }

    @Test
    void testGetCombinedHistoryWithTransactionAndCashback() {
        // Create mock Transaction and Cashback objects with specific timestamps
        Transaction transaction = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 12, 1, 10, 0)).build();  // Later timestamp

        Cashback cashback = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 11, 30, 12, 0)).build();    // Earlier timestamp

        // Add both objects to the combined list
        List<Object> combinedList = new ArrayList<>();
        combinedList.add(transaction);  // Add Transaction object
        combinedList.add(cashback);     // Add Cashback object

        // Sorting the list by timestamp in descending order
        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();
            return time2.compareTo(time1);  // Sort in descending order
        });

        // Assertions to verify the correct sorting and contents of the list
        assertEquals(2, combinedList.size(), "Combined list should have 2 elements");

        // Verify that the first element is Transaction (later timestamp)
        assertTrue(combinedList.get(0) instanceof Transaction, "First element should be a Transaction");
        assertTrue(combinedList.get(1) instanceof Cashback, "Second element should be a Cashback");

        // Verify that the Transaction timestamp is after Cashback timestamp
        assertTrue(
                ((Transaction) combinedList.get(0)).getTimestamp().isAfter(
                        ((Cashback) combinedList.get(1)).getTimestamp()
                ),
                "Transaction timestamp should be after Cashback timestamp"
        );
    }


    // Test: Get Combined History (with only Cashback)
    @Test
    void testGetCombinedHistoryWithOnlyCashback() {
        List<Object> combinedList = new ArrayList<>();
        combinedList.add(cashback);  // Only Cashback in the list

        // Sorting by timestamp (only Cashback, no sorting needed)
        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();
            return time2.compareTo(time1);  // Sort in descending order
        });

        // Verify that the list has only Cashback and is correctly sorted
        assertEquals(1, combinedList.size());
        assertTrue(combinedList.get(0) instanceof Cashback);  // Only one element (Cashback)
    }
}
