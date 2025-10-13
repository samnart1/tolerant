package com.tolerant.payment_service.repo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tolerant.payment_service.model.Payment;
import com.tolerant.payment_service.model.PaymentStatus;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentId(String paymentId);
    Optional<Payment> findByOrderId(String orderId);
    List<Payment> findByCustomerId(String customerId);
    List<Payment> findByStatus(PaymentStatus status);
    List<Payment> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByStatus(PaymentStatus status);
    // Payment save(Payment payment);
}
