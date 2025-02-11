package org.harsh.tuple.paisa.controller;

import lombok.RequiredArgsConstructor;
import org.harsh.tuple.paisa.dto.AnalyticsRequestDto;
import org.harsh.tuple.paisa.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/charts")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @PostMapping("/chartsHistory")
    public ResponseEntity<List<Object>> getCombinedHistory(@RequestBody AnalyticsRequestDto request){
        String userId = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        LocalDateTime startDate = request.getStartDate();
        LocalDateTime endDate = request.getEndDate();
        List<Object> history = analyticsService.getCombinedHistory(userId,startDate,endDate);
        return ResponseEntity.ok(history);
    }
}
