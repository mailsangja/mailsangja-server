package com.mailsangja.db.port;

import com.mailsangja.db.entity.label.Label;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepositoryPort {
    Label save(Label label);
    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);
    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);
    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
