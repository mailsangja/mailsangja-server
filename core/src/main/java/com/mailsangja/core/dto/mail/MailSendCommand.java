package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.user.User;

import java.util.List;
import java.util.UUID;

public record MailSendCommand(
        UUID userId,
        String composeSessionId,
        String from,
        List<String> to,
        List<String> cc,
        List<String> bcc,
        String subject,
        String content,
        List<MailAttachmentCommand> attachments
) {

    public static MailSendCommand from(User user, MailSendRequest request) {
        return new MailSendCommand(
                user.getId(),
                request.composeSessionId(),
                request.from(),
                request.to(),
                request.cc(),
                request.bcc(),
                request.subject(),
                request.content(),
                request.attachments() == null ? List.of() : request.attachments().stream()
                        .map(MailAttachmentCommand::from)
                        .toList()
        );
    }
}
