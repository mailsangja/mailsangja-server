package com.mailsangja.db.port;

import com.mailsangja.db.entity.label.Label;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepositoryPort {

    Label save(Label label);

    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    List<Label> findAllByUserIdAndIdInAndDeletedAtIsNull(UUID userId, List<UUID> ids);

    List<Label> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(UUID userId);

    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String name, UUID excludeId);

    Map<UUID, Long> findUnreadThreadCountsByUserId(UUID userId);
}
