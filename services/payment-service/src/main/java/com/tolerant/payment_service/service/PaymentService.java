package com.tolerant.payment_service.service;

import java.util.List;

import com.tolerant.payment_service.model.Payment;
import com.tolerant.payment_service.model.PaymentRequest;
import com.tolerant.payment_service.model.PaymentResponse;

public interface PaymentService {
    PaymentResponse processPayment(PaymentRequest request);
    Payment getPayment(Long id);
    Payment getPaymentByPaymentId(String paymentId);
    Payment getPaymentByOrderId(String orderId);
    List<Payment> getAllPayments();
    List<Payment> getPaymentsByCustomer(String customerId);
}
