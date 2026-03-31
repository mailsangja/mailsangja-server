package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.MailAccountCreateCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;

    @Transactional
    public MailAccount create(User user, MailAccountCreateCommand command) {
        if (mailAccountRepositoryPort.findByEmailAddress(command.emailAddress()).isPresent()) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED);
        }

        MailAccount mailAccount = MailAccount.builder()
                .accountId(user.getId())
                .provider(command.provider())
                .emailAddress(command.emailAddress())
                .accessToken(command.accessToken())
                .accessTokenExpiresAt(command.accessTokenExpiresAt())
                .refreshToken(command.refreshToken())
                .active(true)
                .syncHistoryId(command.syncHistoryId())
                .build();

        return mailAccountRepositoryPort.save(mailAccount);
    }
}
