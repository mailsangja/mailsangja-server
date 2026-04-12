package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Message;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MessageRepositoryPort {
    Message save(Message message);
    Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId);
    Optional<Message> findByIdIncludingDeleted(UUID messageId);
    List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId);
    List<Message> findAllByThreadIdIncludingDeleted(UUID threadId);
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds);
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
    List<Message> findAllByMailAccountIdAndGmailThreadId(UUID mailAccountId, String gmailThreadId);
}
