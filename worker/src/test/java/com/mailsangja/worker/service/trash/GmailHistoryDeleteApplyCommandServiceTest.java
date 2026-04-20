package com.mailsangja.worker.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailHistoryDeleteApplyCommandService 테스트")
class GmailHistoryDeleteApplyCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private GmailHistoryDeleteApplyCommandService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryDeleteApplyCommandService(
                gmailThreadLockRepositoryPort,
                threadRepositoryPort,
                messageRepositoryPort
        );
    }

    @Nested
    @DisplayName("applyMessageTrashed")
    class ApplyMessageTrashed {

        @Test
        @DisplayName("대상 메시지가 없으면 아무 작업도 하지 않는다")
        void applyMessageTrashed_대상메시지가없으면아무작업도하지않는다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent();
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", "message-1"
            )).willReturn(Optional.empty());

            // when
            service.applyMessageTrashed(mailAccount, event);

            // then
            then(threadRepositoryPort).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("마지막 활성 메시지를 휴지통 처리하면 thread도 soft delete 한다")
        void applyMessageTrashed_마지막활성메시지를휴지통처리하면Thread도SoftDelete한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent();
            Message message = createMessage(mailAccount, false);
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", "message-1"
            )).willReturn(Optional.of(message));
            given(messageRepositoryPort.existsByMailAccountIdAndGmailThreadIdAndDeletedAtIsNullAndGmailMessageIdNot(
                    mailAccount.getId(), "thread-1", "message-1"
            )).willReturn(false);

            // when
            service.applyMessageTrashed(mailAccount, event);

            // then
            assertTrue(message.isDeleted());
            then(threadRepositoryPort).should().bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                    any(), any(), any(LocalDateTime.class)
            );
        }
    }

    @Nested
    @DisplayName("applyMessageRestored")
    class ApplyMessageRestored {

        @Test
        @DisplayName("삭제된 메시지를 복원하면 thread도 복원한다")
        void applyMessageRestored_삭제된메시지를복원하면Thread도복원한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent();
            Message message = createMessage(mailAccount, true);
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                    mailAccount.getId(), "thread-1", "message-1"
            )).willReturn(Optional.of(message));

            // when
            service.applyMessageRestored(mailAccount, event);

            // then
            assertFalse(message.isDeleted());
            then(threadRepositoryPort).should().bulkRestoreByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
        }
    }

    @Nested
    @DisplayName("applyMessagePermanentlyDeleted")
    class ApplyMessagePermanentlyDeleted {

        @Test
        @DisplayName("메시지가 남아있지 않으면 thread도 hard delete 한다")
        void applyMessagePermanentlyDeleted_메시지가남아있지않으면Thread도HardDelete한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            GmailHistoryEvent event = createEvent();
            Message message = createMessage(mailAccount, false);
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageId(
                    mailAccount.getId(), "thread-1", "message-1"
            )).willReturn(Optional.of(message));
            given(messageRepositoryPort.existsByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1")).willReturn(false);

            // when
            service.applyMessagePermanentlyDeleted(mailAccount, event);

            // then
            then(messageRepositoryPort).should().hardDelete(message);
            then(threadRepositoryPort).should().hardDeleteAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
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

    private Message createMessage(MailAccount mailAccount, boolean deleted) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .read(true)
                .messageCount(1)
                .build();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .toAddresses(java.util.List.of("user@gmail.com"))
                .toNames(java.util.List.of("User"))
                .ccAddresses(java.util.List.of())
                .ccNames(java.util.List.of())
                .read(true)
                .build();
        if (deleted) {
            message.delete();
        }
        return message;
    }

    private GmailHistoryEvent createEvent() {
        return new GmailHistoryEvent(GmailHistoryEventType.MESSAGE_TRASHED, UUID.randomUUID(), "message-1", "thread-1", "history-1");
    }
}
