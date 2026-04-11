package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushErrorCode;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.InitialMailSyncMessage;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import com.mailsangja.worker.service.mail.InitialMailSyncCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import com.mailsangja.worker.service.messaging.MailTaskPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialMailSyncFacade {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleMailMessageQueryService googleMailMessageQueryService;
    private final MailTaskPublisherService mailTaskPublisherService;
    private final GoogleMailInitialSyncProperties googleMailInitialSyncProperties;
    private final InitialMailSyncCommandService initialMailSyncCommandService;

    public void handleInitialMailSync(InitialMailSyncMessage message) {
        validateMessage(message);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(message.mailAccountId());
        GoogleMailMessageListResult result = googleMailMessageQueryService.getLatestMessages(mailAccount.getAccessToken());
        List<String> threadIds = extractThreadIds(result);
        List<List<String>> threadBatches = partitionThreadIds(threadIds);

        for (List<String> threadBatch : threadBatches) {
            mailTaskPublisherService.publishInitialMailSyncThreadBatch(new InitialMailSyncThreadBatchMessage(
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress(),
                    threadBatch
            ));
        }

        log.info(
                "Prepared initial mail sync thread batches for mailAccountId={} userId={} emailAddress={} fetchedCount={} uniqueThreadCount={} publishedBatchCount={} resultSizeEstimate={}",
                message.mailAccountId(),
                message.userId(),
                message.emailAddress(),
                result.fetchedCount(),
                threadIds.size(),
                threadBatches.size(),
                result.resultSizeEstimate()
        );
    }

    public void handleInitialMailSyncThreadBatch(InitialMailSyncThreadBatchMessage message) {
        validateThreadBatchMessage(message);

        MailAccount mailAccount = mailAccountQueryService.findActiveMailAccountById(message.mailAccountId());
        List<InitialMailSyncThreadResult> threadResults = googleMailMessageQueryService.getThreads(
                mailAccount.getAccessToken(),
                message.threadIds()
        );
        List<InitialMailSyncThreadSaveCommand> commands = threadResults.stream()
                .map(InitialMailSyncThreadSaveCommand::from)
                .toList();

        initialMailSyncCommandService.saveThreadBatch(mailAccount, commands);

        log.info(
                "Saved initial mail sync thread batch for mailAccountId={} userId={} emailAddress={} threadCount={}",
                message.mailAccountId(),
                message.userId(),
                message.emailAddress(),
                message.threadIds().size()
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

    private void validateThreadBatchMessage(InitialMailSyncThreadBatchMessage message) {
        if (message == null
                || message.mailAccountId() == null
                || message.userId() == null
                || isBlank(message.provider())
                || isBlank(message.emailAddress())
                || message.threadIds() == null
                || message.threadIds().isEmpty()
                || message.threadIds().stream().anyMatch(this::isBlank)) {
            throw new MailPushException(MailPushErrorCode.INVALID_INITIAL_MAIL_SYNC_COMMAND);
        }

        if (!MailProvider.GMAIL.name().equals(message.provider())) {
            throw new MailPushException(MailPushErrorCode.UNSUPPORTED_INITIAL_MAIL_SYNC_PROVIDER);
        }
    }

    private List<String> extractThreadIds(GoogleMailMessageListResult result) {
        if (result == null || result.messages() == null) {
            return List.of();
        }

        LinkedHashSet<String> deduplicatedThreadIds = new LinkedHashSet<>();
        result.messages().stream()
                .filter(message -> message != null && !isBlank(message.threadId()))
                .forEach(message -> deduplicatedThreadIds.add(message.threadId()));
        return List.copyOf(deduplicatedThreadIds);
    }

    private List<List<String>> partitionThreadIds(List<String> threadIds) {
        if (threadIds.isEmpty()) {
            return List.of();
        }

        int threadBatchSize = googleMailInitialSyncProperties.getThreadBatchSize();
        if (threadBatchSize <= 0) {
            throw new MailPushException(MailPushErrorCode.GMAIL_MESSAGES_FETCH_FAILED);
        }

        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < threadIds.size(); start += threadBatchSize) {
            int end = Math.min(start + threadBatchSize, threadIds.size());
            batches.add(List.copyOf(threadIds.subList(start, end)));
        }
        return batches;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
