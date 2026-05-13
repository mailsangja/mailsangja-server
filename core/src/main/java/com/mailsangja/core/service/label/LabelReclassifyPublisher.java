package com.mailsangja.core.service.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.core.config.properties.MailTaskRabbitProperties;
import com.mailsangja.core.dto.label.LabelReclassifyMessage;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class LabelReclassifyPublisher {

    private static final String LATEST_JOB_ID_KEY_PREFIX = "LabelReclassify:latestJobId:";
    private static final Duration LATEST_JOB_ID_TTL = Duration.ofDays(1);

    private final RabbitTemplate rabbitTemplate;
    private final MailTaskRabbitProperties mailTaskRabbitProperties;
    private final LabelReclassifyRabbitProperties labelReclassifyRabbitProperties;
    private final MessageRepositoryPort messageRepositoryPort;
    private final StringRedisTemplate stringRedisTemplate;

    public void publish(UUID userId, Set<UUID> labelIds, String jobId) {
        for (UUID labelId : labelIds) {
            stringRedisTemplate.opsForValue().set(
                    LATEST_JOB_ID_KEY_PREFIX + labelId,
                    jobId,
                    LATEST_JOB_ID_TTL
            );
        }

        List<UUID> allThreadIds = messageRepositoryPort.findActiveThreadIdsByUserId(userId);
        if (allThreadIds.isEmpty()) {
            log.info("LabelReclassify skipped — no active threads for userId={}", userId);
            return;
        }

        int batchSize = labelReclassifyRabbitProperties.getThreadBatchSize();
        if (batchSize <= 0) {
            throw new LabelException(LabelErrorCode.LABEL_RECLASSIFY_INVALID_CONFIG);
        }
        int batchCount = 0;
        for (int start = 0; start < allThreadIds.size(); start += batchSize) {
            int end = Math.min(start + batchSize, allThreadIds.size());
            List<UUID> threadBatch = List.copyOf(allThreadIds.subList(start, end));
            publishBatch(new LabelReclassifyMessage(userId, labelIds, threadBatch, jobId));
            batchCount++;
        }

        log.info("Published label reclassify batches for userId={} labelCount={} totalThreads={} batchCount={} jobId={}",
                userId, labelIds.size(), allThreadIds.size(), batchCount, jobId);
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
