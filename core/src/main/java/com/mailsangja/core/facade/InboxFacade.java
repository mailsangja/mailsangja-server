package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadDetailResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleMailReadCommandService;
import com.mailsangja.core.service.inbox.InboxCommandService;
import com.mailsangja.core.service.inbox.InboxQueryService;
import com.mailsangja.core.dto.inbox.ThreadDetailResult;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InboxFacade {

    private final GoogleMailReadCommandService googleMailReadCommandService;
    private final InboxCommandService inboxCommandService;
    private final InboxQueryService inboxQueryService;
    private final MailAccountQueryService mailAccountQueryService;

    public MarkerSliceResponse<ThreadSummaryResponse> getInbox(User user, UUID marker, int size) {
        ThreadListResult result = inboxQueryService.findInboxThreadsResult(user.getId(), marker, PageRequest.of(0, size));
        return toMarkerSlice(result);
    }

    public MarkerSliceResponse<ThreadSummaryResponse> getSent(User user, UUID marker, int size) {
        ThreadListResult result = inboxQueryService.findSentThreadsResult(user.getId(), marker, PageRequest.of(0, size));
        return toMarkerSlice(result);
    }

    public ThreadDetailResponse getThreadDetail(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        ThreadDetailResult result = inboxQueryService.findThreadDetailResult(thread);
        return ThreadDetailResponse.from(result.thread(), result.messages(), result.contactNameByEmail());
    }

    public void markThreadAsRead(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        if (thread.getMailAccount().getProvider() == MailProvider.GMAIL) {
            googleMailReadCommandService.markThreadAsRead(thread.getMailAccount(), thread);
        }
        inboxCommandService.markThreadAsRead(thread);
    }

    public long getUnreadCount(User user) {
        return inboxQueryService.countUnreadInbox(user.getId());
    }

    private MarkerSliceResponse<ThreadSummaryResponse> toMarkerSlice(ThreadListResult result) {
        List<ThreadSummaryResponse> content = result.threads().getContent().stream()
                .map(thread -> ThreadSummaryResponse.from(
                        thread,
                        result.attachmentsByThreadId().getOrDefault(thread.getId(), List.of()),
                        result.contactNameByEmail()))
                .toList();
        UUID nextMarker = result.threads().hasNext() ? result.threads().getContent().getLast().getId() : null;
        return MarkerSliceResponse.of(content, nextMarker, result.threads().hasNext());
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
