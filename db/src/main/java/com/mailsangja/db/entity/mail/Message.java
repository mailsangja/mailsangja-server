package com.mailsangja.db.entity.mail;

import com.mailsangja.db.entity.common.BaseEntity;
import com.mailsangja.db.entity.label.MessageLabel;
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

    @Column(name = "rfc_message_id", length = 998)
    private String rfcMessageId;

    @Column(name = "references_header", columnDefinition = "text")
    private String referencesHeader;

    @Column(name = "in_reply_to_header", columnDefinition = "text")
    private String inReplyToHeader;

    @Column(name = "reply_to_address", length = 255)
    private String replyToAddress;

    @Column(name = "reply_to_name", length = 255)
    private String replyToName;

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
    @OneToMany(mappedBy = "message", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MessageLabel> messageLabels = new ArrayList<>();

    public static Message from(Thread thread, CreateValues values) {
        CreateValues normalizedValues = values.normalizeNames();
        return Message.builder()
                .thread(thread)
                .gmailMessageId(normalizedValues.gmailMessageId())
                .rfcMessageId(normalizedValues.rfcMessageId())
                .referencesHeader(normalizedValues.referencesHeader())
                .inReplyToHeader(normalizedValues.inReplyToHeader())
                .replyToAddress(normalizedValues.replyToAddress())
                .replyToName(normalizedValues.replyToName())
                .direction(normalizedValues.direction())
                .subject(normalizedValues.subject())
                .fromAddress(normalizedValues.fromAddress())
                .fromName(normalizedValues.fromName())
                .toAddresses(copyList(normalizedValues.toAddresses()))
                .toNames(copyList(normalizedValues.toNames()))
                .ccAddresses(copyList(normalizedValues.ccAddresses()))
                .ccNames(copyList(normalizedValues.ccNames()))
                .snippet(normalizedValues.snippet())
                .read(normalizedValues.read())
                .sentAt(normalizedValues.sentAt())
                .bodyText(normalizedValues.bodyText())
                .bodyHtml(normalizedValues.bodyHtml())
                .attachments(new ArrayList<>())
                .messageLabels(new ArrayList<>())
                .build();
    }

    public record CreateValues(
            String gmailMessageId,
            String rfcMessageId,
            String referencesHeader,
            String inReplyToHeader,
            String replyToAddress,
            String replyToName,
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
        public CreateValues normalizeNames() {
            return new CreateValues(
                    gmailMessageId,
                    rfcMessageId,
                    referencesHeader,
                    inReplyToHeader,
                    replyToAddress,
                    normalizeSingleName(replyToName, replyToAddress),
                    direction,
                    subject,
                    fromAddress,
                    normalizeSingleName(fromName, fromAddress),
                    copyList(toAddresses),
                    normalizeAddressNames(toNames, toAddresses),
                    copyList(ccAddresses),
                    normalizeAddressNames(ccNames, ccAddresses),
                    snippet,
                    read,
                    sentAt,
                    bodyText,
                    bodyHtml
            );
        }
    }

    public void markAsRead() {
        this.read = true;
    }

    public void markAsUnread() {
        this.read = false;
    }

    public void updateFrom(CreateValues values) {
        CreateValues normalizedValues = values.normalizeNames();
        updateBasicContent(
                normalizedValues.rfcMessageId(),
                normalizedValues.referencesHeader(),
                normalizedValues.inReplyToHeader(),
                normalizedValues.replyToAddress(),
                normalizedValues.replyToName(),
                normalizedValues.subject(),
                normalizedValues.fromAddress(),
                normalizedValues.fromName(),
                normalizedValues.toAddresses(),
                normalizedValues.toNames(),
                normalizedValues.ccAddresses(),
                normalizedValues.ccNames(),
                normalizedValues.snippet(),
                normalizedValues.read(),
                normalizedValues.sentAt()
        );
        updateBodyContent(normalizedValues.bodyText(), normalizedValues.bodyHtml());
    }

    public void updateBasicContent(
            String rfcMessageId,
            String referencesHeader,
            String inReplyToHeader,
            String replyToAddress,
            String replyToName,
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
        this.rfcMessageId = rfcMessageId;
        this.referencesHeader = referencesHeader;
        this.inReplyToHeader = inReplyToHeader;
        this.replyToAddress = replyToAddress;
        this.replyToName = replyToName;
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

    public void replaceMessageLabels(List<MessageLabel> newMessageLabels) {
        this.messageLabels.clear();
        if (newMessageLabels != null) {
            this.messageLabels.addAll(newMessageLabels);
        }
    }

    private static List<String> copyList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(values));
    }

    private static List<String> normalizeAddressNames(List<String> names, List<String> addresses) {
        if (addresses == null || addresses.isEmpty()) {
            return List.of();
        }

        List<String> normalizedNames = new ArrayList<>(addresses.size());
        for (int index = 0; index < addresses.size(); index++) {
            String address = addresses.get(index);
            String name = names != null && index < names.size() ? names.get(index) : null;
            normalizedNames.add(normalizeSingleName(name, address));
        }
        return Collections.unmodifiableList(normalizedNames);
    }

    private static String normalizeSingleName(String name, String address) {
        if (name != null) {
            String trimmedName = name.trim();
            if (!trimmedName.isBlank()) {
                return trimmedName;
            }
        }
        return address;
    }
}
