package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncSaveResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import com.mailsangja.worker.messaging.publisher.InitialMailSyncThreadBatchPublisher;
import com.mailsangja.worker.messaging.publisher.LabelReclassifyPublisher;
import com.mailsangja.worker.messaging.publisher.MailEmbeddingPublisher;
import com.mailsangja.worker.service.google.GmailMessageApiService;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.InitialMailSyncCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class InitialMailSyncListener {

    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final GmailMessageApiService gmailMessageApiService;
    private final InitialMailSyncThreadBatchPublisher initialMailSyncThreadBatchPublisher;
    private final InitialMailSyncCommandService initialMailSyncCommandService;
    private final GoogleMailInitialSyncProperties googleMailInitialSyncProperties;
    private final LabelQueryService labelQueryService;
    private final LabelReclassifyPublisher labelReclassifyPublisher;
    private final MailEmbeddingPublisher mailEmbeddingPublisher;

    @RabbitListener(queues = "#{@initialMailSyncQueue.name}")
    public void handle(InitialMailSyncMessage message) {
        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findSyncableMailAccountById(message.mailAccountId())
        );

        List<String> threadIds = gmailMessageApiService.getInitialThreadIds(
                mailAccount.getAccessToken()
        );

        List<List<String>> threadBatches = partitionThreadIds(threadIds);

        for (List<String> threadBatch : threadBatches) {
            initialMailSyncThreadBatchPublisher.publish(new InitialMailSyncThreadBatchMessage(
                    message.mailAccountId(),
                    message.userId(),
                    message.provider(),
                    message.emailAddress(),
                    threadBatch
            ));
        }

        log.info(
                "Prepared initial mail sync thread batches. mailAccountId={} emailAddress={} maxThreadCount={} fetchedThreadCount={} batchCount={}",
                message.mailAccountId(),
                message.emailAddress(),
                googleMailInitialSyncProperties.getMaxThreads(),
                threadIds.size(),
                threadBatches.size()
        );
    }

    @RabbitListener(
            queues = "#{@initialMailSyncThreadBatchQueue.name}",
            containerFactory = "initialMailSyncThreadBatchRabbitListenerContainerFactory"
    )
    public void handleThreadBatch(InitialMailSyncThreadBatchMessage message) {
        MailAccount mailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(
                mailAccountQueryService.findSyncableMailAccountById(message.mailAccountId())
        );

        List<InitialMailSyncThreadResult> threadResults = gmailMessageApiService.getThreads(
                mailAccount.getAccessToken(),
                message.threadIds()
        );

        List<InitialMailSyncThreadSaveCommand> commands = threadResults.stream()
                .map(InitialMailSyncThreadSaveCommand::from)
                .toList();

        InitialMailSyncSaveResult saveResult = initialMailSyncCommandService.saveThreadBatch(mailAccount, commands);
        publishEmbeddingMessages(saveResult.messageIds());

        Set<UUID> activeLabelIds = labelQueryService.findActiveLabelIdsByUserId(message.userId());
        if (!activeLabelIds.isEmpty() && !saveResult.threadIds().isEmpty()) {
            labelReclassifyPublisher.publish(message.userId(), activeLabelIds, saveResult.threadIds());
        }

        log.info(
                "Saved initial mail sync thread batch. mailAccountId={} emailAddress={} threadCount={}",
                message.mailAccountId(),
                message.emailAddress(),
                message.threadIds().size()
        );
    }

    private void publishEmbeddingMessages(List<UUID> messageIds) {
        messageIds.stream()
                .map(MailEmbeddingMessage::new)
                .forEach(mailEmbeddingPublisher::publish);
    }

    private List<List<String>> partitionThreadIds(List<String> threadIds) {
        if (threadIds.isEmpty()) {
            return List.of();
        }
        int batchSize = googleMailInitialSyncProperties.getThreadBatchSize();
        List<List<String>> batches = new ArrayList<>();
        for (int start = 0; start < threadIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, threadIds.size());
            batches.add(List.copyOf(threadIds.subList(start, end)));
        }
        return batches;
    }
}
