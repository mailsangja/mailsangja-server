package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailQueryService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    public MailAccount findActiveSenderMailAccount(UUID userId, String emailAddress) {
        return mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(userId, emailAddress, true)
                .orElseThrow(() -> new MailSendException(MailSendErrorCode.SENDER_MAIL_ACCOUNT_NOT_FOUND));
    }
}
