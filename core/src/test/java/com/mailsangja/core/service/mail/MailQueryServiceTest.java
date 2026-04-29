package com.mailsangja.core.service.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.MailAccountRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MailQueryServiceTest {

    @Test
    void findActiveSenderMailAccount_활성발신계정이면반환한다() {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        MailQueryService service = new MailQueryService(mailAccountRepositoryPort, messageRepositoryPort);
        UUID userId = UUID.randomUUID();
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(userId).build())
                .emailAddress("sender@example.com")
                .active(true)
                .build();
        when(mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(
                userId,
                "sender@example.com",
                true
        )).thenReturn(Optional.of(mailAccount));

        MailAccount result = service.findActiveSenderMailAccount(userId, "sender@example.com");

        assertSame(mailAccount, result);
    }

    @Test
    void findActiveSenderMailAccount_활성발신계정이없으면실패한다() {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        MailQueryService service = new MailQueryService(mailAccountRepositoryPort, messageRepositoryPort);
        UUID userId = UUID.randomUUID();
        when(mailAccountRepositoryPort.findByUserIdAndEmailAddressAndActiveAndDeletedAtIsNull(
                userId,
                "sender@example.com",
                true
        )).thenReturn(Optional.empty());

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> service.findActiveSenderMailAccount(userId, "sender@example.com")
        );

        assertEquals(MailSendErrorCode.SENDER_MAIL_ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findReplyTargetMessage_정상메시지면반환한다() {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        MailQueryService service = new MailQueryService(mailAccountRepositoryPort, messageRepositoryPort);
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .gmailMessageId("gmail-message-id")
                .fromAddress("sender@example.com")
                .build();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));

        Message result = service.findReplyTargetMessage(messageId);

        assertSame(message, result);
    }

    @Test
    void findReplyTargetMessage_messageId가Null이면실패한다() {
        MailQueryService service = new MailQueryService(
                mock(MailAccountRepositoryPort.class),
                mock(MessageRepositoryPort.class)
        );

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> service.findReplyTargetMessage(null)
        );

        assertEquals(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findReplyTargetMessage_메시지가없으면실패한다() {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        MailQueryService service = new MailQueryService(mailAccountRepositoryPort, messageRepositoryPort);
        UUID messageId = UUID.randomUUID();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.empty());

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> service.findReplyTargetMessage(messageId)
        );

        assertEquals(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void findReplyTargetMessage_삭제된메시지면실패한다() {
        MailAccountRepositoryPort mailAccountRepositoryPort = mock(MailAccountRepositoryPort.class);
        MessageRepositoryPort messageRepositoryPort = mock(MessageRepositoryPort.class);
        MailQueryService service = new MailQueryService(mailAccountRepositoryPort, messageRepositoryPort);
        UUID messageId = UUID.randomUUID();
        Message message = Message.builder()
                .id(messageId)
                .gmailMessageId("gmail-message-id")
                .fromAddress("sender@example.com")
                .build();
        message.delete();
        when(messageRepositoryPort.findByIdIncludingDeleted(messageId)).thenReturn(Optional.of(message));

        MailSendException exception = assertThrows(
                MailSendException.class,
                () -> service.findReplyTargetMessage(messageId)
        );

        assertEquals(MailSendErrorCode.REPLY_TARGET_MESSAGE_NOT_FOUND, exception.getErrorCode());
    }
}
