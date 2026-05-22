package com.mailsangja.db.adapter.search;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.module.mail.MessageJpaRepositoryModule;
import com.mailsangja.db.module.search.MailSearchJpaRepositoryModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailSearchRepositoryAdapterTest {

    private static final String SENTINEL_LABEL_ID = "00000000-0000-0000-0000-000000000000";

    @Mock private MailSearchJpaRepositoryModule mailSearchJpaRepositoryModule;
    @Mock private MessageJpaRepositoryModule messageJpaRepositoryModule;

    @InjectMocks
    private MailSearchRepositoryAdapter mailSearchRepositoryAdapter;

    @Test
    void 전체_스레드_검색_첫_페이지는_ID_조회_후_엔티티_순서를_검색결과_순서대로_복원한다() {
        // given
        UUID userId = UUID.randomUUID();
        Thread first = thread();
        Thread second = thread();
        when(mailSearchJpaRepositoryModule.searchThreadIdsFirstPage(userId.toString(), "프로젝트", 3))
                .thenReturn(List.of(first.getId().toString(), second.getId().toString()));
        when(mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(second, first));

        // when
        Slice<Thread> result =
                mailSearchRepositoryAdapter.searchThreads(userId, "프로젝트", null, PageRequest.of(0, 2));

        // then
        assertEquals(List.of(first, second), result.getContent());
        assertFalse(result.hasNext());
    }

    @Test
    void 전체_스레드_검색_다음_페이지는_마커_이후_ID_조회_메서드를_사용한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID markerId = UUID.randomUUID();
        Thread thread = thread();
        when(mailSearchJpaRepositoryModule.searchThreadIdsAfterMarker(userId.toString(), "프로젝트", markerId.toString(), 2))
                .thenReturn(List.of(thread.getId().toString()));
        when(mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(List.of(thread.getId())))
                .thenReturn(List.of(thread));

        // when
        Slice<Thread> result =
                mailSearchRepositoryAdapter.searchThreads(userId, "프로젝트", markerId, PageRequest.of(0, 1));

        // then
        assertEquals(List.of(thread), result.getContent());
        verify(mailSearchJpaRepositoryModule, never()).searchThreadIdsFirstPage(any(), any(), anyInt());
    }

    @Test
    void 인박스_검색_ID가_pageSize보다_많으면_hasNext가_true이고_pageSize만_반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        Thread first = thread();
        Thread second = thread();
        Thread extra = thread();
        List<UUID> labelIds = List.of(UUID.randomUUID());
        when(mailSearchJpaRepositoryModule.searchInboxThreadIdsFirstPage(
                userId.toString(), "검색", List.of(labelIds.getFirst().toString()), false, null, 3))
                .thenReturn(List.of(first.getId().toString(), second.getId().toString(), extra.getId().toString()));
        when(mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(first, second));

        // when
        Slice<Thread> result =
                mailSearchRepositoryAdapter.searchInboxThreads(userId, "검색", labelIds, null, null, PageRequest.of(0, 2));

        // then
        assertTrue(result.hasNext());
        assertEquals(List.of(first, second), result.getContent());
    }

    @Test
    void 라벨이_비어있는_보낸메일_검색은_센티널_라벨과_labelsEmpty_true를_전달한다() {
        // given
        UUID userId = UUID.randomUUID();
        Thread thread = thread();
        when(mailSearchJpaRepositoryModule.searchSentThreadIdsFirstPage(
                userId.toString(),
                "검색",
                List.of(SENTINEL_LABEL_ID),
                true,
                false,
                2
        )).thenReturn(List.of(thread.getId().toString()));
        when(mailSearchJpaRepositoryModule.findAllByIdInWithMailAccount(List.of(thread.getId())))
                .thenReturn(List.of(thread));

        // when
        Slice<Thread> result =
                mailSearchRepositoryAdapter.searchSentThreads(userId, "검색", List.of(), false, null, PageRequest.of(0, 1));

        // then
        assertEquals(List.of(thread), result.getContent());
    }

    @Test
    void 휴지통_검색은_메시지_ID_조회_후_메시지_순서를_검색결과_순서대로_복원한다() {
        // given
        UUID userId = UUID.randomUUID();
        Message first = message();
        Message second = message();
        when(mailSearchJpaRepositoryModule.searchTrashMessageIdsFirstPage(
                userId.toString(), "삭제", List.of(SENTINEL_LABEL_ID), true, null, 3))
                .thenReturn(List.of(first.getId().toString(), second.getId().toString()));
        when(messageJpaRepositoryModule.findAllByIdInWithThread(List.of(first.getId(), second.getId())))
                .thenReturn(List.of(second, first));

        // when
        Slice<Message> result =
                mailSearchRepositoryAdapter.searchTrashMessages(userId, "삭제", null, null, null, PageRequest.of(0, 2));

        // then
        assertEquals(List.of(first, second), result.getContent());
    }

    @Test
    void 읽음_필터가_true이면_읽지않음_카운트는_조회하지_않고_0을_반환한다() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        long inbox = mailSearchRepositoryAdapter.countUnreadInboxThreads(userId, "검색", null, true);
        long sent = mailSearchRepositoryAdapter.countUnreadSentThreads(userId, "검색", null, true);
        long trash = mailSearchRepositoryAdapter.countUnreadTrashMessages(userId, "검색", null, true);

        // then
        assertEquals(0L, inbox);
        assertEquals(0L, sent);
        assertEquals(0L, trash);
        verify(mailSearchJpaRepositoryModule, never()).countUnreadInboxThreads(any(), any(), anyList(), anyBoolean());
        verify(mailSearchJpaRepositoryModule, never()).countUnreadSentThreads(any(), any(), anyList(), anyBoolean());
        verify(mailSearchJpaRepositoryModule, never()).countUnreadTrashMessages(any(), any(), anyList(), anyBoolean());
    }

    @Test
    void count_쿼리_결과가_null이면_0을_반환한다() {
        // given
        UUID userId = UUID.randomUUID();
        when(mailSearchJpaRepositoryModule.countInboxThreads(
                userId.toString(), "검색", List.of(SENTINEL_LABEL_ID), true, null))
                .thenReturn(null);

        // when
        long result = mailSearchRepositoryAdapter.countInboxThreads(userId, "검색", null, null);

        // then
        assertEquals(0L, result);
    }

    private Thread thread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(MailAccount.builder()
                        .id(UUID.randomUUID())
                        .user(User.builder().id(UUID.randomUUID()).build())
                        .provider(MailProvider.GMAIL)
                        .emailAddress("user@example.com")
                        .alias("gmail")
                        .icon("icon")
                        .color("#000000")
                        .accessToken("token")
                        .build())
                .gmailThreadId("gmail-thread-" + UUID.randomUUID())
                .direction(Direction.INBOUND)
                .build();
    }

    private Message message() {
        Thread thread = thread();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-" + UUID.randomUUID())
                .direction(Direction.INBOUND)
                .fromAddress("from@example.com")
                .build();
    }
}
