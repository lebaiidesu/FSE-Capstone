package com.bank.analytics.controller;

import com.bank.analytics.service.AnalyticsConsumer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsConsumer analyticsConsumer;

    public AnalyticsController(AnalyticsConsumer analyticsConsumer) {
        this.analyticsConsumer = analyticsConsumer;
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getSummary() {
        return ResponseEntity.ok(analyticsConsumer.getMetricsSummary());
    }
}
