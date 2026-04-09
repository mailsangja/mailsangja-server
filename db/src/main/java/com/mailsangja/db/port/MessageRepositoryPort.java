package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.Message;

import java.util.List;
import java.util.UUID;

public interface MessageRepositoryPort {
    Message save(Message message);
    List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId);
    List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds);
    List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId);
}
