package com.mailsangja.core.service.label;

import com.mailsangja.core.dto.label.LabelCreateRequest;
import com.mailsangja.core.dto.label.LabelUpdateRequest;
import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.LabelRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LabelCommandService {

    private final LabelRepositoryPort labelRepositoryPort;
    // ⚠️ 라벨 삭제 유스케이스에서 message/thread 라벨 매핑 삭제가 필요해 cross-domain Repository를 함께 참조한다.
    private final MessageRepositoryPort messageRepositoryPort;
    private final ThreadRepositoryPort threadRepositoryPort;

    @Transactional
    public Label create(User user, LabelCreateRequest request) {
        Label label = Label.builder()
                .user(user)
                .name(request.name().trim())
                .colorCode(request.colorCode())
                .notificationEnabled(request.notificationEnabled())
                .displayOrder(request.order())
                .rule(request.rule())
                .build();
        return labelRepositoryPort.save(label);
    }

    @Transactional
    public Label update(Label label, LabelUpdateRequest request) {
        if (request.name() != null) {
            label.updateName(request.name().trim());
        }
        if (request.colorCode() != null) {
            label.updateColorCode(request.colorCode());
        }
        if (request.notificationEnabled() != null) {
            label.updateNotificationEnabled(request.notificationEnabled());
        }
        if (request.order() != null) {
            label.updateDisplayOrder(request.order());
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
        threadRepositoryPort.deleteThreadLabelsByLabelId(label.getId());
        label.delete();
        labelRepositoryPort.save(label);
    }
}
