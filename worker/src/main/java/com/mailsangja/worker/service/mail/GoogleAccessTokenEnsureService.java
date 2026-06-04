package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.service.google.GoogleOAuthApiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class GoogleAccessTokenEnsureService {

    private static final long REFRESH_WINDOW_MINUTES = 10L;

    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthApiService googleOAuthApiService;
    private final MailAccountQueryService mailAccountQueryService;

    public GoogleAccessTokenEnsureService(
            MailAccountCommandService mailAccountCommandService,
            GoogleOAuthApiService googleOAuthApiService,
            MailAccountQueryService mailAccountQueryService
    ) {
        this.mailAccountCommandService = mailAccountCommandService;
        this.googleOAuthApiService = googleOAuthApiService;
        this.mailAccountQueryService = mailAccountQueryService;
    }

    public MailAccount ensureValidGoogleAccessToken(MailAccount mailAccount) {
        validateMailAccountInput(mailAccount);
        validateGoogleMailAccount(mailAccount);

        if (!needsRefresh(mailAccount)) {
            return mailAccount;
        }

        try {
            return mailAccountCommandService.refreshGoogleAccessToken(
                    mailAccount.getId(),
                    googleOAuthApiService.refreshAccessToken(mailAccount.getRefreshToken())
            );
        } catch (MailPushException e) {
            mailAccountCommandService.clearRefreshToken(mailAccount.getId());
            log.warn(
                    "Google access token refresh failed. mailAccountId={} userId={} provider={} emailAddress={} accessTokenExpiresAt={} hasRefreshToken={} errorCode={}",
                    mailAccount.getId(),
                    mailAccount.getUser().getId(),
                    mailAccount.getProvider(),
                    mailAccount.getEmailAddress(),
                    mailAccount.getAccessTokenExpiresAt(),
                    !isBlank(mailAccount.getRefreshToken()),
                    e.getErrorCode().getCode()
            );
            throw e;
        }
    }

    private boolean needsRefresh(MailAccount mailAccount) {
        LocalDateTime refreshThreshold = mailAccountQueryService.getKstNow().plusMinutes(REFRESH_WINDOW_MINUTES);
        return !mailAccount.getAccessTokenExpiresAt().isAfter(refreshThreshold);
    }

    private void validateMailAccountInput(MailAccount mailAccount) {
        if (mailAccount == null || mailAccount.getId() == null) {
            throw new MailPushException(MailPushErrorCode.MAIL_ACCOUNT_NOT_FOUND);
        }
    }

    private void validateGoogleMailAccount(MailAccount mailAccount) {
        if (mailAccount.getProvider() != MailProvider.GMAIL) {
            throw new MailPushException(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE);
        }

        if (!mailAccount.isActive()) {
            throw new MailPushException(MailPushErrorCode.INVALID_MAIL_ACCOUNT_STATE);
        }

        if (isBlank(mailAccount.getRefreshToken())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }

        if (mailAccount.getAccessTokenExpiresAt() == null || isBlank(mailAccount.getAccessToken())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
