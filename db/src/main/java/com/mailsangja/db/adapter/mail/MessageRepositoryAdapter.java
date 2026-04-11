package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class MessageRepositoryAdapter implements MessageRepositoryPort {

    private final MessageJpaRepositoryModule messageJpaRepositoryModule;

    @Override
    public Message save(Message message) {
        return messageJpaRepositoryModule.save(message);
    }

    @Override
    public Optional<Message> findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(UUID threadId, String gmailMessageId) {
        return messageJpaRepositoryModule.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(threadId, gmailMessageId);
    }

    @Override
    public long countByThreadIdAndDeletedAtIsNull(UUID threadId) {
        return messageJpaRepositoryModule.countByThreadIdAndDeletedAtIsNull(threadId);
    }

    @Override
    public List<Message> findAllByThreadIdAndDeletedAtIsNull(UUID threadId) {
        return messageJpaRepositoryModule.findAllByThreadIdAndDeletedAtIsNull(threadId);
    }

    @Override
    public List<Message> findAllByThreadIdInAndDeletedAtIsNull(List<UUID> threadIds) {
        return messageJpaRepositoryModule.findAllByThreadIdInAndDeletedAtIsNull(threadIds);
    }

    @Override
    public List<Message> findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(UUID mailAccountId, String gmailThreadId) {
        return messageJpaRepositoryModule.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
    }
}
