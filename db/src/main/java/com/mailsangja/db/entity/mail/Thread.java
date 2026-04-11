package com.mailsangja.db.entity.mail;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.label.Label;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "threads",
        uniqueConstraints = @UniqueConstraint(columnNames = {"mail_account_id", "gmail_thread_id", "direction"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Thread extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mail_account_id", nullable = false)
    private MailAccount mailAccount;

    @Column(name = "gmail_thread_id", nullable = false, length = 255)
    private String gmailThreadId;

    // 이 행이 받은 편지함(INBOUND)인지 보낸 편지함(OUTBOUND)인지 구분
    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private Direction direction;

    @Column(name = "history_id", length = 255)
    private String historyId;

    // 이 direction 기준 가장 최근 메시지 정보
    @Column(name = "latest_subject", length = 500)
    private String latestSubject;

    @Column(name = "latest_snippet", length = 500)
    private String latestSnippet;

    // INBOUND: 발신자 이메일 / OUTBOUND: 주 수신자 이메일
    @Column(name = "latest_participant_address", length = 255)
    private String latestParticipantAddress;

    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "message_count", nullable = false)
    private int messageCount;

    @Builder.Default
    @OneToMany(mappedBy = "thread", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Message> messages = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "thread_labels",
            joinColumns = @JoinColumn(name = "thread_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<Label> labels = new ArrayList<>();

    public void updateLatestMessageInfo(
            String subject,
            String snippet,
            String participantAddress,
            LocalDateTime lastMessageAt
    ) {
        this.latestSubject = subject;
        this.latestSnippet = snippet;
        this.latestParticipantAddress = participantAddress;
        this.lastMessageAt = lastMessageAt;
    }

    public void updateLatestMessageInfoIfNewer(
            String subject,
            String snippet,
            String participantAddress,
            LocalDateTime lastMessageAt,
            boolean read
    ) {
        if (this.lastMessageAt != null && lastMessageAt != null && this.lastMessageAt.isAfter(lastMessageAt)) {
            return;
        }

        updateLatestMessageInfo(subject, snippet, participantAddress, lastMessageAt);
        this.read = read;
    }

    public void updateReadStatus(boolean read) {
        this.read = read;
    }


    public void incrementMessageCount() {
        this.messageCount++;
    }

    public void updateMessageCount(int messageCount) {
        this.messageCount = Math.max(messageCount, 0);
    }

    public void updateHistoryId(String historyId) {
        this.historyId = historyId;
    }
}
