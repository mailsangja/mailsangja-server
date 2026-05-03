package com.mailsangja.worker.service.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.MessageLabel;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageLabelCommandService {

    private final MessageRepositoryPort messageRepositoryPort;

    /**
     * 각 메시지에 적용할 라벨 목록을 idempotent하게 반영한다.
     * targetLabelIds에 포함된 라벨만 갱신하며, 그 외 라벨은 유지한다.
     *
     * @param messages          업데이트 대상 메시지 목록 (messageLabels 컬렉션이 초기화된 상태여야 함)
     * @param labelsByMessageId 메시지 ID → 대상 라벨 중 적용할 Label 목록
     * @param targetLabelIds    재분류 대상 Label ID 목록
     */
    @Transactional
    public void applyLabels(
            List<Message> messages,
            Map<UUID, List<Label>> labelsByMessageId,
            Set<UUID> targetLabelIds
    ) {
        if (targetLabelIds == null || targetLabelIds.isEmpty()) {
            return;
        }

        List<Message> changed = new ArrayList<>();
        for (Message message : messages) {
            List<Label> targetLabels = labelsByMessageId.getOrDefault(message.getId(), List.of());

            Set<UUID> currentLabelIds = toLabelIdSet(message.getMessageLabels());
            Set<UUID> currentTargetLabelIds = currentLabelIds.stream()
                    .filter(targetLabelIds::contains)
                    .collect(HashSet::new, HashSet::add, HashSet::addAll);
            Set<UUID> desiredTargetLabelIds = toIdSet(targetLabels);

            if (!currentTargetLabelIds.equals(desiredTargetLabelIds)) {
                List<MessageLabel> newTargetEntries = targetLabels.stream()
                        .map(label -> MessageLabel.of(message, label))
                        .toList();
                message.applyTargetLabels(targetLabelIds, newTargetEntries);
                changed.add(message);
            }
        }

        if (!changed.isEmpty()) {
            messageRepositoryPort.saveAll(changed);
            log.info("Applied label reclassification to {} messages", changed.size());
        }
    }

    private Set<UUID> toLabelIdSet(List<MessageLabel> messageLabels) {
        if (messageLabels == null || messageLabels.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (MessageLabel ml : messageLabels) {
            ids.add(ml.getLabel().getId());
        }
        return ids;
    }

    private Set<UUID> toIdSet(List<Label> labels) {
        if (labels == null || labels.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new HashSet<>();
        for (Label label : labels) {
            ids.add(label.getId());
        }
        return ids;
    }
}
