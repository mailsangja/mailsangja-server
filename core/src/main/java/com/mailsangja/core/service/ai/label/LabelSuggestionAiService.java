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
import org.springframework.ai.chat.messages.AssistantMessage;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LabelSuggestionAiService {

    private static final String SYSTEM_PROMPT = """
            You are Mailsangja Label Suggestion Engine.
            Analyze the provided recent inbound email metadata and suggest useful label rules to organize the user's mailbox.

            CONSTRAINTS:
            - Primary sources are subject, fromAddress, and fromName. Prefer these for pattern detection.
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

            TOOL USAGE — getEmailSnippets(messageIds):
            - Call this tool ONLY as a last resort when ALL of the following are true:
              1. The fromAddress/fromDomain alone does NOT reveal a clear sender category.
              2. The subject lines are too generic (e.g., "안녕하세요", "문의드립니다") to form a useful rule.
              3. Reading the snippet of 1-3 specific messages would likely reveal the pattern.
            - Do NOT call this tool if fromDomain or recurring subject keywords already reveal a pattern.
            - Do NOT call this tool more than once per suggestion session.
            - Each snippet is preprocessed: PII is masked and text is truncated to 150 characters.

            Return JSON only. Do not wrap in markdown code blocks.
            If no meaningful pattern can be inferred, return {"suggestions":[]}.
            """;

    private static final String EXAMPLE_USER_MESSAGE = """
            {
              "recentEmails": [
                {"id": "ex-001", "subject": "[OOP Lab] 5주차 과제 안내", "fromAddress": "prof.kim@university.ac.kr", "fromName": "김교수", "toAddresses": ["student@university.ac.kr"]},
                {"id": "ex-002", "subject": "OOP 중간고사 범위 공지", "fromAddress": "prof.kim@university.ac.kr", "fromName": "김교수", "toAddresses": []},
                {"id": "ex-003", "subject": "객체지향 과제 관련 질문 드립니다", "fromAddress": "ta.choi@university.ac.kr", "fromName": "최조교", "toAddresses": []},
                {"id": "ex-004", "subject": "Re: 객체지향 프로그래밍 실습 공지", "fromAddress": "ta.choi@university.ac.kr", "fromName": "최조교", "toAddresses": []}
              ],
              "existingLabelNames": []
            }
            """;

    private static final String EXAMPLE_ASSISTANT_MESSAGE = """
            {
              "suggestions": [
                {
                  "name": "OOP 교수님",
                  "colorCode": "#3366FF",
                  "notificationPolicy": "URGENT",
                  "order": 0,
                  "rule": {
                    "groups": [
                      {"conditions": [{"field": "SUBJECT", "operator": "CONTAINS", "value": "[OOP Lab]"}, {"field": "FROM_ADDRESS", "operator": "EQUALS", "value": "prof.kim@university.ac.kr"}]},
                      {"conditions": [{"field": "SUBJECT", "operator": "CONTAINS", "value": "OOP"}]},
                      {"conditions": [{"field": "HAS_ATTACHMENT", "operator": "BOOLEAN", "value": "true"}]}
                    ]
                  }
                },
                {
                  "name": "객체지향 조교 문의",
                  "colorCode": "#10b981",
                  "notificationPolicy": "INHERIT",
                  "order": 1,
                  "rule": {
                    "groups": [
                      {"conditions": [{"field": "SUBJECT", "operator": "CONTAINS", "value": "객체지향"}]},
                      {"conditions": [{"field": "FROM_ADDRESS", "operator": "EQUALS", "value": "ta.choi@university.ac.kr"}]}
                    ]
                  }
                }
              ]
            }
            """;

    private final MessageRepositoryPort messageRepositoryPort;
    private final ObjectProvider<ChatModel> chatModelProvider;
    private final ObjectMapper objectMapper;
    private final LabelSuggestionProperties properties;
    private final SnippetPreprocessor snippetPreprocessor;

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
        Map<String, String> snippetMap = buildSnippetMap(messages);
        try {
            return ChatClient.create(chatModel())
                    .prompt(createPrompt(messages, existingLabels, converter))
                    .tools(new SnippetTool(snippetMap))
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
                new UserMessage(EXAMPLE_USER_MESSAGE),
                new AssistantMessage(EXAMPLE_ASSISTANT_MESSAGE),
                new UserMessage(buildUserMessage(messages, existingLabels))
        ));
    }

    private String buildUserMessage(List<Message> messages, List<Label> existingLabels) {
        try {
            List<MessageSummary> summaries = messages.stream()
                    .map(m -> new MessageSummary(
                            m.getId().toString(),
                            m.getSubject(),
                            m.getFromAddress(),
                            m.getFromName(),
                            m.getToAddresses()))
                    .toList();
            List<String> existingNames = existingLabels.stream().map(Label::getName).toList();
            return objectMapper.writeValueAsString(new UserInput(summaries, existingNames));
        } catch (JsonProcessingException e) {
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED);
        }
    }

    private Map<String, String> buildSnippetMap(List<Message> messages) {
        return messages.stream()
                .filter(m -> m.getSnippet() != null && !m.getSnippet().isBlank())
                .collect(Collectors.toMap(
                        m -> m.getId().toString(),
                        m -> snippetPreprocessor.process(m.getSnippet())
                ));
    }

    private ChatModel chatModel() {
        ChatModel chatModel = chatModelProvider.getIfAvailable();
        if (chatModel == null) {
            throw new LabelException(LabelErrorCode.LABEL_SUGGESTION_AI_FAILED);
        }
        return chatModel;
    }

    private record MessageSummary(
            String id,
            String subject,
            String fromAddress,
            String fromName,
            List<String> toAddresses
    ) {}

    private record UserInput(List<MessageSummary> recentEmails, List<String> existingLabelNames) {}
}
