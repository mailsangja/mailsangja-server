package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.worker.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.worker.dto.label.LabelReclassifyMessage;
import com.mailsangja.worker.dto.label.MessageBatch;
import com.mailsangja.worker.handler.label.LabelRuleCompiler;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.label.MessageLabelCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 라벨 재분류 MQ 메시지 수신 진입점.
 *
 * 흐름:
 * 1. Publisher가 스레드 ID를 threadBatchSize 단위로 나눠 이 큐에 N개 메시지로 발행한다.
 * 2. 각 메시지에 포함된 threadIds 기준으로 메시지를 로드하고, 라벨 규칙을 컴파일하여 적용한다.
 * 3. Thread 단위로 GmailThreadLock을 획득한 뒤 라벨을 반영한다.
 *
 * 재시도 / DLQ 정책은 labelReclassifyRabbitListenerContainerFactory에 위임한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassificationListener {

    private final LabelQueryService labelQueryService;
    private final MessageLabelCommandService messageLabelCommandService;
    private final LabelRuleCompiler labelRuleCompiler;
    private final LabelReclassifyRabbitProperties labelReclassifyRabbitProperties;

    @RabbitListener(
            queues = "#{@labelReclassifyQueue.name}",
            containerFactory = "labelReclassifyRabbitListenerContainerFactory"
    )
    public void handle(LabelReclassifyMessage message) {
        UUID userId = message.userId();
        Set<UUID> targetLabelIds = message.labelIds();
        List<UUID> threadIds = message.threadIds();

        List<Label> activeLabels = labelQueryService.findAllActiveByUserId(userId);
        List<Label> targetLabels = activeLabels.stream()
                .filter(label -> targetLabelIds.contains(label.getId()))
                .toList();

        if (targetLabels.isEmpty()) {
            log.info("LabelReclassification skipped — no matching active labels: userId={}", userId);
            return;
        }

        List<Message> messages = labelQueryService.findActiveMessagesWithLabelsByThreadIds(threadIds);

        if (messages.isEmpty()) {
            return;
        }

        Set<UUID> messageIdsWithAttachments = labelQueryService.findMessageIdsWithAttachmentsByMessages(messages);
        MessageBatch batch = new MessageBatch(messages, messageIdsWithAttachments);
        Map<UUID, List<Label>> labelsByMessageId = labelRuleCompiler.compile(targetLabels, batch);

        applyLabelsPerThread(messages, labelsByMessageId, targetLabelIds);

        log.info("LabelReclassification batch processed: userId={} threadCount={} messageCount={}",
                userId, threadIds.size(), messages.size());
    }

    private void applyLabelsPerThread(
            List<Message> messages,
            Map<UUID, List<Label>> labelsByMessageId,
            Set<UUID> targetLabelIds
    ) {
        Map<String, List<Message>> byThread = messages.stream()
                .collect(Collectors.groupingBy(m ->
                        m.getThread().getMailAccount().getId() + ":" + m.getThread().getGmailThreadId()
                ));

        for (List<Message> threadMessages : byThread.values()) {
            MailAccount mailAccount = threadMessages.get(0).getThread().getMailAccount();
            String gmailThreadId = threadMessages.get(0).getThread().getGmailThreadId();
            try {
                messageLabelCommandService.applyLabelsWithLock(
                        mailAccount, gmailThreadId, threadMessages, labelsByMessageId, targetLabelIds
                );
            } catch (Exception e) {
                log.warn("Failed to apply labels for gmailThreadId={} mailAccountId={}, skipping thread",
                        gmailThreadId, mailAccount.getId(), e);
            }
        }
    }
}
