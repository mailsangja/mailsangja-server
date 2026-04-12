package com.mailsangja.worker.dto.mail;

import com.mailsangja.db.entity.mail.Direction;

import java.time.LocalDateTime;
import java.util.List;

public record InitialMailSyncMessageResult(
        String gmailMessageId,
        String gmailThreadId,
        String historyId,
        Direction direction,
        String subject,
        String fromAddress,
        List<String> toAddresses,
        List<String> ccAddresses,
        String snippet,
        boolean read,
        LocalDateTime sentAt,
        String bodyText,
        String bodyHtml,
        List<InitialMailSyncAttachmentResult> attachments
) {
}
