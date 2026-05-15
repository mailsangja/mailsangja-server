package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.ai.embedding.MailEmbeddingMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncSaveResult;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.messaging.publisher.InitialMailSyncThreadBatchPublisher;
import com.mailsangja.worker.messaging.publisher.LabelReclassifyPublisher;
import com.mailsangja.worker.messaging.publisher.MailEmbeddingPublisher;
import com.mailsangja.worker.service.google.GmailMessageApiService;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.InitialMailSyncCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InitialMailSyncListenerTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @Mock
    private GmailMessageApiService gmailMessageApiService;

    @Mock
    private InitialMailSyncThreadBatchPublisher initialMailSyncThreadBatchPublisher;

    @Mock
    private InitialMailSyncCommandService initialMailSyncCommandService;

    @Mock
    private GoogleMailInitialSyncProperties googleMailInitialSyncProperties;

    @Mock
    private LabelQueryService labelQueryService;

    @Mock
    private LabelReclassifyPublisher labelReclassifyPublisher;

    @Mock
    private MailEmbeddingPublisher mailEmbeddingPublisher;

    private InitialMailSyncListener listener;

    @BeforeEach
    void setUp() {
        listener = new InitialMailSyncListener(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                gmailMessageApiService,
                initialMailSyncThreadBatchPublisher,
                initialMailSyncCommandService,
                googleMailInitialSyncProperties,
                labelQueryService,
                labelReclassifyPublisher,
                mailEmbeddingPublisher
        );
    }

    @Test
    void handleThreadBatch_저장결과의MessageId마다임베딩메시지를발행한다() {
        // given
        UUID mailAccountId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID threadId = UUID.randomUUID();
        UUID firstMessageId = UUID.randomUUID();
        UUID secondMessageId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        MailAccount mailAccount = createMailAccount(mailAccountId);
        InitialMailSyncThreadBatchMessage message = new InitialMailSyncThreadBatchMessage(
                mailAccountId,
                userId,
                MailProvider.GMAIL.name(),
                "alice@example.com",
                List.of("gmail-thread-1")
        );

        when(mailAccountQueryService.findActiveMailAccountById(mailAccountId)).thenReturn(mailAccount);
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(mailAccount)).thenReturn(mailAccount);
        when(gmailMessageApiService.getThreads("access-token", List.of("gmail-thread-1")))
                .thenReturn(List.of(new InitialMailSyncThreadResult("gmail-thread-1", "history-1", List.of())));
        when(initialMailSyncCommandService.saveThreadBatch(eq(mailAccount), anyList()))
                .thenReturn(new InitialMailSyncSaveResult(List.of(threadId), List.of(firstMessageId, secondMessageId)));
        when(labelQueryService.findActiveLabelIdsByUserId(userId)).thenReturn(Set.of(labelId));

        // when
        listener.handleThreadBatch(message);

        // then
        ArgumentCaptor<MailEmbeddingMessage> embeddingMessageCaptor = ArgumentCaptor.forClass(MailEmbeddingMessage.class);
        verify(mailEmbeddingPublisher, times(2)).publish(embeddingMessageCaptor.capture());
        assertEquals(List.of(firstMessageId, secondMessageId), embeddingMessageCaptor.getAllValues().stream()
                .map(MailEmbeddingMessage::messageId)
                .toList());
        verify(labelReclassifyPublisher).publish(userId, Set.of(labelId), List.of(threadId));
    }

    private MailAccount createMailAccount(UUID mailAccountId) {
        return MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("alice@example.com")
                .alias("Alice")
                .accessToken("access-token")
                .active(true)
                .build();
    }
}
