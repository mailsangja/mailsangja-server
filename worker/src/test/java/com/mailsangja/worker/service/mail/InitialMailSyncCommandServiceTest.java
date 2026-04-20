package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import com.mailsangja.worker.common.exception.mail.MailPushException;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("InitialMailSyncCommandService 테스트")
class InitialMailSyncCommandServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private InitialMailSyncCommandService service;

    @BeforeEach
    void setUp() {
        service = new InitialMailSyncCommandService(threadRepositoryPort, messageRepositoryPort);
    }

    @Nested
    @DisplayName("saveThreadBatch")
    class SaveThreadBatch {

        @Test
        @DisplayName("기존 메시지를 업데이트하면 messageCount를 증가시키지 않는다")
        void saveThreadBatch_기존메시지를업데이트하면MessageCount를증가시키지않는다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
            AtomicReference<Message> storedMessage = new AtomicReference<>();

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.INBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willAnswer(invocation -> Optional.ofNullable(storedMessage.get()));
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> {
                Message message = invocation.getArgument(0);
                storedMessage.set(message);
                return message;
            });

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.INBOUND,
                            "subject",
                            "alice@example.com",
                            "Alice",
                            java.util.List.of("bob@example.com"),
                            java.util.List.of("Bob"),
                            java.util.List.of(),
                            java.util.List.of(),
                            "snippet",
                            false,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-2",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-2",
                            Direction.INBOUND,
                            "updated subject",
                            "alice@example.com",
                            "Alice",
                            java.util.List.of("bob@example.com"),
                            java.util.List.of("Bob"),
                            java.util.List.of(),
                            java.util.List.of(),
                            "updated snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 11, 0)
                    ))
            )));

            // then
            assertEquals(1, thread.getMessageCount());
            assertEquals("updated subject", thread.getLatestSubject());
            assertEquals("updated snippet", thread.getLatestSnippet());
            then(messageRepositoryPort).should(times(1)).save(any(Message.class));
        }

        @Test
        @DisplayName("sentAt이 null인 메시지는 최신 메시지 정보를 덮어쓰지 않는다")
        void saveThreadBatch_sentAt이Null인메시지는최신메시지정보를덮어쓰지않는다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
            AtomicReference<Message> firstMessage = new AtomicReference<>();

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.INBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willAnswer(invocation -> Optional.ofNullable(firstMessage.get()));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-2"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> {
                Message message = invocation.getArgument(0);
                if ("message-1".equals(message.getGmailMessageId())) {
                    firstMessage.set(message);
                }
                return message;
            });

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.INBOUND,
                            "subject-1",
                            "alice@example.com",
                            "Alice",
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of(),
                            "snippet-1",
                            false,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-2",
                    java.util.List.of(createMessageCommand(
                            "message-2",
                            "history-2",
                            Direction.INBOUND,
                            "subject-2",
                            "carol@example.com",
                            "Carol",
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of(),
                            "snippet-2",
                            true,
                            null
                    ))
            )));

            // then
            assertEquals("subject-1", thread.getLatestSubject());
            assertEquals("snippet-1", thread.getLatestSnippet());
            assertEquals(LocalDateTime.of(2026, 4, 11, 10, 0), thread.getLastMessageAt());
            assertEquals(2, thread.getMessageCount());
        }

        @Test
        @DisplayName("이름이 없으면 주소를 fallback으로 사용한다")
        void saveThreadBatch_이름이없으면주소를Fallback으로사용한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.OUTBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.OUTBOUND,
                            "subject",
                            "sender@example.com",
                            null,
                            java.util.List.of("first@example.com", "second@example.com"),
                            Arrays.asList((String) null),
                            java.util.List.of("cc@example.com"),
                            java.util.List.of(""),
                            "snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));

            // then
            then(messageRepositoryPort).should().save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();
            assertEquals("sender@example.com", savedMessage.getFromName());
            assertEquals(java.util.List.of("first@example.com", "second@example.com"), savedMessage.getToNames());
            assertEquals(java.util.List.of("cc@example.com"), savedMessage.getCcNames());
            assertEquals("first@example.com", thread.getLatestParticipantAddress());
            assertEquals("first@example.com", thread.getLatestParticipantName());
        }

        @Test
        @DisplayName("이름 필드를 저장 전에 trim 처리한다")
        void saveThreadBatch_이름필드를저장전에Trim처리한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);
            ArgumentCaptor<Message> messageCaptor = ArgumentCaptor.forClass(Message.class);

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.OUTBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.OUTBOUND,
                            "subject",
                            "sender@example.com",
                            "  Sender Name  ",
                            java.util.List.of("first@example.com"),
                            java.util.List.of("  First Receiver  "),
                            java.util.List.of("cc@example.com"),
                            java.util.List.of("  "),
                            "snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));

            // then
            then(messageRepositoryPort).should().save(messageCaptor.capture());
            Message savedMessage = messageCaptor.getValue();
            assertEquals("Sender Name", savedMessage.getFromName());
            assertEquals(java.util.List.of("First Receiver"), savedMessage.getToNames());
            assertEquals(java.util.List.of("cc@example.com"), savedMessage.getCcNames());
            assertEquals("First Receiver", thread.getLatestParticipantName());
        }

        @Test
        @DisplayName("To가 비어 있으면 Cc를 최신 참여자로 사용한다")
        void saveThreadBatch_to가비어있으면Cc를최신참여자로사용한다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.OUTBOUND);

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.OUTBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willReturn(Optional.empty());
            given(messageRepositoryPort.save(any(Message.class))).willAnswer(invocation -> invocation.getArgument(0));

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.OUTBOUND,
                            "subject",
                            "sender@example.com",
                            "Sender",
                            java.util.List.of(),
                            java.util.List.of(),
                            java.util.List.of("cc@example.com"),
                            java.util.List.of("CC Receiver"),
                            "snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));

            // then
            assertEquals("cc@example.com", thread.getLatestParticipantAddress());
            assertEquals("CC Receiver", thread.getLatestParticipantName());
        }

        @Test
        @DisplayName("soft delete된 메시지는 재삽입하지 않는다")
        void saveThreadBatch_softDelete된메시지는재삽입하지않는다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
            Message deletedMessage = createExistingMessage(thread, "message-1");
            deletedMessage.delete();

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.INBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willReturn(Optional.of(deletedMessage));

            // when
            service.saveThreadBatch(mailAccount, java.util.List.of(new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.INBOUND,
                            "subject",
                            "alice@example.com",
                            "Alice",
                            java.util.List.of("user@gmail.com"),
                            java.util.List.of("User"),
                            java.util.List.of(),
                            java.util.List.of(),
                            "snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            )));

            // then
            assertEquals(0, thread.getMessageCount());
            then(messageRepositoryPort).should(times(0)).save(any(Message.class));
        }
    }

    @Nested
    @DisplayName("saveMissingMessagesFromThreadSnapshot")
    class SaveMissingMessagesFromThreadSnapshot {

        @Test
        @DisplayName("기존 메시지가 있으면 재삽입하지 않는다")
        void saveMissingMessagesFromThreadSnapshot_기존메시지가있으면재삽입하지않는다() {
            // given
            MailAccount mailAccount = createMailAccount();
            Thread thread = createThread(mailAccount, "thread-1", Direction.INBOUND);
            Message existingMessage = createExistingMessage(thread, "message-1");

            given(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                    mailAccount.getId(), "thread-1", Direction.INBOUND
            )).willReturn(Optional.of(thread));
            given(messageRepositoryPort.findByThreadIdAndGmailMessageId(thread.getId(), "message-1"))
                    .willReturn(Optional.of(existingMessage));

            // when
            service.saveMissingMessagesFromThreadSnapshot(mailAccount, new InitialMailSyncThreadSaveCommand(
                    "thread-1",
                    "history-1",
                    java.util.List.of(createMessageCommand(
                            "message-1",
                            "history-1",
                            Direction.INBOUND,
                            "subject",
                            "alice@example.com",
                            "Alice",
                            java.util.List.of("user@gmail.com"),
                            java.util.List.of("User"),
                            java.util.List.of(),
                            java.util.List.of(),
                            "snippet",
                            true,
                            LocalDateTime.of(2026, 4, 11, 10, 0)
                    ))
            ));

            // then
            assertEquals(0, thread.getMessageCount());
            then(messageRepositoryPort).should(times(0)).save(any(Message.class));
        }

        @Test
        @DisplayName("command가 없으면 예외를 반환한다")
        void saveMissingMessagesFromThreadSnapshot_command가없으면예외를반환한다() {
            // when
            MailPushException exception = assertThrows(
                    MailPushException.class,
                    () -> service.saveMissingMessagesFromThreadSnapshot(createMailAccount(), null)
            );

            // then
            assertEquals("MS-MAIL-INVALID-INITIAL-MAIL-SYNC-COMMAND", exception.getErrorCode().getCode());
        }
    }

    private InitialMailSyncMessageSaveCommand createMessageCommand(
            String gmailMessageId,
            String historyId,
            Direction direction,
            String subject,
            String fromAddress,
            String fromName,
            java.util.List<String> toAddresses,
            java.util.List<String> toNames,
            java.util.List<String> ccAddresses,
            java.util.List<String> ccNames,
            String snippet,
            boolean read,
            LocalDateTime sentAt
    ) {
        return new InitialMailSyncMessageSaveCommand(
                gmailMessageId,
                historyId,
                direction,
                subject,
                fromAddress,
                fromName,
                toAddresses,
                toNames,
                ccAddresses,
                ccNames,
                snippet,
                read,
                sentAt,
                "body",
                null,
                java.util.List.of()
        );
    }

    private Message createExistingMessage(Thread thread, String gmailMessageId) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(gmailMessageId)
                .direction(Direction.INBOUND)
                .fromAddress("alice@example.com")
                .toAddresses(java.util.List.of("user@gmail.com"))
                .toNames(java.util.List.of("User"))
                .ccAddresses(java.util.List.of())
                .ccNames(java.util.List.of())
                .read(true)
                .build();
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .build();
    }

    private Thread createThread(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(direction)
                .read(true)
                .messageCount(0)
                .build();
    }
}
