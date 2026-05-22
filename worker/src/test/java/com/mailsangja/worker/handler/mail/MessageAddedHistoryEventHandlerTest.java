package com.mailsangja.worker.handler.mail;

import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.user.Plan;
import com.mailsangja.db.entity.user.Role;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.label.MessageBatch;
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
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
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

    @Test
    void handle_동기화결과가비어있으면후속작업을하지않는다() {
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(UUID.randomUUID(), mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.empty());

        handler.handle(mailAccount, event);

        verifyNoInteractions(mailEmbeddingPublisher);
        verifyNoInteractions(replyDraftSuggestionPublisher);
        verifyNoInteractions(labelQueryService);
        verifyNoInteractions(fcmPushCommandService);
    }

    @Test
    void handle_수신메일이고답장추천대상이면답장추천메시지를발행한다() {
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
                2
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(replyDraftSuggestionQueryService.isEligible(2)).thenReturn(true);
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        handler.handle(mailAccount, event);

        ArgumentCaptor<ReplyDraftSuggestionMessage> messageCaptor = ArgumentCaptor.forClass(ReplyDraftSuggestionMessage.class);
        verify(replyDraftSuggestionPublisher).publish(messageCaptor.capture());
        assertEquals(messageId, messageCaptor.getValue().messageId());
    }

    @Test
    void handle_발신메일이면FCM과답장추천을발행하지않는다() {
        UUID userId = UUID.randomUUID();
        UUID mailAccountId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(userId, mailAccountId);
        GmailHistoryEvent event = createEvent(mailAccountId);
        NewMailPushContext context = new NewMailPushContext(
                mailAccountId,
                "Alice",
                "subject",
                "snippet",
                UUID.randomUUID(),
                UUID.randomUUID(),
                Direction.OUTBOUND,
                1
        );

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of());

        handler.handle(mailAccount, event);

        verify(fcmPushCommandService, never()).sendNewMailPush(context);
        verifyNoInteractions(replyDraftSuggestionPublisher);
    }

    @Test
    void handle_매칭된라벨이Silent이면FCM을보내지않는다() {
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
                1
        );
        Label silentLabel = createLabel(NotificationPolicy.SILENT);
        Message message = Message.builder()
                .id(messageId)
                .gmailMessageId("gmail-message-1")
                .build();

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of(silentLabel));
        when(labelQueryService.findActiveMessageWithLabelsById(messageId)).thenReturn(Optional.of(message));
        when(attachmentRepositoryPort.findAllByMessageIdAndDeletedAtIsNull(messageId)).thenReturn(List.of());
        when(labelRuleCompiler.compile(List.of(silentLabel), new MessageBatch(List.of(message), Set.of())))
                .thenReturn(Map.of(messageId, List.of(silentLabel)));

        handler.handle(mailAccount, event);

        verify(messageLabelCommandService).applyLabels(List.of(message), Map.of(messageId, List.of(silentLabel)), Set.of(silentLabel.getId()));
        verify(fcmPushCommandService, never()).sendNewMailPush(context);
    }

    @Test
    void handle_라벨적용중예외가나면FCM발송으로폴백한다() {
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
                1
        );
        Label normalLabel = createLabel(NotificationPolicy.INHERIT);

        when(gmailNewMessageSyncCommandService.syncNewMessage(mailAccount, event)).thenReturn(Optional.of(context));
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of(normalLabel));
        when(labelQueryService.findActiveMessageWithLabelsById(messageId)).thenThrow(new RuntimeException("label failed"));

        handler.handle(mailAccount, event);

        verify(fcmPushCommandService).sendNewMailPush(context);
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

    private Label createLabel(NotificationPolicy notificationPolicy) {
        return Label.builder()
                .id(UUID.randomUUID())
                .name("label")
                .colorCode("#111111")
                .notificationPolicy(notificationPolicy)
                .displayOrder(1)
                .build();
    }
}
