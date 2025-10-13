package com.tolerant.payment_service.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.tolerant.payment_service.exception.PaymentException;
import com.tolerant.payment_service.model.Payment;
import com.tolerant.payment_service.model.PaymentRequest;
import com.tolerant.payment_service.model.PaymentResponse;
import com.tolerant.payment_service.model.PaymentStatus;
import com.tolerant.payment_service.repo.PaymentRepository;

import io.micrometer.core.annotation.Timed;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService implements PaymentService {

    private final PaymentRepository paymentRepo;
    private final Random random = new Random();

    @Value("${chaos.enabled:false}")
    private boolean chaosEnabled;

    @Value("${chaos.failure-rate:0.0}")
    private double chaosFailureRate;

    @Value("${chaos.delay-ms:0}")
    private long chaosDelayMs;

    @Value("${payment.processing.base-delay-ms:100}")
    private long baseProcessingDelayMs;

    @Value("${payment.processing.random-delay-ms:200}")
    private long randomProcessingDelayMs;

    @Override
    @Transactional
    @Timed(value = "payment.process", description = "Time taken to process payment")
    public PaymentResponse processPayment(PaymentRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing payment for order: {}, amount: {}", request.getOrderId(), request.getAmount());

        // create payment record
        Payment payment = Payment.builder()
            .orderId(request.getOrderId())
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .paymentMethod(request.getPaymentMethod())
            .status(PaymentStatus.PENDING)
            .build();

        payment = paymentRepo.save(payment);

        try {
            // apply chaos engineering if enabled
            if (chaosEnabled) {
                applyChaos();
            }

            simulateProcessingDelay();  //simulate payment processing delay

            // process payment
            String transactionId = UUID.randomUUID().toString();
            payment.setTransactionId(transactionId);
            payment.setStatus(PaymentStatus.COMPLETED);
            payment.setProcessedAt(LocalDateTime.now());

            log.info("Payment successful for order: {}, transaction: {}", request.getOrderId(), transactionId);

        } catch (PaymentException e) {
            log.error("Payment failed for order: {}, reason: {}", request.getOrderId(), e.getMessage());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(e.getMessage());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Payment processing interrupted for order: {}", request.getOrderId());
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Processing interrupted");
        }

        payment.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        payment = paymentRepo.save(payment);

        return PaymentResponse.builder()
            .paymentId(payment.getPaymentId())
            .status(payment.getStatus().name())
            .message(payment.getStatus() == PaymentStatus.COMPLETED ? "Payment processed successfully" : "Payment failed: " + payment.getFailureReason())
            .processingTimeMs(payment.getProcessingTimeMs())
            .tractionId(payment.getTransactionId())
            .build();

    }

    private void applyChaos() {
        if (chaosDelayMs > 0) {
            try {
                log.debug("applying chaos delay: {}ms", chaosDelayMs);
                Thread.sleep(chaosDelayMs);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        if (chaosFailureRate > 0 && random.nextDouble() < chaosFailureRate) {
            log.warn("Chaos: Injecting payment failure");
            throw new PaymentException("Chaos engineering: Random payment failure");
        }
    }

    @Override
    public Payment getPayment(Long id) {
        return paymentRepo.findById(id)
            .orElseThrow(() -> new PaymentException("Payment not found: " + id));
            
    }

    @Override
    public Payment getPaymentByPaymentId(String paymentId) {
        return paymentRepo.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentException("Payment not found: " + paymentId));
    }

    @Override
    public Payment getPaymentByOrderId(String orderId) {
        return paymentRepo.findByOrderId(orderId)
            .orElseThrow(() -> new PaymentException("Payment not found for order: " + orderId));
    }

    @Override
    public List<Payment> getAllPayments() {
        return paymentRepo.findAll();
    }

    @Override
    public List<Payment> getPaymentsByCustomer(String customerId) {
        return paymentRepo.findByCustomerId(customerId);
    }

    private void simulateProcessingDelay() throws InterruptedException {
        long delay = baseProcessingDelayMs + random.nextInt((int) randomProcessingDelayMs);
        log.debug("simulating payment processing delay: {}ms", delay);
        Thread.sleep(delay);
    }

    public void updateChaosConfig(double failureRate, long delayMs) {
        this.chaosFailureRate = Math.min(Math.max(failureRate, 0.0), 1.0);
        this.chaosDelayMs = Math.max(delayMs, 0);
        log.info("Chaos config updated. Failure Rate: {}%, Delay: {}ms", this.chaosFailureRate * 100, this.chaosDelayMs);
    }    

    public ChaosConfig getChaosConfig() {
        return new ChaosConfig(chaosEnabled, chaosFailureRate, chaosDelayMs);
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ChaosConfig {
        private boolean enabled;
        private double failureRate;
        private long delayMs;
    }
    
}
