package org.harsh.tuple.paisa.repository;

import org.harsh.tuple.paisa.model.Cashback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface CashbackRepository extends MongoRepository<Cashback, String> {
    List<Cashback> findByUserId(String userId);
    Page<Cashback> findByUserId(String userId, Pageable pageable);
    List<Cashback> findByUserIdAndTimestampBetween(String userId, LocalDateTime startDate , LocalDateTime endDate);
}
