package com.tolerant.order_service.service;

import java.util.List;

import com.tolerant.order_service.model.Order;
import com.tolerant.order_service.model.OrderRequest;

public interface OrderService {

    Order createOrder(OrderRequest request);

    Order getOrder(Long id);

    Order getOrderByNumber(String orderNumber);

    List<Order> getAllOrders();

    List<Order> getOrdersByCustomer(String customerId);

}
