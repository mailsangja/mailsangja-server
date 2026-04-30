package com.mailsangja.core.service.label;

import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.port.LabelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LabelQueryService {

    private final LabelRepositoryPort labelRepositoryPort;

    public List<Label> findAllActiveByUserId(UUID userId) {
        return labelRepositoryPort.findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(userId);
    }

    public Label findActiveByIdAndUserId(UUID labelId, UUID userId) {
        return labelRepositoryPort.findByIdAndUserIdAndDeletedAtIsNull(labelId, userId)
                .orElseThrow(() -> new LabelException(LabelErrorCode.LABEL_NOT_FOUND));
    }

    public Map<UUID, Long> findUnreadThreadCountsByUserId(UUID userId) {
        return labelRepositoryPort.findUnreadThreadCountsByUserId(userId);
    }

    public boolean existsByUserIdAndName(UUID userId, String name) {
        return labelRepositoryPort.existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name);
    }

    public boolean existsByUserIdAndNameExcludingId(UUID userId, String name, UUID excludeId) {
        return labelRepositoryPort.existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(userId, name, excludeId);
    }
}
