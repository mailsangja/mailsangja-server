package com.mailsangja.db.entity.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "labels")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Label extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "color_code", nullable = false, length = 7)
    private String colorCode;

    @Column(name = "notification_policy", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private NotificationPolicy notificationPolicy;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "rule", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private LabelRule rule;

    @Builder.Default
    @Column(name = "confirmed", nullable = false)
    private boolean confirmed = true;

    public void confirm() {
        this.confirmed = true;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updateColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public void updateNotificationPolicy(NotificationPolicy notificationPolicy) {
        this.notificationPolicy = notificationPolicy;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void updateRule(LabelRule rule) {
        this.rule = rule;
    }
}
