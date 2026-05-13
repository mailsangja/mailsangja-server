package com.mailsangja.db.port;

import com.mailsangja.db.entity.label.LabelGroup;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelGroupRepositoryPort {

    LabelGroup save(LabelGroup labelGroup);

    List<LabelGroup> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(UUID userId);

    Optional<LabelGroup> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    List<LabelGroup> findActiveGroupsByLabelId(UUID labelId);

    List<LabelGroup> saveAll(List<LabelGroup> labelGroups);

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String name, UUID excludeId);
}
