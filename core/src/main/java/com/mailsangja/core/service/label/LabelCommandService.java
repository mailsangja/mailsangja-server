package com.mailsangja.core.service.label;

import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.LabelGroup;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.LabelGroupRepositoryPort;
import com.mailsangja.db.port.LabelRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelCommandService {

    private final LabelRepositoryPort labelRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;
    private final LabelGroupRepositoryPort labelGroupRepositoryPort;

    @Transactional
    public Label create(User user, LabelCreateRequest request) {
        NotificationPolicy policy = request.notificationPolicy() != null
                ? request.notificationPolicy()
                : NotificationPolicy.INHERIT;
        Label label = Label.builder()
                .user(user)
                .name(request.name().trim())
                .colorCode(request.colorCode())
                .notificationPolicy(policy)
                .displayOrder(request.order())
                .rule(request.rule())
                .confirmed(true)
                .isSensitive(Boolean.TRUE.equals(request.isSensitive()))
                .build();
        return labelRepositoryPort.save(label);
    }

    @Transactional
    public Label createSuggestion(User user, LabelCreateRequest request) {
        NotificationPolicy policy = request.notificationPolicy() != null
                ? request.notificationPolicy()
                : NotificationPolicy.INHERIT;
        Label label = Label.builder()
                .user(user)
                .name(request.name().trim())
                .colorCode(request.colorCode())
                .notificationPolicy(policy)
                .displayOrder(request.order())
                .rule(request.rule())
                .confirmed(false)
                .isSensitive(Boolean.TRUE.equals(request.isSensitive()))
                .build();
        return labelRepositoryPort.save(label);
    }

    @Transactional
    public Label approveSuggestion(Label suggestion, LabelCreateRequest request) {
        suggestion.updateName(request.name().trim());
        suggestion.updateColorCode(request.colorCode());
        suggestion.updateNotificationPolicy(
                request.notificationPolicy() != null ? request.notificationPolicy() : NotificationPolicy.INHERIT
        );
        suggestion.updateDisplayOrder(request.order());
        suggestion.updateRule(request.rule());
        suggestion.updateSensitive(Boolean.TRUE.equals(request.isSensitive()));
        suggestion.confirm();
        return labelRepositoryPort.save(suggestion);
    }

    @Transactional
    public Label update(Label label, LabelUpdateRequest request) {
        if (request.name() != null) {
            label.updateName(request.name().trim());
        }
        if (request.colorCode() != null) {
            label.updateColorCode(request.colorCode());
        }
        if (request.notificationPolicy() != null) {
            label.updateNotificationPolicy(request.notificationPolicy());
        }
        if (request.order() != null) {
            label.updateDisplayOrder(request.order());
        }
        if (request.isSensitive() != null) {
            label.updateSensitive(request.isSensitive());
        }
        return labelRepositoryPort.save(label);
    }

    @Transactional
    public Label updateRule(Label label, LabelRule rule) {
        label.updateRule(rule);
        return labelRepositoryPort.save(label);
    }

    @Transactional
    public void delete(Label label) {
        messageRepositoryPort.deleteMessageLabelsByLabelId(label.getId());
        List<LabelGroup> labelGroups = labelGroupRepositoryPort.findActiveGroupsByLabelId(label.getId());
        labelGroups.forEach(labelGroup -> labelGroup.deleteLabelMapping(label.getId()));
        labelGroupRepositoryPort.saveAll(labelGroups);
        label.delete();
        labelRepositoryPort.save(label);
    }
}
