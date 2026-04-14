package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MailCommandService {

    private final MailQueryService mailQueryService;

    public void validateSendCommand(MailSendCommand command) {
        MailAccount senderMailAccount = mailQueryService.findActiveSenderMailAccount(command.userId(), command.from());
        validateSenderMailAccount(senderMailAccount);
    }

    private void validateSenderMailAccount(MailAccount senderMailAccount) {
        if (senderMailAccount == null || senderMailAccount.getId() == null || !senderMailAccount.isActive()) {
            throw new MailSendException(MailSendErrorCode.SENDER_MAIL_ACCOUNT_NOT_FOUND);
        }
    }
}
