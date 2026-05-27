package com.mailsangja.core.service.ai.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mailsangja.core.dto.mail.MailReviewCommand;
import com.mailsangja.core.dto.mail.MailReviewIssueType;
import com.mailsangja.core.dto.mail.MailReviewResult;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.port.MailReviewRateLimitCachePort;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MailReviewCommandServiceTest {

    @Test
    void pro사용자는한도초과여도카운트만소모하고허용한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailReviewRateLimitCachePort cachePort = mock(MailReviewRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(false);
        MailReviewCommandService service = new MailReviewCommandService(
                cachePort,
                chatModelProvider(chatModel("{\"issues\":[]}")),
                new ObjectMapper(),
                new MailReviewQueryService()
        );
        MailReviewCommand command = new MailReviewCommand(userId, "", "검토할 본문입니다.", 0, List.of());

        // when & then
        assertDoesNotThrow(() -> service.review(command, Plan.PRO));

        // then
        verify(cachePort).tryConsumeWeeklyLimit(userId);
    }

    @Test
    void llm후보를검증해서적용가능한issue만반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailReviewRateLimitCachePort cachePort = mock(MailReviewRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(true);
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
        MailReviewResult result = service.review(new MailReviewCommand(userId, "회의 일정 확입 요청", "", 0, List.of()));

        // then
        assertEquals(1, result.issues().size());
        assertEquals(MailReviewIssueType.SPELLING, result.issues().getFirst().type());
        assertEquals(6, result.issues().getFirst().globalStartOffset());
        assertEquals("확인", result.issues().getFirst().replacementText());
    }

    @Test
    void 첨부를언급했지만첨부파일이없으면첨부누락이슈를반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailReviewRateLimitCachePort cachePort = mock(MailReviewRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(true);
        ChatModel chatModel = chatModel("""
                {
                  "issues": [
                    {
                      "segmentId": "BODY:000:7a0427592046ec48",
                      "type": "ATTACHMENT_MISSING",
                      "severity": "HIGH",
                      "originalText": "자료를 첨부드립니다",
                      "replacementText": "첨부파일을 추가해 주세요",
                      "contextBefore": "요청하신 ",
                      "contextAfter": ".",
                      "reason": "본문에서 첨부를 언급했지만 첨부파일이 없습니다."
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
        MailReviewResult result = service.review(new MailReviewCommand(
                userId,
                "",
                "요청하신 자료를 첨부드립니다.",
                0,
                List.of()
        ));

        // then
        assertEquals(1, result.issues().size());
        assertEquals(MailReviewIssueType.ATTACHMENT_MISSING, result.issues().getFirst().type());
    }

    @Test
    void 첨부파일이있으면llm의첨부누락이슈를폐기한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailReviewRateLimitCachePort cachePort = mock(MailReviewRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(true);
        ChatModel chatModel = chatModel("""
                {
                  "issues": [
                    {
                      "segmentId": "BODY:000:7a0427592046ec48",
                      "type": "ATTACHMENT_MISSING",
                      "severity": "HIGH",
                      "originalText": "자료를 첨부드립니다",
                      "replacementText": "첨부파일을 추가해 주세요",
                      "contextBefore": "요청하신 ",
                      "contextAfter": ".",
                      "reason": "본문에서 첨부를 언급했지만 첨부파일이 없습니다."
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
        MailReviewResult result = service.review(new MailReviewCommand(
                userId,
                "",
                "요청하신 자료를 첨부드립니다.",
                1,
                List.of("자료.pdf")
        ));

        // then
        assertEquals(0, result.issues().size());
    }

    @Test
    void replacementText가빈문자열인삭제제안도파싱하고반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        MailReviewRateLimitCachePort cachePort = mock(MailReviewRateLimitCachePort.class);
        when(cachePort.tryConsumeWeeklyLimit(userId)).thenReturn(true);
        ChatModel chatModel = chatModel("""
                {
                  "issues": [
                    {
                      "segmentId": "BODY:000:7103778b4ffb3901",
                      "type": "SPELLING",
                      "severity": "MEDIUM",
                      "originalText": "ㅜ",
                      "replacementText": "",
                      "contextBefore": "제",
                      "contextAfter": " 이름은",
                      "reason": "불필요한 자음이 포함되어 있습니다."
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
        MailReviewResult result = service.review(new MailReviewCommand(
                userId,
                "",
                "제ㅜ 이름은 천진강입니다.",
                0,
                List.of()
        ));

        // then
        assertEquals(1, result.issues().size());
        assertEquals("", result.issues().getFirst().replacementText());
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
