package com.mailsangja.db.adapter.label;

import com.mailsangja.db.entity.label.LabelGroup;
import com.mailsangja.db.module.label.LabelGroupJpaRepositoryModule;
import com.mailsangja.db.port.LabelGroupRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LabelGroupRepositoryAdapter implements LabelGroupRepositoryPort {

    private final LabelGroupJpaRepositoryModule labelGroupJpaRepositoryModule;

    @Override
    public LabelGroup save(LabelGroup labelGroup) {
        return labelGroupJpaRepositoryModule.save(labelGroup);
    }

    @Override
    public List<LabelGroup> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(UUID userId) {
        return labelGroupJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(userId);
    }

    @Override
    public Optional<LabelGroup> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId) {
        return labelGroupJpaRepositoryModule.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name) {
        return labelGroupJpaRepositoryModule.existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String name, UUID excludeId) {
        return labelGroupJpaRepositoryModule.existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(userId, name, excludeId);
    }
}
