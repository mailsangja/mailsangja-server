package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncMessage;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialMailSyncFacade {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;

    public void handleInitialMailSync(InitialMailSyncMessage message) {
        validateMessage(message);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(message.mailAccountId());
        GoogleMailMessageListResult result = googleMailMessageQueryService.getLatestMessages(mailAccount.getAccessToken());

        log.info(
                "Completed initial mail sync for mailAccountId={} userId={} emailAddress={} fetchedCount={} resultSizeEstimate={}",
                message.mailAccountId(),
                message.userId(),
                message.emailAddress(),
                result.fetchedCount(),
                result.resultSizeEstimate()
        );
    }

    private void validateMessage(InitialMailSyncMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        if (!MailProvider.GMAIL.name().equals(message.provider())) {
            throw new MailPushException(MailPushErrorCode.UNSUPPORTED_INITIAL_MAIL_SYNC_PROVIDER);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
