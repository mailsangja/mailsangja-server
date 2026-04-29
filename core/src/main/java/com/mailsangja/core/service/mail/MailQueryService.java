package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailQueryService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    public MailAccount findActiveSenderMailAccount(UUID userId, String emailAddress) {
        return mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(userId, emailAddress, true)
                .orElseThrow(() -> new MailSendException(MailSendErrorCode.SENDER_MAIL_ACCOUNT_NOT_FOUND));
    }

    public Message findReplyTargetMessage(UUID messageId) {
        if (messageId == null) {
            throw new MailSendException(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND);
        }

        Message message = messageRepositoryPort.findByIdIncludingDeleted(messageId)
                .orElseThrow(() -> new MailSendException(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND));
        if (message.isDeleted()) {
            throw new MailSendException(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND);
        }

        return message;
    }
}
