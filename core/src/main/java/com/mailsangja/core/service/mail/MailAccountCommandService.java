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

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountRepositoryPort mailAccountRepositoryPort;
    private final Clock clock;

    public void validateGoogleMailAccountCreation(User user, GoogleMailAccountResult result) {
        validateGoogleMailAccountResult(result);

        validateSameOwnerDuplicate(
                mailAccountRepositoryPort.findByUserIdAndProviderAndEmailAddressAndDeletedAtIsNull(
                        user.getId(),
                        MailProvider.GMAIL,
                        result.emailAddress()
                )
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
    public MailAccount reauthorizeGoogleMailAccount(
            User user,
            UUID mailAccountId,
            GoogleMailAccountResult result,
            GoogleMailWatchResult watchResult
    ) {
        validateGoogleMailAccountResult(result);

        MailAccount mailAccount = findById(mailAccountId);
        validateOwnership(mailAccount, user);
        validateReauthorizableGoogleMailAccount(mailAccount, result);
        validateReauthorizationWatchResult(watchResult);

        mailAccount.reauthorizeGoogle(
                result.accessToken(),
                result.accessTokenExpiresAt(),
                result.refreshToken(),
                watchResult.historyId(),
                watchResult.expirationAt()
        );

        return mailAccount;
    }

    @Transactional
    public MailAccount refreshGoogleAccessToken(UUID mailAccountId, GoogleOAuthTokenResult tokenResult) {
        validateRefreshGoogleAccessTokenInput(mailAccountId, tokenResult);

        MailAccount mailAccount = findActiveById(mailAccountId);
        validateRefreshableGoogleMailAccount(mailAccount);
        String updatedRefreshToken = isBlank(tokenResult.refreshToken())
                ? mailAccount.getRefreshToken()
                : tokenResult.refreshToken();

        mailAccountRepositoryPort.updateGoogleTokenIfAccessTokenMatches(
                mailAccountId,
                mailAccount.getAccessToken(),
                tokenResult.accessToken(),
                LocalDateTime.now(clock).plusSeconds(tokenResult.expiresIn()),
                updatedRefreshToken
        );

        return findActiveById(mailAccountId);
    }

    @Transactional
    public void clearRefreshToken(UUID mailAccountId) {
        if (mailAccountId == null) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND);
        }

        mailAccountRepositoryPort.clearRefreshToken(mailAccountId);
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

    private void validateReauthorizableGoogleMailAccount(MailAccount mailAccount, GoogleMailAccountResult result) {
        if (mailAccount.getProvider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (!mailAccount.getEmailAddress().equalsIgnoreCase(result.emailAddress())) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_REAUTHORIZATION_EMAIL_MISMATCH);
        }

        if (!isBlank(mailAccount.getRefreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_REAUTHORIZATION_NOT_REQUIRED);
        }
    }

    private void validateReauthorizationWatchResult(GoogleMailWatchResult watchResult) {
        if (watchResult == null
                || isBlank(watchResult.historyId())
                || watchResult.expirationAt() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }
    }

    @Transactional
    public void updateMailAccountAppearance(User user, UUID mailAccountId, String alias, String icon, String color) {
        MailAccount mailAccount = findById(mailAccountId);
        validateOwnership(mailAccount, user);

        if (alias != null && !alias.isBlank()) mailAccount.updateAlias(alias);
        if (icon != null && !icon.isBlank()) mailAccount.updateIcon(icon);
        if (color != null && !color.isBlank()) mailAccount.updateColor(color);
    }

    @Transactional
    public void activateMailAccount(User user, UUID mailAccountId) {
        MailAccount mailAccount = findById(mailAccountId);
        validateOwnership(mailAccount, user);
        mailAccount.activate();
    }

    @Transactional
    public void deactivateMailAccount(User user, UUID mailAccountId) {
        MailAccount mailAccount = findById(mailAccountId);
        validateOwnership(mailAccount, user);
        mailAccount.deactivate();
    }

    @Transactional
    public void deleteMailAccount(User user, UUID mailAccountId) {
        MailAccount mailAccount = findById(mailAccountId);
        validateOwnership(mailAccount, user);
        mailAccount.deactivate();
        mailAccount.delete();
    }

    private MailAccount findById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    private MailAccount findActiveById(UUID id) {
        return mailAccountRepositoryPort.findByIdAndActiveAndDeletedAtIsNull(id, true)
                .orElseThrow(() -> new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND));
    }

    private void validateOwnership(MailAccount mailAccount, User user) {
        if (!mailAccount.getUser().getId().equals(user.getId())) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ACCESS_DENIED);
        }
    }

    private void validateSameOwnerDuplicate(Optional<MailAccount> existingMailAccount) {
        if (existingMailAccount.isPresent()) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_ALREADY_CONNECTED);
        }
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
