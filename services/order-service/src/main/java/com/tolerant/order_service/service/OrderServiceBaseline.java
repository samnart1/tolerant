package com.tolerant.order_service.service;

import java.time.Duration;
import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.tolerant.order_service.client.InventoryClient;
import com.tolerant.order_service.client.NotificationClient;
import com.tolerant.order_service.client.PaymentClient;
import com.tolerant.order_service.exception.OrderProcessingException;
import com.tolerant.order_service.model.InventoryRequest;
import com.tolerant.order_service.model.InventoryResponse;
import com.tolerant.order_service.model.NotificationRequest;
import com.tolerant.order_service.model.NotificationResponse;
import com.tolerant.order_service.model.Order;
import com.tolerant.order_service.model.OrderRequest;
import com.tolerant.order_service.model.OrderStatus;
import com.tolerant.order_service.model.PaymentRequest;
import com.tolerant.order_service.model.PaymentResponse;
import com.tolerant.order_service.repos.OrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("baseline")
@RequiredArgsConstructor
@Slf4j
public class OrderServiceBaseline implements OrderService {

    private final OrderRepository orderRepo;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;
    
    @Override
    @Transactional
    public Order createOrder(OrderRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing order (BASELINE) for customer: {}", request.getCustomerId());

        // create order
        Order order = Order.builder()
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .amount(request.getAmount())
            .status(OrderStatus.PENDING)
            .build();

        order = orderRepo.save(order);

        // process payment withou resilient patterns
        try {
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepo.save(order);

            PaymentRequest paymentRequest = PaymentRequest.builder()
                .orderId(order.getOrderNumber())
                .customerId(request.getCustomerId())
                .amount(request.getAmount())
                .paymentMethod("CREDIT_CARD")
                .build();

            PaymentResponse paymentResponse = paymentClient.processPaymentSync(paymentRequest, Duration.ofMinutes(10)); // long timout or no timeout

            order.setPaymentId(paymentResponse.getPaymentId());
            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepo.save(order);

            // reserve inventory (no timeout, no retry)
            InventoryRequest inventoryRequest = InventoryRequest.builder()
                .orderId(order.getOrderNumber())
                .productId(request.getProductId())
                .quantity(request.getQuantity())
                .build();

            InventoryResponse inventoryResponse = inventoryClient.reserveInventorySync(inventoryRequest, Duration.ofMinutes(10)); // same as before for the duration

            if (!inventoryResponse.isAvailable()) {
                throw new OrderProcessingException("Inventory no t available");
            }

            order.setInventoryReservationId(inventoryResponse.getReservationId());
            order.setStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepo.save(order);

            // send notification 
            NotificationRequest notificationRequest = NotificationRequest.builder()
                .orderId(order.getOrderNumber())
                .customerId(request.getCustomerId())
                .email(request.getCustomerEmail())
                .phone(request.getCustomerPhone())
                .message("You order has been confirmed")
                .notificationType("ORDER_CONFIRMATION")
                .build();

            NotificationResponse notificationResponse = notificationClient.sendNotificationSync(notificationRequest, Duration.ofMinutes(10));

            order.setNotificationId(notificationResponse.getNotificationId());
            order.setStatus(OrderStatus.COMPLETED);


        } catch (Exception e) {
            log.error("Order processing failed: {}", e.getMessage());
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(e.getMessage());
        }

        order.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return orderRepo.save(order);
    }

    @Override
    public Order getOrder(Long id) {
        return orderRepo.findById(id)
            .orElseThrow(() -> new OrderProcessingException("Order not found: " + id));
    }

    @Override
    public Order getOrderByNumber(String orderNumber) {
        return orderRepo.findByOrderNumber(orderNumber)
            .orElseThrow(() -> new OrderProcessingException("Order not found with number: " + orderNumber));
    }

    @Override
    public List<Order> getAllOrders() {
        List<Order> orders = orderRepo.findAll();
        return orders;
    }

    @Override
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepo.findByCustomerId(customerId);
    }
    
}
