package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadDetailResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleGmailApiService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.InlineImageService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.search.MailSearchQueryService;
import com.mailsangja.core.service.trash.TrashCommandService;
import com.mailsangja.core.service.trash.TrashQueryService;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import com.mailsangja.db.entity.mail.Attachment;
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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void 검색어가_null이면_휴지통_기본_조회_서비스를_사용한다() {
        // given
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        // when
        trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        verify(trashQueryService).findDeletedMessagesByUserId(user.getId(), null, 50, null, null);
        verify(mailSearchQueryService, never()).searchTrashMessages(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 검색어가_공백이면_휴지통_기본_조회_서비스를_사용한다() {
        // given
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        // when
        trashFacade.getTrashThreads(user, null, 50, null, null, "   ");

        // then
        verify(trashQueryService).findDeletedMessagesByUserId(user.getId(), null, 50, null, null);
        verify(mailSearchQueryService, never()).searchTrashMessages(any(), any(), any(), any(), any(), any());
    }

    @Test
    void 검색어가_있으면_휴지통_검색_서비스에_trim된_검색어와_필터를_전달한다() {
        // given
        User user = user();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        // when
        trashFacade.getTrashThreads(user, marker, 30, labelIds, false, "  프로젝트  ");

        // then
        verify(mailSearchQueryService).searchTrashMessages(
                user.getId(), "프로젝트", labelIds, false, marker, PageRequest.of(0, 30));
        verify(mailSearchQueryService).countUnreadTrashMessages(user.getId(), "프로젝트", labelIds, false);
        verify(mailSearchQueryService).countTrashMessages(user.getId(), "프로젝트", labelIds, false);
        verify(trashQueryService, never()).findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any());
    }

    @Test
    void 비검색_조회는_marker_size_labelIds_read를_그대로_전달한다() {
        // given
        User user = user();
        UUID marker = UUID.randomUUID();
        List<UUID> labelIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        // when
        trashFacade.getTrashThreads(user, marker, 25, labelIds, true, null);

        // then
        verify(trashQueryService).findDeletedMessagesByUserId(user.getId(), marker, 25, labelIds, true);
        verify(trashQueryService).countUnreadDeletedMessagesByUserId(user.getId(), labelIds, true);
        verify(trashQueryService).countDeletedMessagesByUserId(user.getId(), labelIds, true);
    }

    @Test
    void 검색시_labelIds와_read가_null이면_null로_검색_서비스에_전달한다() {
        // given
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(any(), any(), any(), any())).thenReturn(0L);
        when(mailSearchQueryService.countTrashMessages(any(), any(), any(), any())).thenReturn(0L);

        // when
        trashFacade.getTrashThreads(user, null, 50, null, null, "검색어");

        // then
        verify(mailSearchQueryService).searchTrashMessages(
                eq(user.getId()), eq("검색어"), isNull(), isNull(), isNull(), eq(PageRequest.of(0, 50)));
        verify(mailSearchQueryService).countUnreadTrashMessages(user.getId(), "검색어", null, null);
        verify(mailSearchQueryService).countTrashMessages(user.getId(), "검색어", null, null);
    }

    @Test
    void 메시지가_없으면_빈_content와_nextMarker_null을_반환한다() {
        // given
        User user = user();
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertTrue(result.content().isEmpty());
        assertNull(result.nextMarker());
        assertFalse(result.hasNext());
    }

    @Test
    void 같은_메일계정과_gmailThreadId의_메시지들은_하나의_항목으로_그룹핑한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        Message inboundMessage = message(inbound);
        Message outboundMessage = message(outbound);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(inboundMessage, outboundMessage));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(1, result.content().size());
        assertEquals(2, result.content().getFirst().messageCount());
    }

    @Test
    void 다른_gmailThreadId의_메시지들은_별도_항목으로_반환한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Message first = message(thread(account, "gmail-thread-1", Direction.INBOUND));
        Message second = message(thread(account, "gmail-thread-2", Direction.INBOUND));
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(first, second));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(2, result.content().size());
    }

    @Test
    void 그룹에_INBOUND_스레드가_있으면_INBOUND_스레드를_대표로_선택한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(message(outbound), message(inbound)));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(inbound.getId(), result.content().getFirst().threadId());
    }

    @Test
    void 그룹에_INBOUND_스레드가_없으면_첫번째_메시지의_스레드를_대표로_선택한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread outbound = thread(account, "gmail-thread-1", Direction.OUTBOUND);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(message(outbound), message(outbound)));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(outbound.getId(), result.content().getFirst().threadId());
    }

    @Test
    void 대표_스레드의_라벨과_그룹_메시지의_첨부파일을_목록_응답에_포함한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread inbound = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message message = message(inbound);
        Attachment attachment = attachment(message);
        ThreadMessageLabelView label = new ThreadMessageLabelView(inbound.getId(), UUID.randomUUID(), "업무", "#123456");
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(message));
        when(trashQueryService.findLabelsByThreadIds(List.of(inbound.getId())))
                .thenReturn(Map.of(inbound.getId(), List.of(label)));
        when(trashQueryService.findAttachmentsByMessageIds(List.of(message.getId())))
                .thenReturn(Map.of(message.getId(), List.of(attachment)));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(1, result.content().getFirst().labels().size());
        assertEquals("업무", result.content().getFirst().labels().getFirst().name());
        assertEquals(1, result.content().getFirst().attachments().size());
        assertEquals("file.pdf", result.content().getFirst().attachments().getFirst().filename());
    }

    @Test
    void 참여자_이메일이_null이어도_NPE_없이_응답을_반환한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = threadWithNullParticipant(account, "gmail-thread-1", Direction.INBOUND);
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceOf(message(thread)));

        // when & then
        assertDoesNotThrow(() -> trashFacade.getTrashThreads(user, null, 50, null, null, null));
    }

    @Test
    void hasNext가_true이면_마지막_메시지_ID를_nextMarker로_반환한다() {
        // given
        User user = user();
        Message message = message(thread(mailAccount(user), "gmail-thread-1", Direction.INBOUND));
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceWithHasNext(List.of(message), true));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertTrue(result.hasNext());
        assertEquals(message.getId(), result.nextMarker());
    }

    @Test
    void hasNext가_false이면_nextMarker는_null이다() {
        // given
        User user = user();
        Message message = message(thread(mailAccount(user), "gmail-thread-1", Direction.INBOUND));
        stubTrashCounts(user.getId());
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(sliceWithHasNext(List.of(message), false));

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertFalse(result.hasNext());
        assertNull(result.nextMarker());
    }

    @Test
    void 비검색_조회는_휴지통_카운트_값을_응답에_포함한다() {
        // given
        User user = user();
        when(trashQueryService.findDeletedMessagesByUserId(any(), any(), anyInt(), any(), any()))
                .thenReturn(emptySlice());
        when(trashQueryService.countUnreadDeletedMessagesByUserId(user.getId(), null, null)).thenReturn(7L);
        when(trashQueryService.countDeletedMessagesByUserId(user.getId(), null, null)).thenReturn(42L);

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, null);

        // then
        assertEquals(7L, result.unreadCount());
        assertEquals(42L, result.totalCount());
    }

    @Test
    void 검색_조회는_검색_카운트_값을_응답에_포함한다() {
        // given
        User user = user();
        when(mailSearchQueryService.searchTrashMessages(any(), any(), any(), any(), any(), any()))
                .thenReturn(emptySlice());
        when(mailSearchQueryService.countUnreadTrashMessages(user.getId(), "키워드", null, null)).thenReturn(3L);
        when(mailSearchQueryService.countTrashMessages(user.getId(), "키워드", null, null)).thenReturn(15L);

        // when
        MarkerSliceResponse<TrashThreadSummaryResponse> result =
                trashFacade.getTrashThreads(user, null, 50, null, null, "키워드");

        // then
        assertEquals(3L, result.unreadCount());
        assertEquals(15L, result.totalCount());
    }

    @Test
    void 휴지통_상세_조회는_삭제된_메시지의_from_to_cc를_중복_제거해_연락처_조회에_사용한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message first = detailedMessage(
                thread,
                "from@example.com",
                List.of("to@example.com", "from@example.com", " "),
                List.of("cc@example.com")
        );
        Message second = detailedMessage(thread, "from@example.com", List.of("to@example.com"), null);
        ThreadMessageLabelView label = new ThreadMessageLabelView(thread.getId(), UUID.randomUUID(), "중요", "#ff0000");
        when(trashQueryService.findThreadByIdIncludingDeleted(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(trashQueryService.findDeletedMessagesByMailAccountIdAndGmailThreadId(account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of(first, second));
        when(trashQueryService.findContactNamesByEmails(
                user.getId(), List.of("from@example.com", "to@example.com", "cc@example.com")))
                .thenReturn(Map.of("from@example.com", "보낸사람"));
        when(trashQueryService.findLabelsByThreadIds(List.of(thread.getId())))
                .thenReturn(Map.of(thread.getId(), List.of(label)));
        when(inlineImageService.renderInlineImageUrls(first)).thenReturn("<p>first</p>");
        when(inlineImageService.renderInlineImageUrls(second)).thenReturn("<p>second</p>");

        // when
        TrashThreadDetailResponse result = trashFacade.getTrashThreadDetail(user, thread.getId());

        // then
        assertEquals(thread.getId(), result.threadId());
        assertEquals(2, result.messages().size());
        assertEquals(1, result.labels().size());
        assertEquals("<p>first</p>", result.messages().getFirst().bodyHtml());
        verify(trashQueryService).findContactNamesByEmails(
                user.getId(), List.of("from@example.com", "to@example.com", "cc@example.com"));
    }

    @Test
    void 휴지통_상세_조회에서_삭제된_메시지가_없으면_빈_이메일_목록으로_연락처를_조회하고_빈_메시지를_반환한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findThreadByIdIncludingDeleted(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(trashQueryService.findDeletedMessagesByMailAccountIdAndGmailThreadId(account.getId(), thread.getGmailThreadId()))
                .thenReturn(List.of());
        when(trashQueryService.findContactNamesByEmails(user.getId(), List.of())).thenReturn(Map.of());
        when(trashQueryService.findLabelsByThreadIds(List.of(thread.getId()))).thenReturn(Map.of());

        // when
        TrashThreadDetailResponse result = trashFacade.getTrashThreadDetail(user, thread.getId());

        // then
        assertTrue(result.messages().isEmpty());
        assertTrue(result.labels().isEmpty());
        verify(inlineImageService, never()).renderInlineImageUrls(any());
    }

    @Test
    void 휴지통_상세_조회에서_소유_계정이_아니면_상세_조회를_진행하지_않고_예외가_발생한다() {
        // given
        User user = user();
        MailAccount userAccount = mailAccount(user);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build());
        Thread thread = thread(otherAccount, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findThreadByIdIncludingDeleted(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(userAccount));

        // when & then
        assertThrows(TrashException.class, () -> trashFacade.getTrashThreadDetail(user, thread.getId()));
        verify(trashQueryService, never()).findDeletedMessagesByMailAccountIdAndGmailThreadId(any(), any());
    }

    @Test
    void 스레드_삭제는_소유권_검증_후_로컬_삭제와_구글_휴지통_API를_호출한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findActiveThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(account)).thenReturn(account);

        // when
        trashFacade.deleteThread(user, thread.getId());

        // then
        verify(trashCommandService).softDeleteThread(thread);
        verify(googleGmailApiService).trashThread(account.getAccessToken(), thread.getGmailThreadId());
    }

    @Test
    void 메시지_삭제는_소유권_검증_후_로컬_삭제와_구글_메시지_휴지통_API를_호출한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message message = message(thread);
        when(trashQueryService.findActiveMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(account)).thenReturn(account);

        // when
        trashFacade.deleteMessage(user, message.getId());

        // then
        verify(trashCommandService).softDeleteMessage(message);
        verify(googleGmailApiService).trashMessage(account.getAccessToken(), message.getGmailMessageId());
    }

    @Test
    void 스레드_복구는_소유권_검증_후_로컬_복구와_구글_복구_API를_호출한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findDeletedThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(account)).thenReturn(account);

        // when
        trashFacade.restoreThread(user, thread.getId());

        // then
        verify(trashCommandService).restoreThread(thread);
        verify(googleGmailApiService).untrashThread(account.getAccessToken(), thread.getGmailThreadId());
    }

    @Test
    void 메시지_복구는_소유권_검증_후_로컬_복구와_구글_메시지_복구_API를_호출한다() {
        // given
        User user = user();
        MailAccount account = mailAccount(user);
        Thread thread = thread(account, "gmail-thread-1", Direction.INBOUND);
        Message message = message(thread);
        when(trashQueryService.findDeletedMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(account));
        when(googleAccessTokenEnsureService.ensureValidGoogleAccessToken(account)).thenReturn(account);

        // when
        trashFacade.restoreMessage(user, message.getId());

        // then
        verify(trashCommandService).restoreMessage(message);
        verify(googleGmailApiService).untrashMessage(account.getAccessToken(), message.getGmailMessageId());
    }

    @Test
    void 사용자_소유_계정의_스레드가_아니면_삭제를_수행하지_않고_예외가_발생한다() {
        // given
        User user = user();
        MailAccount userAccount = mailAccount(user);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build());
        Thread thread = thread(otherAccount, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findActiveThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(userAccount));

        // when & then
        assertThrows(TrashException.class, () -> trashFacade.deleteThread(user, thread.getId()));
        verify(trashCommandService, never()).softDeleteThread(any());
        verify(googleGmailApiService, never()).trashThread(any(), any());
    }

    @Test
    void 사용자_소유_계정의_메시지가_아니면_메시지_삭제를_수행하지_않고_예외가_발생한다() {
        // given
        User user = user();
        MailAccount userAccount = mailAccount(user);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build());
        Message message = message(thread(otherAccount, "gmail-thread-1", Direction.INBOUND));
        when(trashQueryService.findActiveMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(userAccount));

        // when & then
        assertThrows(TrashException.class, () -> trashFacade.deleteMessage(user, message.getId()));
        verify(trashCommandService, never()).softDeleteMessage(any());
        verify(googleGmailApiService, never()).trashMessage(any(), any());
    }

    @Test
    void 사용자_소유_계정의_스레드가_아니면_복구를_수행하지_않고_예외가_발생한다() {
        // given
        User user = user();
        MailAccount userAccount = mailAccount(user);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build());
        Thread thread = thread(otherAccount, "gmail-thread-1", Direction.INBOUND);
        when(trashQueryService.findDeletedThreadById(thread.getId())).thenReturn(thread);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(userAccount));

        // when & then
        assertThrows(TrashException.class, () -> trashFacade.restoreThread(user, thread.getId()));
        verify(trashCommandService, never()).restoreThread(any());
        verify(googleGmailApiService, never()).untrashThread(any(), any());
    }

    @Test
    void 사용자_소유_계정의_메시지가_아니면_메시지_복구를_수행하지_않고_예외가_발생한다() {
        // given
        User user = user();
        MailAccount userAccount = mailAccount(user);
        MailAccount otherAccount = mailAccount(User.builder().id(UUID.randomUUID()).build());
        Message message = message(thread(otherAccount, "gmail-thread-1", Direction.INBOUND));
        when(trashQueryService.findDeletedMessageById(message.getId())).thenReturn(message);
        when(mailAccountQueryService.findAllActiveByUserId(user.getId())).thenReturn(List.of(userAccount));

        // when & then
        assertThrows(TrashException.class, () -> trashFacade.restoreMessage(user, message.getId()));
        verify(trashCommandService, never()).restoreMessage(any());
        verify(googleGmailApiService, never()).untrashMessage(any(), any());
    }

    private void stubTrashCounts(UUID userId) {
        lenient().when(trashQueryService.countUnreadDeletedMessagesByUserId(eq(userId), any(), any()))
                .thenReturn(0L);
        lenient().when(trashQueryService.countDeletedMessagesByUserId(eq(userId), any(), any()))
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

    private Message detailedMessage(Thread thread, String fromAddress, List<String> toAddresses, List<String> ccAddresses) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(thread.getDirection())
                .subject("subject")
                .fromAddress(fromAddress)
                .toAddresses(toAddresses)
                .ccAddresses(ccAddresses)
                .bodyHtml("<p>raw</p>")
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
