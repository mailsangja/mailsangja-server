package com.mailsangja.core.facade;

import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleGmailApiService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.InlineImageService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.search.MailSearchQueryService;
import com.mailsangja.core.service.trash.TrashCommandService;
import com.mailsangja.core.service.trash.TrashQueryService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class TrashFacadeTest {

    @Mock private TrashCommandService trashCommandService;
    @Mock private TrashQueryService trashQueryService;
    @Mock private GoogleGmailApiService googleGmailApiService;
    @Mock private MailAccountQueryService mailAccountQueryService;
    @Mock private GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    @Mock private InlineImageService inlineImageService;
    @Mock private MailSearchQueryService mailSearchQueryService;

    @InjectMocks
    private TrashFacade trashFacade;

    @BeforeEach
    void setUp() {
        lenient().when(trashQueryService.findLabelsByThreadIds(anyList())).thenReturn(Map.of());
        lenient().when(trashQueryService.findContactNamesByEmails(any(), anyList())).thenReturn(Map.of());
        lenient().when(trashQueryService.findAttachmentsByMessageIds(anyList())).thenReturn(Map.of());
    }

    // ── 검색어 라우팅 ──────────────────────────────────────────────────────────────

    @Test
    void q가_null이면_trashQueryService로_메시지_조회() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any());
        verify(mailSearchQueryService, never()).searchTrashMessages(any(), any(), any(), any(), any(), any());
    }

    @Test
    void q가_공백이면_trashQueryService로_메시지_조회() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, "   ");

        verify(trashQueryService).findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any());
        verify(mailSearchQueryService, never()).searchTrashMessages(any(), any(), any(), any(), any(), any());
    }

    @Test
    void q가_있으면_mailSearchQueryService로_메시지_조회() {
        User user = user();
        stubSearchCounts(user.getId(), "프로젝트");
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, "프로젝트");

        verify(mailSearchQueryService).searchTrashMessages(any(), any(), any(), any(), any(), any());
        verify(trashQueryService, never()).findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any());
    }

    @Test
    void q_앞뒤_공백은_trim되어_검색에_사용() {
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        trashFacade.getTrashThreads(user, null, 50, null, null, "  미팅  ");

        verify(mailSearchQueryService).searchTrashMessages(
                eq(user.getId()), eq("미팅"), any(), any(), any(), any());
    }

    // ── 비검색 파라미터 전달 검증 ──────────────────────────────────────────────────

    @Test
    void marker가_null이면_null로_trashQueryService에_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), isNull(), isNull());
    }

    @Test
    void marker가_있으면_그대로_trashQueryService에_전달() {
        User user = user();
        UUID marker = UUID.randomUUID();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, marker, 50, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), eq(marker), eq(50), isNull(), isNull());
    }

    @Test
    void size_파라미터가_trashQueryService에_그대로_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 30, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(30), isNull(), isNull());
    }

    @Test
    void labelIds가_null이면_null로_trashQueryService에_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), isNull(), isNull());
        verify(trashQueryService).countUnreadDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull());
        verify(trashQueryService).countDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull());
    }

    @Test
    void labelIds가_있으면_그대로_trashQueryService에_전달() {
        User user = user();
        List<UUID> labelIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, labelIds, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), eq(labelIds), isNull());
        verify(trashQueryService).countUnreadDeletedMessagesByUserId(eq(user.getId()), eq(labelIds), isNull());
        verify(trashQueryService).countDeletedMessagesByUserId(eq(user.getId()), eq(labelIds), isNull());
    }

    @Test
    void read_null이면_null로_trashQueryService에_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, null, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), isNull(), isNull());
        verify(trashQueryService).countUnreadDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull());
        verify(trashQueryService).countDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull());
    }

    @Test
    void read_true이면_true로_trashQueryService에_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, true, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), isNull(), eq(true));
        verify(trashQueryService).countUnreadDeletedMessagesByUserId(eq(user.getId()), isNull(), eq(true));
        verify(trashQueryService).countDeletedMessagesByUserId(eq(user.getId()), isNull(), eq(true));
    }

    @Test
    void read_false이면_false로_trashQueryService에_전달() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        trashFacade.getTrashThreads(user, null, 50, null, false, null);

        verify(trashQueryService).findDeletedMessagesByUserId(
                eq(user.getId()), isNull(), eq(50), isNull(), eq(false));
    }

    // ── 검색 파라미터 전달 검증 ────────────────────────────────────────────────────

    @Test
    void 검색시_marker_size_labelIds_read가_mailSearchQueryService에_그대로_전달() {
        User user = user();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        trashFacade.getTrashThreads(user, marker, 30, labelIds, false, "테스트");

        verify(mailSearchQueryService).searchTrashMessages(
                eq(user.getId()), eq("테스트"), eq(labelIds), eq(false), eq(marker), eq(PageRequest.of(0, 30)));
        verify(mailSearchQueryService).countUnreadTrashMessages(
                eq(user.getId()), eq("테스트"), eq(labelIds), eq(false));
        verify(mailSearchQueryService).countTrashMessages(
                eq(user.getId()), eq("테스트"), eq(labelIds), eq(false));
    }

    @Test
    void 검색시_labelIds_null이면_null로_mailSearchQueryService에_전달() {
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        trashFacade.getTrashThreads(user, null, 50, null, null, "검색어");

        verify(mailSearchQueryService).searchTrashMessages(
                eq(user.getId()), eq("검색어"), isNull(), isNull(), isNull(), any());
    }

    @Test
    void 검색시_read_null이면_null로_mailSearchQueryService에_전달() {
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        trashFacade.getTrashThreads(user, null, 50, null, null, "검색어");

        verify(mailSearchQueryService).countUnreadTrashMessages(
                eq(user.getId()), eq("검색어"), isNull(), isNull());
        verify(mailSearchQueryService).countTrashMessages(
                eq(user.getId()), eq("검색어"), isNull(), isNull());
    }

    // ── 그룹핑 로직 ───────────────────────────────────────────────────────────────

    @Test
    void 메시지가_없으면_빈_content와_nextMarker_null_반환() {
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertTrue(result.content().isEmpty());
        assertNull(result.nextMarker());
        assertFalse(result.hasNext());
    }

    @Test
    void 같은_gmailThreadId의_메시지들은_하나의_그룹으로_묶임() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        Message msg1 = message(inbound);
        Message msg2 = message(outbound);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(msg1, msg2));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(1, result.content().size());
        assertEquals(2, result.content().get(0).messageCount());
    }

    @Test
    void 다른_gmailThreadId의_메시지들은_별개_그룹으로_반환() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread1 = thread(account, "gmail-thread-1", Direction.INBOUND);
        Thread thread2 = thread(account, "gmail-thread-2", Direction.INBOUND);
        Message msg1 = message(thread1);
        Message msg2 = message(thread2);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(msg1, msg2));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(2, result.content().size());
    }

    @Test
    void 그룹내_INBOUND_스레드가_있으면_INBOUND_스레드가_대표로_선택() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        Message inboundMsg = message(inbound);
        Message outboundMsg = message(outbound);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(inboundMsg, outboundMsg));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(inbound.getId(), result.content().get(0).threadId());
    }

    @Test
    void 그룹내_INBOUND_스레드가_없으면_첫번째_메시지_스레드가_대표로_선택() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        Message msg1 = message(outbound);
        Message msg2 = message(outbound);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(msg1, msg2));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(outbound.getId(), result.content().get(0).threadId());
    }

    @Test
    void 참여자_이메일이_null이어도_NPE_없이_응답_반환() {
        // latestParticipantAddress = null 인 스레드가 있을 때 MailAddressResponse.of 에서 NPE 가 발생하는 버그
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = threadWithNullParticipant(account, "gmail-thread-1", Direction.INBOUND);
        Message msg = message(thread);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(msg));

        assertDoesNotThrow(() -> trashFacade.getTrashThreads(user, null, 50, null, null, null));
    }

    @Test
    void 삭제된_메시지가_단건이면_messageCount_1() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message msg = message(inbound);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(msg));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(1, result.content().get(0).messageCount());
    }

    // ── 페이지네이션 ───────────────────────────────────────────────────────────────

    @Test
    void hasNext가_true이면_nextMarker가_마지막_메시지_ID() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message msg = message(thread);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceWithHasNext(List.of(msg), true));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertTrue(result.hasNext());
        assertEquals(msg.getId(), result.nextMarker());
    }

    @Test
    void hasNext가_false이면_nextMarker_null() {
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message msg = message(thread);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceWithHasNext(List.of(msg), false));

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertFalse(result.hasNext());
        assertNull(result.nextMarker());
    }

    // ── unreadCount / totalCount ───────────────────────────────────────────────────

    @Test
    void 비검색_시_unreadCount와_totalCount가_trashQueryService_값으로_반환() {
        User user = user();
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());
        when(trashQueryService.countUnreadDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull()))
                .thenReturn(7L);
        when(trashQueryService.countDeletedMessagesByUserId(eq(user.getId()), isNull(), isNull()))
                .thenReturn(42L);

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        assertEquals(7L, result.unreadCount());
        assertEquals(42L, result.totalCount());
    }

    @Test
    void 검색_시_unreadCount와_totalCount가_mailSearchQueryService_값으로_반환() {
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(eq(user.getId()), eq("키워드"), isNull(), isNull()))
                .thenReturn(3L);
        when(mailSearchQueryService.countTrashMessages(eq(user.getId()), eq("키워드"), isNull(), isNull()))
                .thenReturn(15L);

        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, "키워드");

        assertEquals(3L, result.unreadCount());
        assertEquals(15L, result.totalCount());
    }

    // ── helpers ───────────────────────────────────────────────────────────────────

    private void stubTrashCounts(UUID userId) {
        lenient().when(trashQueryService.countUnreadDeletedMessagesByUserId(eq(userId), any(), any()))
                .thenReturn(0L);
        lenient().when(trashQueryService.countDeletedMessagesByUserId(eq(userId), any(), any()))
                .thenReturn(0L);
    }

    private void stubSearchCounts(UUID userId, String query) {
        lenient().when(mailSearchQueryService.countUnreadTrashMessages(eq(userId), eq(query), any(), any()))
                .thenReturn(0L);
        lenient().when(mailSearchQueryService.countTrashMessages(eq(userId), eq(query), any(), any()))
                .thenReturn(0L);
    }

    private Slice<Message> emptySlice() {
        return new SliceImpl<>(List.of(), PageRequest.of(0, 50), false);
    }

    private Slice<Message> sliceOf(Message... messages) {
        return sliceWithHasNext(List.of(messages), false);
    }

    private Slice<Message> sliceWithHasNext(List<Message> messages, boolean hasNext) {
        return new SliceImpl<>(messages, PageRequest.of(0, 50), hasNext);
    }

    private User user() {
        return User.builder()
                .id(UUID.randomUUID())
                .build();
    }

    private MailAccount mailAccount(User user) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("test@gmail.com")
                .alias("Test")
                .icon("icon")
                .color("#000000")
                .accessToken("token")
                .accessTokenExpiresAt(LocalDateTime.now().plusHours(1))
                .build();
    }

    private Thread thread(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(direction)
                .latestParticipantAddress("participant@example.com")
                .build();
    }

    private Thread threadWithNullParticipant(MailAccount mailAccount, String gmailThreadId, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(direction)
                .build();
    }

    private Message message(Thread thread) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(thread.getDirection())
                .fromAddress("from@example.com")
                .build();
    }
}
