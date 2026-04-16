package com.mailsangja.db.entity.mail;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.label.Label;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "messages",
        uniqueConstraints = @UniqueConstraint(columnNames = {"thread_id", "gmail_message_id"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class Message extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "CHAR(36)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    private Thread thread;

    @Column(name = "gmail_message_id", nullable = false, length = 255)
    private String gmailMessageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "direction", nullable = false, length = 20)
    private Direction direction;

    @Column(name = "subject", length = 500)
    private String subject;

    // From 헤더 이메일
    @Column(name = "from_address", nullable = false, length = 255)
    private String fromAddress;

    @Column(name = "from_name", length = 255)
    private String fromName;

    // To 수신자 목록
    @Column(name = "to_addresses", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> toAddresses;

    @Column(name = "to_names", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> toNames;

    // CC 수신자 목록
    @Column(name = "cc_addresses", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> ccAddresses;

    @Column(name = "cc_names", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private List<String> ccNames;

    // Gmail 메시지 snippet (180자 이내 미리보기)
    @Column(name = "snippet", length = 500)
    private String snippet;

    @Column(name = "is_read", nullable = false)
    private boolean read;


    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "body_text", columnDefinition = "text")
    private String bodyText;

    @Column(name = "body_html", columnDefinition = "text")
    private String bodyHtml;

    @Builder.Default
    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "message_labels",
            joinColumns = @JoinColumn(name = "message_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    private List<Label> labels = new ArrayList<>();

    public static Message from(Thread thread, CreateValues values) {
        return Message.builder()
                .thread(thread)
                .gmailMessageId(values.gmailMessageId())
                .direction(values.direction())
                .subject(values.subject())
                .fromAddress(values.fromAddress())
                .fromName(values.fromName())
                .toAddresses(copyList(values.toAddresses()))
                .toNames(copyList(values.toNames()))
                .ccAddresses(copyList(values.ccAddresses()))
                .ccNames(copyList(values.ccNames()))
                .snippet(values.snippet())
                .read(values.read())
                .sentAt(values.sentAt())
                .bodyText(values.bodyText())
                .bodyHtml(values.bodyHtml())
                .attachments(new ArrayList<>())
                .labels(Collections.emptyList())
                .build();
    }

    public record CreateValues(
            String gmailMessageId,
            Direction direction,
            String subject,
            String fromAddress,
            String fromName,
            List<String> toAddresses,
            List<String> toNames,
            List<String> ccAddresses,
            List<String> ccNames,
            String snippet,
            boolean read,
            LocalDateTime sentAt,
            String bodyText,
            String bodyHtml
    ) {
    }

    public void markAsRead() {
        this.read = true;
    }

    public void markAsUnread() {
        this.read = false;
    }

    public void updateFrom(CreateValues values) {
        updateBasicContent(
                values.subject(),
                values.fromAddress(),
                values.fromName(),
                values.toAddresses(),
                values.toNames(),
                values.ccAddresses(),
                values.ccNames(),
                values.snippet(),
                values.read(),
                values.sentAt()
        );
        updateBodyContent(values.bodyText(), values.bodyHtml());
    }

    public void updateBasicContent(
            String subject,
            String fromAddress,
            String fromName,
            List<String> toAddresses,
            List<String> toNames,
            List<String> ccAddresses,
            List<String> ccNames,
            String snippet,
            boolean read,
            LocalDateTime sentAt
    ) {
        this.subject = subject;
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        this.toAddresses = copyList(toAddresses);
        this.toNames = copyList(toNames);
        this.ccAddresses = copyList(ccAddresses);
        this.ccNames = copyList(ccNames);
        this.snippet = snippet;
        this.read = read;
        this.sentAt = sentAt;
    }

    public void updateBodyContent(String bodyText, String bodyHtml) {
        this.bodyText = bodyText;
        this.bodyHtml = bodyHtml;
    }

    public void replaceAttachments(List<Attachment> attachments) {
        this.attachments.clear();
        if (attachments != null) {
            this.attachments.addAll(attachments);
        }
    }

    private static List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }
}
