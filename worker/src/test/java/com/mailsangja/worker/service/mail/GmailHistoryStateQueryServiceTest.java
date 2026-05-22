package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GmailHistoryStateQueryServiceTest {

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    private GmailHistoryStateQueryService service;

    @BeforeEach
    void setUp() {
        service = new GmailHistoryStateQueryService(messageRepositoryPort);
    }

    @Test
    void findMessage_입력이유효하지않으면Repository를호출하지않고빈값을반환한다() {
        assertTrue(service.findMessage(null, "thread-1", "message-1").isEmpty());
        assertTrue(service.findMessage(UUID.randomUUID(), " ", "message-1").isEmpty());
        assertTrue(service.findMessage(UUID.randomUUID(), "thread-1", null).isEmpty());

        verify(messageRepositoryPort, never())
                .findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(null, "thread-1", "message-1");
    }

    @Test
    void existsMessage_조회결과존재여부를반환한다() {
        UUID mailAccountId = UUID.randomUUID();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("message-1")
                .build();

        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-1"
        )).thenReturn(Optional.of(message));
        when(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                mailAccountId, "thread-1", "message-2"
        )).thenReturn(Optional.empty());

        assertTrue(service.existsMessage(mailAccountId, "thread-1", "message-1"));
        assertFalse(service.existsMessage(mailAccountId, "thread-1", "message-2"));
    }
}
