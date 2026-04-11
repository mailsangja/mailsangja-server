package com.mailsangja.worker.dto.mail;

import com.mailsangja.db.entity.mail.Direction;

import java.time.LocalDateTime;
import java.util.List;

public record InitialMailSyncMessageSaveCommand(
        String gmailMessageId,
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
    public static InitialMailSyncMessageSaveCommand from(InitialMailSyncMessageResult result) {
        return new InitialMailSyncMessageSaveCommand(
                result.gmailMessageId(),
                result.historyId(),
                result.direction(),
                result.subject(),
                result.fromAddress(),
                result.toAddresses(),
                result.ccAddresses(),
                result.snippet(),
                result.read(),
                result.sentAt(),
                result.bodyText(),
                result.bodyHtml(),
                result.attachments()
        );
    }
}
