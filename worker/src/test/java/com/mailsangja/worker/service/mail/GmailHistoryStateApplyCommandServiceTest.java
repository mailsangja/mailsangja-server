package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushException;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailHistoryStateApplyCommandService 테스트")
class GmailHistoryStateApplyCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private GmailHistoryStateQueryService gmailHistoryStateQueryService;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private InitialMailSyncCommandService initialMailSyncCommandService;

    private GmailHistoryStateApplyCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryStateApplyCommandService(
                gmailThreadLockRepositoryPort,
                gmailHistoryStateQueryService,
                threadRepositoryPort,
                messageRepositoryPort,
                initialMailSyncCommandService
        );
    }

    @Nested
    @DisplayName("applyMessageReadState")
    class ApplyMessageReadState {

        @Test
        @DisplayName("sync command가 있고 메시지가 없으면 snapshot 저장 후 읽음 상태를 갱신한다")
        void applyMessageReadState_syncCommand가있고메시지가없으면Snapshot저장후읽음상태를갱신한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent("history-2");
            Thread inboundThread = createThread(mailAccount, true, "history-1");
            Message targetMessage = createMessage(inboundThread, "message-1", false);
            Message otherMessage = createMessage(inboundThread, "message-2", true);
            InitialMailSyncThreadSaveCommand syncCommand = new InitialMailSyncThreadSaveCommand("thread-1", "history-2", List.of());

            given(gmailHistoryStateQueryService.existsMessage(mailAccount.getId(), "thread-1", "message-1")).willReturn(false);
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(targetMessage, otherMessage));
            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(inboundThread));

            // when
            service.applyMessageReadState(mailAccount, event, true, syncCommand);

            // then
            then(initialMailSyncCommandService).should().saveMissingMessagesFromThreadSnapshot(mailAccount, syncCommand);
            assertTrue(targetMessage.isRead());
            assertTrue(inboundThread.isRead());
            assertEquals("history-2", inboundThread.getHistoryId());
        }

        @Test
        @DisplayName("대상 메시지가 없으면 예외를 반환한다")
        void applyMessageReadState_대상메시지가없으면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent("history-2");
            Thread inboundThread = createThread(mailAccount, false, "history-1");
            Message otherMessage = createMessage(inboundThread, "message-2", true);

            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(otherMessage));

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.applyMessageReadState(mailAccount, event, false, null)
            );

            // then
            assertEquals("MS-MAIL-GMAIL-HISTORY-RESULT-INVALID", exception.getErrorCode().getCode());
        }

        @Test
        @DisplayName("다른 unread 메시지가 있으면 thread는 unread 상태를 유지한다")
        void applyMessageReadState_다른Unread메시지가있으면Thread는Unread상태를유지한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent(" ");
            Thread inboundThread = createThread(mailAccount, true, "history-1");
            Message targetMessage = createMessage(inboundThread, "message-1", true);
            Message otherUnreadMessage = createMessage(inboundThread, "message-2", false);

            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(targetMessage, otherUnreadMessage));
            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(inboundThread));

            // when
            service.applyMessageReadState(mailAccount, event, true, null);

            // then
            assertTrue(targetMessage.isRead());
            assertFalse(inboundThread.isRead());
            assertEquals("history-1", inboundThread.getHistoryId());
        }

        @Test
        @DisplayName("thread가 없으면 예외를 반환한다")
        void applyMessageReadState_thread가없으면예외를반환한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent("history-2");
            Thread inboundThread = createThread(mailAccount, true, "history-1");
            Message targetMessage = createMessage(inboundThread, "message-1", true);

            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(targetMessage));
            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of());

            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.applyMessageReadState(mailAccount, event, false, null)
            );

            // then
            assertEquals("MS-MAIL-GMAIL-HISTORY-RESULT-INVALID", exception.getErrorCode().getCode());
        }
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .refreshToken("refresh-token")
                .active(true)
                .build();
    }

    private Thread createThread(MailAccount mailAccount, boolean read, String historyId) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .historyId(historyId)
                .read(read)
                .messageCount(2)
                .build();
    }

    private Message createMessage(Thread thread, String gmailMessageId, boolean read) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(gmailMessageId)
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .fromName("Sender")
                .toAddresses(List.of("user@gmail.com"))
                .toNames(List.of("User"))
                .ccAddresses(List.of())
                .ccNames(List.of())
                .read(read)
                .build();
    }

    private GmailHistoryEvent createEvent(String historyId) {
        return new GmailHistoryEvent(GmailHistoryEventType.MESSAGE_READ, UUID.randomUUID(), "message-1", "thread-1", historyId);
    }
}
