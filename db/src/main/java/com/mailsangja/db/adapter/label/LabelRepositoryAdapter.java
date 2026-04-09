package com.mailsangja.db.adapter.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.module.label.LabelJpaRepositoryModule;
import com.mailsangja.db.port.LabelRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    public Optional<Label> findByIdAndDeletedAtIsNull(UUID id) {
        return labelJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId) {
        return labelJpaRepositoryModule.findByIdAndUserIdAndDeletedAtIsNull(id, userId);
    }
}
