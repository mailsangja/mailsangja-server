package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.service.inbox.InboxCommandService;
import com.mailsangja.core.service.inbox.InboxQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StarFacadeTest {

    @Mock private InboxCommandService inboxCommandService;
    @Mock private InboxQueryService inboxQueryService;
    @Mock private MailAccountQueryService mailAccountQueryService;

    @InjectMocks
    private StarFacade starFacade;

    @Test
    void 별표_목록_조회는_QueryService를_호출하고_MarkerSlice를_조립한다() {
        User user = user();
        Thread thread = thread(mailAccount(user, MailProvider.GMAIL), Direction.INBOUND);
        when(inboxQueryService.findStarredThreadsResult(user.getId(), null, PageRequest.of(0, 20)))
                .thenReturn(threadListResult(List.of(thread), false));
        when(inboxQueryService.countStarred(user.getId())).thenReturn(3L);

        MarkerSliceResponse<ThreadSummaryResponse> result = starFacade.getStarred(user, null, 20);

        assertEquals(1, result.content().size());
        assertEquals(3L, result.totalCount());
        assertEquals(0L, result.unreadCount());
        assertNull(result.nextMarker());
    }

    @Test
    void 별표_목록이_비어있으면_nextMarker가_null이다() {
        User user = user();
        when(inboxQueryService.findStarredThreadsResult(user.getId(), null, PageRequest.of(0, 10)))
                .thenReturn(threadListResult(List.of(), false));
        when(inboxQueryService.countStarred(user.getId())).thenReturn(0L);

        MarkerSliceResponse<ThreadSummaryResponse> result = starFacade.getStarred(user, null, 10);

        assertEquals(0, result.content().size());
        assertNull(result.nextMarker());
        assertEquals(0L, result.totalCount());
    }

    @Test
    void 별표_목록에_다음_페이지가_있으면_마지막_스레드_ID가_nextMarker가_된다() {
        User user = user();
        Thread thread = thread(mailAccount(user, MailProvider.GMAIL), Direction.INBOUND);
        when(inboxQueryService.findStarredThreadsResult(user.getId(), null, PageRequest.of(0, 5)))
                .thenReturn(threadListResult(List.of(thread), true));
        when(inboxQueryService.countStarred(user.getId())).thenReturn(10L);

        MarkerSliceResponse<ThreadSummaryResponse> result = starFacade.getStarred(user, null, 5);

        assertEquals(thread.getId(), result.nextMarker());
        assertTrue(result.hasNext());
    }

    @Test
    void 스레드_별표_토글은_소유_계정_스레드면_CommandService를_호출한다() {
        User user = user();
        MailAccount account = mailAccount(user, MailProvider.GMAIL);
        Thread thread = thread(account, Direction.INBOUND);
        when(inboxQueryService.findThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(inboxCommandService.toggleStar(thread)).thenReturn(true);

        boolean result = starFacade.toggleThreadStar(user, thread.getId());

        assertTrue(result);
        verify(inboxCommandService).toggleStar(thread);
    }

    @Test
    void 스레드_별표_토글은_타인_계정_스레드에_접근하면_예외가_발생한다() {
        User user = user();
        MailAccount ownerAccount = mailAccount(user, MailProvider.GMAIL);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build(), MailProvider.GMAIL);
        Thread thread = thread(otherAccount, Direction.INBOUND);
        when(inboxQueryService.findThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(ownerAccount));

        assertThrows(InboxException.class, () -> starFacade.toggleThreadStar(user, thread.getId()));
        verify(inboxCommandService, never()).toggleStar(any());
    }

    @Test
    void 메시지_별표_토글은_소유_계정_메시지면_CommandService를_호출한다() {
        User user = user();
        MailAccount account = mailAccount(user, MailProvider.GMAIL);
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread);
        when(inboxQueryService.findActiveMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(inboxCommandService.toggleMessageStar(message)).thenReturn(true);

        boolean result = starFacade.toggleMessageStar(user, message.getId());

        assertTrue(result);
        verify(inboxCommandService).toggleMessageStar(message);
    }

    @Test
    void 메시지_별표_토글은_타인_계정_메시지에_접근하면_예외가_발생한다() {
        User user = user();
        MailAccount ownerAccount = mailAccount(user, MailProvider.GMAIL);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build(), MailProvider.GMAIL);
        Thread thread = thread(otherAccount, Direction.INBOUND);
        Message message = message(thread);
        when(inboxQueryService.findActiveMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(ownerAccount));

        assertThrows(InboxException.class, () -> starFacade.toggleMessageStar(user, message.getId()));
        verify(inboxCommandService, never()).toggleMessageStar(any());
    }

    private ThreadListResult threadListResult(List<Thread> threads, boolean hasNext) {
        return new ThreadListResult(new SliceImpl<>(threads, PageRequest.of(0, 50), hasNext), Map.of(), Map.of(), Map.of());
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).build();
    }

    private MailAccount mailAccount(User user, MailProvider provider) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(provider)
                .emailAddress("user@example.com")
                .alias("mail")
                .icon("icon")
                .color("#000000")
                .accessToken("token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private Thread thread(MailAccount mailAccount, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread-" + UUID.randomUUID())
                .direction(direction)
                .latestSubject("subject")
                .latestSnippet("snippet")
                .latestParticipantAddress("participant@example.com")
                .latestParticipantName("participant")
                .lastMessageAt(LocalDateTime.now())
                .messageCount(1)
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
}
