package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.dto.MailDraftReferenceMessageResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.port.MailDraftReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MailDraftReferenceQueryAdapter implements MailDraftReferenceQueryPort {

    private final MessageJpaRepositoryModule messageJpaRepositoryModule;

    @Override
    public List<MailDraftReferenceMessageResult> findRecentWrittenMessages(UUID userId, UUID mailAccountId, int limit) {
        List<Message> messages = messageJpaRepositoryModule.findRecentByUserIdAndMailAccountIdAndDirection(
                userId, mailAccountId, Direction.OUTBOUND, PageRequest.of(0, limit)
        );
        return toResults(messages);
    }

    @Override
    public List<MailDraftReferenceMessageResult> findWrittenMessagesByHints(UUID userId, UUID mailAccountId,
                                                                            List<String> hints, int limit) {
        if (hints == null || hints.isEmpty()) {
            return List.of();
        }
        return toResults(findHintMessages(userId, mailAccountId, hints, limit));
    }

    @Override
    public List<MailDraftReferenceMessageResult> findMessagesByIds(List<UUID> messageIds) {
        if (messageIds == null || messageIds.isEmpty()) {
            return List.of();
        }
        return toResults(messageJpaRepositoryModule.findActiveByIdIn(messageIds));
    }

    @Override
    public List<MailDraftReferenceMessageResult> findThreadContextMessages(UUID replyMessageId) {
        return toResults(messageJpaRepositoryModule.findThreadContextByReplyMessageId(replyMessageId));
    }

    private List<Message> findHintMessages(UUID userId, UUID mailAccountId, List<String> hints, int limit) {
        List<Message> messages = new ArrayList<>();
        for (String hint : hints) {
            addHintMessages(messages, userId, mailAccountId, hint, limit);
        }
        return messages;
    }

    private void addHintMessages(List<Message> messages, UUID userId, UUID mailAccountId, String hint, int limit) {
        int remaining = limit - messages.size();
        if (remaining <= 0 || hint == null || hint.isBlank()) {
            return;
        }
        List<Message> found = messageJpaRepositoryModule.findWrittenByUserIdAndMailAccountIdAndHint(
                userId.toString(), mailAccountId.toString(), hint, PageRequest.of(0, remaining)
        );
        addUniqueMessages(messages, found, limit);
    }

    private void addUniqueMessages(List<Message> messages, List<Message> found, int limit) {
        for (Message message : found) {
            addUniqueMessage(messages, message, limit);
        }
    }

    private void addUniqueMessage(List<Message> messages, Message message, int limit) {
        if (messages.size() < limit && !containsMessage(messages, message)) {
            messages.add(message);
        }
    }

    private boolean containsMessage(List<Message> messages, Message message) {
        for (Message value : messages) {
            if (value.getId().equals(message.getId())) {
                return true;
            }
        }
        return false;
    }

    private List<MailDraftReferenceMessageResult> toResults(List<Message> messages) {
        return messages.stream()
                .map(this::toResult)
                .toList();
    }

    private MailDraftReferenceMessageResult toResult(Message message) {
        return new MailDraftReferenceMessageResult(
                message.getId(),
                message.getThread().getMailAccount().getId(),
                message.getDirection(),
                message.getSubject(),
                bodyOf(message)
        );
    }

    private String bodyOf(Message message) {
        if (message.getBodyText() != null && !message.getBodyText().isBlank()) {
            return message.getBodyText();
        }
        return htmlTextOf(message.getBodyHtml());
    }

    private String htmlTextOf(String bodyHtml) {
        if (bodyHtml == null || bodyHtml.isBlank()) {
            return "";
        }
        return Jsoup.parse(bodyHtml).text();
    }
}
