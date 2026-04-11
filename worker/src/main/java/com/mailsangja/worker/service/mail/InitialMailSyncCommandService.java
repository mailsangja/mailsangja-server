package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailThreadResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class InitialMailSyncCommandService {

    private static final ZoneId KST_ZONE_ID = ZoneId.of("Asia/Seoul");

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    @Transactional
    public void saveThreadBatch(MailAccount mailAccount, List<GoogleMailThreadResponse> threadResponses) {
        if (mailAccount == null || threadResponses == null || threadResponses.isEmpty()) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        for (GoogleMailThreadResponse threadResponse : threadResponses) {
            saveThread(mailAccount, threadResponse);
        }
    }

    private void saveThread(MailAccount mailAccount, GoogleMailThreadResponse threadResponse) {
        if (threadResponse == null || isBlank(threadResponse.id()) || threadResponse.messages() == null) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }

        for (GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse : threadResponse.messages()) {
            saveMessage(mailAccount, threadResponse, messageResponse);
        }
    }

    private void saveMessage(
            MailAccount mailAccount,
            GoogleMailThreadResponse threadResponse,
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse
    ) {
        validateThreadMessage(threadResponse, messageResponse);

        Direction direction = resolveDirection(messageResponse.labelIds());
        boolean read = isRead(messageResponse.labelIds());
        String fromAddress = extractRequiredHeaderValue(messageResponse, "From");
        String subject = extractHeaderValue(messageResponse, "Subject");
        List<String> toAddresses = extractAddresses(messageResponse, "To");
        List<String> ccAddresses = extractAddresses(messageResponse, "Cc");
        LocalDateTime sentAt = resolveSentAt(messageResponse);
        MimeBodyContent bodyContent = extractBodyContent(messageResponse.payload());

        Thread thread = findOrCreateThread(
                mailAccount,
                threadResponse.id(),
                direction
        );

        Optional<Message> existingMessage = messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                thread.getId(),
                messageResponse.id()
        );

        if (existingMessage.isPresent()) {
            Message message = existingMessage.get();
            message.updateBasicContent(
                    subject,
                    fromAddress,
                    toAddresses,
                    ccAddresses,
                    messageResponse.snippet(),
                    read,
                    sentAt
            );
            message.updateBodyContent(bodyContent.text(), bodyContent.html());
            message.replaceAttachments(createAttachments(message, messageResponse.payload()));
        } else {
            Message message = Message.builder()
                    .thread(thread)
                    .gmailMessageId(messageResponse.id())
                    .direction(direction)
                    .subject(subject)
                    .fromAddress(fromAddress)
                    .toAddresses(toAddresses)
                    .ccAddresses(ccAddresses)
                    .snippet(messageResponse.snippet())
                    .read(read)
                    .sentAt(sentAt)
                    .bodyText(bodyContent.text())
                    .bodyHtml(bodyContent.html())
                    .attachments(new ArrayList<>())
                    .labels(Collections.emptyList())
                    .build();
            message.replaceAttachments(createAttachments(message, messageResponse.payload()));
            messageRepositoryPort.save(message);
        }

        thread.updateHistoryId(firstNonBlank(messageResponse.historyId(), threadResponse.historyId()));
        thread.updateLatestMessageInfoIfNewer(
                subject,
                messageResponse.snippet(),
                resolveLatestParticipantAddress(direction, fromAddress, toAddresses),
                sentAt,
                read
        );
        thread.updateMessageCount((int) messageRepositoryPort.countByThreadIdAndDeletedAtIsNull(thread.getId()));
    }

    private Thread findOrCreateThread(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                        mailAccount.getId(),
                        gmailThreadId,
                        direction
                )
                .orElseGet(() -> threadRepositoryPort.save(Thread.builder()
                        .mailAccount(mailAccount)
                        .gmailThreadId(gmailThreadId)
                        .direction(direction)
                        .read(true)
                        .messageCount(0)
                        .build()));
    }

    private void validateThreadMessage(
            GoogleMailThreadResponse threadResponse,
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse
    ) {
        if (messageResponse == null
                || isBlank(messageResponse.id())
                || isBlank(messageResponse.threadId())
                || !threadResponse.id().equals(messageResponse.threadId())) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private Direction resolveDirection(List<String> labelIds) {
        return labelIds != null && labelIds.contains("SENT") ? Direction.OUTBOUND : Direction.INBOUND;
    }

    private boolean isRead(List<String> labelIds) {
        return labelIds == null || !labelIds.contains("UNREAD");
    }

    private String extractRequiredHeaderValue(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        String headerValue = extractHeaderValue(messageResponse, headerName);
        if (isBlank(headerValue)) {
            log.warn(
                    "Initial mail sync message is missing required header. threadId={} gmailMessageId={} headerName={}",
                    messageResponse.threadId(),
                    messageResponse.id(),
                    headerName
            );
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
        return headerValue;
    }

    private String extractHeaderValue(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        if (messageResponse.payload() == null || messageResponse.payload().headers() == null) {
            return null;
        }

        return messageResponse.payload().headers().stream()
                .filter(header -> header != null
                        && !isBlank(header.name())
                        && headerName.equalsIgnoreCase(header.name()))
                .map(GoogleMailThreadResponse.GoogleMailHeaderResponse::value)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<String> extractAddresses(
            GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse,
            String headerName
    ) {
        String headerValue = extractHeaderValue(messageResponse, headerName);
        if (isBlank(headerValue)) {
            return Collections.emptyList();
        }

        return List.of(headerValue.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .toList();
    }

    private MimeBodyContent extractBodyContent(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload) {
        if (payload == null) {
            return new MimeBodyContent(null, null);
        }

        String text = decodeBody(findBodyData(payload, "text/plain"));
        String html = decodeBody(findBodyData(payload, "text/html"));
        return new MimeBodyContent(text, html);
    }

    private String findBodyData(GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload, String mimeType) {
        if (payload == null) {
            return null;
        }

        if (mimeType.equalsIgnoreCase(payload.mimeType())
                && payload.body() != null
                && !isBlank(payload.body().data())) {
            return payload.body().data();
        }

        if (payload.parts() == null || payload.parts().isEmpty()) {
            return null;
        }

        for (GoogleMailThreadResponse.GoogleMailThreadPayloadResponse part : payload.parts()) {
            String data = findBodyData(part, mimeType);
            if (!isBlank(data)) {
                return data;
            }
        }

        return null;
    }

    private String decodeBody(String encodedData) {
        if (isBlank(encodedData)) {
            return null;
        }

        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedData);
            return new String(decoded, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private List<Attachment> createAttachments(
            Message message,
            GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload
    ) {
        List<GoogleMailThreadResponse.GoogleMailThreadPayloadResponse> attachmentParts = new ArrayList<>();
        collectAttachmentParts(payload, attachmentParts);

        return attachmentParts.stream()
                .map(part -> Attachment.builder()
                        .message(message)
                        .gmailAttachmentId(part.body() == null ? null : part.body().attachmentId())
                        .filename(part.filename())
                        .mimeType(part.mimeType())
                        .size(part.body() == null ? null : part.body().size())
                        .build())
                .toList();
    }

    private void collectAttachmentParts(
            GoogleMailThreadResponse.GoogleMailThreadPayloadResponse payload,
            List<GoogleMailThreadResponse.GoogleMailThreadPayloadResponse> attachmentParts
    ) {
        if (payload == null) {
            return;
        }

        if (!isBlank(payload.filename())
                && payload.body() != null
                && !isBlank(payload.body().attachmentId())) {
            attachmentParts.add(payload);
        }

        if (payload.parts() == null || payload.parts().isEmpty()) {
            return;
        }

        payload.parts().forEach(part -> collectAttachmentParts(part, attachmentParts));
    }

    private LocalDateTime resolveSentAt(GoogleMailThreadResponse.GoogleMailThreadMessageResponse messageResponse) {
        if (isBlank(messageResponse.internalDate())) {
            return null;
        }

        try {
            return LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(Long.parseLong(messageResponse.internalDate())),
                    KST_ZONE_ID
            );
        } catch (NumberFormatException e) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_RESULT_INVALID);
        }
    }

    private String resolveLatestParticipantAddress(Direction direction, String fromAddress, List<String> toAddresses) {
        if (direction == Direction.OUTBOUND) {
            return toAddresses.isEmpty() ? null : toAddresses.getFirst();
        }
        return fromAddress;
    }

    private String firstNonBlank(String primary, String secondary) {
        return !isBlank(primary) ? primary : secondary;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record MimeBodyContent(
            String text,
            String html
    ) {
    }
}
