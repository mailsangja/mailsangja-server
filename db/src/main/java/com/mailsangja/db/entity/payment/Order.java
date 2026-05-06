package com.mailsangja.db.entity.payment;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.user.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * 결제 주문 Entity.
 *
 * 결제 시작 시 PENDING 상태로 생성(Pre-Order)되며,
 * 포트원 웹훅이 도착한 후 결제 검증이 완료되면 COMPLETED로 전환됩니다.
 *
 * PENDING 상태에서 {@code webhookId}와 {@code paymentId}는 null입니다.
 * COMPLETED 전환 시 두 필드가 함께 설정됩니다.
 *
 * 포트원 웹훅 처리 시 멱등성 보장을 위해 {@code webhookId}를 UNIQUE 제약으로 관리합니다.
 * 동일한 {@code webhookId}로 중복 수신된 웹훅은 저장이 거부되어 이중 플랜 업그레이드를 방지합니다.
 */
@Entity
@Table(name = "orders")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @Column(name = "webhook_id", nullable = true, length = 255)
    private String webhookId;

    @Column(name = "payment_id", nullable = true, length = 255)
    private String paymentId;

    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 20)
    private Plan plan;

    @Column(name = "amount", nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * 결제 완료 처리 — COMPLETED 상태로 전환하고 webhookId, paymentId를 설정합니다.
     */
    public void complete(String webhookId, String paymentId) {
        this.webhookId = webhookId;
        this.paymentId = paymentId;
        this.status = OrderStatus.COMPLETED;
    }
}
