package com.mailsangja.db.module.mail;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplyDraftSuggestionJpaRepositoryModule extends JpaRepository<ReplyDraftSuggestion, UUID> {
    List<ReplyDraftSuggestion> findAllByMessageIdAndDeletedAtIsNull(UUID messageId);
    Optional<ReplyDraftSuggestion> findByIdAndDeletedAtIsNull(UUID id);
    boolean existsByMessageId(UUID messageId);
}
