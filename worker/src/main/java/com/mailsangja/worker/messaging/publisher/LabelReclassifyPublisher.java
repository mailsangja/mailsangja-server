package com.mailsangja.worker.messaging.publisher;

import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.worker.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.worker.config.properties.MailTaskRabbitProperties;
import com.mailsangja.worker.dto.label.LabelReclassifyMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassifyPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final LabelReclassifyRabbitProperties labelReclassifyRabbitProperties;
    private final MessageRepositoryPort messageRepositoryPort;

    public void publish(UUID userId, Set<UUID> labelIds) {
        List<UUID> allThreadIds = messageRepositoryPort.findActiveThreadIdsByUserId(userId);
        if (allThreadIds.isEmpty()) {
            log.info("LabelReclassify skipped — no active threads for userId={}", userId);
            return;
        }

        publishWithThreadIds(userId, labelIds, allThreadIds);
    }

    public void publish(UUID userId, Set<UUID> labelIds, List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            log.info("LabelReclassify skipped — no threads provided for userId={}", userId);
            return;
        }

        publishWithThreadIds(userId, labelIds, threadIds);
    }

    private void publishWithThreadIds(UUID userId, Set<UUID> labelIds, List<UUID> threadIds) {
        int batchSize = labelReclassifyRabbitProperties.getThreadBatchSize();
        int batchCount = 0;
        for (int start = 0; start < threadIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, threadIds.size());
            List<UUID> threadBatch = List.copyOf(threadIds.subList(start, end));
            publishBatch(new LabelReclassifyMessage(userId, labelIds, threadBatch));
            batchCount++;
        }

        log.info("Published label reclassify batches for userId={} labelCount={} totalThreads={} batchCount={}",
                userId, labelIds.size(), threadIds.size(), batchCount);
    }

    private void publishBatch(LabelReclassifyMessage message) {
        try {
            rabbitTemplate.convertAndSend(
                    mailTaskRabbitProperties.getExchange(),
                    labelReclassifyRabbitProperties.getRoutingKey(),
                    message,
                    new CorrelationData(message.userId() + ":" + message.threadIds().size())
            );
        } catch (AmqpException e) {
            log.warn("Failed to publish label reclassify batch for userId={} threadCount={}",
                    message.userId(), message.threadIds().size(), e);
            throw e;
        }
    }
}
