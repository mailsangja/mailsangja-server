package com.mailsangja.worker.dto.mail.sync;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;

import java.time.LocalDateTime;
import java.util.List;

public record InitialMailSyncMessageSaveCommand(
        String gmailMessageId,
        String historyId,
        String rfcMessageId,
        String referencesHeader,
        String inReplyToHeader,
        String replyToAddress,
        String replyToName,
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
    public static InitialMailSyncMessageSaveCommand from(InitialMailSyncMessageResult result) {
        return new InitialMailSyncMessageSaveCommand(
                result.gmailMessageId(),
                result.historyId(),
                result.rfcMessageId(),
                result.referencesHeader(),
                result.inReplyToHeader(),
                result.replyToAddress(),
                result.replyToName(),
                result.direction(),
                result.subject(),
                result.fromAddress(),
                result.fromName(),
                result.toAddresses(),
                result.toNames(),
                result.ccAddresses(),
                result.ccNames(),
                result.snippet(),
                result.read(),
                result.sentAt(),
                result.bodyText(),
                result.bodyHtml(),
                result.attachments()
        );
    }

    public Message.CreateValues toCreateValues() {
        return new Message.CreateValues(
                gmailMessageId,
                rfcMessageId,
                referencesHeader,
                inReplyToHeader,
                replyToAddress,
                replyToName,
                direction,
                subject,
                fromAddress,
                fromName,
                toAddresses,
                toNames,
                ccAddresses,
                ccNames,
                snippet,
                read,
                sentAt,
                bodyText,
                bodyHtml
        );
    }
}
