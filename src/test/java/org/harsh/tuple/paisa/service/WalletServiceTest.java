package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.harsh.tuple.paisa.model.*;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.harsh.tuple.paisa.exception.InsufficientBalanceException;
import org.harsh.tuple.paisa.exception.InvalidTransactionAmountException;
import org.harsh.tuple.paisa.exception.WalletNotFoundException;
import org.harsh.tuple.paisa.repository.TransactionRepository;
import org.harsh.tuple.paisa.repository.UserRepository;
import org.harsh.tuple.paisa.repository.WalletRepository;
import org.springframework.data.domain.*;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


public class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CashbackService cashbackService;

    @Mock
    private EmailService emailService;

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

    private static Wallet senderWallet;
    private static Wallet recipientWallet;
    private static User recipientUser;
    private final int page = 0;
    private final int size = 10;

    @BeforeAll
    static void setUpAll() {
        senderWallet = Wallet.builder()
                .id("sender-wallet-id")
                .userId("sender-user-id")
                .balance(1000.0)
                .build();

        recipientWallet = Wallet.builder()
                .id("recipient-wallet-id")
                .userId("recipient-user-id")
                .balance(500.0)
                .build();

        recipientUser = User.builder()
                .id("recipient-user-id")
                .username("recipient-username")
                .email("recipient@example.com")
                .build();


    }
    @AfterEach
    void setUpEach() {
        senderWallet.setBalance(1000.0);
    }
    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    }

    @Test
    public void testRechargeWallet_Success() {
        // Arrange
       double amount = 1000.0;

        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.save(any(Wallet.class))).thenReturn(senderWallet);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Transaction result = walletService.rechargeWallet(senderWallet.getUserId(),amount);

        assertEquals(2000.0, senderWallet.getBalance() , 000.1);
        assertEquals(TransactionType.RECHARGE, result.getType());
        assertEquals(amount, result.getAmount());

        verify(walletRepository).save(senderWallet);
        verify(transactionRepository).save(any(Transaction.class));
        verify(cashbackService).applyCashback(senderWallet.getUserId(), amount);
    }

    @Test
    public void testRechargeWallet_NegativeAmount() {
        // Arrange
        String userId = "user-id";
        double amount = -500.0;

        // Act & Assert
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.rechargeWallet(userId, amount));
    }

    @Test
    public void testRechargeWallet_WalletNotFound() {
        // Arrange
        String userId = "user-id";
        double amount = 500.0;

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class,
                () -> walletService.rechargeWallet(userId, amount));
    }

    @Test
    public void testTransferWallet_InsufficientBalance() {
        // Arrange
        double amount = 2000.0;

        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(recipientWallet.getUserId())).thenReturn(Optional.of(recipientWallet));

        // Act & Assert
        assertThrows(InsufficientBalanceException.class,
                () -> walletService.transferWallet(senderWallet.getUserId(), recipientWallet.getUserId(), amount));
    }
    @Test
    void transferWallet_Success() throws Exception {
        // Given
        double amount = 100.0;
        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(recipientWallet.getUserId())).thenReturn(Optional.of(recipientWallet));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> i.getArguments()[0]);
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(i -> i.getArguments()[0]);
        when(userRepository.findById(recipientUser.getId())).thenReturn(Optional.of(recipientUser));

        // Mock ObjectMapper for email details serialization
        when(objectMapper.writeValueAsString(any(EmailDetails.class))).thenReturn("{\"email\":\"test@example.com\"}");

        // When
        List<Transaction> results = walletService.transferWallet(senderWallet.getUserId(), recipientWallet.getUserId(), amount);

        // Then
        assertEquals(2, results.size());
        assertEquals(TransactionType.TRANSFER, results.get(0).getType());
        assertEquals(amount, results.get(0).getAmount());

        // Verify wallet updates
        verify(walletRepository, times(2)).save(any(Wallet.class));

        // Verify email was sent via Kafka
        verify(kafkaTemplate).send(eq("email-notifications"), any(String.class));

        // Verify balances were updated correctly
        assertEquals(900.0, senderWallet.getBalance()); // 1000 - 100
        assertEquals(600.0, recipientWallet.getBalance()); // 500 + 100
    }

    @Test
    void getCombinedHistory_ShouldReturnSortedCombinedList() {
        // Mock data
         String userId = "user123";
        LocalDateTime now = LocalDateTime.now();
        Transaction transaction = Transaction.builder()
                .timestamp(now.minusHours(1))
                .build();
        Cashback cashback = Cashback.builder()
                .timestamp(now)
                .build();

        Page<Transaction> transactionPage = new PageImpl<>(List.of(transaction));
        Page<Cashback> cashbackPage = new PageImpl<>(List.of(cashback));

        // Mock repository calls
        when(transactionRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(transactionPage);
        when(cashbackRepository.findByUserId(eq(userId), any(Pageable.class)))
                .thenReturn(cashbackPage);

        // Test
        List<Object> result = walletService.getCombinedHistory(userId, page, size);

        // Assertions
        assertEquals(2, result.size());
        assertTrue(result.get(0) instanceof Cashback);
        assertTrue(result.get(1) instanceof Transaction);

        // Verify pagination
        Pageable expectedPageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));
        verify(transactionRepository).findByUserId(userId, expectedPageable);
        verify(cashbackRepository).findByUserId(userId, expectedPageable);
    }
    @Test
    @DisplayName("Should throw InvalidTransactionAmountException when recharge amount is zero")
    void rechargeWallet_ZeroAmount_ThrowsInvalidTransactionAmountException() {
        String userId = "user-id";
        double amount = 0;
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.rechargeWallet(userId, amount));
    }

    @Test
    @DisplayName("Should throw InvalidTransactionAmountException when recharge amount is negative")
    void rechargeWallet_NegativeAmount_ThrowsInvalidTransactionAmountException() {
        String userId = "user-id";
        double amount = -100;
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.rechargeWallet(userId, amount));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when wallet doesn't exist during recharge")
    void rechargeWallet_WalletNotFound_ThrowsWalletNotFoundException() {
        String userId = "user-id";
        double amount = 100;
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class,
                () -> walletService.rechargeWallet(userId, amount));
    }

    @Test
    @DisplayName("Should throw InvalidTransactionAmountException when transfer amount is zero")
    void transferWallet_ZeroAmount_ThrowsInvalidTransactionAmountException() {
        String userId = "user-id123";
        String recipientId = "user-id256";
        double amount = 0;
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.transferWallet(userId, recipientId, amount));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when sender wallet doesn't exist")
    void transferWallet_SenderWalletNotFound_ThrowsWalletNotFoundException() {
        String userId = "user-id123";
        String recipientId = "user-id256";
        double amount = 100;
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet(userId, recipientId, amount));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when recipient wallet doesn't exist")
    void transferWallet_RecipientWalletNotFound_ThrowsWalletNotFoundException() {
        String userId = "user-id123";
        String recipientId = "user-id256";
        double amount = 100;
        senderWallet.setBalance(200);
        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(recipientWallet.getUserId())).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet(senderWallet.getUserId(), recipientWallet.getUserId(), amount));
    }

    @Test
    @DisplayName("Should throw InsufficientBalanceException when sender has insufficient funds")
    void transferWallet_InsufficientBalance_ThrowsInsufficientBalanceException() {
        double amount = 100;
        senderWallet.setBalance(50);
        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(recipientWallet.getUserId())).thenReturn(Optional.of(recipientWallet));
        assertThrows(InsufficientBalanceException.class,
                () -> walletService.transferWallet(senderWallet.getUserId(), recipientWallet.getUserId(), amount));
    }

    @Test
    @DisplayName("Should throw WalletNotFoundException when getting balance for non-existent wallet")
    void getBalance_WalletNotFound_ThrowsWalletNotFoundException() {
        when(walletRepository.findByUserId(senderWallet.getUserId())).thenReturn(Optional.empty());
        assertThrows(WalletNotFoundException.class,
                () -> walletService.getBalance(senderWallet.getUserId()));
    }


    @Test
    public void testTransferWallet_NegativeAmount() {
        // Arrange
        String senderId = "sender-user-id";
        String recipientId = "recipient-user-id";
        double amount = -200.0;

        // Act & Assert
        assertThrows(InvalidTransactionAmountException.class,
                () -> walletService.transferWallet(senderId, recipientId, amount));
    }

    @Test
    void getCombinedHistory_EmptyResults_ShouldReturnEmptyList() {
        String userId = "user123";
        when(transactionRepository.findByUserId(eq(userId), any()))
                .thenReturn(Page.empty());
        when(cashbackRepository.findByUserId(eq(userId), any()))
                .thenReturn(Page.empty());

        List<Object> result = walletService.getCombinedHistory(userId, page, size);

        assertTrue(result.isEmpty());
    }
    @Test
    void addHistory_ShouldStoreInUserHistoryMap() {
        String userId = "user123";
        List<Map<String, Object>> history = List.of(
                Map.of("type", "transaction", "amount", 100),
                Map.of("type", "cashback", "amount", 10)
        );

        walletService.addHistory(userId, history);

        List<Object> storedHistory = walletService.getHistory(userId);
        assertEquals(2, storedHistory.size());
        assertTrue(storedHistory.containsAll(history));
    }

    @Test
    void addHistory_MultipleCalls_ShouldAggregateData() {
        String userId = "user123";
        List<Map<String, Object>> firstBatch = List.of(Map.of("id", 1));
        List<Map<String, Object>> secondBatch = List.of(Map.of("id", 2));

        walletService.addHistory(userId, firstBatch);
        walletService.addHistory(userId, secondBatch);

        List<Object> result = walletService.getHistory(userId);
        assertEquals(2, result.size());
    }

    @Test
    void getHistory_NoData_ShouldReturnEmptyList() {
        List<Object> result = walletService.getHistory("nonExistingUser");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTransferWallet_SenderWalletNotFound() {
        // Arrange
        String senderId = "sender-user-id";
        String recipientId = "recipient-user-id";
        double amount = 200.0;

        when(walletRepository.findByUserId(senderId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet(senderId, recipientId, amount));
    }

    @Test
    public void testTransferWallet_RecipientWalletNotFound() {
        // Arrange
        String senderId = "sender-user-id";
        String recipientId = "recipient-user-id";
        double amount = 200.0;

        when(walletRepository.findByUserId(senderId)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByUserId(recipientId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class,
                () -> walletService.transferWallet(senderId, recipientId, amount));
    }

    @Test
    public void testGetBalance_Success() {
        // Arrange
        String userId = "user-id";
        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(500.0)
                .build();

        when(walletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

        // Act
        double balance = walletService.getBalance(userId);

        // Assert
        assertEquals(500.0, balance, 0.001);
    }

    @Test
    public void testGetBalance_WalletNotFound() {
        // Arrange
        String userId = "user-id";
        when(walletRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(WalletNotFoundException.class,
                () -> walletService.getBalance(userId));
    }
    @Test
    void getCombinedHistory_Success() throws Exception {
        // Given
        List<Transaction> transactions = Arrays.asList(
                Transaction.builder().timestamp(LocalDateTime.now()).build()
        );
        List<Cashback> cashbacks = Arrays.asList(
                Cashback.builder().timestamp(LocalDateTime.now()).build()
        );
        when(transactionRepository.findByUserId(recipientUser.getId())).thenReturn(transactions);
        when(cashbackRepository.findByUserId(recipientUser.getId())).thenReturn(cashbacks);
        when(objectMapper.writeValueAsString(any())).thenReturn("{}");

        // When
        walletService.getCombinedHistory(recipientUser.getId());

        // Then
        verify(kafkaTemplate).send(eq("wallet-history"), any(String.class));
    }



    @Test
    void sendEmail_Success() throws Exception {
        // Given
        double amount = 100.0;
        when(userRepository.findById(recipientUser.getId())).thenReturn(Optional.of(recipientUser));
        when(objectMapper.writeValueAsString(any(EmailDetails.class))).thenReturn("{}");

        // When
        walletService.sendEmail(recipientUser.getId(), amount);

        // Then
        verify(kafkaTemplate).send(eq("email-notifications"), any(String.class));
    }

}
