package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionListResponse;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionResponse;
import com.mailsangja.core.service.ai.draft.ReplyDraftSuggestionCommandService;
import com.mailsangja.core.service.ai.draft.ReplyDraftSuggestionQueryService;
import com.mailsangja.core.service.mail.MailQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ReplyDraftSuggestionFacade {

    private final MailQueryService mailQueryService;
    private final ReplyDraftSuggestionQueryService replyDraftSuggestionQueryService;
    private final ReplyDraftSuggestionCommandService replyDraftSuggestionCommandService;

    public ReplyDraftSuggestionListResponse findByMessageId(User user, UUID messageId) {
        validateUserAndId(user, messageId);
        Message message = mailQueryService.findReplyTargetMessage(messageId);
        validateMessageOwner(user, message);
        List<ReplyDraftSuggestion> suggestions = replyDraftSuggestionQueryService.findActiveByMessageId(messageId);
        return ReplyDraftSuggestionListResponse.from(suggestions);
    }

    public ReplyDraftSuggestionResponse select(User user, UUID suggestionId) {
        validateUserAndId(user, suggestionId);
        ReplyDraftSuggestion suggestion = replyDraftSuggestionQueryService.findActiveById(suggestionId);
        validateMessageOwner(user, suggestion.getMessage());
        ReplyDraftSuggestionResponse response = ReplyDraftSuggestionResponse.from(suggestion);
        replyDraftSuggestionCommandService.deleteAllByMessageId(suggestion.getMessage().getId());
        return response;
    }

    private void validateUserAndId(User user, UUID id) {
        if (user == null || user.getId() == null || id == null) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }

    private void validateMessageOwner(User user, Message message) {
        if (message == null || message.isDeleted()) {
            throw new MailDraftException(MailDraftErrorCode.ACCESS_DENIED);
        }
        MailAccount account = message.getThread().getMailAccount();
        if (account == null || account.getUser() == null || !user.getId().equals(account.getUser().getId())) {
            throw new MailDraftException(MailDraftErrorCode.ACCESS_DENIED);
        }
    }
}
