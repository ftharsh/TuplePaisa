package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HistoryConsumerService {
    private final ObjectMapper objectMapper;
    private final WalletService walletService;

    @KafkaListener(topics = "wallet-history", groupId = "wallet-history")
    public void consumeHistory(String message) {
        try {
            log.info("Received history message:{}", message);
            List<Map<String, Object>> transactions = objectMapper.readValue(message, new TypeReference<List<Map<String, Object>>>() {
            });
            log.debug("Payload structure: {}", transactions);
            if (!transactions.isEmpty()) {
                String userId = (String) transactions.getFirst().get("userId");
                walletService.addHistory(userId, transactions);
            }

        } catch (Exception e) {
            log.error("Error while consuming History message:", e);
        }
    }


}
