package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("InboxCommandService 테스트")
class InboxCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @InjectMocks
    private InboxCommandService inboxCommandService;

    @Nested
    @DisplayName("markThreadAsRead")
    class MarkThreadAsRead {

        @Test
        @DisplayName("같은 지메일 스레드의 양방향 스레드와 메시지를 모두 읽음 처리한다")
        void markThreadAsRead_같은지메일스레드의양방향스레드와메시지를모두읽음처리한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-1";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread inboundThread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, false);
            Thread outboundThread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, false);
            Message inboundMessage = createMessage(inboundThread, "gmail-message-1", false);
            Message outboundMessage = createMessage(outboundThread, "gmail-message-2", false);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundThread, outboundThread));
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundMessage, outboundMessage));

            // when
            inboxCommandService.markThreadAsRead(inboundThread);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertTrue(inboundThread.isRead());
            assertTrue(outboundThread.isRead());
            assertTrue(inboundMessage.isRead());
            assertTrue(outboundMessage.isRead());
        }

        @Test
        @DisplayName("이미 읽은 메시지에도 안전하게 동작한다")
        void markThreadAsRead_이미읽은메시지에도안전하게동작한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-2";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread thread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, true);
            Message readMessage = createMessage(thread, "gmail-message-3", true);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(thread));
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(readMessage));

            // when
            inboxCommandService.markThreadAsRead(thread);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertTrue(thread.isRead());
            assertTrue(readMessage.isRead());
        }
    }

    @Nested
    @DisplayName("markThreadAsUnread")
    class MarkThreadAsUnread {

        @Test
        @DisplayName("같은 지메일 스레드의 양방향 스레드와 메시지를 모두 안읽음 처리한다")
        void markThreadAsUnread_같은지메일스레드의양방향스레드와메시지를모두안읽음처리한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-3";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread inboundThread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, true);
            Thread outboundThread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, true);
            Message inboundMessage = createMessage(inboundThread, "gmail-message-4", true);
            Message outboundMessage = createMessage(outboundThread, "gmail-message-5", true);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundThread, outboundThread));
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundMessage, outboundMessage));

            // when
            inboxCommandService.markThreadAsUnread(inboundThread);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertFalse(inboundThread.isRead());
            assertFalse(outboundThread.isRead());
            assertFalse(inboundMessage.isRead());
            assertFalse(outboundMessage.isRead());
        }

        @Test
        @DisplayName("이미 안읽은 메시지에도 안전하게 동작한다")
        void markThreadAsUnread_이미안읽은메시지에도안전하게동작한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-4";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread thread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, false);
            Message unreadMessage = createMessage(thread, "gmail-message-6", false);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(thread));
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(unreadMessage));

            // when
            inboxCommandService.markThreadAsUnread(thread);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertFalse(thread.isRead());
            assertFalse(unreadMessage.isRead());
        }
    }

    @Nested
    @DisplayName("markMessageAsRead")
    class MarkMessageAsRead {

        @Test
        @DisplayName("모든 메시지가 읽음이면 스레드도 읽음 처리한다")
        void markMessageAsRead_모든메시지가읽음이면스레드도읽음처리한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-5";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread inboundThread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, false);
            Thread outboundThread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, false);
            Message targetMessage = createMessage(inboundThread, "gmail-message-7", false);
            Message alreadyReadMessage = createMessage(outboundThread, "gmail-message-8", true);

            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(targetMessage, alreadyReadMessage));
            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundThread, outboundThread));

            // when
            inboxCommandService.markMessageAsRead(targetMessage);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertTrue(targetMessage.isRead());
            assertTrue(inboundThread.isRead());
            assertTrue(outboundThread.isRead());
        }

        @Test
        @DisplayName("안읽은 메시지가 남아 있으면 스레드는 안읽음으로 유지한다")
        void markMessageAsRead_안읽은메시지가남아있으면스레드는안읽음으로유지한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-6";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread thread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, false);
            Message targetMessage = createMessage(thread, "gmail-message-9", false);
            Message unreadSiblingMessage = createMessage(thread, "gmail-message-10", false);

            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(targetMessage, unreadSiblingMessage));
            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(thread));

            // when
            inboxCommandService.markMessageAsRead(targetMessage);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertTrue(targetMessage.isRead());
            assertFalse(thread.isRead());
        }
    }

    @Nested
    @DisplayName("markMessageAsUnread")
    class MarkMessageAsUnread {

        @Test
        @DisplayName("대상 메시지를 안읽음 처리하고 스레드도 안읽음 처리한다")
        void markMessageAsUnread_대상메시지를안읽음처리하고스레드도안읽음처리한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-7";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread inboundThread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, true);
            Thread outboundThread = createThread(mailAccount, gmailThreadId, Direction.OUTBOUND, true);
            Message targetMessage = createMessage(inboundThread, "gmail-message-11", true);
            Message alreadyReadMessage = createMessage(outboundThread, "gmail-message-12", true);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(inboundThread, outboundThread));

            // when
            inboxCommandService.markMessageAsUnread(targetMessage);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertFalse(targetMessage.isRead());
            assertTrue(alreadyReadMessage.isRead());
            assertFalse(inboundThread.isRead());
            assertFalse(outboundThread.isRead());
        }

        @Test
        @DisplayName("이미 안읽은 메시지에도 안전하게 동작한다")
        void markMessageAsUnread_이미안읽은메시지에도안전하게동작한다() {
            // given
            UUID mailAccountId = UUID.randomUUID();
            String gmailThreadId = "gmail-thread-8";
            MailAccount mailAccount = createMailAccount(mailAccountId);
            Thread thread = createThread(mailAccount, gmailThreadId, Direction.INBOUND, false);
            Message unreadMessage = createMessage(thread, "gmail-message-13", false);

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                    .willReturn(List.of(thread));

            // when
            inboxCommandService.markMessageAsUnread(unreadMessage);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, gmailThreadId);
            assertFalse(unreadMessage.isRead());
            assertFalse(thread.isRead());
        }
    }

    private MailAccount createMailAccount(UUID mailAccountId) {
        return MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();
    }

    private Thread createThread(MailAccount mailAccount, String gmailThreadId, Direction direction, boolean read) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(direction)
                .read(read)
                .build();
    }

    private Message createMessage(Thread thread, String gmailMessageId, boolean read) {
        return Message.builder()
                .thread(thread)
                .gmailMessageId(gmailMessageId)
                .direction(thread.getDirection())
                .fromAddress("sender@example.com")
                .read(read)
                .build();
    }
}
