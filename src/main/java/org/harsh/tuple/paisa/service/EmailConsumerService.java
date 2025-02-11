package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.harsh.tuple.paisa.dto.EmailDetails;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailConsumerService {
    public final EmailService emailService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "email-notifications", groupId = "email-service")
    public void listenEmailNotifications(String message) {

        try {
            EmailDetails email = objectMapper.readValue(message, EmailDetails.class);
            log.info("Message received: {}", email);
            emailService.sendEmail(email.getEmail(), email.getSubject(), email.getBody());
        } catch (Exception e) {
            log.error("Error while consuming email message: ", e);
        }
    }
}
