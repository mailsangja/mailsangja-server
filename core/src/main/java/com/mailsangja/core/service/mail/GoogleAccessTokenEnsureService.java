package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailAccountErrorCode;
import com.mailsangja.core.common.exception.mail.MailAccountException;
import com.mailsangja.core.service.google.GoogleOAuthQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
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
        validateGoogleMailAccount(mailAccount);

        if (!needsRefresh(mailAccount)) {
            return mailAccount;
        }

        if (isBlank(mailAccount.getRefreshToken())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_REFRESH_TOKEN_MISSING);
        }

        return mailAccountCommandService.refreshGoogleAccessToken(
                mailAccount.getId(),
                googleOAuthQueryService.refreshAccessToken(mailAccount.getRefreshToken())
        );
    }

    private boolean needsRefresh(MailAccount mailAccount) {
        LocalDateTime refreshThreshold = mailAccountQueryService.getKstNow().plusMinutes(REFRESH_WINDOW_MINUTES);
        return !mailAccount.getAccessTokenExpiresAt().isAfter(refreshThreshold);
    }

    private void validateMailAccountInput(MailAccount mailAccount) {
        if (mailAccount == null || mailAccount.getId() == null) {
            throw new MailAccountException(MailAccountErrorCode.MAIL_ACCOUNT_NOT_FOUND);
        }
    }

    private void validateGoogleMailAccount(MailAccount mailAccount) {
        if (mailAccount.getProvider() != MailProvider.GMAIL) {
            throw new MailAccountException(MailAccountErrorCode.UNSUPPORTED_MAIL_PROVIDER);
        }

        if (mailAccount.getAccessTokenExpiresAt() == null) {
            throw new MailAccountException(MailAccountErrorCode.INVALID_OAUTH_RESULT);
        }

        if (isBlank(mailAccount.getAccessToken())) {
            throw new MailAccountException(MailAccountErrorCode.GOOGLE_TOKEN_REFRESH_FAILED);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
