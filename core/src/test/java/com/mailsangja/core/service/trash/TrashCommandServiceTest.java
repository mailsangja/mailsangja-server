package com.mailsangja.core.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashCommandServiceTest {

    @Mock private ThreadRepositoryPort threadRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;
    @Mock private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @InjectMocks
    private TrashCommandService trashCommandService;

    @Test
    void 스레드_삭제는_락을_획득하고_같은_지메일_스레드의_스레드와_메시지를_일괄_삭제한다() {
        // given
        MailAccount account = mailAccount();
        Thread thread = thread(account, Direction.INBOUND);

        // when
        trashCommandService.softDeleteThread(thread);

        // then
        verify(gmailThreadLockRepositoryPort).acquireThreadLock(account, thread.getGmailThreadId());
        verify(threadRepositoryPort).bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                org.mockito.ArgumentMatchers.eq(account.getId()), org.mockito.ArgumentMatchers.eq(thread.getGmailThreadId()), any());
        verify(messageRepositoryPort).bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                org.mockito.ArgumentMatchers.eq(account.getId()), org.mockito.ArgumentMatchers.eq(thread.getGmailThreadId()), any());
    }

    @Test
    void 메시지_삭제_후_활성_메시지가_없으면_스레드도_일괄_삭제한다() {
        // given
        MailAccount account = mailAccount();
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread, LocalDateTime.now(), true);
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of());

        // when
        trashCommandService.softDeleteMessage(message);

        // then
        assertTrue(message.isDeleted());
        verify(messageRepositoryPort).save(message);
        verify(threadRepositoryPort).bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                org.mockito.ArgumentMatchers.eq(account.getId()), org.mockito.ArgumentMatchers.eq(thread.getGmailThreadId()), any());
    }

    @Test
    void 메시지_삭제_후_활성_메시지가_남아있으면_스레드_최신정보와_카운트를_갱신한다() {
        // given
        MailAccount account = mailAccount();
        Thread inbound = thread(account, Direction.INBOUND);
        Thread outbound = thread(account, Direction.OUTBOUND);
        Message deleteTarget = message(inbound, LocalDateTime.now().minusDays(1), true);
        Message latest = Message.builder()
                .id(UUID.randomUUID())
                .thread(inbound)
                .gmailMessageId("latest")
                .direction(Direction.INBOUND)
                .subject("최신 제목")
                .snippet("최신 내용")
                .fromAddress("sender@example.com")
                .fromName("보낸사람")
                .toAddresses(List.of("to@example.com"))
                .toNames(List.of("받는사람"))
                .read(false)
                .sentAt(LocalDateTime.now())
                .build();
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), inbound.getGmailThreadId()))
                .thenReturn(List.of(latest));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), inbound.getGmailThreadId()))
                .thenReturn(List.of(inbound, outbound));

        // when
        trashCommandService.softDeleteMessage(deleteTarget);

        // then
        assertEquals("최신 제목", inbound.getLatestSubject());
        assertEquals("sender@example.com", inbound.getLatestParticipantAddress());
        assertEquals("to@example.com", outbound.getLatestParticipantAddress());
        assertFalse(inbound.isRead());
        assertEquals(1, inbound.getMessageCount());
        verify(threadRepositoryPort, never()).bulkSoftDeleteByMailAccountIdAndGmailThreadId(any(), any(), any());
    }

    @Test
    void 스레드_복구는_락을_획득하고_스레드와_메시지를_일괄_복구한다() {
        // given
        MailAccount account = mailAccount();
        Thread thread = thread(account, Direction.INBOUND);
        Message activeMessage = message(thread, LocalDateTime.now(), true);
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(activeMessage));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(thread));

        // when
        trashCommandService.restoreThread(thread);

        // then
        verify(gmailThreadLockRepositoryPort).acquireThreadLock(account, thread.getGmailThreadId());
        verify(threadRepositoryPort).bulkRestoreByMailAccountIdAndGmailThreadId(account.getId(), thread.getGmailThreadId());
        verify(messageRepositoryPort).bulkRestoreByMailAccountIdAndGmailThreadId(account.getId(), thread.getGmailThreadId());
        assertTrue(thread.isRead());
    }

    @Test
    void 메시지_복구는_메시지를_복구하고_스레드도_복구한다() {
        // given
        MailAccount account = mailAccount();
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread, LocalDateTime.now(), true);
        message.delete();
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(message));
        when(threadRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(thread));

        // when
        trashCommandService.restoreMessage(message);

        // then
        assertFalse(message.isDeleted());
        verify(messageRepositoryPort).save(message);
        verify(threadRepositoryPort).bulkRestoreByMailAccountIdAndGmailThreadId(account.getId(), thread.getGmailThreadId());
    }

    private MailAccount mailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(User.builder().id(UUID.randomUUID()).build())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("icon")
                .color("#000000")
                .accessToken("token")
                .build();
    }

    private Thread thread(MailAccount account, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(account)
                .gmailThreadId("gmail-thread")
                .direction(direction)
                .read(true)
                .messageCount(2)
                .build();
    }

    private Message message(Thread thread, LocalDateTime sentAt, boolean read) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-" + UUID.randomUUID())
                .direction(thread.getDirection())
                .subject("제목")
                .snippet("본문")
                .fromAddress("from@example.com")
                .fromName("발신자")
                .toAddresses(List.of("to@example.com"))
                .toNames(List.of("수신자"))
                .read(read)
                .sentAt(sentAt)
                .build();
    }
}
