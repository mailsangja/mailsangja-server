package com.mailsangja.db.module.label;

import com.mailsangja.db.dto.LabelUnreadCountProjection;
import com.mailsangja.db.entity.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelJpaRepositoryModule extends JpaRepository<Label, UUID> {

    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    @Query("SELECT l FROM Label l WHERE l.user.id = :userId AND l.confirmed = true AND l.deletedAt IS NULL")
    List<Label> findAllConfirmedByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId);

    List<Label> findAllByUserIdAndIdInAndDeletedAtIsNull(UUID userId, List<UUID> ids);

    @Query("SELECT l FROM Label l WHERE l.user.id = :userId AND l.confirmed = true AND l.deletedAt IS NULL ORDER BY l.displayOrder ASC, l.createdAt ASC")
    List<Label> findAllConfirmedByUserIdAndDeletedAtIsNullOrderByDisplayOrder(@Param("userId") UUID userId);

    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

    @Query("SELECT l FROM Label l WHERE l.id = :id AND l.user.id = :userId AND l.confirmed = true AND l.deletedAt IS NULL")
    Optional<Label> findConfirmedByIdAndUserIdAndDeletedAtIsNull(@Param("id") UUID id, @Param("userId") UUID userId);

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    @Query("""
            SELECT COUNT(l) > 0
            FROM Label l
            WHERE l.user.id = :userId
              AND lower(l.name) = lower(:name)
              AND l.id <> :excludeId
              AND l.deletedAt IS NULL
            """)
    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId
    );

    @Query("""
            SELECT ml.label.id as labelId, COUNT(DISTINCT ml.message.thread.id) as unreadCount
            FROM MessageLabel ml
            WHERE ml.label.user.id = :userId
              AND ml.deletedAt IS NULL
              AND ml.label.deletedAt IS NULL
              AND ml.message.deletedAt IS NULL
              AND ml.message.thread.deletedAt IS NULL
              AND ml.message.thread.mailAccount.active = true
              AND ml.message.thread.mailAccount.deletedAt IS NULL
              AND ml.message.thread.read = false
              AND ml.message.thread.direction = com.mailsangja.db.entity.mail.Direction.INBOUND
            GROUP BY ml.label.id
            """)
    List<LabelUnreadCountProjection> findUnreadThreadCountsByUserId(@Param("userId") UUID userId);

    @Query("SELECT l FROM Label l WHERE l.user.id = :userId AND l.confirmed = false AND l.deletedAt IS NULL")
    List<Label> findAllSuggestionsByUserIdAndDeletedAtIsNull(@Param("userId") UUID userId);

    @Query("SELECT l FROM Label l WHERE l.id = :id AND l.user.id = :userId AND l.confirmed = false AND l.deletedAt IS NULL")
    Optional<Label> findSuggestionByIdAndUserIdAndDeletedAtIsNull(@Param("id") UUID id, @Param("userId") UUID userId);

    @Query("SELECT COUNT(l) > 0 FROM Label l WHERE l.user.id = :userId AND lower(l.name) = lower(:name) AND l.confirmed = false AND l.deletedAt IS NULL")
    boolean existsSuggestionByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(@Param("userId") UUID userId, @Param("name") String name);
}
