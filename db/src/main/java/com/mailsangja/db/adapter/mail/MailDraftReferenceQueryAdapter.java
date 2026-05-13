package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.dto.MailDraftReferenceMessageResult;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.port.MailDraftReferenceQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

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
        return message.getBodyHtml();
    }
}
