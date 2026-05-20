package com.mailsangja.core.service.ai.draft;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReplyDraftSuggestionCommandService {

    private final ReplyDraftSuggestionRepositoryPort replyDraftSuggestionRepositoryPort;
    private final ReplyDraftSuggestionQueryService replyDraftSuggestionQueryService;

    @Transactional
    public void deleteAllByMessageId(UUID messageId) {
        validateMessageId(messageId);
        List<ReplyDraftSuggestion> suggestions = replyDraftSuggestionQueryService.findActiveByMessageId(messageId);
        for (ReplyDraftSuggestion suggestion : suggestions) {
            replyDraftSuggestionRepositoryPort.delete(suggestion);
        }
    }

    private void validateMessageId(UUID messageId) {
        if (messageId == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
