package com.mailsangja.db.module.label;

import com.mailsangja.db.entity.label.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LabelJpaRepositoryModule extends JpaRepository<Label, UUID> {

    List<Label> findAllByUserIdAndDeletedAtIsNull(UUID userId);

    Optional<Label> findByIdAndDeletedAtIsNull(UUID id);

    Optional<Label> findByIdAndUserIdAndDeletedAtIsNull(UUID id, UUID userId);
}
