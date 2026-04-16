package com.mailsangja.worker.dto.mail.sync;

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
        String fromName,
        List<String> toAddresses,
        List<String> toNames,
        List<String> ccAddresses,
        List<String> ccNames,
        String snippet,
        boolean read,
        LocalDateTime sentAt,
        String bodyText,
        String bodyHtml,
        List<InitialMailSyncAttachmentResult> attachments
) {
}
