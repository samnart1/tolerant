package com.tolerant.payment_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.payment_service.service.PaymentProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chaos")
@RequiredArgsConstructor
@Slf4j
public class ChaosController {
    
    private final PaymentProcessingService paymentProcessingService;

    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;

    @PostMapping("/config")
    public ResponseEntity<Map<String, Object>> configureChaos(
        @RequestParam(required = false, defaultValue = "0.0") double failureRate,
        @RequestParam(required = false, defaultValue = "0") long delayMs
    ) {
        if (!chaosEnabled) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Chaos engineering is not enabled"));
        }

        paymentProcessingService.updateChaosConfig(failureRate, delayMs);

        Map<String, Object> response = new HashMap<>();
        response.put("failureRate", failureRate);
        response.put("delayMs", delayMs);
        response.put("message", "Chaos configuration updated successfully");

        log.warn("Chaos configuration updated. Failrue Rate: {}%, Delay: {}ms", failureRate * 100, delayMs);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/config")
    public ResponseEntity<PaymentProcessingService.ChaosConfig> getChaosConfig() {
        PaymentProcessingService.ChaosConfig config = paymentProcessingService.getChaosConfig();
        return ResponseEntity.ok(config);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        paymentProcessingService.updateChaosConfig(0.0, 0);
        log.info("Chaos configuration reset");
        return ResponseEntity.ok(Map.of(
            "message", "Chaos configuration reset",
            "failureRate", 0.0,
            "delayMs", 0
        ));
    }
}
