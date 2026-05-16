package com.mailsangja.core.service.ai.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.dto.mail.MailReviewCommand;
import com.mailsangja.core.dto.mail.MailReviewIssueType;
import com.mailsangja.core.dto.mail.MailReviewResult;
import com.mailsangja.db.port.MailDraftRateLimitCachePort;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailReviewCommandServiceTest {

    @Test
    void llm후보를검증해서적용가능한issue만반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailDraftRateLimitCachePort cachePort = mock(MailDraftRateLimitCachePort.class);
        when(cachePort.tryConsumeMonthlyLimit(userId)).thenReturn(true);
        ChatModel chatModel = chatModel("""
                {
                  "issues": [
                    {
                      "segmentId": "SUBJECT:000:d3d8270a39666bd1",
                      "type": "SPELLING",
                      "severity": "LOW",
                      "originalText": "확입",
                      "replacementText": "확인",
                      "contextBefore": "회의 일정 ",
                      "contextAfter": " 요청",
                      "reason": "맞춤법 오류입니다."
                    }
                  ]
                }
                """);
        MailReviewCommandService service = new MailReviewCommandService(
                cachePort,
                chatModelProvider(chatModel),
                new ObjectMapper(),
                new MailReviewQueryService()
        );

        // when
        MailReviewResult result = service.review(new MailReviewCommand(userId, "회의 일정 확입 요청", ""));

        // then
        assertEquals(1, result.issues().size());
        assertEquals(MailReviewIssueType.SPELLING, result.issues().getFirst().type());
        assertEquals(6, result.issues().getFirst().globalStartOffset());
        assertEquals("확인", result.issues().getFirst().replacementText());
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> chatModelProvider(ChatModel chatModel) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);
        return provider;
    }

    private ChatModel chatModel(String text) {
        return new StubChatModel(response(text));
    }

    private ChatResponse response(String text) {
        Generation generation = new Generation(AssistantMessage.builder().content(text).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model("gpt-test").build();
        return new ChatResponse(List.of(generation), metadata);
    }

    private record StubChatModel(ChatResponse response) implements ChatModel {

        @Override
        public ChatResponse call(Prompt prompt) {
            return response;
        }

        @Override
        public ChatOptions getDefaultOptions() {
            return ChatOptions.builder().model("gpt-test").build();
        }
    }
}
