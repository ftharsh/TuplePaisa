package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.harsh.tuple.paisa.dto.EmailDetails;
import org.harsh.tuple.paisa.exception.InsufficientBalanceException;
import org.harsh.tuple.paisa.exception.InvalidTransactionAmountException;
import org.harsh.tuple.paisa.exception.WalletNotFoundException;
import org.harsh.tuple.paisa.model.*;
import org.harsh.tuple.paisa.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CashbackService cashbackService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CashbackRepository cashbackRepository;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private WalletService walletService;

    private User testUser;
    private Wallet testWallet;
    private Transaction testTransaction;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id("user1")
                .username("testUser")
                .email("test@example.com")
                .build();

        testWallet = Wallet.builder()
                .id("wallet1")
                .userId("user1")
                .balance(1000.0)
                .build();

        testTransaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .walletId("wallet1")
                .amount(100.0)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @Test
    void rechargeWallet_Success() {
        double rechargeAmount = 100.0;
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Transaction expectedTransaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .walletId("wallet1")
                .type(TransactionType.RECHARGE)
                .amount(rechargeAmount)
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

        Transaction result = walletService.rechargeWallet("user1", rechargeAmount);

        assertNotNull(result);
        assertEquals(rechargeAmount, result.getAmount());
        assertEquals(TransactionType.RECHARGE, result.getType());
        verify(cashbackService).applyCashback("user1", rechargeAmount);
        verify(walletRepository).save(any(Wallet.class));
    }

    @Test
    void rechargeWallet_WithDecimalAmount() {
        double rechargeAmount = 100.55;
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Transaction expectedTransaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .walletId("wallet1")
                .type(TransactionType.RECHARGE)
                .amount(rechargeAmount)
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

        Transaction result = walletService.rechargeWallet("user1", rechargeAmount);

        assertNotNull(result);
        assertEquals(rechargeAmount, result.getAmount());
        assertEquals(1100.55, testWallet.getBalance(), 0.001);
    }

    @Test
    void rechargeWallet_ZeroAmount() {
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.rechargeWallet("user1", 0.0));
    }

    @Test
    void rechargeWallet_NegativeAmount() {
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.rechargeWallet("user1", -100.0));
    }

    @Test
    void rechargeWallet_WalletNotFound() {
        when(walletRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.rechargeWallet("nonexistent", 100.0));
    }

    @Test
    void transferWallet_Success() {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        List<Transaction> results = walletService.transferWallet("user1", "user2", 100.0);

        assertEquals(2, results.size());
        assertEquals(900.0, testWallet.getBalance());
        assertEquals(600.0, recipientWallet.getBalance());
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void transferWallet_ExactBalance() {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        List<Transaction> results = walletService.transferWallet("user1", "user2", 1000.0);

        assertEquals(2, results.size());
        assertEquals(0.0, testWallet.getBalance());
        assertEquals(1500.0, recipientWallet.getBalance());
    }

    @Test
    void transferWallet_InsufficientBalance() {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));

        assertThrows(InsufficientBalanceException.class,
                () -> walletService.transferWallet("user1", "user2", 2000.0));
    }

    @Test
    void transferWallet_SenderNotFound() {
        when(walletRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet("nonexistent", "user2", 100.0));
    }

    @Test
    void transferWallet_RecipientNotFound() {
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet("user1", "nonexistent", 100.0));
    }

    @Test
    void getBalance_Success() {
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));

        double balance = walletService.getBalance("user1");

        assertEquals(1000.0, balance);
    }

    @Test
    void getBalance_WalletNotFound() {
        when(walletRepository.findByUserId("nonexistent")).thenReturn(Optional.empty());

        assertThrows(WalletNotFoundException.class,
                () -> walletService.getBalance("nonexistent"));
    }

    @Test
    void getCombinedHistory_Success() {
        List<Transaction> transactions = Arrays.asList(testTransaction);
        List<Cashback> cashbacks = Arrays.asList(
                Cashback.builder()
                        .id("cash1")
                        .userId("user1")
                        .amount(10.0)
                        .timestamp(LocalDateTime.now())
                        .build()
        );

        Page<Transaction> transactionPage = new PageImpl<>(transactions);
        Page<Cashback> cashbackPage = new PageImpl<>(cashbacks);

        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(transactionPage);
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(cashbackPage);

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertNotNull(history);
        assertEquals(2, history.size());
    }

    @Test
    void sendEmail_Success() throws Exception {
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(objectMapper.writeValueAsString(any(EmailDetails.class))).thenReturn("emailJson");

        walletService.sendEmail("user1", 100.0);

        verify(kafkaTemplate).send(eq("email-notifications"), anyString());
    }

    @Test
    void sendEmail_UserNotFound() {
        when(userRepository.findById("nonexistent")).thenReturn(Optional.empty());

        walletService.sendEmail("nonexistent", 100.0);

        verify(kafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    void transferWallet_ZeroAmount() {
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.transferWallet("user1", "user2", 0.0));
    }

    @Test
    void transferWallet_NegativeAmount() {
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.transferWallet("user1", "user2", -50.0));
    }

    @Test
    void getCombinedHistory_EmptyResults() {
        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertTrue(history.isEmpty());
    }

    @Test
    void getCombinedHistory_OnlyTransactions() {
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder()
                        .id("trans1")
                        .userId("user1")
                        .amount(100.0)
                        .timestamp(LocalDateTime.now())
                        .build(),
                Transaction.builder()
                        .id("trans2")
                        .userId("user1")
                        .amount(200.0)
                        .timestamp(LocalDateTime.now().plusHours(1))
                        .build()
        );

        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(transactions));
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(item -> item instanceof Transaction));
    }

    @Test
    void getCombinedHistory_OnlyCashbacks() {
        List<Cashback> cashbacks = Arrays.asList(
                Cashback.builder()
                        .id("cash1")
                        .userId("user1")
                        .amount(10.0)
                        .timestamp(LocalDateTime.now())
                        .build(),
                Cashback.builder()
                        .id("cash2")
                        .userId("user1")
                        .amount(20.0)
                        .timestamp(LocalDateTime.now().plusHours(1))
                        .build()
        );

        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(cashbacks));

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertEquals(2, history.size());
        assertTrue(history.stream().allMatch(item -> item instanceof Cashback));
    }

    @Test
    void getCombinedHistory_MixedResultsSorting() {
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction1 = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .amount(100.0)
                .timestamp(now.minusHours(1))
                .build();

        Transaction transaction2 = Transaction.builder()
                .id("trans2")
                .userId("user1")
                .amount(200.0)
                .timestamp(now.plusHours(1))
                .build();

        Cashback cashback1 = Cashback.builder()
                .id("cash1")
                .userId("user1")
                .amount(10.0)
                .timestamp(now)
                .build();

        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(transaction1, transaction2)));
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(cashback1)));

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertEquals(3, history.size());
        assertTrue(history.get(0) instanceof Transaction); // Most recent transaction
        assertTrue(history.get(1) instanceof Cashback);
        assertTrue(history.get(2) instanceof Transaction); // Oldest transaction
    }

    @Test
    void sendEmail_JsonProcessingException() throws Exception {
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));
        when(objectMapper.writeValueAsString(any(EmailDetails.class)))
                .thenThrow(new JsonProcessingException("Test exception") {});

        assertThrows(RuntimeException.class, () -> walletService.sendEmail("user1", 100.0));
    }

    @Test
    void addHistory_Success() {
        List<Map<String, Object>> historyEntries = Arrays.asList(
                Map.of("type", "RECHARGE", "amount", 100.0),
                Map.of("type", "TRANSFER", "amount", 50.0)
        );

        walletService.addHistory("user1", historyEntries);
        List<Object> history = walletService.getHistory("user1");

        assertEquals(2, history.size());
    }

    @Test
    void getHistory_EmptyHistory() {
        List<Object> history = walletService.getHistory("nonexistent");
        assertTrue(history.isEmpty());
    }

    @Test
    void transferWallet_SameSenderAndRecipient() {
        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet("user1", "user1", 100.0));
    }

    @Test
    void rechargeWallet_MaximumAmount() {
        double maxAmount = Double.MAX_VALUE;
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);

        Transaction expectedTransaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .walletId("wallet1")
                .type(TransactionType.RECHARGE)
                .amount(maxAmount)
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

        Transaction result = walletService.rechargeWallet("user1", maxAmount);

        assertNotNull(result);
        assertEquals(maxAmount, result.getAmount());
    }

    @Test
    void getCombinedHistory_InvalidPageNumber() {
        assertThrows(IllegalArgumentException.class,
                () -> walletService.getCombinedHistory("user1", -1, 10));
    }

    @Test
    void getCombinedHistory_InvalidPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> walletService.getCombinedHistory("user1", 0, 0));
    }

    @Test
    void addHistory_NullHistory() {
        assertThrows(NullPointerException.class,
                () -> walletService.addHistory("user1", null));
    }

    @Test
    void transferWallet_TransactionDetails() {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        when(transactionRepository.save(transactionCaptor.capture()))
                .thenReturn(Transaction.builder()
                        .id("trans1")
                        .userId("user1")
                        .senderId(null)
                        .recipientId("user2")
                        .walletId("wallet1")
                        .type(TransactionType.TRANSFER)
                        .amount(100.0)
                        .timestamp(LocalDateTime.now())
                        .build())
                .thenReturn(Transaction.builder()
                        .id("trans2")
                        .userId("user2")
                        .senderId("user1")
                        .recipientId(null)
                        .walletId("wallet2")
                        .type(TransactionType.TRANSFER)
                        .amount(100.0)
                        .timestamp(LocalDateTime.now())
                        .build());

        List<Transaction> results = walletService.transferWallet("user1", "user2", 100.0);

        assertEquals(2, results.size());

        Transaction senderTransaction = results.get(0);
        assertEquals("user1", senderTransaction.getUserId());
        assertNull(senderTransaction.getSenderId());
        assertEquals("user2", senderTransaction.getRecipientId());
        assertEquals(TransactionType.TRANSFER, senderTransaction.getType());

        Transaction recipientTransaction = results.get(1);
        assertEquals("user2", recipientTransaction.getUserId());
        assertEquals("user1", recipientTransaction.getSenderId());
        assertNull(recipientTransaction.getRecipientId());
        assertEquals(TransactionType.TRANSFER, recipientTransaction.getType());

        List<Transaction> capturedTransactions = transactionCaptor.getAllValues();
        assertEquals(2, capturedTransactions.size());

        Transaction capturedSenderTransaction = capturedTransactions.get(0);
        assertEquals("user1", capturedSenderTransaction.getUserId());

        Transaction capturedRecipientTransaction = capturedTransactions.get(1);
        assertEquals("user2", capturedRecipientTransaction.getUserId());
    }

    @Test
    void rechargeWallet_TransactionDetails() {
        double rechargeAmount = 100.0;
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));

        Transaction expectedTransaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .senderId(null)
                .recipientId("self")
                .walletId("wallet1")
                .type(TransactionType.RECHARGE)
                .amount(rechargeAmount)
                .timestamp(LocalDateTime.now())
                .build();

        when(transactionRepository.save(any(Transaction.class))).thenReturn(expectedTransaction);

        Transaction result = walletService.rechargeWallet("user1", rechargeAmount);

        assertEquals("user1", result.getUserId());
        assertNull(result.getSenderId());
        assertEquals("self", result.getRecipientId());
        assertEquals("wallet1", result.getWalletId());
        assertEquals(TransactionType.RECHARGE, result.getType());
        assertEquals(rechargeAmount, result.getAmount());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void getCombinedHistory_SortingWithEqualTimestamps() {
        LocalDateTime now = LocalDateTime.now();

        Transaction transaction = Transaction.builder()
                .id("trans1")
                .userId("user1")
                .amount(100.0)
                .timestamp(now)
                .build();

        Cashback cashback = Cashback.builder()
                .id("cash1")
                .userId("user1")
                .amount(10.0)
                .timestamp(now)
                .build();

        when(transactionRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(transaction)));
        when(cashbackRepository.findByUserId(eq("user1"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cashback)));

        List<Object> history = walletService.getCombinedHistory("user1", 0, 10);

        assertEquals(2, history.size());
        assertTrue(history.get(0) instanceof Cashback);
        assertTrue(history.get(1) instanceof Transaction);
    }

    @Test
    void getCombinedHistory_MultiplePages() {
        Pageable pageable = PageRequest.of(1, 5, Sort.by(Sort.Direction.DESC, "timestamp"));

        List<Transaction> transactions = Arrays.asList(
                Transaction.builder().userId("user1").amount(100.0).timestamp(LocalDateTime.now()).build(),
                Transaction.builder().userId("user1").amount(200.0).timestamp(LocalDateTime.now()).build()
        );

        doReturn(new PageImpl<>(transactions))
                .when(transactionRepository)
                .findByUserId(eq("user1"), any(Pageable.class));

        doReturn(new PageImpl<>(Collections.emptyList()))
                .when(cashbackRepository)
                .findByUserId(eq("user1"), any(Pageable.class));

        List<Object> history = walletService.getCombinedHistory("user1", 1, 5);

        assertEquals(2, history.size());
    }

    @Test
    void rechargeWallet_VerifyEmailSent() {
        double rechargeAmount = 100.0;
        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(testWallet);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);
        when(userRepository.findById("user1")).thenReturn(Optional.of(testUser));

        walletService.rechargeWallet("user1", rechargeAmount);

        verify(userRepository).findById("user1");
        verify(cashbackService).applyCashback("user1", rechargeAmount);
    }

    @Test
    void transferWallet_VerifyEmailContent() throws Exception {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();
        User recipient = User.builder()
                .id("user2")
                .username("recipient")
                .email("recipient@test.com")
                .build();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));
        when(userRepository.findById("user2")).thenReturn(Optional.of(recipient));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        walletService.transferWallet("user1", "user2", 100.0);

        verify(userRepository).findById("user2");
        verify(objectMapper).writeValueAsString(any(EmailDetails.class));
    }

    @Test
    void addHistory_MultipleEntries() {
        List<Map<String, Object>> historyEntries1 = Arrays.asList(
                Map.of("type", "RECHARGE", "amount", 100.0)
        );
        List<Map<String, Object>> historyEntries2 = Arrays.asList(
                Map.of("type", "TRANSFER", "amount", 50.0)
        );

        walletService.addHistory("user1", historyEntries1);
        walletService.addHistory("user1", historyEntries2);

        List<Object> history = walletService.getHistory("user1");
        assertEquals(2, history.size());
    }

    @Test
    void getHistory_MultipleUsers() {
        List<Map<String, Object>> historyUser1 = Arrays.asList(
                Map.of("type", "RECHARGE", "amount", 100.0)
        );
        List<Map<String, Object>> historyUser2 = Arrays.asList(
                Map.of("type", "TRANSFER", "amount", 50.0)
        );

        walletService.addHistory("user1", historyUser1);
        walletService.addHistory("user2", historyUser2);

        List<Object> historyForUser1 = walletService.getHistory("user1");
        List<Object> historyForUser2 = walletService.getHistory("user2");

        assertEquals(1, historyForUser1.size());
        assertEquals(1, historyForUser2.size());
    }

    @Test
    void addHistory_EmptyList() {
        walletService.addHistory("user1", Collections.emptyList());
        List<Object> history = walletService.getHistory("user1");
        assertTrue(history.isEmpty());
    }

    @Test
    void transferWallet_CompleteFlow() {
        Wallet recipientWallet = Wallet.builder()
                .id("wallet2")
                .userId("user2")
                .balance(500.0)
                .build();
        User recipient = User.builder()
                .id("user2")
                .username("recipient")
                .email("recipient@test.com")
                .build();

        double transferAmount = 100.0;
        double initialSenderBalance = testWallet.getBalance();
        double initialRecipientBalance = recipientWallet.getBalance();

        when(walletRepository.findByUserId("user1")).thenReturn(Optional.of(testWallet));
        when(walletRepository.findByUserId("user2")).thenReturn(Optional.of(recipientWallet));
        when(userRepository.findById("user2")).thenReturn(Optional.of(recipient));
        when(transactionRepository.save(any(Transaction.class))).thenReturn(testTransaction);

        List<Transaction> results = walletService.transferWallet("user1", "user2", transferAmount);

        assertEquals(initialSenderBalance - transferAmount, testWallet.getBalance());
        assertEquals(initialRecipientBalance + transferAmount, recipientWallet.getBalance());
        assertEquals(2, results.size());
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(transactionRepository, times(2)).save(any(Transaction.class));
        verify(userRepository).findById("user2");
    }
}