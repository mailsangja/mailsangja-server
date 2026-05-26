package com.mailsangja.core.service.mail;

import com.mailsangja.core.dto.mail.GoogleMailMessageResult;
import com.mailsangja.core.dto.mail.MailAddressCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendPersistCommand;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailCommandServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    private MailCommandService service;

    @BeforeEach
    void setUp() {
        service = new MailCommandService(threadRepositoryPort, messageRepositoryPort, gmailThreadLockRepositoryPort);
    }

    @Test
    void saveSentMail_updatesAllDirectionThreadCountsWithFullConversationCount() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .emailAddress("me@example.com")
                .build();
        Thread inboundThread = createThread(mailAccount, Direction.INBOUND, 1);
        Thread outboundThread = createThread(mailAccount, Direction.OUTBOUND, 0);
        List<Message> activeMessages = new ArrayList<>();
        activeMessages.add(Message.from(inboundThread, new Message.CreateValues(
                "message-inbound",
                null,
                null,
                null,
                null,
                null,
                Direction.INBOUND,
                "inbound subject",
                "alice@example.com",
                "Alice",
                List.of("me@example.com"),
                List.of("Me"),
                List.of(),
                List.of(),
                "inbound snippet",
                false,
                LocalDateTime.of(2026, 4, 11, 10, 0),
                null,
                null
        )));

        when(threadRepositoryPort.findByMailAccountIdAndGmailThreadIdAndDirectionAndDeletedAtIsNull(
                mailAccount.getId(), "gmail-thread-1", Direction.OUTBOUND
        )).thenReturn(Optional.of(outboundThread));
        when(messageRepositoryPort.findByThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                outboundThread.getId(), "message-outbound"
        )).thenReturn(Optional.empty());
        when(messageRepositoryPort.save(any(Message.class))).thenAnswer(invocation -> {
            Message message = invocation.getArgument(0);
            activeMessages.add(message);
            return message;
        });
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(), "gmail-thread-1"
        )).thenAnswer(invocation -> List.copyOf(activeMessages));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                mailAccount.getId(), "gmail-thread-1"
        )).thenReturn(List.of(inboundThread, outboundThread));

        service.saveSentMail(new MailSendPersistCommand(
                mailAccount,
                new GoogleMailMessageResult(
                        "message-outbound",
                        "gmail-thread-1",
                        "history-1",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "outbound subject",
                        "me@example.com",
                        "Me",
                        List.of("alice@example.com"),
                        List.of("Alice"),
                        List.of(),
                        List.of(),
                        "outbound snippet",
                        LocalDateTime.of(2026, 4, 11, 11, 0),
                        "body",
                        null,
                        List.of()
                ),
                new MailSendCommand(
                        UUID.randomUUID(),
                        new MailAddressCommand("Me", "me@example.com"),
                        null,
                        List.of(new MailAddressCommand("Alice", "alice@example.com")),
                        List.of(),
                        List.of(),
                        "outbound subject",
                        "body",
                        List.of()
                )
        ));

        assertEquals(2, inboundThread.getMessageCount());
        assertEquals(2, outboundThread.getMessageCount());
    }

    private Thread createThread(MailAccount mailAccount, Direction direction, int messageCount) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread-1")
                .direction(direction)
                .read(true)
                .messageCount(messageCount)
                .build();
    }
}
