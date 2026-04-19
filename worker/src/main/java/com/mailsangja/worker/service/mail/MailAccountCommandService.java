package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountQueryService mailAccountQueryService;

    @Transactional
    public void updateSyncHistoryId(MailAccount mailAccount, String syncHistoryId) {
        validateSyncHistoryUpdate(mailAccount, syncHistoryId);

        mailAccount.updateSyncHistoryId(syncHistoryId);
    }

    private void validateSyncHistoryUpdate(MailAccount mailAccount, String syncHistoryId) {
        if (mailAccount == null || isBlank(syncHistoryId)) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }
    }

    @Transactional
    public void renewGoogleWatch(RenewGoogleWatchCommand command) {
        validateRenewGoogleWatchCommand(command);

        UUID mailAccountId = command.mailAccountId();
        GoogleOAuthTokenResult tokenResult = command.tokenResult();
        GoogleMailWatchResult watchResult = command.watchResult();

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccountId);
        mailAccount.updateAccessToken(tokenResult.accessToken());
        mailAccount.updateAccessTokenExpiresAt(mailAccountQueryService.getKstNow().plusSeconds(tokenResult.expiresIn()));
        if (!isBlank(tokenResult.refreshToken())) {
            mailAccount.updateRefreshToken(tokenResult.refreshToken());
        }
        mailAccount.updateSyncHistoryId(watchResult.historyId());
        mailAccount.updateWatchExpiresAt(watchResult.expirationAt());
    }

    @Transactional
    public MailAccount refreshGoogleAccessToken(UUID mailAccountId, GoogleOAuthTokenResult tokenResult) {
        validateRefreshGoogleAccessTokenInput(mailAccountId, tokenResult);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccountId);
        validateRefreshableGoogleMailAccount(mailAccount);

        mailAccount.updateAccessToken(tokenResult.accessToken());
        mailAccount.updateAccessTokenExpiresAt(mailAccountQueryService.getKstNow().plusSeconds(tokenResult.expiresIn()));
        if (!isBlank(tokenResult.refreshToken())) {
            mailAccount.updateRefreshToken(tokenResult.refreshToken());
        }

        return mailAccount;
    }

    private void validateRenewGoogleWatchCommand(RenewGoogleWatchCommand command) {
        if (command == null
                || command.mailAccountId() == null
                || command.tokenResult() == null
                || command.watchResult() == null) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }

        validateGoogleOAuthTokenResult(command.tokenResult());
        validateGoogleMailWatchResult(command.watchResult());
    }

    private void validateRefreshGoogleAccessTokenInput(UUID mailAccountId, GoogleOAuthTokenResult tokenResult) {
        if (mailAccountId == null
                || tokenResult == null
                || isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private void validateRefreshableGoogleMailAccount(MailAccount mailAccount) {
        if (mailAccount.getProvider() != MailProvider.GMAIL) {
            throw new MailPushException(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE);
        }

        if (isBlank(mailAccount.getRefreshToken())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }
    }

    private void validateGoogleOAuthTokenResult(GoogleOAuthTokenResult tokenResult) {
        if (isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }
    }

    private void validateGoogleMailWatchResult(GoogleMailWatchResult watchResult) {
        if (isBlank(watchResult.historyId())
                || watchResult.expirationAt() == null) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
