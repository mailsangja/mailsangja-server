package com.mailsangja.worker.facade;

import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.config.properties.GoogleMailInitialSyncProperties;
import com.mailsangja.worker.dto.gmail.message.GoogleMailMessageListResult;
import com.mailsangja.worker.dto.gmail.message.GoogleMailMessageResponse;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadBatchMessage;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadResult;
import com.mailsangja.worker.service.google.GoogleMailMessageQueryService;
import com.mailsangja.worker.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.worker.service.mail.InitialMailSyncCommandService;
import com.mailsangja.worker.service.mail.MailAccountQueryService;
import com.mailsangja.worker.service.messaging.MailTaskPublisherService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitialMailSyncFacade 테스트")
class InitialMailSyncFacadeTest {

    @Mock
    private MailAccountQueryService mailAccountQueryService;

    @Mock
    private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    @Mock
    private GoogleMailMessageQueryService googleMailMessageQueryService;

    @Mock
    private MailTaskPublisherService mailTaskPublisherService;

    @Mock
    private InitialMailSyncCommandService initialMailSyncCommandService;

    private final GoogleMailInitialSyncProperties googleMailInitialSyncProperties = new GoogleMailInitialSyncProperties();

    private InitialMailSyncFacade initialMailSyncFacade;

    @BeforeEach
    void setUp() {
        initialMailSyncFacade = new InitialMailSyncFacade(
                mailAccountQueryService,
                googleAccessTokenEnsureService,
                googleMailMessageQueryService,
                mailTaskPublisherService,
                googleMailInitialSyncProperties,
                initialMailSyncCommandService
        );
    }

    @Nested
    @DisplayName("handleInitialMailSync")
    class HandleInitialMailSync {

        @Test
        @DisplayName("중복 threadId를 제거하고 batch size 기준으로 분할 발행한다")
        void handleInitialMailSync_중복ThreadId를제거하고BatchSize기준으로분할발행한다() {
            // given
            googleMailInitialSyncProperties.setThreadBatchSize(2);
            MailAccount mailAccount = createMailAccount();
            InitialMailSyncMessage message = new InitialMailSyncMessage(
                    mailAccount.getId(),
                    UUID.randomUUID(),
                    "GMAIL",
                    mailAccount.getEmailAddress()
            );
            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleMailMessageQueryService.getLatestMessages(mailAccount.getAccessToken())).willReturn(
                    new GoogleMailMessageListResult(
                            List.of(
                                    new GoogleMailMessageResponse("m1", "t1"),
                                    new GoogleMailMessageResponse("m2", "t2"),
                                    new GoogleMailMessageResponse("m3", "t1"),
                                    new GoogleMailMessageResponse("m4", "t3")
                            ),
                            4
                    )
            );

            // when
            initialMailSyncFacade.handleInitialMailSync(message);

            // then
            ArgumentCaptor<InitialMailSyncThreadBatchMessage> captor = ArgumentCaptor.forClass(InitialMailSyncThreadBatchMessage.class);
            then(mailTaskPublisherService).should(org.mockito.Mockito.times(2)).publishInitialMailSyncThreadBatch(captor.capture());
            assertEquals(List.of("t1", "t2"), captor.getAllValues().getFirst().threadIds());
            assertEquals(List.of("t3"), captor.getAllValues().get(1).threadIds());
        }

        @Test
        @DisplayName("지원하지 않는 provider면 예외를 반환한다")
        void handleInitialMailSync_지원하지않는Provider면예외를반환한다() {
            // given
            InitialMailSyncMessage message = new InitialMailSyncMessage(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "NAVER",
                    "user@naver.com"
            );

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> initialMailSyncFacade.handleInitialMailSync(message)
            );

            // then
            assertEquals("MS-MAIL-UNSUPPORTED-INITIAL-MAIL-SYNC-PROVIDER", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("handleInitialMailSyncThreadBatch")
    class HandleInitialMailSyncThreadBatch {

        @Test
        @DisplayName("조회한 thread 결과를 저장 command로 변환해 저장한다")
        void handleInitialMailSyncThreadBatch_조회한Thread결과를저장Command로변환해저장한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            InitialMailSyncThreadBatchMessage message = new InitialMailSyncThreadBatchMessage(
                    mailAccount.getId(),
                    UUID.randomUUID(),
                    "GMAIL",
                    mailAccount.getEmailAddress(),
                    List.of("thread-1")
            );
            InitialMailSyncThreadResult threadResult = new InitialMailSyncThreadResult("thread-1", "history-1", List.of());

            given(mailAccountQueryService.findActiveMailAccountById(mailAccount.getId())).willReturn(mailAccount);
            given(googleMailMessageQueryService.getThreads(mailAccount.getAccessToken(), List.of("thread-1")))
                    .willReturn(List.of(threadResult));

            // when
            initialMailSyncFacade.handleInitialMailSyncThreadBatch(message);

            // then
            then(initialMailSyncCommandService).should().saveThreadBatch(eq(mailAccount), any());
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .accessToken("access-token")
                .build();
    }
}
