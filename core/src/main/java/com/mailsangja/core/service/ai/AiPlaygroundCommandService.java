package com.mailsangja.core.service.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.common.exception.ai.AiPlaygroundErrorCode;
import com.mailsangja.core.common.exception.ai.AiPlaygroundException;
import com.mailsangja.core.dto.ai.AiPlaygroundChatRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundChatResult;
import com.mailsangja.core.dto.ai.AiPlaygroundMessageRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundOptionsRequest;
import com.mailsangja.core.dto.ai.AiPlaygroundUsageResult;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.AiPlaygroundRateLimitCachePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiPlaygroundCommandService {

    private final AiPlaygroundRateLimitCachePort rateLimitCachePort;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;

    public AiPlaygroundChatResult chat(UUID userId, AiPlaygroundChatRequest request) {
        return chat(userId, request, Plan.FREE);
    }

    public AiPlaygroundChatResult chat(UUID userId, AiPlaygroundChatRequest request, Plan plan) {
        if (request == null) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        }
        validateWeeklyRateLimit(userId, plan);
        Prompt prompt = createPrompt(request);
        try {
            ChatResponse response = chatModel().call(prompt);
            return toResult(request, response);
        } catch (AiPlaygroundException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("AI playground provider call failed. provider={} model={}", request.provider(), request.model(), e);
            throw new AiPlaygroundException(AiPlaygroundErrorCode.PROVIDER_FAILURE, providerFailureMessage(e));
        }
    }

    private void validateWeeklyRateLimit(UUID userId, Plan plan) {
        boolean allowed = rateLimitCachePort.tryConsumeWeeklyLimit(userId);
        if (plan != Plan.PRO && !allowed) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    private Prompt createPrompt(AiPlaygroundChatRequest request) {
        return new Prompt(messagesOf(request), optionsOf(request));
    }

    private List<Message> messagesOf(AiPlaygroundChatRequest request) {
        if (!request.messages().isEmpty()) {
            return request.messages().stream()
                    .map(this::messageOf)
                    .toList();
        }

        List<Message> messages = new ArrayList<>();
        if (hasText(request.systemPrompt())) {
            messages.add(new SystemMessage(request.systemPrompt()));
        }
        if (!request.context().isEmpty() || hasText(request.userPrompt())) {
            messages.add(new UserMessage(userPromptWithContext(request)));
        }
        if (messages.isEmpty()) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        }
        return messages;
    }

    private Message messageOf(AiPlaygroundMessageRequest request) {
        if (request == null || !hasText(request.role()) || request.content() == null) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        }

        return switch (request.role().strip().toLowerCase(Locale.ROOT)) {
            case "system" -> new SystemMessage(request.content());
            case "developer" -> new SystemMessage(request.content());
            case "assistant" -> new AssistantMessage(request.content());
            case "user" -> new UserMessage(request.content());
            default -> throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        };
    }

    private String userPromptWithContext(AiPlaygroundChatRequest request) {
        if (request.context().isEmpty()) {
            return safeText(request.userPrompt());
        }
        if (!hasText(request.userPrompt())) {
            return contextJson(request.context());
        }
        return "Context:\n" + contextJson(request.context()) + "\n\nUser Prompt:\n" + request.userPrompt();
    }

    private String contextJson(Map<String, Object> context) {
        try {
            return objectMapper.writeValueAsString(context);
        } catch (JsonProcessingException e) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        }
    }

    private OpenAiChatOptions optionsOf(AiPlaygroundChatRequest request) {
        OpenAiChatOptions.Builder builder = OpenAiChatOptions.builder();
        if (hasText(request.model())) {
            builder.model(request.model().strip());
        }

        AiPlaygroundOptionsRequest options = request.options();
        if (options == null) {
            return builder.build();
        }

        builder.temperature(options.temperature())
                .maxTokens(options.maxOutputTokens())
                .maxCompletionTokens(options.maxCompletionTokens())
                .topP(options.topP())
                .presencePenalty(options.presencePenalty())
                .frequencyPenalty(options.frequencyPenalty())
                .seed(options.seed())
                .stop(options.stop())
                .reasoningEffort(options.reasoningEffort())
                .verbosity(options.verbosity())
                .serviceTier(options.serviceTier())
                .extraBody(options.extraBody());

        if (hasText(options.responseFormat())) {
            builder.responseFormat(responseFormatOf(options));
        }
        return builder.build();
    }

    private OpenAiChatModel.ResponseFormat responseFormatOf(AiPlaygroundOptionsRequest options) {
        OpenAiChatModel.ResponseFormat.Type type = switch (options.responseFormat().strip().toUpperCase(Locale.ROOT)) {
            case "JSON", "JSON_OBJECT" -> OpenAiChatModel.ResponseFormat.Type.JSON_OBJECT;
            case "JSON_SCHEMA" -> OpenAiChatModel.ResponseFormat.Type.JSON_SCHEMA;
            case "TEXT" -> OpenAiChatModel.ResponseFormat.Type.TEXT;
            default -> throw new AiPlaygroundException(AiPlaygroundErrorCode.INVALID_REQUEST);
        };

        return OpenAiChatModel.ResponseFormat.builder()
                .type(type)
                .jsonSchema(options.jsonSchema())
                .build();
    }

    private AiPlaygroundChatResult toResult(AiPlaygroundChatRequest request, ChatResponse response) {
        ChatResponseMetadata metadata = metadataOf(response);
        Usage usage = usageOf(metadata);
        return new AiPlaygroundChatResult(
                request.provider(),
                modelOf(request, metadata),
                textOf(response),
                new AiPlaygroundUsageResult(promptTokens(usage), completionTokens(usage), totalTokens(usage)),
                rawOf(metadata)
        );
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new AiPlaygroundException(AiPlaygroundErrorCode.CHAT_MODEL_NOT_AVAILABLE);
        }
        return chatModel;
    }

    private String textOf(ChatResponse response) {
        if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
            return "";
        }
        return safeText(response.getResult().getOutput().getText());
    }

    private ChatResponseMetadata metadataOf(ChatResponse response) {
        if (response == null) {
            return ChatResponseMetadata.builder().build();
        }
        return response.getMetadata();
    }

    private Usage usageOf(ChatResponseMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        return metadata.getUsage();
    }

    private String modelOf(AiPlaygroundChatRequest request, ChatResponseMetadata metadata) {
        if (metadata != null && hasText(metadata.getModel())) {
            return metadata.getModel();
        }
        return request.model();
    }

    private Map<String, Object> rawOf(ChatResponseMetadata metadata) {
        Map<String, Object> raw = new LinkedHashMap<>();
        if (metadata == null) {
            return raw;
        }
        raw.put("model", metadata.getModel());
        raw.put("usage", usageMap(usageOf(metadata)));
        return raw;
    }

    private Map<String, Object> usageMap(Usage usage) {
        Map<String, Object> rawUsage = new LinkedHashMap<>();
        if (usage == null) {
            return rawUsage;
        }
        rawUsage.put("inputTokens", promptTokens(usage));
        rawUsage.put("outputTokens", completionTokens(usage));
        rawUsage.put("totalTokens", totalTokens(usage));
        return rawUsage;
    }

    private Integer promptTokens(Usage usage) {
        if (usage == null) {
            return null;
        }
        return usage.getPromptTokens();
    }

    private Integer completionTokens(Usage usage) {
        if (usage == null) {
            return null;
        }
        return usage.getCompletionTokens();
    }

    private Integer totalTokens(Usage usage) {
        if (usage == null) {
            return null;
        }
        return usage.getTotalTokens();
    }

    private String providerFailureMessage(RuntimeException e) {
        if (hasText(e.getMessage())) {
            return e.getMessage();
        }
        return AiPlaygroundErrorCode.PROVIDER_FAILURE.getMessage();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }
}
