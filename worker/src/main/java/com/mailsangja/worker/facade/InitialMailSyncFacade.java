package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncCommand;
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

    public void handleInitialMailSync(InitialMailSyncCommand command) {
        validateCommand(command);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(command.mailAccountId());
        GoogleMailMessageListResult result = googleMailMessageQueryService.getLatestMessages(mailAccount.getAccessToken());

        log.info(
                "Completed initial mail sync for mailAccountId={} userId={} emailAddress={} fetchedCount={} resultSizeEstimate={}",
                command.mailAccountId(),
                command.userId(),
                command.emailAddress(),
                result.fetchedCount(),
                result.resultSizeEstimate()
        );
    }

    private void validateCommand(InitialMailSyncCommand command) {
        if (command == null
                || command.mailAccountId() == null
                || command.userId() == null
                || isBlank(command.provider())
                || isBlank(command.emailAddress())) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        if (!MailProvider.GMAIL.name().equals(command.provider())) {
            throw new MailPushException(MailPushErrorCode.UNSUPPORTED_INITIAL_MAIL_SYNC_PROVIDER);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
