package com.tolerant.inventory_service.controller;

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

import com.tolerant.inventory_service.service.InventoryManagementService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chaos")
@Slf4j
@RequiredArgsConstructor
public class ChaosController {
    
    private final InventoryManagementService inventoryManagementService;

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

        inventoryManagementService.updateChaosConfig(failureRate, delayMs);

        Map<String, Object> response = new HashMap<>();
        response.put("failureRate", failureRate);
        response.put("delayMs", delayMs);
        response.put("message", "Chaos configuration updated successfully");

        log.warn("Chaos configuration updated. Failure Rate: {}%, Delay: {}ms", failureRate * 100, delayMs);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/config")
    public ResponseEntity<InventoryManagementService.ChaosConfig> getChaosConfig() {
        InventoryManagementService.ChaosConfig config = inventoryManagementService.getChaosConfig();
        return ResponseEntity.ok(config);
    }

    @PostMapping("resert")
    public ResponseEntity<Map<String, Object>> resetChaos() {
        inventoryManagementService.updateChaosConfig(0.0, 0);

        log.info("Chaos configuration resert");

        return ResponseEntity.ok(Map.of(
            "message", "Chaos configuration reset",
            "failure rate", 0.0,
            "delayMs", 0
        ));
    }
}
