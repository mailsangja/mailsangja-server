package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleMailReadCommandService;
import com.mailsangja.core.service.inbox.InboxCommandService;
import com.mailsangja.core.service.inbox.InboxQueryService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.InlineImageService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.search.MailSearchQueryService;
import com.mailsangja.db.dto.ThreadMessageLabelView;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxFacadeTest {

    @Mock private GoogleMailReadCommandService googleMailReadCommandService;
    @Mock private InboxCommandService inboxCommandService;
    @Mock private InboxQueryService inboxQueryService;
    @Mock private MailAccountQueryService mailAccountQueryService;
    @Mock private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    @Mock private InlineImageService inlineImageService;
    @Mock private MailSearchQueryService mailSearchQueryService;

    @InjectMocks
    private InboxFacade inboxFacade;

    @Test
    void 검색어가_null이면_인박스_기본_조회_서비스를_사용한다() {
        // given
        User user = user();
        Thread thread = thread(mailAccount(user, MailProvider.GMAIL), Direction.INBOUND);
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), UUID.randomUUID(), "민감", "#123456", true);
        when(inboxQueryService.findInboxThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 50)))
                .thenReturn(threadListResult(List.of(thread), Map.of(thread.getId(), List.of(label)), false));
        when(inboxQueryService.countUnreadInbox(user.getId(), null, null)).thenReturn(3L);
        when(inboxQueryService.countInbox(user.getId(), null, null)).thenReturn(7L);

        // when
        MarkerSliceResponse<ThreadSummaryResponse> result =
                inboxFacade.getInbox(user, null, 50, null, null, null);

        // then
        assertEquals(1, result.content().size());
        assertEquals(label.isSensitive(), result.content().getFirst().labels().getFirst().isSensitive());
        assertEquals(3L, result.unreadCount());
        assertEquals(7L, result.totalCount());
        verify(mailSearchQueryService, never()).searchInboxThreadsResult(any(), any(), anyList(), any(), any(), any());
    }

    @Test
    void 검색어가_공백이면_인박스_기본_조회_서비스를_사용한다() {
        // given
        User user = user();
        when(inboxQueryService.findInboxThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 50)))
                .thenReturn(threadListResult(List.of(), false));
        when(inboxQueryService.countUnreadInbox(user.getId(), null, null)).thenReturn(0L);
        when(inboxQueryService.countInbox(user.getId(), null, null)).thenReturn(0L);

        // when
        MarkerSliceResponse<ThreadSummaryResponse> result =
                inboxFacade.getInbox(user, null, 50, null, null, "   ");

        // then
        assertEquals(0, result.content().size());
        verify(inboxQueryService).findInboxThreadsResult(user.getId(), null, null, null, PageRequest.of(0, 50));
        verify(mailSearchQueryService, never()).searchInboxThreadsResult(any(), any(), anyList(), any(), any(), any());
    }

    @Test
    void 검색어가_있으면_인박스_검색_서비스에_trim된_검색어와_필터를_전달한다() {
        // given
        User user = user();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        Thread thread = thread(mailAccount(user, MailProvider.GMAIL), Direction.INBOUND);
        when(mailSearchQueryService.searchInboxThreadsResult(
                user.getId(), "프로젝트", labelIds, false, marker, PageRequest.of(0, 30)))
                .thenReturn(threadListResult(List.of(thread), true));
        when(mailSearchQueryService.countUnreadInboxThreads(user.getId(), "프로젝트", labelIds, false)).thenReturn(1L);
        when(mailSearchQueryService.countInboxThreads(user.getId(), "프로젝트", labelIds, false)).thenReturn(2L);

        // when
        MarkerSliceResponse<ThreadSummaryResponse> result =
                inboxFacade.getInbox(user, marker, 30, labelIds, false, "  프로젝트  ");

        // then
        assertEquals(thread.getId(), result.nextMarker());
        assertEquals(1L, result.unreadCount());
        assertEquals(2L, result.totalCount());
        verify(inboxQueryService, never()).findInboxThreadsResult(any(), any(), anyList(), any(), any());
    }

    @Test
    void 검색어가_있으면_보낸메일_검색_서비스에_trim된_검색어와_필터를_전달한다() {
        // given
        User user = user();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        Thread thread = thread(mailAccount(user, MailProvider.GMAIL), Direction.OUTBOUND);
        when(mailSearchQueryService.searchSentThreadsResult(
                user.getId(), "계약서", labelIds, true, marker, PageRequest.of(0, 20)))
                .thenReturn(threadListResult(List.of(thread), false));
        when(mailSearchQueryService.countUnreadSentThreads(user.getId(), "계약서", labelIds, true)).thenReturn(0L);
        when(mailSearchQueryService.countSentThreads(user.getId(), "계약서", labelIds, true)).thenReturn(1L);

        // when
        MarkerSliceResponse<ThreadSummaryResponse> result =
                inboxFacade.getSent(user, marker, 20, labelIds, true, " 계약서 ");

        // then
        assertNull(result.nextMarker());
        assertEquals(1, result.content().size());
        verify(inboxQueryService, never()).findSentThreadsResult(any(), any(), anyList(), any(), any());
    }

    @Test
    void 지메일_스레드_읽음처리는_구글_API_호출_후_로컬_상태를_변경한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user, MailProvider.GMAIL);
        Thread thread = thread(account, Direction.INBOUND);
        when(inboxQueryService.findThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(account)).thenReturn(account);

        // when
        inboxFacade.markThreadAsRead(user, thread.getId());

        // then
        verify(googleMailReadCommandService).markThreadAsRead(account, thread);
        verify(inboxCommandService).markThreadAsRead(thread);
    }

    @Test
    void 지메일이_아닌_메시지_안읽음처리는_외부_API를_호출하지_않고_로컬_상태만_변경한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user, MailProvider.NAVER);
        Thread thread = thread(account, Direction.INBOUND);
        Message message = message(thread);
        when(inboxQueryService.findActiveMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));

        // when
        inboxFacade.markMessageAsUnread(user, message.getId());

        // then
        verify(googleAccessTokenEnsureService, never()).ensureValidGoogleAccessToken(any());
        verify(googleMailReadCommandService, never()).markMessageAsUnread(any(), any());
        verify(inboxCommandService).markMessageAsUnread(message);
    }

    @Test
    void 사용자_소유_계정의_스레드가_아니면_접근_예외가_발생한다() {
        // given
        User user = user();
        MailAccount ownerAccount = mailAccount(user, MailProvider.GMAIL);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build(), MailProvider.GMAIL);
        Thread thread = thread(otherAccount, Direction.INBOUND);
        when(inboxQueryService.findThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(ownerAccount));

        // when & then
        assertThrows(InboxException.class, () -> inboxFacade.markThreadAsRead(user, thread.getId()));
        verify(inboxCommandService, never()).markThreadAsRead(any());
    }

    private ThreadListResult threadListResult(List<Thread> threads, boolean hasNext) {
        return new ThreadListResult(new SliceImpl<>(threads, PageRequest.of(0, 50), hasNext), Map.of(), Map.of(), Map.of());
    }

    private ThreadListResult threadListResult(
            List<Thread> threads,
            Map<UUID, List<ThreadMessageLabelView>> labelsByThreadId,
            boolean hasNext
    ) {
        return new ThreadListResult(new SliceImpl<>(threads, PageRequest.of(0, 50), hasNext), Map.of(), Map.of(), labelsByThreadId);
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
