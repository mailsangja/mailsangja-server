package com.mailsangja.db.adapter.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.module.label.LabelJpaRepositoryModule;
import com.mailsangja.db.port.LabelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class LabelRepositoryAdapter implements LabelRepositoryPort {

    private final LabelJpaRepositoryModule labelJpaRepositoryModule;

    @Override
    public Label save(Label label) {
        return labelJpaRepositoryModule.save(label);
    }

    @Override
    public List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId) {
        return labelJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNull(userId);
    }

    @Override
    public List<Label> findAllByUserIdAndIdInAndDeletedAtIsNull(UUID userId, List<UUID> ids) {
        return labelJpaRepositoryModule.findAllByUserIdAndIdInAndDeletedAtIsNull(userId, ids);
    }

    @Override
    public List<Label> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(UUID userId) {
        return labelJpaRepositoryModule.findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(userId);
    }

    @Override
    public Optional<Label> findByIdAndDeletedAtIsNull(UUID id) {
        return labelJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId) {
        return labelJpaRepositoryModule.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name) {
        return labelJpaRepositoryModule.existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(userId, name);
    }

    @Override
    public boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String name, UUID excludeId) {
        return labelJpaRepositoryModule.existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(userId, name, excludeId);
    }

    @Override
    public Map<UUID, Long> findUnreadThreadCountsByUserId(UUID userId) {
        return labelJpaRepositoryModule.findUnreadThreadCountsByUserId(userId)
                .stream()
                .collect(Collectors.toMap(
                        p -> p.getLabelId(),
                        p -> p.getUnreadCount()
                ));
    }
}
