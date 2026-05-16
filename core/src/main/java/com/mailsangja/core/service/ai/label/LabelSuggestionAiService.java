package com.mailsangja.core.service.ai.label;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.label.LabelErrorCode;
import com.mailsangja.core.common.exception.label.LabelException;
import com.mailsangja.core.config.properties.LabelSuggestionProperties;
import com.mailsangja.core.dto.label.LlmLabelSuggestionResult;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.ai.converter.StructuredOutputConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelSuggestionAiService {

    private static final String SYSTEM_PROMPT = """
            You are Mailsangja Label Suggestion Engine.
            Analyze the provided recent inbound email metadata and suggest useful label rules to organize the user's mailbox.

            CONSTRAINTS:
            - Use ONLY the subject, fromAddress, and fromName fields. Never infer patterns from body text or snippet.
            - Suggest 3 to 7 labels that cover meaningful email patterns (e.g., newsletters, billing, work projects, services).
            - Prefer Korean label names when the email metadata suggests a Korean-speaking user.
            - Each label MUST include at least one rule condition.
            - Do not suggest labels that duplicate existing labels listed in existingLabelNames.

            RULE STRUCTURE:
            {
              "groups": [
                {
                  "conditions": [
                    { "field": "<FIELD>", "operator": "<OPERATOR>", "value": "<string>" }
                  ]
                }
              ]
            }
            Groups are combined with OR. Conditions within a group are combined with AND.

            ALLOWED FIELD + OPERATOR COMBINATIONS:
            - FROM_ADDRESS: EQUALS, CONTAINS
            - FROM_DOMAIN: EQUALS, CONTAINS
            - SUBJECT: CONTAINS, NOT_CONTAINS
            - HAS_ATTACHMENT: BOOLEAN (value must be "true" or "false")

            notificationPolicy options: URGENT, INHERIT, SILENT
            order: sequential integers starting from 0
            colorCode: valid hex color (e.g., "#FF5733")

            Return JSON only. Do not wrap in markdown code blocks.
            If no meaningful pattern can be inferred, return {"suggestions":[]}.
            """;

    private final MessageRepositoryPort messageRepositoryPort;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;
    private final LabelSuggestionProperties properties;

    public LlmLabelSuggestionResult suggest(UUID userId, List<Label> existingLabels) {
        List<Message> recentMessages = messageRepositoryPort.findRecentByUserIdAndDirection(
                userId, Direction.INBOUND, PageRequest.of(0, properties.getRecentMailCount())
        );
        if (recentMessages.isEmpty()) {
            return new LlmLabelSuggestionResult(List.of());
        }
        return requestSuggestions(recentMessages, existingLabels);
    }

    private LlmLabelSuggestionResult requestSuggestions(List<Message> messages, List<Label> existingLabels) {
        StructuredOutputConverter<LlmLabelSuggestionResult> converter = new BeanOutputConverter<>(LlmLabelSuggestionResult.class);
        try {
            return ChatClient.create(chatModel())
                    .prompt(createPrompt(messages, existingLabels, converter))
                    .call()
                    .entity(converter);
        } catch (RuntimeException e) {
            log.warn("Label suggestion AI structured response failed.", e);
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED);
        }
    }

    private Prompt createPrompt(List<Message> messages, List<Label> existingLabels,
                                StructuredOutputConverter<LlmLabelSuggestionResult> converter) {
        return new Prompt(List.of(
                new SystemMessage(SYSTEM_PROMPT + "\n" + converter.getFormat()),
                new UserMessage(buildUserMessage(messages, existingLabels))
        ));
    }

    private String buildUserMessage(List<Message> messages, List<Label> existingLabels) {
        try {
            List<MessageSummary> summaries = messages.stream()
                    .map(m -> new MessageSummary(m.getSubject(), m.getFromAddress(), m.getFromName()))
                    .toList();
            List<String> existingNames = existingLabels.stream().map(Label::getName).toList();
            return objectMapper.writeValueAsString(new UserInput(summaries, existingNames));
        } catch (JsonProcessingException e) {
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED);
        }
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED);
        }
        return chatModel;
    }

    private record MessageSummary(String subject, String fromAddress, String fromName) {}

    private record UserInput(List<MessageSummary> recentEmails, List<String> existingLabelNames) {}
}
