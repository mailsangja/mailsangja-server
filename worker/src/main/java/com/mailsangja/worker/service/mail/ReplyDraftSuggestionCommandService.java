package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionLlmResult;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionOptionResult;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionPromptResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReplyDraftSuggestionCommandService {

    private static final int MAX_ACTIVE_SUGGESTIONS_PER_MESSAGE = 4;

    private final ObjectProvider<ChatModel> chatModelProvider;
    private final MessageRepositoryPort messageRepositoryPort;
    private final ReplyDraftSuggestionRepositoryPort replyDraftSuggestionRepositoryPort;
    private final ReplyDraftSuggestionQueryService replyDraftSuggestionQueryService;
    private final TransactionTemplate transactionTemplate;

    public void generate(UUID messageId) {
        com.mailsangja.db.entity.mail.Message message = findActiveMessage(messageId);
        if (replyDraftSuggestionQueryService.existsByMessageId(message.getId())) {
            log.info("Reply draft suggestion skipped because suggestions already exist. messageId={}", message.getId());
            return;
        }
        Optional<ReplyDraftSuggestionLlmResult> result = requestSuggestions(messageId);
        if (result.isEmpty()) {
            log.info("Reply draft suggestion skipped because AI response is invalid. messageId={}", messageId);
            return;
        }
        transactionTemplate.executeWithoutResult(status -> saveSuggestions(message, result.get().suggestions()));
    }

    private com.mailsangja.db.entity.mail.Message findActiveMessage(UUID messageId) {
        if (messageId == null) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
        }
        com.mailsangja.db.entity.mail.Message message = messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId)
                .orElseThrow(() -> new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE));
        if (message.isDeleted()) {
            throw new MqException(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE);
        }
        return message;
    }

    private Optional<ReplyDraftSuggestionLlmResult> requestSuggestions(UUID messageId) {
        StructuredOutputConverter<ReplyDraftSuggestionLlmResult> converter =
                new BeanOutputConverter<>(ReplyDraftSuggestionLlmResult.class);
        ReplyDraftSuggestionPromptResult prompt = replyDraftSuggestionQueryService.createPrompt(
                messageId,
                converter.getFormat()
        );
        ChatModel chatModel = chatModel();
        try {
            return Optional.ofNullable(ChatClient.create(chatModel)
                    .prompt(createPrompt(prompt))
                    .call()
                    .entity(converter));
        } catch (RuntimeException e) {
            if (isInvalidAiResponse(e)) {
                log.warn("Reply draft suggestion AI response invalid. messageId={}", messageId, e);
                return Optional.empty();
            }
            throw e;
        }
    }

    private Prompt createPrompt(ReplyDraftSuggestionPromptResult prompt) {
        List<Message> messages = List.of(
                new SystemMessage(prompt.systemPrompt()),
                new UserMessage(prompt.userPrompt())
        );
        return new Prompt(messages);
    }

    private void saveSuggestions(
            com.mailsangja.db.entity.mail.Message message,
            List<ReplyDraftSuggestionOptionResult> suggestions
    ) {
        int inserted = replyDraftSuggestionRepositoryPort.saveAllByMessageIdUpToActiveLimit(
                message.getId(),
                suggestions.stream()
                        .map(suggestion -> ReplyDraftSuggestion.builder()
                                .message(message)
                                .type(suggestion.type())
                                .subject(suggestion.subject())
                                .body(suggestion.body())
                                .build())
                        .toList(),
                MAX_ACTIVE_SUGGESTIONS_PER_MESSAGE
        );
        if (inserted < suggestions.size()) {
            log.info(
                    "Reply draft suggestion save limited by active count. messageId={} requested={} inserted={} limit={}",
                    message.getId(),
                    suggestions.size(),
                    inserted,
                    MAX_ACTIVE_SUGGESTIONS_PER_MESSAGE
            );
        }
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new MqException(MqErrorCode.REPLY_DRAFT_SUGGESTION_CHAT_MODEL_NOT_AVAILABLE);
        }
        return chatModel;
    }

    private boolean isInvalidAiResponse(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof MqException mqException
                    && mqException.getErrorCode() == MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_AI_RESPONSE) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
