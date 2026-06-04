package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.dto.gmail.oauth.GoogleOAuthTokenResult;
import com.mailsangja.worker.dto.gmail.watch.GoogleMailWatchResult;
import com.mailsangja.worker.dto.mail.watch.RenewGoogleWatchCommand;
import com.mailsangja.worker.dto.mail.watch.WatchRenewalMessage;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.service.google.GmailWatchApiService;
import com.mailsangja.worker.service.google.GoogleOAuthApiService;
import com.mailsangja.worker.service.mail.MailAccountCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class GmailWatchRenewalListener {

    private final MailAccountQueryService mailAccountQueryService;
    private final MailAccountCommandService mailAccountCommandService;
    private final GoogleOAuthApiService googleOAuthApiService;
    private final GmailWatchApiService gmailWatchApiService;

    @RabbitListener(
            queues = "#{@watchRenewalQueue.name}",
            containerFactory = "watchRenewalRabbitListenerContainerFactory"
    )
    public void handle(WatchRenewalMessage message, Message rawMessage) {
        handle(message);
    }

    public void handle(WatchRenewalMessage message) {
        MailAccount mailAccount = mailAccountQueryService.findSyncableMailAccountById(message.mailAccountId());
        GoogleOAuthTokenResult tokenResult = refreshAccessToken(mailAccount);
        GoogleMailWatchResult watchResult = gmailWatchApiService.watch(tokenResult.accessToken());

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

    private GoogleOAuthTokenResult refreshAccessToken(MailAccount mailAccount) {
        try {
            return googleOAuthApiService.refreshAccessToken(mailAccount.getRefreshToken());
        } catch (MailPushException e) {
            mailAccountCommandService.clearRefreshToken(mailAccount.getId());
            throw e;
        }
    }
}
