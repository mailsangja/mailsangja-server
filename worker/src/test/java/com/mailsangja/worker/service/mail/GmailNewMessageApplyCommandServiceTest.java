package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEvent;
import com.mailsangja.worker.dto.gmail.history.GmailHistoryEventType;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncMessageSaveCommand;
import com.mailsangja.worker.dto.mail.sync.InitialMailSyncThreadSaveCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailNewMessageApplyCommandService 테스트")
class GmailNewMessageApplyCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private GmailNewMessageApplyCommandService service;

    @BeforeEach
    void setUp() {
        InitialMailSyncCommandService initialMailSyncCommandService =
                new InitialMailSyncCommandService(threadRepositoryPort, messageRepositoryPort);
        service = new GmailNewMessageApplyCommandService(
                gmailThreadLockRepositoryPort,
                threadRepositoryPort,
                messageRepositoryPort,
                initialMailSyncCommandService
        );
    }

    @Nested
    @DisplayName("applyNewMessageSync")
    class ApplyNewMessageSync {

        @Test
        @DisplayName("삭제된 thread면 thread만 복원하고 새 메시지를 저장한다")
        void applyNewMessageSync_삭제된Thread면Thread만복원하고새메시지를저장한다() {
            // given
            MailAccount mailAccount = MailAccount.builder()
                    .id(UUID.randomUUID())
                    .build();
            Thread deletedThread = Thread.builder()
                    .id(UUID.randomUUID())
                    .mailAccount(mailAccount)
                    .gmailThreadId("thread-1")
                    .direction(Direction.INBOUND)
                    .read(false)
                    .messageCount(1)
                    .build();
            deletedThread.delete();

            Message oldMessage = Message.from(deletedThread, new Message.CreateValues(
                    "message-old", Direction.INBOUND, "old subject", "sender@example.com", "Sender",
                    List.of("me@example.com"), List.of("Me"), List.of(), List.of(), "old snippet", false,
                    LocalDateTime.of(2026, 4, 10, 9, 0), null, null
            ));
            oldMessage.delete();
            AtomicReference<Message> savedMessageRef = new AtomicReference<>();

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                    .willReturn(List.of(deletedThread));
            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.INBOUND
            )).willAnswer(invocation -> deletedThread.isDeleted() ? Optional.empty() : Optional.of(deletedThread));
            given(threadRepositoryPort.bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1"))
                    .willAnswer(invocation -> {
                        deletedThread.restore();
                        deletedThread.updateMessageCount(0);
                        return 1;
                    });
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(deletedThread.getId(), "message-old"))
                    .willReturn(Optional.of(oldMessage));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(deletedThread.getId(), "message-new"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> {
                Message savedMessage = invocation.getArgument(0);
                savedMessageRef.set(savedMessage);
                return savedMessage;
            });
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", "message-new"
            )).willAnswer(invocation -> Optional.ofNullable(savedMessageRef.get()));

            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_ADDED,
                    mailAccount.getId(),
                    "message-new",
                    "thread-1",
                    "history-2"
            );
            InitialMailSyncThreadSaveCommand syncCommand = createSyncCommand("thread-1", "history-2", "message-new", "new subject", "new snippet");

            // when
            service.applyNewMessageSync(mailAccount, event, syncCommand);

            // then
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);
            then(messageRepositoryPort).should().save(messageCaptor.capture());
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, "thread-1");
            then(threadRepositoryPort).should().bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-1");
            assertFalse(deletedThread.isDeleted());
            assertTrue(oldMessage.isDeleted());
            assertEquals("message-new", messageCaptor.getValue().getGmailMessageId());
            assertEquals(1, deletedThread.getMessageCount());
            assertEquals("new subject", deletedThread.getLatestSubject());
            assertEquals("new snippet", deletedThread.getLatestSnippet());
            assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), deletedThread.getLastMessageAt());
        }

        @Test
        @DisplayName("활성 thread면 복원 없이 새 메시지를 저장하고 thread 정보를 갱신한다")
        void applyNewMessageSync_활성Thread면복원없이새메시지를저장하고Thread정보를갱신한다() {
            // given
            MailAccount mailAccount = MailAccount.builder()
                    .id(UUID.randomUUID())
                    .build();
            Thread activeThread = Thread.builder()
                    .id(UUID.randomUUID())
                    .mailAccount(mailAccount)
                    .gmailThreadId("thread-2")
                    .direction(Direction.INBOUND)
                    .read(false)
                    .messageCount(1)
                    .build();

            Message existingMessage = Message.from(activeThread, new Message.CreateValues(
                    "message-old", Direction.INBOUND, "old subject", "sender@example.com", "Sender",
                    List.of("me@example.com"), List.of("Me"), List.of(), List.of(), "old snippet", false,
                    LocalDateTime.of(2026, 4, 10, 9, 0), null, null
            ));
            AtomicReference<Message> savedMessageRef = new AtomicReference<>();

            given(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadId(mailAccount.getId(), "thread-2"))
                    .willReturn(List.of(activeThread));
            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-2", Direction.INBOUND
            )).willReturn(Optional.of(activeThread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(activeThread.getId(), "message-old"))
                    .willReturn(Optional.of(existingMessage));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(activeThread.getId(), "message-new"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> {
                Message savedMessage = invocation.getArgument(0);
                savedMessageRef.set(savedMessage);
                return savedMessage;
            });
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-2", "message-new"
            )).willAnswer(invocation -> Optional.ofNullable(savedMessageRef.get()));

            GmailHistoryEvent event = new GmailHistoryEvent(
                    GmailHistoryEventType.MESSAGE_ADDED,
                    mailAccount.getId(),
                    "message-new",
                    "thread-2",
                    "history-2"
            );
            InitialMailSyncThreadSaveCommand syncCommand = createSyncCommand("thread-2", "history-2", "message-new", "new subject", "new snippet");

            // when
            service.applyNewMessageSync(mailAccount, event, syncCommand);

            // then
            then(gmailThreadLockRepositoryPort).should().acquireThreadLock(mailAccount, "thread-2");
            then(threadRepositoryPort).should(never()).bulkRestoreAndResetMessageCountByMailAccountIdAndGmailThreadId(any(), any());
            assertFalse(activeThread.isDeleted());
            assertEquals(2, activeThread.getMessageCount());
            assertEquals("new subject", activeThread.getLatestSubject());
            assertEquals(LocalDateTime.of(2026, 4, 14, 10, 0), activeThread.getLastMessageAt());
        }
    }

    private InitialMailSyncThreadSaveCommand createSyncCommand(
            String gmailThreadId,
            String historyId,
            String newMessageId,
            String newSubject,
            String newSnippet
    ) {
        return new InitialMailSyncThreadSaveCommand(
                gmailThreadId,
                historyId,
                List.of(
                        new InitialMailSyncMessageSaveCommand(
                                "message-old", "history-1", Direction.INBOUND,
                                "old subject", "sender@example.com", "Sender", List.of("me@example.com"),
                                List.of("Me"), List.of(), List.of(), "old snippet", false,
                                LocalDateTime.of(2026, 4, 10, 9, 0), null, null, List.of()
                        ),
                        new InitialMailSyncMessageSaveCommand(
                                newMessageId, historyId, Direction.INBOUND,
                                newSubject, "sender@example.com", "Sender", List.of("me@example.com"),
                                List.of("Me"), List.of(), List.of(), newSnippet, false,
                                LocalDateTime.of(2026, 4, 14, 10, 0), null, null, List.of()
                        )
                )
        );
    }
}
