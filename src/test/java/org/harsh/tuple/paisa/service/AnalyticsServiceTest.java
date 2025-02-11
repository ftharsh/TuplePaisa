package org.harsh.tuple.paisa.service;

import org.harsh.tuple.paisa.model.Cashback;
import org.harsh.tuple.paisa.model.Transaction;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.harsh.tuple.paisa.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private CashbackRepository cashbackRepository;

    @InjectMocks
    private AnalyticsService analyticsService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testGetCombinedHistory_HappyPath() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
                .timestamp(LocalDateTime.of(2024, 6, 1, 11, 0)).build();

        Transaction transaction2 = Transaction.builder()
                .timestamp(LocalDateTime.of(2024, 7, 1, 12, 0)).build();

        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2024, 6, 1, 11, 0)).build();

        Cashback cashback2 = Cashback.builder()
                .timestamp(LocalDateTime.of(2024, 8, 1, 13, 0)).build();



        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(transaction1, transaction2));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(cashback1, cashback2));

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(4, result.size());
        assertEquals(cashback2, result.get(0)); // Latest timestamp
        assertEquals(transaction2, result.get(1));
        assertEquals(cashback1, result.get(2));
        assertEquals(transaction1, result.get(3)); // Earliest timestamp
    }

    @Test
    void testGetCombinedHistory_EmptyTransactions() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 6, 1, 11, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(cashback1));

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(cashback1, result.get(0));
    }

    @Test
    void testGetCombinedHistory_EmptyCashbacks() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
        .timestamp(LocalDateTime.of(2023, 5, 1, 10, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(transaction1));
        when(cashbackRepository.findByUserIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(1, result.size());
        assertEquals(transaction1, result.get(0));
    }

    @Test
    void testGetCombinedHistory_BothEmpty() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        when(transactionRepository.findByUserIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(cashbackRepository.findByUserIdAndTimestampBetween(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(0, result.size());
    }

    @Test
    void testGetCombinedHistory_SortingLogic() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 1, 1, 10, 0)).build();
        Transaction transaction2 = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 3, 1, 12, 0)).build();

        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 2, 1, 11, 0)).build();
        Cashback cashback2 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 4, 1, 13, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(transaction1, transaction2));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(cashback1, cashback2));

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(4, result.size());
        assertEquals(cashback2, result.get(0)); // Latest timestamp
        assertEquals(transaction2, result.get(1));
        assertEquals(cashback1, result.get(2));
        assertEquals(transaction1, result.get(3)); // Earliest timestamp
    }

    @Test
    void testGetCombinedHistory_SortingLogic_TransactionFirst() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
        .timestamp(LocalDateTime.of(2023, 1, 1, 10, 0)).build();
        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 2, 1, 11, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(transaction1));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(cashback1));

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        assertEquals(cashback1, result.get(0)); // Latest timestamp
        assertEquals(transaction1, result.get(1)); // Earliest timestamp
    }

    @Test
    void testGetCombinedHistory_SortingLogic_CashbackFirst() {
        // Arrange
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 2, 1, 10, 0)).build();
        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 1, 1, 11, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(transaction1));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Collections.singletonList(cashback1));

        // Act
        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        // Assert
        assertEquals(2, result.size());
        assertEquals(transaction1, result.get(0)); // Latest timestamp
        assertEquals(cashback1, result.get(1)); // Earliest timestamp
    }

    @Test
    void testSortingLogic_CashbackBeforeTransaction() {
        Transaction transaction = Transaction.builder()
                .timestamp(LocalDateTime.now()).build();

        Cashback cashback = Cashback.builder()
                .timestamp(transaction.getTimestamp()).build();

        List<Object> combinedList = new ArrayList<>();
        combinedList.add(transaction);
        combinedList.add(cashback);

        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();

            int timeComparison = time2.compareTo(time1);
            if (timeComparison != 0) {
                return timeComparison;
            }

            if (a instanceof Cashback && b instanceof Transaction) {
                return -1;
            } else if (a instanceof Transaction && b instanceof Cashback) {
                return 1;
            }
            return 0;
        });

        assertEquals(cashback, combinedList.get(0));
        assertEquals(transaction, combinedList.get(1));
    }

    @Test
    void testSortingLogic_CashbackBeforeTransactionDone() {
        Transaction transaction = Transaction.builder()
                .timestamp(LocalDateTime.now()).build();

        Cashback cashback = Cashback.builder()
                .timestamp(LocalDateTime.now()).build();

        List<Object> combinedList = new ArrayList<>();
        combinedList.add(transaction);
        combinedList.add(cashback);

        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();

            int timeComparison = time2.compareTo(time1);
            if (timeComparison != 0) {
                return timeComparison;
            }

            if (a instanceof Cashback && b instanceof Transaction) {
                return -1;
            } else if (a instanceof Transaction && b instanceof Cashback) {
                return 1;
            }

            return 0;
        });

        assertEquals(cashback, combinedList.get(0));
        assertEquals(transaction, combinedList.get(1));

        System.out.println("combinedList: " + combinedList);
    }

    @Test
    void testSortingLogic_CashbackBeforeTransactionSecond() {
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        Transaction transaction1 = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 2, 1, 10, 0)).build();

        Cashback cashback1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2023, 1, 1, 11, 0)).build();

        Transaction transaction2 = Transaction.builder()
                .timestamp(LocalDateTime.of(2023, 1, 1, 11, 0)).build();

        List<Object> combinedList = new ArrayList<>();
        combinedList.add(transaction1);
        combinedList.add(cashback1);
        combinedList.add(transaction2);

        combinedList.sort((a, b) -> {
            LocalDateTime time1 = (a instanceof Transaction) ?
                    ((Transaction) a).getTimestamp() : ((Cashback) a).getTimestamp();
            LocalDateTime time2 = (b instanceof Transaction) ?
                    ((Transaction) b).getTimestamp() : ((Cashback) b).getTimestamp();

            int timeComparison = time2.compareTo(time1);
            if (timeComparison != 0) {
                return timeComparison;
            }

            if (a instanceof Cashback && b instanceof Transaction) {
                return -1;
            } else if (a instanceof Transaction && b instanceof Cashback) {
                return 1;
            }

            return 0;
        });

        assertEquals(transaction1, combinedList.get(0));
        assertEquals(cashback1, combinedList.get(1));
        assertEquals(transaction2, combinedList.get(2));

        System.out.println("combinedList: " + combinedList);
    }

    @Test
    void testGetCombinedHistory_MultipleItemsSameTimestamp() {
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);
        LocalDateTime sameTimestamp = LocalDateTime.of(2024, 6, 1, 12, 0);

        Transaction t1 = Transaction.builder().timestamp(sameTimestamp).build();
        Transaction t2 = Transaction.builder().timestamp(sameTimestamp).build();
        Cashback c1 = Cashback.builder().timestamp(sameTimestamp).build();
        Cashback c2 = Cashback.builder().timestamp(sameTimestamp).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(t1, t2));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(c1, c2));

        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        assertEquals(4, result.size());
        assertEquals(c1, result.get(0));
        assertEquals(c2, result.get(1));
        assertEquals(t1, result.get(2));
        assertEquals(t2, result.get(3));
    }

    @Test
    void testGetCombinedHistory_InterleavedTimestamps() {
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        Transaction t1 = Transaction.builder()
                .timestamp(LocalDateTime.of(2024, 1, 1, 12, 0)).build();
        Transaction t2 = Transaction.builder()
                .timestamp(LocalDateTime.of(2024, 1, 1, 14, 0)).build();
        Cashback c1 = Cashback.builder()
                .timestamp(LocalDateTime.of(2024, 1, 1, 13, 0)).build();
        Cashback c2 = Cashback.builder()
                .timestamp(LocalDateTime.of(2024, 1, 1, 15, 0)).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(t1, t2));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(c1, c2));

        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        assertEquals(4, result.size());
        assertEquals(c2, result.get(0));
        assertEquals(t2, result.get(1));
        assertEquals(c1, result.get(2));
        assertEquals(t1, result.get(3));
    }

    @Test
    void testGetCombinedHistory_ExactBoundaryDates() {
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        Transaction t1 = Transaction.builder()
                .timestamp(startDate).build();
        Transaction t2 = Transaction.builder()
                .timestamp(endDate).build();
        Cashback c1 = Cashback.builder()
                .timestamp(startDate).build();
        Cashback c2 = Cashback.builder()
                .timestamp(endDate).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(t1, t2));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(Arrays.asList(c1, c2));

        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        assertEquals(4, result.size());
        assertEquals(c2, result.get(0));
        assertEquals(t2, result.get(1));
        assertEquals(c1, result.get(2));
        assertEquals(t1, result.get(3));
    }

    @Test
    void testGetCombinedHistory_SingleMillisecondDifference() {
        String userId = "user1";
        LocalDateTime startDate = LocalDateTime.of(2024, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2024, 12, 31, 23, 59);

        LocalDateTime time1 = LocalDateTime.of(2024, 1, 1, 12, 0, 0);
        LocalDateTime time2 = LocalDateTime.of(2024, 1, 1, 12, 0, 0, 1);

        Transaction t1 = Transaction.builder().timestamp(time1).build();
        Cashback c1 = Cashback.builder().timestamp(time2).build();

        when(transactionRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(List.of(t1));
        when(cashbackRepository.findByUserIdAndTimestampBetween(eq(userId), eq(startDate), eq(endDate)))
                .thenReturn(List.of(c1));

        List<Object> result = analyticsService.getCombinedHistory(userId, startDate, endDate);

        assertEquals(2, result.size());
        assertEquals(c1, result.get(0));
        assertEquals(t1, result.get(1));
    }
}