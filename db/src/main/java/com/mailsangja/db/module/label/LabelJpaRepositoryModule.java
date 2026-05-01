package com.mailsangja.db.module.label;

import com.mailsangja.db.entity.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelJpaRepositoryModule extends JpaRepository<Label, UUID> {

    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    @Query("SELECT l FROM Label l WHERE l.user.id = :userId AND l.deletedAt IS NULL ORDER BY l.displayOrder ASC, l.createdAt ASC")
    List<Label> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(@Param("userId") UUID userId);

    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);

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
            SELECT tl.label.id as labelId, COUNT(DISTINCT tl.thread.id) as unreadCount
            FROM ThreadLabel tl
            WHERE tl.label.user.id = :userId
              AND tl.deletedAt IS NULL
              AND tl.label.deletedAt IS NULL
              AND tl.thread.deletedAt IS NULL
              AND tl.thread.read = false
              AND tl.thread.direction = com.mailsangja.db.entity.mail.Direction.INBOUND
            GROUP BY tl.label.id
            """)
    List<LabelUnreadCountProjection> findUnreadThreadCountsByUserId(@Param("userId") UUID userId);
}
