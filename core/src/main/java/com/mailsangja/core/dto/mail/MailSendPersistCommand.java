package com.mailsangja.core.dto.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;

public record MailSendPersistCommand(
        MailAccount mailAccount,
        GoogleMailMessageResult messageResult
) {

    public Message.CreateValues toCreateValues() {
        return new Message.CreateValues(
                messageResult.gmailMessageId(),
                Direction.OUTBOUND,
                messageResult.subject(),
                messageResult.fromAddress(),
                messageResult.fromName(),
                messageResult.toAddresses(),
                messageResult.toNames(),
                messageResult.ccAddresses(),
                messageResult.ccNames(),
                messageResult.snippet(),
                true,
                messageResult.sentAt(),
                messageResult.bodyText(),
                messageResult.bodyHtml()
        );
    }

    public String latestParticipantAddress() {
        return messageResult.toAddresses() == null || messageResult.toAddresses().isEmpty()
                ? null
                : messageResult.toAddresses().getFirst();
    }
}
