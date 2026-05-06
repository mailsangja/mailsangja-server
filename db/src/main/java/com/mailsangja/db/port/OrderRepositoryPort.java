package com.mailsangja.db.port;

import com.mailsangja.db.entity.payment.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepositoryPort {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    Optional<Order> findByIdWithLock(UUID id);

    boolean existsByWebhookId(String webhookId);
}
