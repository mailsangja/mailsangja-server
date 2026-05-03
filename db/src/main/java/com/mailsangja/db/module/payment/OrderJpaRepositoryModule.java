package com.mailsangja.db.module.payment;

import com.mailsangja.db.entity.payment.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderJpaRepositoryModule extends JpaRepository<Order, UUID> {

    boolean existsByWebhookId(String webhookId);
}
