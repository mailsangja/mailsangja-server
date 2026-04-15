package com.mailsangja.core.dto.mail;

import java.time.LocalDateTime;
import java.util.List;

public record GoogleMailMessageResult(
        String gmailMessageId,
        String gmailThreadId,
        String historyId,
        String subject,
        String fromAddress,
        List<String> toAddresses,
        List<String> ccAddresses,
        String snippet,
        LocalDateTime sentAt,
        String bodyText,
        String bodyHtml,
        List<GoogleMailAttachmentResult> attachments
) {
}
