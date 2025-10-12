package com.tolerant.order_service.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/chaos")
@Slf4j
public class ChaosController {
    
    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;
}
