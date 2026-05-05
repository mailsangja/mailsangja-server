package com.mailsangja.core.service.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.LabelGroup;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.LabelGroupRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelGroupCommandService {

    private final LabelGroupRepositoryPort labelGroupRepositoryPort;

    @Transactional
    public LabelGroup create(User user, String name, int order, List<Label> labels) {
        LabelGroup labelGroup = LabelGroup.builder()
                .user(user)
                .name(name.trim())
                .displayOrder(order)
                .build();
        LabelGroup saved = labelGroupRepositoryPort.save(labelGroup);
        saved.replaceLabels(labels);
        return labelGroupRepositoryPort.save(saved);
    }

    @Transactional
    public LabelGroup update(LabelGroup labelGroup, String name, Integer order, List<Label> labels) {
        if (name != null) {
            labelGroup.updateName(name.trim());
        }
        if (order != null) {
            labelGroup.updateDisplayOrder(order);
        }
        if (labels != null) {
            labelGroup.replaceLabels(labels);
        }
        return labelGroupRepositoryPort.save(labelGroup);
    }

    @Transactional
    public void delete(LabelGroup labelGroup) {
        labelGroup.deleteWithLabels();
        labelGroupRepositoryPort.save(labelGroup);
    }
}
