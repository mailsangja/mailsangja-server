package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.service.google.GoogleOAuthQueryService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class GoogleAccessTokenEnsureService {

    private static final long REFRESH_WINDOW_MINUTES = 10L;

    private final MailAccountQueryService mailAccountQueryService;
    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthQueryService googleOAuthQueryService;

    public GoogleAccessTokenEnsureService(
            MailAccountQueryService mailAccountQueryService,
            MailAccountCommandService mailAccountCommandService,
            GoogleOAuthQueryService googleOAuthQueryService
    ) {
        this.mailAccountQueryService = mailAccountQueryService;
        this.mailAccountCommandService = mailAccountCommandService;
        this.googleOAuthQueryService = googleOAuthQueryService;
    }

    public MailAccount ensureValidGoogleAccessToken(MailAccount mailAccount) {
        validateMailAccountInput(mailAccount);

        MailAccount latestMailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccount.getId());
        validateGoogleMailAccount(latestMailAccount);

        if (!needsRefresh(latestMailAccount)) {
            return latestMailAccount;
        }

        if (isBlank(latestMailAccount.getRefreshToken())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }

        return mailAccountCommandService.refreshGoogleAccessToken(
                latestMailAccount.getId(),
                googleOAuthQueryService.refreshAccessToken(latestMailAccount.getRefreshToken())
        );
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

        if (mailAccount.getAccessTokenExpiresAt() == null || isBlank(mailAccount.getAccessToken())) {
            throw new MailPushException(MailPushErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
