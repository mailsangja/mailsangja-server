package com.mailsangja.core.service.inbox;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.inbox.ThreadDetailResult;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxQueryServiceTest {

    @Mock private ThreadRepositoryPort threadRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;
    @Mock private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private InboxQueryService inboxQueryService;

    @Test
    void 인박스_목록_조회는_라벨_ID에서_null과_중복을_제거하고_부가정보를_조립한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread);
        Attachment attachment = attachment(message);
        message.replaceAttachments(List.of(attachment));
        UUID labelId = UUID.randomUUID();
        List<UUID> labelIds = Arrays.asList(labelId, null, labelId);
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), labelId, "업무", "#123456", true);
        when(threadRepositoryPort.findInboxByUserIdAndFilters(
                user.getId(), List.of(labelId), false, null, PageRequest.of(0, 20)))
                .thenReturn(new SliceImpl<>(List.of(thread), PageRequest.of(0, 20), false));
        when(messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(List.of(thread.getId())))
                .thenReturn(List.of(message));
        when(messageRepositoryPort.findLabelsByThreadIdIn(List.of(thread.getId()))).thenReturn(List.of(label));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                user.getId(), List.of("participant@example.com")))
                .thenReturn(List.of(Contact.create(user, "참여자", "participant@example.com")));

        // when
        ThreadListResult result =
                inboxQueryService.findInboxThreadsResult(user.getId(), null, labelIds, false, PageRequest.of(0, 20));

        // then
        assertEquals(1, result.threads().getContent().size());
        assertEquals(List.of(attachment), result.attachmentsByThreadId().get(thread.getId()));
        assertEquals("참여자", result.contactNameByEmail().get("participant@example.com"));
        assertEquals(List.of(label), result.labelsByThreadId().get(thread.getId()));
    }

    @Test
    void 보낸메일_목록이_비어있으면_라벨과_첨부와_연락처를_조회하지_않는다() {
        // given
        User user = user();
        when(threadRepositoryPort.findSentByUserIdAndFilters(
                user.getId(), List.of(), null, null, PageRequest.of(0, 10)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        // when
        ThreadListResult result =
                inboxQueryService.findSentThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 10));

        // then
        assertEquals(0, result.threads().getContent().size());
        verify(messageRepositoryPort, never()).findAllByThreadIdInAndDeletedAtIsNull(anyList());
        verify(messageRepositoryPort, never()).findLabelsByThreadIdIn(anyList());
        verify(contactRepositoryPort, never()).findAllByUserIdAndEmailInAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void 스레드_상세_조회는_같은_지메일_스레드_메시지와_연락처와_라벨을_조립한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, Direction.INBOUND);
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message")
                .direction(Direction.INBOUND)
                .fromAddress("from@example.com")
                .toAddresses(List.of("to@example.com"))
                .ccAddresses(List.of("cc@example.com"))
                .build();
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), UUID.randomUUID(), "중요", "#ff0000", true);
        when(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(message));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                user.getId(), List.of("from@example.com", "to@example.com", "cc@example.com")))
                .thenReturn(List.of(Contact.create(user, "보낸사람", "from@example.com")));
        when(messageRepositoryPort.findLabelsByThreadIdIn(List.of(thread.getId()))).thenReturn(List.of(label));

        // when
        ThreadDetailResult result = inboxQueryService.findThreadDetailResult(thread);

        // then
        assertEquals(List.of(message), result.messages());
        assertEquals("보낸사람", result.contactNameByEmail().get("from@example.com"));
        assertEquals(List.of(label), result.labels());
    }

    @Test
    void 메시지가_삭제되어_있으면_활성_메시지_조회에서_예외가_발생한다() {
        // given
        Message message = message(thread(mailAccount(user()), Direction.INBOUND));
        message.delete();
        when(messageRepositoryPort.findByIdIncludingDeleted(message.getId())).thenReturn(Optional.of(message));

        // when & then
        assertThrows(InboxException.class, () -> inboxQueryService.findActiveMessageById(message.getId()));
    }

    @Test
    void 별표_스레드_목록_조회는_부가정보를_조립한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread);
        Attachment attachment = attachment(message);
        message.replaceAttachments(List.of(attachment));
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), UUID.randomUUID(), "중요", "#FF0000", false);
        when(threadRepositoryPort.findStarredByUserIdAndFilters(user.getId(), List.of(), null, null, PageRequest.of(0, 20)))
                .thenReturn(new SliceImpl<>(List.of(thread), PageRequest.of(0, 20), false));
        when(messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(List.of(thread.getId())))
                .thenReturn(List.of(message));
        when(messageRepositoryPort.findLabelsByThreadIdIn(List.of(thread.getId()))).thenReturn(List.of(label));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                user.getId(), List.of("participant@example.com")))
                .thenReturn(List.of());

        // when
        ThreadListResult result =
                inboxQueryService.findStarredThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 20));

        // then
        assertEquals(1, result.threads().getContent().size());
        assertEquals(List.of(attachment), result.attachmentsByThreadId().get(thread.getId()));
        assertEquals(List.of(label), result.labelsByThreadId().get(thread.getId()));
    }

    @Test
    void 별표_스레드_목록이_비어있으면_라벨과_첨부와_연락처를_조회하지_않는다() {
        // given
        User user = user();
        when(threadRepositoryPort.findStarredByUserIdAndFilters(user.getId(), List.of(), null, null, PageRequest.of(0, 10)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        // when
        ThreadListResult result =
                inboxQueryService.findStarredThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 10));

        // then
        assertEquals(0, result.threads().getContent().size());
        verify(messageRepositoryPort, never()).findAllByThreadIdInAndDeletedAtIsNull(anyList());
        verify(messageRepositoryPort, never()).findLabelsByThreadIdIn(anyList());
        verify(contactRepositoryPort, never()).findAllByUserIdAndEmailInAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void 별표_스레드_목록_조회는_라벨_ID에서_null과_중복을_제거하고_read_필터를_전달한다() {
        // given
        User user = user();
        UUID labelId = UUID.randomUUID();
        List<UUID> labelIds = Arrays.asList(labelId, null, labelId);
        when(threadRepositoryPort.findStarredByUserIdAndFilters(user.getId(), List.of(labelId), false, null, PageRequest.of(0, 10)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        // when
        ThreadListResult result =
                inboxQueryService.findStarredThreadsResult(user.getId(), null, labelIds, false, PageRequest.of(0, 10));

        // then
        verify(threadRepositoryPort).findStarredByUserIdAndFilters(user.getId(), List.of(labelId), false, null, PageRequest.of(0, 10));
    }

    @Test
    void 별표_스레드_카운트는_port에_필터를_전달한다() {
        // given
        UUID userId = UUID.randomUUID();
        when(threadRepositoryPort.countStarredByUserIdAndFilters(userId, List.of(), null)).thenReturn(5L);

        // when
        long result = inboxQueryService.countStarred(userId, null, null);

        // then
        assertEquals(5L, result);
        verify(threadRepositoryPort).countStarredByUserIdAndFilters(userId, List.of(), null);
    }

    @Test
    void 읽지_않은_별표_카운트는_read_필터와_정규화된_라벨_ID를_전달한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        when(threadRepositoryPort.countUnreadStarredByUserIdAndFilters(userId, List.of(labelId), null)).thenReturn(3L);

        // when
        long result = inboxQueryService.countUnreadStarred(userId, Arrays.asList(labelId, null, labelId), null);

        // then
        assertEquals(3L, result);
        verify(threadRepositoryPort).countUnreadStarredByUserIdAndFilters(userId, List.of(labelId), null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void 읽지_않은_인박스_카운트는_정규화된_라벨_ID를_전달한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        when(threadRepositoryPort.countUnreadInboxByUserIdAndFilters(userId, List.of(labelId), null)).thenReturn(5L);

        // when
        long result = inboxQueryService.countUnreadInbox(userId, Arrays.asList(labelId, null, labelId), null);

        // then
        assertEquals(5L, result);
        ArgumentCaptor<List<UUID>> captor = ArgumentCaptor.forClass(List.class);
        verify(threadRepositoryPort).countUnreadInboxByUserIdAndFilters(
                org.mockito.ArgumentMatchers.eq(userId), captor.capture(), org.mockito.ArgumentMatchers.isNull());
        assertEquals(List.of(labelId), captor.getValue());
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).build();
    }

    private MailAccount mailAccount(User user) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
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
                .latestParticipantAddress("participant@example.com")
                .build();
    }

    private Message message(Thread thread) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-" + UUID.randomUUID())
                .direction(thread.getDirection())
                .fromAddress("from@example.com")
                .build();
    }

    private Attachment attachment(Message message) {
        return Attachment.builder()
                .id(UUID.randomUUID())
                .message(message)
                .filename("file.pdf")
                .mimeType("application/pdf")
                .build();
    }
}
