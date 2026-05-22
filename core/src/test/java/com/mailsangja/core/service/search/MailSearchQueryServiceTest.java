package com.mailsangja.core.service.search;

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
import com.mailsangja.db.port.MailSearchRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSearchQueryServiceTest {

    @Mock private MailSearchRepositoryPort mailSearchRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;
    @Mock private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private MailSearchQueryService mailSearchQueryService;

    @Test
    void 인박스_검색_결과는_스레드와_첨부파일과_라벨과_연락처를_조립한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, Direction.INBOUND, "sender@example.com");
        Message message = message(thread);
        Attachment attachment = attachment(message);
        message.replaceAttachments(List.of(attachment));
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), UUID.randomUUID(), "업무", "#123456");
        when(mailSearchRepositoryPort.searchInboxThreads(
                user.getId(), "프로젝트", List.of(), null, null, PageRequest.of(0, 20)))
                .thenReturn(new SliceImpl<>(List.of(thread), PageRequest.of(0, 20), false));
        when(messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(List.of(thread.getId())))
                .thenReturn(List.of(message));
        when(messageRepositoryPort.findLabelsByThreadIdIn(List.of(thread.getId()))).thenReturn(List.of(label));
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(user.getId(), List.of("sender@example.com")))
                .thenReturn(List.of(Contact.create(user, "보낸사람", "sender@example.com")));

        // when
        ThreadListResult result = mailSearchQueryService.searchInboxThreadsResult(
                user.getId(), "프로젝트", List.of(), null, null, PageRequest.of(0, 20));

        // then
        assertEquals(List.of(thread), result.threads().getContent());
        assertEquals(List.of(attachment), result.attachmentsByThreadId().get(thread.getId()));
        assertEquals(List.of(label), result.labelsByThreadId().get(thread.getId()));
        assertEquals("보낸사람", result.contactNameByEmail().get("sender@example.com"));
    }

    @Test
    void 보낸메일_검색_결과가_비어있으면_부가정보를_조회하지_않는다() {
        // given
        UUID userId = UUID.randomUUID();
        when(mailSearchRepositoryPort.searchSentThreads(
                userId, "검색어", null, true, null, PageRequest.of(0, 10)))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 10), false));

        // when
        ThreadListResult result =
                mailSearchQueryService.searchSentThreadsResult(userId, "검색어", null, true, null, PageRequest.of(0, 10));

        // then
        assertEquals(0, result.threads().getContent().size());
        verify(messageRepositoryPort, never()).findAllByThreadIdInAndDeletedAtIsNull(anyList());
        verify(messageRepositoryPort, never()).findLabelsByThreadIdIn(anyList());
        verify(contactRepositoryPort, never()).findAllByUserIdAndEmailInAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void 휴지통_검색은_검색_포트의_메시지_Slice를_그대로_반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        Slice<Message> expected = new SliceImpl<>(List.of(), PageRequest.of(0, 30), false);
        when(mailSearchRepositoryPort.searchTrashMessages(userId, "휴지통", labelIds, false, marker, PageRequest.of(0, 30)))
                .thenReturn(expected);

        // when
        Slice<Message> result =
                mailSearchQueryService.searchTrashMessages(userId, "휴지통", labelIds, false, marker, PageRequest.of(0, 30));

        // then
        assertEquals(expected, result);
    }

    @Test
    void 검색_카운트_메서드는_각_포트_메서드에_그대로_위임한다() {
        // given
        UUID userId = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        when(mailSearchRepositoryPort.countInboxThreads(userId, "q", labelIds, null)).thenReturn(10L);
        when(mailSearchRepositoryPort.countUnreadInboxThreads(userId, "q", labelIds, null)).thenReturn(3L);
        when(mailSearchRepositoryPort.countSentThreads(userId, "q", labelIds, true)).thenReturn(4L);
        when(mailSearchRepositoryPort.countUnreadSentThreads(userId, "q", labelIds, true)).thenReturn(0L);
        when(mailSearchRepositoryPort.countTrashMessages(userId, "q", labelIds, false)).thenReturn(2L);
        when(mailSearchRepositoryPort.countUnreadTrashMessages(userId, "q", labelIds, false)).thenReturn(1L);

        // when
        long inbox = mailSearchQueryService.countInboxThreads(userId, "q", labelIds, null);
        long unreadInbox = mailSearchQueryService.countUnreadInboxThreads(userId, "q", labelIds, null);
        long sent = mailSearchQueryService.countSentThreads(userId, "q", labelIds, true);
        long unreadSent = mailSearchQueryService.countUnreadSentThreads(userId, "q", labelIds, true);
        long trash = mailSearchQueryService.countTrashMessages(userId, "q", labelIds, false);
        long unreadTrash = mailSearchQueryService.countUnreadTrashMessages(userId, "q", labelIds, false);

        // then
        assertEquals(10L, inbox);
        assertEquals(3L, unreadInbox);
        assertEquals(4L, sent);
        assertEquals(0L, unreadSent);
        assertEquals(2L, trash);
        assertEquals(1L, unreadTrash);
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

    private Thread thread(MailAccount account, Direction direction, String participantAddress) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(account)
                .gmailThreadId("gmail-thread")
                .direction(direction)
                .latestParticipantAddress(participantAddress)
                .build();
    }

    private Message message(Thread thread) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message")
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
