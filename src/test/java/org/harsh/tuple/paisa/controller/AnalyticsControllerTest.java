package org.harsh.tuple.paisa.controller;

import org.harsh.tuple.paisa.dto.AnalyticsRequestDto;
import org.harsh.tuple.paisa.service.AnalyticsService;
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
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AnalyticsControllerTest {

    @InjectMocks
    private AnalyticsController analyticsController;

    @Mock
    private AnalyticsService analyticsService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testGetCombinedHistory_HappyPath() {
        // Arrange
        String userId = "testUser";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .startDate(startDate)
                .endDate(endDate).build();

        List<Object> mockResponse = Arrays.asList("data1", "data2");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(analyticsService.getCombinedHistory(userId, startDate, endDate)).thenReturn(mockResponse);

        // Act
        ResponseEntity<List<Object>> response = analyticsController.getCombinedHistory(request);

        // Assert
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockResponse, response.getBody());

        verify(analyticsService, times(1)).getCombinedHistory(userId, startDate, endDate);
    }

    @Test
    void testGetCombinedHistory_ServiceThrowsException() {
        // Arrange
        String userId = "testUser";
        LocalDateTime startDate = LocalDateTime.of(2023, 1, 1, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(2023, 12, 31, 23, 59);

        AnalyticsRequestDto request = AnalyticsRequestDto.builder()
                .startDate(startDate)
                .endDate(endDate).build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userId);
        when(analyticsService.getCombinedHistory(userId, startDate, endDate)).thenThrow(new RuntimeException("Service Error"));

        // Act & Assert
        assertThrows(RuntimeException.class, () -> analyticsController.getCombinedHistory(request));
    }
}