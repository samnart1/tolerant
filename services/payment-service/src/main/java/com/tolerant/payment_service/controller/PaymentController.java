package com.tolerant.payment_service.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tolerant.payment_service.model.Payment;
import com.tolerant.payment_service.model.PaymentRequest;
import com.tolerant.payment_service.model.PaymentResponse;
import com.tolerant.payment_service.service.PaymentService;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {
    
    private final PaymentService paymentService;

    @PostMapping("/process")
    @Timed(value = "payment.process.api", description = "Time taken for payment API call")
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody PaymentRequest request) {
        log.info("Received payment request for order: {}", request.getOrderId());
        PaymentResponse response = paymentService.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @Timed(value = "payment.get", description = "Time taken to get payment")
    public ResponseEntity<Payment> getPayment(@PathVariable Long id) {
        Payment payment = paymentService.getPayment(id);
        return ResponseEntity.ok(payment);
    }

    @GetMapping("/payment-id/{paymentId}")
    @Timed(value = "payment.getByPaymentId", description = "Time taken to get payment by ID")
    public ResponseEntity<Payment> getPaymentByPaymentId(@RequestParam String paymentId) {
        Payment payment = paymentService.getPaymentByPaymentId(paymentId);
        return new ResponseEntity<>(payment, HttpStatus.OK);
    }

    @GetMapping("/order/{orderId}")
    @Timed(value = "payment.getByOrderId", description = "Time taken to get payment by order")
    public ResponseEntity<Payment> getPaymentByOrderId(@PathVariable String orderId) {
        Payment payment = paymentService.getPaymentByOrderId(orderId);
        return new ResponseEntity<>(payment, HttpStatus.OK);
    }

    @GetMapping
    @Timed(value = "payment.getAll", description = "Time taken to get all payments")
    public ResponseEntity<List<Payment>> getAllPayments() {
        List<Payment> payments = paymentService.getAllPayments();
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    @GetMapping("/customer/{customerId}")
    @Timed(value = "payment.getByCustomer", description = "Time taken to get payments by customer")
    public ResponseEntity<List<Payment>> getPaymentsByCustomer(@PathVariable String customerId) {
        List<Payment> payments = paymentService.getPaymentsByCustomer(customerId);
        return new ResponseEntity<>(payments, HttpStatus.OK);
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Payment Service is healthy");
    }
    
}
