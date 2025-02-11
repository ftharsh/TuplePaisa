package org.harsh.tuple.paisa.repository;

import org.harsh.tuple.paisa.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends MongoRepository<Transaction, String> {
    Page<Transaction> findByUserId(String userId, Pageable pageable);
    List<Transaction> findByUserIdAndTimestampBetween(String userId, LocalDateTime startDate, LocalDateTime endDate);

}
