package com.mailsangja.db.entity.label;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;
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

    @Column(name = "notification_enabled", nullable = false)
    private boolean notificationEnabled;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "rule", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String rule;

    @Builder.Default
    @ManyToMany(mappedBy = "labels", fetch = FetchType.LAZY)
    private List<Thread> threads = new ArrayList<>();

    @Builder.Default
    @ManyToMany(mappedBy = "labels", fetch = FetchType.LAZY)
    private List<Message> messages = new ArrayList<>();

    public void updateName(String name) {
        this.name = name;
    }

    public void updateColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public void updateNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public void updateDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public void updateRule(String rule) {
        this.rule = rule;
    }
}
