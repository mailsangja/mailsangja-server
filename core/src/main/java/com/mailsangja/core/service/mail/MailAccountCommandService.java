package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.dto.mail.GoogleMailAccountResult;
import com.mailsangja.core.dto.mail.GoogleOAuthTokenResult;
import com.mailsangja.core.dto.mail.GoogleMailWatchResult;
import com.mailsangja.core.dto.mail.MailAccountCreateCommand;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;
    private final MailAccountQueryService mailAccountQueryService;

    public void validateGoogleMailAccountCreation(User user, GoogleMailAccountResult result) {
        validateGoogleMailAccountResult(result);

        validateSameOwnerDuplicate(
                mailAccountQueryService.findByUserIdAndProviderAndEmailAddress(
                        user.getId(),
                        MailProvider.GMAIL,
                        result.emailAddress()
                )
        );

        validateAnotherOwnerDuplicate(
                mailAccountQueryService.findByProviderAndEmailAddress(MailProvider.GMAIL, result.emailAddress()),
                user
        );
    }

    @Transactional
    public MailAccount createGoogleMailAccount(
            User user,
            GoogleMailAccountResult result,
            String alias,
            String icon,
            String color,
            GoogleMailWatchResult watchResult
    ) {
        MailAccountCreateCommand command = MailAccountCreateCommand.from(
                MailProvider.GMAIL,
                result,
                alias,
                icon,
                color,
                watchResult
        );
        validateCreateCommand(command);

        MailAccount mailAccount = MailAccount.builder()
                .user(user)
                .provider(command.provider())
                .emailAddress(command.emailAddress())
                .alias(command.alias())
                .icon(command.icon())
                .color(command.color())
                .accessToken(command.accessToken())
                .accessTokenExpiresAt(command.accessTokenExpiresAt())
                .refreshToken(command.refreshToken())
                .active(true)
                .syncHistoryId(command.syncHistoryId())
                .watchExpiresAt(command.watchExpiresAt())
                .build();

        MailAccount savedMailAccount = mailAccountRepositoryPort.save(mailAccount);
        validateSavedMailAccount(savedMailAccount);
        return savedMailAccount;
    }

    @Transactional
    public MailAccount refreshGoogleAccessToken(UUID mailAccountId, GoogleOAuthTokenResult tokenResult) {
        validateRefreshGoogleAccessTokenInput(mailAccountId, tokenResult);

        MailAccount mailAccount = mailAccountQueryService.findActiveById(mailAccountId);
        validateRefreshableGoogleMailAccount(mailAccount);

        mailAccount.updateAccessToken(tokenResult.accessToken());
        mailAccount.updateAccessTokenExpiresAt(mailAccountQueryService.getKstNow().plusSeconds(tokenResult.expiresIn()));
        if (!isBlank(tokenResult.refreshToken())) {
            mailAccount.updateRefreshToken(tokenResult.refreshToken());
        }

        return mailAccount;
    }

    private void validateGoogleMailAccountResult(GoogleMailAccountResult result) {
        if (result == null
                || isBlank(result.emailAddress())
                || isBlank(result.accessToken())
                || result.accessTokenExpiresAt() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }

        if (isBlank(result.refreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
    }

    private void validateCreateCommand(MailAccountCreateCommand command) {
        if (command.provider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (isBlank(command.emailAddress())
                || isBlank(command.alias())
                || isBlank(command.icon())
                || isBlank(command.color())
                || isBlank(command.accessToken())
                || command.accessTokenExpiresAt() == null
                || isBlank(command.syncHistoryId())
                || command.watchExpiresAt() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }

        if (isBlank(command.refreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
    }

    private void validateRefreshGoogleAccessTokenInput(UUID mailAccountId, GoogleOAuthTokenResult tokenResult) {
        if (mailAccountId == null
                || tokenResult == null
                || isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private void validateRefreshableGoogleMailAccount(MailAccount mailAccount) {
        if (mailAccount.getProvider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (isBlank(mailAccount.getRefreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
    }

    private void validateSameOwnerDuplicate(Optional<MailAccount> existingMailAccount) {
        if (existingMailAccount.isPresent()) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED);
        }
    }

    private void validateAnotherOwnerDuplicate(Optional<MailAccount> existingMailAccount, User user) {
        existingMailAccount
                .filter(existing -> !existing.getUser().getId().equals(user.getId()))
                .ifPresent(existing -> {
                    throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED_BY_ANOTHER_USER);
                });
    }

    private void validateSavedMailAccount(MailAccount mailAccount) {
        if (mailAccount == null || mailAccount.getId() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
