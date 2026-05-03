package com.mailsangja.db.adapter.payment;

import com.mailsangja.db.entity.payment.Order;
import com.mailsangja.db.module.payment.OrderJpaRepositoryModule;
import com.mailsangja.db.port.OrderRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class OrderRepositoryAdapter implements OrderRepositoryPort {

    private final OrderJpaRepositoryModule orderJpaRepositoryModule;

    @Override
    public Order save(Order order) {
        return orderJpaRepositoryModule.save(order);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return orderJpaRepositoryModule.findById(id);
    }

    @Override
    public boolean existsByWebhookId(String webhookId) {
        return orderJpaRepositoryModule.existsByWebhookId(webhookId);
    }
}
