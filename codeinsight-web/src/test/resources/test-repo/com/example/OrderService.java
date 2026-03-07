package com.example;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing orders.
 */
public class OrderService {

    private final OrderRepository orderRepository;
    private final PaymentService paymentService;

    public OrderService(OrderRepository orderRepository, PaymentService paymentService) {
        this.orderRepository = orderRepository;
        this.paymentService = paymentService;
    }

    public Order createOrder(String customerId, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("Order must have at least one item");
        }

        double total = items.stream()
                .mapToDouble(item -> item.getPrice() * item.getQuantity())
                .sum();

        Order order = new Order(customerId, items, total);
        orderRepository.save(order);
        return order;
    }

    public Optional<Order> findById(String orderId) {
        return orderRepository.findById(orderId);
    }

    public void processPayment(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found: " + orderId));

        if (order.isPaid()) {
            throw new IllegalStateException("Order already paid");
        }

        paymentService.charge(order.getCustomerId(), order.getTotal());
        order.markAsPaid();
        orderRepository.save(order);
    }
}
