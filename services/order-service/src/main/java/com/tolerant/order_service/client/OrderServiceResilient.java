package com.tolerant.order_service.client;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

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
import com.tolerant.order_service.service.OrderService;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Profile("!baseline")
@RequiredArgsConstructor
@Slf4j
public class OrderServiceResilient implements OrderService {
    
    private final OrderRepository orderRepo;
    private final PaymentClient paymentClient;
    private final InventoryClient inventoryClient;
    private final NotificationClient notificationClient;

    @Value("${resilience.timeout.duration:3s}")
    private Duration timeout;

    @Value("${messaging.enabled:false}")
    private boolean asyncMessagingEnabled;

    @Override
    @Transactional
    @CircuitBreaker(name = "orderService", fallbackMethod = "createOrderFallback")
    @Retry(name = "orderService")
    @Bulkhead(name = "orderService")
    public Order createOrder(OrderRequest request) {
        long startTime = System.currentTimeMillis();
        log.info("Processing order (RESILIENT) for customer: {}", request.getCustomerId());

        Order order = Order.builder()
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .amount(request.getAmount())
            .status(OrderStatus.PENDING)
            .build();

        order = orderRepo.save(order);

        try {
            // 1. process payment with resilience
            order.setStatus(OrderStatus.PAYMENT_PROCESSING);
            orderRepo.save(order);

            PaymentResponse paymentResponse = processPaymentWithResilience(order, request);
            order.setPaymentId(paymentResponse.getPaymentId());
            order.setStatus(OrderStatus.PAYMENT_COMPLETED);
            orderRepo.save(order);

            // 2. reserve inventory with resilience
            InventoryResponse inventoryResponse = reserveInventoryWithResilience(order, request);

            if (!inventoryResponse.isAvailable()) {
                throw new OrderProcessingException("Inventory not available");
            }

            order.setInventoryReservationId(inventoryResponse.getReservationId());
            order.setStatus(OrderStatus.INVENTORY_RESERVED);
            orderRepo.save(order);

            // 3. send notification (async if enabled)
            if (asyncMessagingEnabled) {
                sendNotificationAsync(order, request);

            } else {
                NotificationResponse notificationResponse = sendNotificationWithResilience(order, request);
                order.setNotificationId(notificationResponse.getNotificationId());
            }

            order.setStatus(OrderStatus.COMPLETED);

        } catch (Exception e) {
            log.error("Order processing failed: {}", e.getMessage());
            order.setStatus(OrderStatus.FAILED);
            order.setFailureReason(e.getMessage());
        }

        order.setProcessingTimeMs(System.currentTimeMillis() - startTime);
        return orderRepo.save(order);
    }

    @CircuitBreaker(name = "payment", fallbackMethod = "paymentFallback")
    @Retry(name = "payment")
    @TimeLimiter(name = "payment")
    @Bulkhead(name = "payment")
    private PaymentResponse processPaymentWithResilience(Order order, OrderRequest request) {
        log.info("Processing payment with resilience for order: {}", order.getOrderNumber());

        PaymentRequest paymentRequest = PaymentRequest.builder()
            .orderId(order.getOrderNumber())
            .customerId(request.getCustomerId())
            .amount(request.getAmount())
            .paymentMethod("CREDIT_CARD")
            .build();

        return paymentClient.processPaymentSync(paymentRequest, timeout);
    }

    private PaymentResponse paymentFallback(Order order, OrderRequest request, Exception e) {
        log.warn("Payment fallback triggered for order: {}", order.getOrderNumber());

        return PaymentResponse.builder()
            .paymentId("FALLBACK-" + System.currentTimeMillis())
            .status("PENDING")
            .message("Payment queued for retry: " + e.getMessage())
            .build();
    }

    @CircuitBreaker(name = "inventory", fallbackMethod = "inventoryFallback")
    @Retry(name = "inventory")
    @TimeLimiter(name = "inventory")
    @Bulkhead(name = "inventory")
    private InventoryResponse reserveInventoryWithResilience(Order order, OrderRequest request) {
        log.info("Reserving inventory with resilience for order: {}", order.getOrderNumber());

        InventoryRequest inventoryRequest = InventoryRequest.builder()
            .orderId(order.getOrderNumber())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .build();

        return inventoryClient.reserveInventorySync(inventoryRequest, timeout);
    }

    private InventoryResponse inventoryFallback(Order order, OrderRequest request, Exception e) {
        log.warn("Inventory fallback triggered for order: {}", order.getOrderNumber());
        return InventoryResponse.builder()
            .reservationId("FALLBACK-" + System.currentTimeMillis())
            .available(false)
            .message("Inventory check failed, will retry: " + e.getMessage())
            .build();
    }

    @CircuitBreaker(name = "notification", fallbackMethod = "notificationFallback")
    @Retry(name = "notification")
    @TimeLimiter(name = "notification")
    private NotificationResponse sendNotificationWithResilience(Order order, OrderRequest request) {
        log.info("Sending notification with resilience for order: {}", order.getOrderNumber());

        NotificationRequest notificationRequest = NotificationRequest.builder()
            .orderId(order.getOrderNumber())
            .customerId(request.getCustomerId())
            .email(request.getCustomerEmail())
            .phone(request.getCustomerPhone())
            .message("Your order has been confirmed")
            .notificationType("ORDER_CONFIRMED")
            .build();

        return notificationClient.sendNotificationSync(notificationRequest, timeout);
    }

    private NotificationResponse notificationFallback(Order order, OrderRequest request, Exception e) {
        log.warn("Notification fallback triggered, sending to queue for order: {}", order.getOrderNumber());

        NotificationRequest notificationRequest = NotificationRequest.builder()
            .orderId(order.getOrderNumber())
            .customerId(request.getCustomerId())
            .email(request.getCustomerEmail())
            .phone(request.getCustomerPhone())
            .message("Your order has been confirmed!")
            .notificationType("ORDER_CONFIRMATION")
            .build();

        notificationClient.sendNotificationAsync(notificationRequest);

        return NotificationResponse.builder()
            .notificationId("ASYNC-" + System.currentTimeMillis())
            .status("QUEUED")
            .build();
    }

    private void sendNotificationAsync(Order order, OrderRequest request) {
        NotificationRequest notificationRequest = NotificationRequest.builder()
            .orderId(order.getOrderNumber())
            .customerId(request.getCustomerId())
            .email(request.getCustomerEmail())
            .phone(request.getCustomerEmail())
            .message("Your order has been confirmed!")
            .notificationType("ORDER_CONFIRMATION")
            .build();

        notificationClient.sendNotificationAsync(notificationRequest);
        order.setNotificationId("ASYNC-"+ System.currentTimeMillis());
    }

    private Order createOrderFallback(OrderRequest request, Exception e) {
        log.error("Order creation failed completely, fallback  triggered: {}", e.getMessage());

        Order order = Order.builder()
            .customerId(request.getCustomerId())
            .productId(request.getProductId())
            .quantity(request.getQuantity())
            .amount(request.getAmount())
            .status(OrderStatus.FAILED)
            .failureReason("Service unavailable: " + e.getMessage())
            .processingTimeMs(0L)
            .build();

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
            .orElseThrow(() -> new OrderProcessingException("Order not found: " + orderNumber));
    }

    @Override
    public List<Order> getAllOrders() {
        return orderRepo.findAll();
    }

    @Override
    public List<Order> getOrdersByCustomer(String customerId) {
        return orderRepo.findByCustomerId(customerId);
    }
}
