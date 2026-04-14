package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import com.mailsangja.worker.service.google.GoogleMailWatchQueryService;
import com.mailsangja.worker.service.google.GoogleOAuthQueryService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailAccountFacade {

    private final MailAccountQueryService mailAccountQueryService;
    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthQueryService googleOAuthQueryService;
    private final GoogleMailWatchQueryService googleMailWatchQueryService;

    public void handleWatchRenewal(WatchRenewalMessage message) {
        validateWatchRenewalMessage(message);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(message.mailAccountId());
        GoogleOAuthTokenResult tokenResult = googleOAuthQueryService.refreshAccessToken(mailAccount.getRefreshToken());
        GoogleMailWatchResult watchResult = googleMailWatchQueryService.watch(tokenResult.accessToken());

        mailAccountCommandService.renewGoogleWatch(
                RenewGoogleWatchCommand.of(mailAccount.getId(), tokenResult, watchResult)
        );

        log.info(
                "Completed Gmail watch renewal for mailAccountId={} userId={} emailAddress={} historyId={} watchExpiresAt={}",
                message.mailAccountId(),
                message.userId(),
                message.emailAddress(),
                watchResult.historyId(),
                watchResult.expirationAt()
        );
    }

    private void validateWatchRenewalMessage(WatchRenewalMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_COMMAND);
        }

        if (!MailProvider.GMAIL.name().equals(message.provider())) {
            throw new MailPushException(MailPushErrorCode.INVALID_GMAIL_WATCH_RENEWAL_COMMAND);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
