package org.harsh.tuple.paisa.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.harsh.tuple.paisa.dto.EmailDetails;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EmailConsumerServiceTest {

    @Mock
    private EmailService emailService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private EmailConsumerService emailConsumerService;

    private String message;
    private EmailDetails emailDetails;

    @BeforeEach
    void setUp() {
        message = "{\"email\":\"test@example.com\",\"subject\":\"Test Subject\",\"body\":\"Test Body\"}";
        emailDetails = new EmailDetails("test@example.com", "Test Subject", "Test Body");
    }

    @Test
    void testListenEmailNotifications() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(EmailDetails.class))).thenReturn(emailDetails);

        emailConsumerService.listenEmailNotifications(message);

        verify(objectMapper, times(1)).readValue(any(String.class), eq(EmailDetails.class));
        verify(emailService, times(1)).sendEmail(emailDetails.getEmail(), emailDetails.getSubject(), emailDetails.getBody());
    }

    @Test
    void testListenEmailNotifications_Exception() throws Exception {
        when(objectMapper.readValue(any(String.class), eq(EmailDetails.class))).thenThrow(new RuntimeException("Error"));

        emailConsumerService.listenEmailNotifications(message);

        verify(objectMapper, times(1)).readValue(any(String.class), eq(EmailDetails.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
    }
}