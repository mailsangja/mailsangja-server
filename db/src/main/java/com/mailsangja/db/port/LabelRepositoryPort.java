package com.mailsangja.db.port;

import com.mailsangja.db.entity.label.Label;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface LabelRepositoryPort {

    Label save(Label label);

    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    List<Label> findAllConfirmedByUserIdAndDeletedAtIsNull(UUID userId);

    List<Label> findAllByUserIdAndIdInAndDeletedAtIsNull(UUID userId, List<UUID> ids);

    List<Label> findAllConfirmedByUserIdAndDeletedAtIsNullOrderByDisplayOrder(UUID userId);

    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    Optional<Label> findConfirmedByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(UUID userId, String name, UUID excludeId);

    Map<UUID, Long> findUnreadThreadCountsByUserId(UUID userId);

    List<Label> findAllSuggestionsByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Label> findSuggestionByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    boolean existsSuggestionByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);
}
