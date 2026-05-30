package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.ReplyDraftSuggestion;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ReplyDraftSuggestionRepositoryPort;
import com.mailsangja.worker.common.exception.mq.MqErrorCode;
import com.mailsangja.worker.common.exception.mq.MqException;
import com.mailsangja.worker.config.properties.AiModelProperties;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionPromptResult;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReplyDraftSuggestionCommandServiceTest {

    @Test
    void generate_이미초안이존재하면LLM호출과저장을하지않는다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel("{}")),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate,
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(true);

        // when
        service.generate(messageId);

        // then
        verify(queryService, never()).createPrompt(eq(messageId), any());
        verifyNoInteractions(transactionTemplate);
        verify(suggestionRepositoryPort, never()).save(any());
        verify(suggestionRepositoryPort, never()).saveAllByMessageIdUpToActiveLimit(any(), any(), any(Integer.class));
    }

    @Test
    void generate_LLM이반환한추천초안을저장한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        ChatModel chatModel = chatModel("""
                {
                  "suggestions": [
                    {
                      "type": "승낙",
                      "subject": "Re: 회의 일정",
                      "body": "좋습니다. 해당 일정으로 진행하겠습니다."
                    },
                    {
                      "type": "제안",
                      "subject": "Re: 회의 일정",
                      "body": "가능하다면 오후 시간으로 조정 가능할까요?"
                    }
                  ]
                }
                """);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate,
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(false);
        when(queryService.createPrompt(eq(messageId), contains("suggestions")))
                .thenReturn(new ReplyDraftSuggestionPromptResult("system", "user"));

        // when
        service.generate(messageId);

        // then
        org.mockito.ArgumentCaptor<List<ReplyDraftSuggestion>> captor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(suggestionRepositoryPort).saveAllByMessageIdUpToActiveLimit(eq(messageId), captor.capture(), eq(4));
        assertEquals(2, captor.getValue().size());
        assertEquals("승낙", captor.getValue().get(0).getType());
        assertEquals("Re: 회의 일정", captor.getValue().get(0).getSubject());
        assertEquals("좋습니다. 해당 일정으로 진행하겠습니다.", captor.getValue().get(0).getBody());
        assertEquals(message, captor.getValue().get(0).getMessage());
        assertEquals("제안", captor.getValue().get(1).getType());
        verify(suggestionRepositoryPort, never()).save(any());
        verify(suggestionRepositoryPort, never()).delete(any());
    }

    @Test
    void generate_저장시원자적제한insert를호출한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        ChatModel chatModel = chatModel("""
                {
                  "suggestions": [
                    {
                      "type": "승낙",
                      "subject": "Re: 회의 일정",
                      "body": "좋습니다. 해당 일정으로 진행하겠습니다."
                    },
                    {
                      "type": "제안",
                      "subject": "Re: 회의 일정",
                      "body": "가능하다면 오후 시간으로 조정 가능할까요?"
                    }
                  ]
                }
                """);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate,
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(false);
        when(queryService.createPrompt(eq(messageId), contains("suggestions")))
                .thenReturn(new ReplyDraftSuggestionPromptResult("system", "user"));

        // when
        service.generate(messageId);

        // then
        verify(suggestionRepositoryPort).saveAllByMessageIdUpToActiveLimit(eq(messageId), any(), eq(4));
        verify(suggestionRepositoryPort, never()).save(any());
    }

    @Test
    void generate_답장추천모델을Prompt옵션에설정한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        TransactionTemplate transactionTemplate = transactionTemplate();
        ChatModel chatModel = mock(ChatModel.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate,
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(false);
        when(queryService.createPrompt(eq(messageId), contains("suggestions")))
                .thenReturn(new ReplyDraftSuggestionPromptResult("system", "user"));
        when(chatModel.getDefaultOptions()).thenReturn(ChatOptions.builder().model("gpt-test").build());
        when(chatModel.call(any(Prompt.class))).thenReturn(response("""
                {
                  "suggestions": [
                    {
                      "type": "승낙",
                      "subject": "Re: 회의 일정",
                      "body": "좋습니다. 해당 일정으로 진행하겠습니다."
                    },
                    {
                      "type": "제안",
                      "subject": "Re: 회의 일정",
                      "body": "가능하다면 오후 시간으로 조정 가능할까요?"
                    }
                  ]
                }
                """));

        // when
        service.generate(messageId);

        // then
        org.mockito.ArgumentCaptor<Prompt> captor = org.mockito.ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(captor.capture());
        assertEquals("reply-test", captor.getValue().getOptions().getModel());
    }

    @Test
    void generate_LLM응답이유효하지않으면저장하지않고종료한다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        ChatModel chatModel = chatModel("""
                {
                  "suggestions": [
                    {
                      "type": "승낙",
                      "subject": "Re: 회의 일정",
                      "body": "좋습니다. 해당 일정으로 진행하겠습니다."
                    }
                  ]
                }
                """);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate,
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(false);
        when(queryService.createPrompt(eq(messageId), contains("suggestions")))
                .thenReturn(new ReplyDraftSuggestionPromptResult("system", "user"));

        // when & then
        assertDoesNotThrow(() -> service.generate(messageId));

        // then
        verifyNoInteractions(transactionTemplate);
        verify(suggestionRepositoryPort, never()).save(any());
        verify(suggestionRepositoryPort, never()).saveAllByMessageIdUpToActiveLimit(any(), any(), any(Integer.class));
    }

    @Test
    void generate_메시지를찾지못하면커스텀예외를던진다() {
        // given
        UUID messageId = UUID.randomUUID();
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(chatModel("{}")),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate(),
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.empty());

        // when
        MqException exception = assertThrows(MqException.class, () -> service.generate(messageId));

        // then
        assertEquals(MqErrorCode.INVALID_REPLY_DRAFT_SUGGESTION_MESSAGE, exception.getErrorCode());
        verifyNoInteractions(queryService, suggestionRepositoryPort);
    }

    @Test
    void generate_ChatModel이없으면모델사용불가예외를던진다() {
        // given
        UUID messageId = UUID.randomUUID();
        Message message = createMessage(messageId);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        ReplyDraftSuggestionRepositoryPort suggestionRepositoryPort = mock(ReplyDraftSuggestionRepositoryPort.class);
        ReplyDraftSuggestionQueryService queryService = mock(ReplyDraftSuggestionQueryService.class);
        ReplyDraftSuggestionCommandService service = new ReplyDraftSuggestionCommandService(
                chatModelProvider(null),
                messageRepositoryPort,
                suggestionRepositoryPort,
                queryService,
                transactionTemplate(),
                modelProperties()
        );
        when(messageRepositoryPort.findByIdIncludingDeletedAndSensitiveLabelsExcluded(messageId))
                .thenReturn(Optional.of(message));
        when(queryService.existsByMessageId(messageId)).thenReturn(false);
        when(queryService.createPrompt(eq(messageId), contains("suggestions")))
                .thenReturn(new ReplyDraftSuggestionPromptResult("system", "user"));

        // when
        MqException exception = assertThrows(MqException.class, () -> service.generate(messageId));

        // then
        assertEquals(MqErrorCode.REPLY_DRAFT_SUGGESTION_CHAT_MODEL_NOT_AVAILABLE, exception.getErrorCode());
        verify(suggestionRepositoryPort, never()).save(any());
        verify(suggestionRepositoryPort, never()).saveAllByMessageIdUpToActiveLimit(any(), any(), any(Integer.class));
    }

    @SuppressWarnings("unchecked")
    private ObjectProvider<ChatModel> chatModelProvider(ChatModel chatModel) {
        ObjectProvider<ChatModel> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(chatModel);
        return provider;
    }

    private TransactionTemplate transactionTemplate() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        org.mockito.Mockito.doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        return transactionTemplate;
    }

    private ChatModel chatModel(String text) {
        return new StubChatModel(response(text));
    }

    private AiModelProperties modelProperties() {
        AiModelProperties properties = new AiModelProperties();
        properties.setDefaultModel("gpt-test");
        properties.setReplyDraftSuggestionModel("reply-test");
        properties.setAllowedModels(List.of("gpt-test", "reply-test"));
        return properties;
    }

    private ChatResponse response(String text) {
        Generation generation = new Generation(AssistantMessage.builder().content(text).build());
        ChatResponseMetadata metadata = ChatResponseMetadata.builder().model("gpt-test").build();
        return new ChatResponse(List.of(generation), metadata);
    }

    private Message createMessage(UUID messageId) {
        User user = User.builder()
                .id(UUID.randomUUID())
                .name("Alice")
                .username("alice")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("alice@example.com")
                .alias("Alice")
                .accessToken("access-token")
                .active(true)
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread-1")
                .direction(Direction.INBOUND)
                .messageCount(3)
                .read(false)
                .build();
        return Message.builder()
                .id(messageId)
                .thread(thread)
                .gmailMessageId("gmail-message-1")
                .direction(Direction.INBOUND)
                .subject("회의 일정")
                .fromAddress("sender@example.com")
                .bodyText("회의 일정 확인 부탁드립니다.")
                .read(false)
                .build();
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
