package com.mailsangja.db.entity.mail;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mail_accounts")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class MailAccount extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 50)
    private MailProvider provider;

    @Column(name = "email_address", nullable = false, length = 255)
    private String emailAddress;

    @Column(name = "alias", nullable = false, length = 255)
    private String alias;

    @Column(name = "icon", nullable = false, length = 255)
    private String icon;

    @Column(name = "color", nullable = false, length = 7)
    private String color;

    @Column(name = "access_token", nullable = false, columnDefinition = "TEXT")
    private String accessToken;

    @Column(name = "access_token_expires_at", nullable = false)
    private LocalDateTime accessTokenExpiresAt;

    @Column(name = "refresh_token", columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(name = "sync_history_id", length = 255)
    private String syncHistoryId;

    @Column(name = "watch_expires_at")
    private LocalDateTime watchExpiresAt;

    public void updateAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void updateAccessTokenExpiresAt(LocalDateTime accessTokenExpiresAt) {
        this.accessTokenExpiresAt = accessTokenExpiresAt;
    }

    public void updateRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public void updateAlias(String alias) {
        this.alias = alias;
    }

    public void updateIcon(String icon) {
        this.icon = icon;
    }

    public void updateColor(String color) {
        this.color = color;
    }

    public void updateSyncHistoryId(String syncHistoryId) {
        this.syncHistoryId = syncHistoryId;
    }

    public void updateWatchExpiresAt(LocalDateTime watchExpiresAt) {
        this.watchExpiresAt = watchExpiresAt;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }
}
