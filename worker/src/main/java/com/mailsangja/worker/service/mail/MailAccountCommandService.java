package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.GoogleMailWatchResult;
import com.mailsangja.worker.dto.gmail.GoogleOAuthTokenResult;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MailAccountCommandService {

    private final MailAccountQueryService mailAccountQueryService;

    @Transactional
    public void updateSyncHistoryId(UUID mailAccountId, String syncHistoryId) {
        if (mailAccountId == null || isBlank(syncHistoryId)) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_PUSH_NOTIFICATION);
        }

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccountId);
        mailAccount.updateSyncHistoryId(syncHistoryId);
    }

    @Transactional
    public void renewGoogleWatch(UUID mailAccountId, GoogleOAuthTokenResult tokenResult, GoogleMailWatchResult watchResult) {
        if (mailAccountId == null
                || tokenResult == null
                || watchResult == null
                || isBlank(tokenResult.accessToken())
                || tokenResult.expiresIn() == null
                || tokenResult.expiresIn() <= 0
                || isBlank(watchResult.historyId())
                || watchResult.expirationAt() == null) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_REQUEST);
        }

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(mailAccountId);
        mailAccount.updateAccessToken(tokenResult.accessToken());
        mailAccount.updateAccessTokenExpiresAt(mailAccountQueryService.getKstNow().plusSeconds(tokenResult.expiresIn()));
        if (!isBlank(tokenResult.refreshToken())) {
            mailAccount.updateRefreshToken(tokenResult.refreshToken());
        }
        mailAccount.updateSyncHistoryId(watchResult.historyId());
        mailAccount.updateWatchExpiresAt(watchResult.expirationAt());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
