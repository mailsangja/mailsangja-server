package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReplyDraftSuggestionQueryService {

    private final ReplyDraftSuggestionRepositoryPort replyDraftSuggestionRepositoryPort;

    public List<ReplyDraftSuggestion> findActiveByMessageId(UUID messageId) {
        validateId(messageId);
        return replyDraftSuggestionRepositoryPort.findAllByMessageIdAndDeletedAtIsNull(messageId);
    }

    public ReplyDraftSuggestion findActiveById(UUID suggestionId) {
        validateId(suggestionId);
        return replyDraftSuggestionRepositoryPort.findByIdAndDeletedAtIsNull(suggestionId)
                .orElseThrow(() -> new MailDraftException(MailDraftErrorCode.REPLY_DRAFT_SUGGESTION_NOT_FOUND));
    }

    private void validateId(UUID id) {
        if (id == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
