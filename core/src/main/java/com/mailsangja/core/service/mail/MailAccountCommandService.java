package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.MailAccountCreateCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
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
        validateCommand(command);

        if (mailAccountRepositoryPort.findByAccountIdAndProviderAndEmailAddress(
                user.getId(),
                command.provider(),
                command.emailAddress()
        ).isPresent()) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED);
        }

        mailAccountRepositoryPort.findByProviderAndEmailAddress(command.provider(), command.emailAddress())
                .filter(existing -> !existing.getAccountId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED_BY_ANOTHER_USER);
                });

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

    private void validateCommand(MailAccountCreateCommand command) {
        if (command.provider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (isBlank(command.emailAddress())
                || isBlank(command.accessToken())
                || command.accessTokenExpiresAt() == null
                || isBlank(command.refreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
