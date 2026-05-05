package com.mailsangja.db.module.label;

import com.mailsangja.db.entity.label.LabelGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelGroupJpaRepositoryModule extends JpaRepository<LabelGroup, UUID> {

    @Query("""
            SELECT DISTINCT lg
            FROM LabelGroup lg
            LEFT JOIN FETCH lg.labelGroupLabels lgl
            LEFT JOIN FETCH lgl.label
            WHERE lg.user.id = :userId
              AND lg.deletedAt IS NULL
            ORDER BY lg.displayOrder ASC, lg.createdAt ASC
            """)
    List<LabelGroup> findAllByUserIdAndDeletedAtIsNullOrderByDisplayOrder(@Param("userId") UUID userId);

    @Query("""
            SELECT DISTINCT lg
            FROM LabelGroup lg
            LEFT JOIN FETCH lg.labelGroupLabels lgl
            LEFT JOIN FETCH lgl.label
            WHERE lg.id = :id
              AND lg.user.id = :userId
              AND lg.deletedAt IS NULL
            """)
    Optional<LabelGroup> findByIdAndUserIdAndDeletedAtIsNull(
            @Param("id") UUID id,
            @Param("userId") UUID userId
    );

    boolean existsByUserIdAndNameIgnoreCaseAndDeletedAtIsNull(UUID userId, String name);

    @Query("""
            SELECT COUNT(lg) > 0
            FROM LabelGroup lg
            WHERE lg.user.id = :userId
              AND lower(lg.name) = lower(:name)
              AND lg.id <> :excludeId
              AND lg.deletedAt IS NULL
            """)
    boolean existsByUserIdAndNameIgnoreCaseAndIdNotAndDeletedAtIsNull(
            @Param("userId") UUID userId,
            @Param("name") String name,
            @Param("excludeId") UUID excludeId
    );
}
