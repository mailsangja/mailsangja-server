package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.worker.dto.label.LabelReclassifyMessage;
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

/**
 * 라벨 재분류 MQ 메시지 수신 진입점.
 *
 * 흐름:
 * 1. 대상 라벨 ID 목록 기준으로 사용자 활성 라벨 로드
 * 2. 사용자의 활성 메시지 로드 (labels EntityGraph)
 * 3. 첨부파일 보유 메시지 ID 집합 로드 (별도 쿼리)
 * 4. LabelRuleCompiler로 메시지별 대상 라벨 적용 여부 계산
 * 5. MessageLabelCommandService로 대상 라벨만 idempotent 반영
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

    @RabbitListener(
            queues = "#{@labelReclassifyQueue.name}",
            containerFactory = "labelReclassifyRabbitListenerContainerFactory"
    )
    public void handle(LabelReclassifyMessage message) {
        UUID userId = message.userId();
        Set<UUID> targetLabelIds = message.labelIds();

        log.info("LabelReclassification started for userId={} targetLabelCount={}", userId, targetLabelIds.size());

        List<Label> activeLabels = labelQueryService.findAllActiveByUserId(userId);
        List<Label> targetLabels = activeLabels.stream()
                .filter(label -> targetLabelIds.contains(label.getId()))
                .toList();
        List<Message> messages = labelQueryService.findAllActiveMessagesWithLabelsByUserId(userId);

        if (messages.isEmpty()) {
            log.info("LabelReclassification skipped — no messages for userId={}", userId);
            return;
        }

        Set<UUID> messageIdsWithAttachments = labelQueryService.findMessageIdsWithAttachmentsByUserId(userId);

        Map<UUID, List<Label>> labelsByMessageId = labelRuleCompiler.compile(targetLabels, messages, messageIdsWithAttachments);
        messageLabelCommandService.applyLabels(messages, labelsByMessageId, targetLabelIds);

        log.info("LabelReclassification completed for userId={} targetLabelCount={} messagesProcessed={}",
                userId, targetLabelIds.size(), messages.size());
    }
}
