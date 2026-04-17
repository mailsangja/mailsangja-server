package com.mailsangja.core.service.inbox;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxCommandServiceTest {

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @InjectMocks
    private InboxCommandService inboxCommandService;

    @Test
    void markThreadAsRead_같은지메일스레드의양방향스레드와메시지를모두읽음처리한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-1";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread inboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Thread outboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(false)
                .build();
        Message inboundMessage = Message.builder()
                .thread(inboundThread)
                .gmailMessageId("gmail-message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        Message outboundMessage = Message.builder()
                .thread(outboundThread)
                .gmailMessageId("gmail-message-2")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundThread, outboundThread));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundMessage, outboundMessage));

        inboxCommandService.markThreadAsRead(inboundThread);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertTrue(inboundThread.isRead());
        assertTrue(outboundThread.isRead());
        assertTrue(inboundMessage.isRead());
        assertTrue(outboundMessage.isRead());
    }

    @Test
    void markThreadAsRead_이미읽은메시지에도안전하게동작한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-2";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(true)
                .build();
        Message readMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-3")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(thread));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(readMessage));

        inboxCommandService.markThreadAsRead(thread);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertTrue(thread.isRead());
        assertTrue(readMessage.isRead());
    }

    @Test
    void markThreadAsUnread_같은지메일스레드의양방향스레드와메시지를모두안읽음처리한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-3";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread inboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(true)
                .build();
        Thread outboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(true)
                .build();
        Message inboundMessage = Message.builder()
                .thread(inboundThread)
                .gmailMessageId("gmail-message-4")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();
        Message outboundMessage = Message.builder()
                .thread(outboundThread)
                .gmailMessageId("gmail-message-5")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundThread, outboundThread));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundMessage, outboundMessage));

        inboxCommandService.markThreadAsUnread(inboundThread);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        org.junit.jupiter.api.Assertions.assertFalse(inboundThread.isRead());
        org.junit.jupiter.api.Assertions.assertFalse(outboundThread.isRead());
        org.junit.jupiter.api.Assertions.assertFalse(inboundMessage.isRead());
        org.junit.jupiter.api.Assertions.assertFalse(outboundMessage.isRead());
    }

    @Test
    void markThreadAsUnread_이미안읽은메시지에도안전하게동작한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-4";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(false)
                .build();
        Message unreadMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-6")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(thread));
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(unreadMessage));

        inboxCommandService.markThreadAsUnread(thread);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        org.junit.jupiter.api.Assertions.assertFalse(thread.isRead());
        org.junit.jupiter.api.Assertions.assertFalse(unreadMessage.isRead());
    }

    @Test
    void markMessageAsRead_모든메시지가읽음이면스레드도읽음처리한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-5";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread inboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Thread outboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(false)
                .build();
        Message targetMessage = Message.builder()
                .thread(inboundThread)
                .gmailMessageId("gmail-message-7")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        Message alreadyReadMessage = Message.builder()
                .thread(outboundThread)
                .gmailMessageId("gmail-message-8")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();

        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(targetMessage, alreadyReadMessage));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundThread, outboundThread));

        inboxCommandService.markMessageAsRead(targetMessage);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertTrue(targetMessage.isRead());
        assertTrue(inboundThread.isRead());
        assertTrue(outboundThread.isRead());
    }

    @Test
    void markMessageAsRead_안읽은메시지가남아있으면스레드는안읽음으로유지한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-6";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Message targetMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-9")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
        Message unreadSiblingMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-10")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(targetMessage, unreadSiblingMessage));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(thread));

        inboxCommandService.markMessageAsRead(targetMessage);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertTrue(targetMessage.isRead());
        assertFalse(thread.isRead());
    }

    @Test
    void markMessageAsUnread_대상메시지를안읽음처리하고스레드도안읽음처리한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-7";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread inboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(true)
                .build();
        Thread outboundThread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.OUTBOUND)
                .read(true)
                .build();
        Message targetMessage = Message.builder()
                .thread(inboundThread)
                .gmailMessageId("gmail-message-11")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();
        Message alreadyReadMessage = Message.builder()
                .thread(outboundThread)
                .gmailMessageId("gmail-message-12")
                .direction(Direction.OUTBOUND)
                .fromAddress("sender@example.com")
                .read(true)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(inboundThread, outboundThread));

        inboxCommandService.markMessageAsUnread(targetMessage);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertFalse(targetMessage.isRead());
        assertTrue(alreadyReadMessage.isRead());
        assertFalse(inboundThread.isRead());
        assertFalse(outboundThread.isRead());
    }

    @Test
    void markMessageAsUnread_이미안읽은메시지에도안전하게동작한다() {
        UUID mailAccountId = UUID.randomUUID();
        String gmailThreadId = "gmail-thread-8";
        MailAccount mailAccount = MailAccount.builder()
                .id(mailAccountId)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("gmail")
                .color("#4285F4")
                .accessToken("token")
                .build();

        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .read(false)
                .build();
        Message unreadMessage = Message.builder()
                .thread(thread)
                .gmailMessageId("gmail-message-13")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();

        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId))
                .thenReturn(List.of(thread));

        inboxCommandService.markMessageAsUnread(unreadMessage);

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, gmailThreadId);
        assertFalse(unreadMessage.isRead());
        assertFalse(thread.isRead());
    }
}
