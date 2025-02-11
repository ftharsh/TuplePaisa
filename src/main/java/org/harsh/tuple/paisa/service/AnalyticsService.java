package org.harsh.tuple.paisa.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.model.Cashback;
import org.harsh.tuple.paisa.model.Transaction;
import org.harsh.tuple.paisa.repository.CashbackRepository;
import org.harsh.tuple.paisa.repository.TransactionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TransactionRepository transactionRepository;
    private final CashbackRepository cashbackRepository;


    public List<Object> getCombinedHistory(String userId ,LocalDateTime startDate ,LocalDateTime endDate) {

        List<Object> combinedList = new ArrayList<>();
        List<Transaction> transactions = transactionRepository.findByUserIdAndTimestampBetween(userId , startDate ,endDate );
        List<Cashback> cashbacks = cashbackRepository.findByUserIdAndTimestampBetween(userId , startDate ,endDate );

        combinedList.addAll(transactions);
        log.info("transactions: {}", transactions);
        combinedList.addAll(cashbacks);
        log.info("cashbacks: {}", cashbacks);


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
                return -1; // Cashback comes before Transaction
            } else if (a instanceof Transaction && b instanceof Cashback) {
                return 1;
            }

            return 0;
        });
        log.info("combinedList: {}", combinedList);
        return combinedList;
    }

}
