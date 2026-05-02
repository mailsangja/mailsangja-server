package com.mailsangja.core.service.label;

import com.mailsangja.core.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import com.mailsangja.core.dto.label.LabelReclassifyMessage;
import com.mailsangja.db.port.MessageRepositoryPort;
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

        int batchSize = labelReclassifyRabbitProperties.getThreadBatchSize();
        int batchCount = 0;
        for (int start = 0; start < allThreadIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, allThreadIds.size());
            List<UUID> threadBatch = List.copyOf(allThreadIds.subList(start, end));
            publishBatch(new LabelReclassifyMessage(userId, labelIds, threadBatch));
            batchCount++;
        }

        log.info("Published label reclassify batches for userId={} labelCount={} totalThreads={} batchCount={}",
                userId, labelIds.size(), allThreadIds.size(), batchCount);
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
