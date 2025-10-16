package com.tolerant.notification_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.notification_service.service.NotificationProcessingService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chaos")
@RequiredArgsConstructor
@Slf4j
public class ChaosController {
    
    private final NotificationProcessingService notificationProcessingService;

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

        notificationProcessingService.updateChaosConfig(failureRate, delayMs);

        Map<String, Object> response = new HashMap<>();
        response.put("failureRate", failureRate);
        response.put("delayMs", delayMs);
        response.put("message", "Chaos configuration updated successfully");

        log.warn("Chaos configuration udpated. Failre Rate: {}%, Delay: {}ms", failureRate, delayMs);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/config")
    public ResponseEntity<NotificationProcessingService.ChaosConfig> getChaosConfig() {
        NotificationProcessingService.ChaosConfig config = notificationProcessingService.getChaosConfig();
        return new ResponseEntity<>(config, HttpStatus.OK);
    } 

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        notificationProcessingService.updateChaosConfig(0.0, 0);
        log.info("Chaos configuration reset");
        return ResponseEntity.ok(Map.of(
            "message", "Chaos configuration reset",
            "failureRate", 0.0,
            "delayMs", 0
        ));
    }
}
