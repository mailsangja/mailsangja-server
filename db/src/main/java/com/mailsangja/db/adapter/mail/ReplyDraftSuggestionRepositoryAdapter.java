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
    public int saveAllByMessageIdUpToActiveLimit(UUID messageId, List<ReplyDraftSuggestion> replyDraftSuggestions, int limit) {
        if (messageId == null || replyDraftSuggestions == null || replyDraftSuggestions.isEmpty() || limit <= 0) {
            return 0;
        }
        int inserted = 0;
        for (ReplyDraftSuggestion suggestion : replyDraftSuggestions) {
            inserted += insertIfActiveCountBelowLimit(messageId, fillId(suggestion), limit);
        }
        return inserted;
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

    private ReplyDraftSuggestion fillId(ReplyDraftSuggestion suggestion) {
        if (suggestion.getId() != null) {
            return suggestion;
        }
        return ReplyDraftSuggestion.builder()
                .id(UUID.randomUUID())
                .message(suggestion.getMessage())
                .type(suggestion.getType())
                .subject(suggestion.getSubject())
                .body(suggestion.getBody())
                .build();
    }

    private int insertIfActiveCountBelowLimit(UUID messageId, ReplyDraftSuggestion suggestion, int limit) {
        return replyDraftSuggestionJpaRepositoryModule.insertIfActiveCountBelowLimit(
                suggestion.getId().toString(),
                messageId.toString(),
                suggestion.getType(),
                suggestion.getSubject(),
                suggestion.getBody(),
                limit
        );
    }
}
