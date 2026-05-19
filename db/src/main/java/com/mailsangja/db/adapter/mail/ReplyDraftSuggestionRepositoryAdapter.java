package com.mailsangja.db.adapter.mail;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.module.mail.ReplyDraftSuggestionJpaRepositoryModule;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ReplyDraftSuggestionRepositoryAdapter implements ReplyDraftSuggestionRepositoryPort {

    private final ReplyDraftSuggestionJpaRepositoryModule replyDraftSuggestionJpaRepositoryModule;

    @Override
    public ReplyDraftSuggestion save(ReplyDraftSuggestion replyDraftSuggestion) {
        return replyDraftSuggestionJpaRepositoryModule.save(replyDraftSuggestion);
    }

    @Override
    public void delete(ReplyDraftSuggestion replyDraftSuggestion) {
        replyDraftSuggestion.delete();
        replyDraftSuggestionJpaRepositoryModule.save(replyDraftSuggestion);
    }

    @Override
    public List<ReplyDraftSuggestion> findAllByMessageIdAndDeletedAtIsNull(UUID messageId) {
        return replyDraftSuggestionJpaRepositoryModule.findAllByMessageIdAndDeletedAtIsNull(messageId);
    }

    @Override
    public Optional<ReplyDraftSuggestion> findByIdAndDeletedAtIsNull(UUID id) {
        return replyDraftSuggestionJpaRepositoryModule.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByMessageId(UUID messageId) {
        return replyDraftSuggestionJpaRepositoryModule.existsByMessageId(messageId);
    }
}
