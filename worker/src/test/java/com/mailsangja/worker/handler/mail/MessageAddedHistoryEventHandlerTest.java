package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.reply.ReplyDraftSuggestionMessage;
import com.mailsangja.worker.dto.notification.NewMailPushContext;
import com.mailsangja.worker.handler.label.LabelRuleCompiler;
import com.mailsangja.worker.messaging.publisher.MailEmbeddingPublisher;
import com.mailsangja.worker.messaging.publisher.ReplyDraftSuggestionPublisher;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.label.MessageLabelCommandService;
import com.mailsangja.worker.service.mail.GmailNewMessageSyncCommandService;
import com.mailsangja.worker.service.mail.ReplyDraftSuggestionQueryService;
import com.mailsangja.worker.service.notification.FcmPushCommandService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageAddedHistoryEventHandlerTest {

    @Mock
    private GmailNewMessageSyncCommandService gmailNewMessageSyncCommandService;

    @Mock
    private LabelQueryService labelQueryService;

    @Mock
    private LabelRuleCompiler labelRuleCompiler;

    @Mock
    private MessageLabelCommandService messageLabelCommandService;

    @Mock
    private AttachmentRepositoryPort attachmentRepositoryPort;

    @Mock
    private FcmPushCommandService fcmPushCommandService;

    @Mock
    private MailEmbeddingPublisher mailEmbeddingPublisher;

    @Mock
    private ReplyDraftSuggestionQueryService replyDraftSuggestionQueryService;

    @Mock
    private ReplyDraftSuggestionPublisher replyDraftSuggestionPublisher;

    private MessageAddedHistoryEventHandler handler;

    @BeforeEach
    void setUp() {
        handler = new MessageAddedHistoryEventHandler(
                gmailNewMessageSyncCommandService,
                labelQueryService,
                labelRuleCompiler,
                messageLabelCommandService,
                attachmentRepositoryPort,
                fcmPushCommandService,
                mailEmbeddingPublisher,
                replyDraftSuggestionQueryService,
                replyDraftSuggestionPublisher
        );
    }

    @Test
    void handle_새메일Context가있으면MessageId로임베딩메시지를발행한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                threadId,
                messageId,
                Direction.INBOUND
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        // when
        handler.handle(mailAccount, event);

        // then
        ArgumentCaptor<MailEmbeddingMessage> messageCaptor = ArgumentCaptor.forClass(MailEmbeddingMessage.class);
        verify(mailEmbeddingPublisher).publish(messageCaptor.capture());
        assertEquals(messageId, messageCaptor.getValue().messageId());
        verify(fcmPushCommandService).sendNewMailPush(context);
    }

    @Test
    void handle_수신메일To에연결계정이있으면답장초안메시지를발행한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                threadId,
                messageId,
                Direction.INBOUND,
                List.of(" ALICE@example.com "),
                3
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(replyDraftSuggestionQueryService.isEligible(mailAccountId, "gmail-thread-1", 3)).thenReturn(true);
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        // when
        handler.handle(mailAccount, event);

        // then
        ArgumentCaptor<ReplyDraftSuggestionMessage> messageCaptor = ArgumentCaptor.forClass(ReplyDraftSuggestionMessage.class);
        verify(replyDraftSuggestionPublisher).publish(messageCaptor.capture());
        assertEquals(messageId, messageCaptor.getValue().messageId());
    }

    @Test
    void handle_수신메일To에연결계정이있어도답장초안대상이아니면발행하지않는다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                UUID.randomUUID(),
                messageId,
                Direction.INBOUND,
                List.of("alice@example.com"),
                3
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(replyDraftSuggestionQueryService.isEligible(mailAccountId, "gmail-thread-1", 3)).thenReturn(false);
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        // when
        handler.handle(mailAccount, event);

        // then
        verifyNoInteractions(replyDraftSuggestionPublisher);
    }

    @Test
    void handle_수신메일To에연결계정이없으면답장초안메시지를발행하지않는다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                UUID.randomUUID(),
                messageId,
                Direction.INBOUND,
                List.of("other@example.com"),
                3
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        // when
        handler.handle(mailAccount, event);

        // then
        verifyNoInteractions(replyDraftSuggestionQueryService);
        verifyNoInteractions(replyDraftSuggestionPublisher);
    }

    @Test
    void handle_임베딩메시지발행에실패하면예외를전파하고FCM은보내지않는다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                UUID.randomUUID(),
                messageId,
                Direction.INBOUND
        );
        RuntimeException publishException = new RuntimeException("publish failed");

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        doThrow(publishException).when(mailEmbeddingPublisher).publish(new MailEmbeddingMessage(messageId));

        // when & then
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.handle(mailAccount, event));
        assertEquals(publishException, thrown);
        verifyNoInteractions(fcmPushCommandService);
    }

    private MailAccount createMailAccount(UUID userId, UUID mailAccountId) {
        User user = User.builder()
                .id(userId)
                .name("Alice")
                .username("alice")
                .password("password")
                .plan(Plan.FREE)
                .role(Role.USER)
                .build();
        return MailAccount.builder()
                .id(mailAccountId)
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("alice@example.com")
                .alias("Alice")
                .accessToken("access-token")
                .active(true)
                .build();
    }

    private GmailHistoryEvent createEvent(UUID mailAccountId) {
        return new GmailHistoryEvent(
                GmailHistoryEventType.MESSAGE_ADDED,
                mailAccountId,
                "gmail-message-1",
                "gmail-thread-1",
                "history-1"
        );
    }
}
