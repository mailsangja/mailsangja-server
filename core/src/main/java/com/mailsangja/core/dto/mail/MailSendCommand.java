package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.user.User;

import java.util.List;
import java.util.UUID;

public record MailSendCommand(
        UUID userId,
        MailAddressCommand from,
        MailAddressCommand replyTo,
        List<MailAddressCommand> to,
        List<MailAddressCommand> cc,
        List<MailAddressCommand> bcc,
        String subject,
        String content,
        List<MailAttachmentCommand> attachments
) {

    public static MailSendCommand from(User user, MailSendRequest request) {
        return new MailSendCommand(
                user.getId(),
                MailAddressCommand.fromRaw(request.from()),
                request.replyTo() == null || request.replyTo().isBlank() ? null : MailAddressCommand.fromRaw(request.replyTo()),
                request.to() == null ? List.of() : request.to().stream()
                        .map(MailAddressCommand::fromRaw)
                        .toList(),
                request.cc() == null ? List.of() : request.cc().stream()
                        .map(MailAddressCommand::fromRaw)
                        .toList(),
                request.bcc() == null ? List.of() : request.bcc().stream()
                        .map(MailAddressCommand::fromRaw)
                        .toList(),
                request.subject(),
                request.content(),
                request.attachments() == null ? List.of() : request.attachments().stream()
                        .map(MailAttachmentCommand::from)
                        .toList()
        );
    }

    public void validateReplyTargetAccess(Message replyTargetMessage) {
        MailAccount replyTargetMailAccount = replyTargetMessage.getThread().getMailAccount();
        if (userId == null
                || replyTargetMailAccount == null
                || replyTargetMailAccount.getUser() == null
                || !userId.equals(replyTargetMailAccount.getUser().getId())) {
            throw new MailSendException(MailSendErrorCode.REPLY_TARGET_MESSAGE_ACCESS_DENIED);
        }
    }

    public void validateReplySender(MailAccount senderMailAccount, Message replyTargetMessage) {
        MailAccount replyTargetMailAccount = replyTargetMessage.getThread().getMailAccount();
        if (replyTargetMailAccount == null || !senderMailAccount.getId().equals(replyTargetMailAccount.getId())) {
            throw new MailSendException(MailSendErrorCode.REPLY_SENDER_ACCOUNT_MISMATCH);
        }
    }
}
