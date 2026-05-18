package com.mailsangja.db.port;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplyDraftSuggestionRepositoryPort {
    ReplyDraftSuggestion save(ReplyDraftSuggestion replyDraftSuggestion);
    void delete(ReplyDraftSuggestion replyDraftSuggestion);
    List<ReplyDraftSuggestion> findAllByMessageIdAndDeletedAtIsNull(UUID messageId);
    Optional<ReplyDraftSuggestion> findByIdAndDeletedAtIsNull(UUID id);
}
