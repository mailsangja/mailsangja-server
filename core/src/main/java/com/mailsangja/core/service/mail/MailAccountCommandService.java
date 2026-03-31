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

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;
    private final MailAccountQueryService mailAccountQueryService;

    @Transactional
    public MailAccount create(User user, MailAccountCreateCommand command) {
        validateSameOwnerDuplicate(
                mailAccountQueryService.findByAccountIdAndProviderAndEmailAddress(
                user.getId(),
                command.provider(),
                command.emailAddress()
        ));

        validateAnotherOwnerDuplicate(
                mailAccountQueryService.findByProviderAndEmailAddress(command.provider(), command.emailAddress()),
                user
        );

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

        MailAccount savedMailAccount = mailAccountRepositoryPort.save(mailAccount);
        validateSavedMailAccount(savedMailAccount);
        return savedMailAccount;
    }

    private void validateSameOwnerDuplicate(Optional<MailAccount> existingMailAccount) {
        if (existingMailAccount.isPresent()) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED);
        }
    }

    private void validateAnotherOwnerDuplicate(Optional<MailAccount> existingMailAccount, User user) {
        existingMailAccount
                .filter(existing -> !existing.getAccountId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED_BY_ANOTHER_USER);
                });
    }

    private void validateSavedMailAccount(MailAccount mailAccount) {
        if (mailAccount == null || mailAccount.getId() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }
}
