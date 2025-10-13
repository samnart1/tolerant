package com.tolerant.order_service.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.service.annotation.GetExchange;

// import com.rabbitmq.client.RpcClient.Response;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chaos")
@Slf4j
public class ChaosController {
    
    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;

    private double failureRate = 0.0;
    private long delayMs = 0;

    public ResponseEntity<Map<String, Object>> configureChaos(
        @RequestParam(required = false, defaultValue = "0.0") double failureRate,
        @RequestParam(required = false, defaultValue = "0") long delayMs
    ) {
        if (!chaosEnabled) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chaos engineering is not enabled"));
        }

        this.failureRate = Math.min(Math.max(failureRate, 0), 1.0);
        this.delayMs = Math.max(delayMs, 0);

        log.warn("Chaos configuration updated. Failure Rate: {}%, Delay: {}ms", failureRate * 100, delayMs);

        Map<String, Object> response = new HashMap<>();
        response.put("failureRate", this.failureRate);
        response.put("delayRate", this.delayMs);
        response.put("message", "Chaos configuration updated successfully");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetExchange("/config")
    public ResponseEntity<Map<String, Object>> getChaosConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("enabled", chaosEnabled);
        config.put("failureRate", failureRate);
        config.put("delayMs", delayMs);
        return new ResponseEntity<>(config, HttpStatus.OK);
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        this.failureRate = 0.0;
        this.delayMs = 0;

        log.info("Chaos configuration reset!");

        return new ResponseEntity<>(Map.of("message", "Chaos configuration reset", "failureRate", failureRate, "delayMs", delayMs), HttpStatus.OK);
    }


}
