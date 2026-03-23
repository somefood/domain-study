package com.ecommerce.order.infrastructure;

import com.ecommerce.order.domain.model.Order;
import com.ecommerce.order.domain.model.OrderId;
import com.ecommerce.order.domain.repository.OrderRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryOrderRepository implements OrderRepository {

    private final Map<OrderId, Order> store = new ConcurrentHashMap<>();

    @Override
    public Order save(Order order) {
        store.put(order.getOrderId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(OrderId orderId) {
        return Optional.ofNullable(store.get(orderId));
    }
}
