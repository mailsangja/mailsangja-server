package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GmailHistoryStateQueryService {

    private final MessageRepositoryPort messageRepositoryPort;

    public boolean existsMessage(UUID mailAccountId, String gmailThreadId, String gmailMessageId) {
        return findMessage(mailAccountId, gmailThreadId, gmailMessageId).isPresent();
    }

    public Optional<Message> findMessage(UUID mailAccountId, String gmailThreadId, String gmailMessageId) {
        if (mailAccountId == null || isBlank(gmailThreadId) || isBlank(gmailMessageId)) {
            return Optional.empty();
        }

        return messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId,
                gmailThreadId,
                gmailMessageId
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
