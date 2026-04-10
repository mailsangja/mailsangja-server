package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadDetailResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.service.inbox.InboxQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InboxFacade {

    private final InboxQueryService inboxQueryService;
    private final MailAccountQueryService mailAccountQueryService;

    public MarkerSliceResponse<ThreadSummaryResponse> getInbox(User user, UUID marker, int size) {
        return getThreadList(user, marker, size, false);
    }

    public MarkerSliceResponse<ThreadSummaryResponse> getSent(User user, UUID marker, int size) {
        return getThreadList(user, marker, size, true);
    }

    public ThreadDetailResponse getThreadDetail(User user, UUID threadId) {
        List<MailAccount> userAccounts = mailAccountQueryService.findAllByUserId(user.getId());
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(userAccounts, thread);

        // 같은 gmail_thread_id의 INBOUND + OUTBOUND 메시지를 모두 반환 (전체 대화)
        List<Message> messages = inboxQueryService.findAllMessagesByThread(
                thread.getMailAccount().getId(),
                thread.getGmailThreadId()
        );
        return ThreadDetailResponse.from(thread, messages);
    }

    public long getUnreadCount(User user) {
        return inboxQueryService.countUnreadInbox(user.getId());
    }

    private MarkerSliceResponse<ThreadSummaryResponse> getThreadList(User user, UUID marker, int size, boolean isSent) {
        Pageable pageable = PageRequest.of(0, size);
        Slice<Thread> threads = isSent
                ? inboxQueryService.findSentThreadsByUserId(user.getId(), marker, pageable)
                : inboxQueryService.findInboxThreadsByUserId(user.getId(), marker, pageable);

        List<UUID> threadIds = threads.getContent().stream().map(Thread::getId).toList();
        Map<UUID, List<Attachment>> attachmentsByThreadId = inboxQueryService.findAttachmentsByThreadIds(threadIds);

        List<ThreadSummaryResponse> content = threads.getContent().stream()
                .map(thread -> ThreadSummaryResponse.from(thread, attachmentsByThreadId.getOrDefault(thread.getId(), List.of())))
                .toList();

        UUID nextMarker = threads.hasNext() ? threads.getContent().getLast().getId() : null;
        return MarkerSliceResponse.of(content, nextMarker, threads.hasNext());
    }

    private void validateThreadAccess(List<MailAccount> userAccounts, Thread thread) {
        Set<UUID> userAccountIds = userAccounts.stream()
                .map(MailAccount::getId)
                .collect(Collectors.toSet());

        if (!userAccountIds.contains(thread.getMailAccount().getId())) {
            throw new InboxException(InboxErrorCode.THREAD_ACCESS_DENIED);
        }
    }
}
