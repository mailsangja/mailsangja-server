package com.mailsangja.core.dto.mail;

import java.time.LocalDateTime;
import java.util.List;

public record GoogleMailMessageResult(
        String gmailMessageId,
        String gmailThreadId,
        String historyId,
        String rfcMessageId,
        String referencesHeader,
        String inReplyToHeader,
        String replyToAddress,
        String replyToName,
        String subject,
        String fromAddress,
        String fromName,
        List<String> toAddresses,
        List<String> toNames,
        List<String> ccAddresses,
        List<String> ccNames,
        String snippet,
        LocalDateTime sentAt,
        String bodyText,
        String bodyHtml,
        List<GoogleMailAttachmentResult> attachments
) {
}
