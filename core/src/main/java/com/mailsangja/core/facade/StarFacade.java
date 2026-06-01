package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.service.inbox.InboxCommandService;
import com.mailsangja.core.service.inbox.InboxQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.search.MailSearchQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
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
public class StarFacade {

    private final InboxCommandService inboxCommandService;
    private final InboxQueryService inboxQueryService;
    private final MailAccountQueryService mailAccountQueryService;
    private final MailSearchQueryService mailSearchQueryService;

    public MarkerSliceResponse<ThreadSummaryResponse> getStarred(
            User user,
            UUID marker,
            int size,
            List<UUID> labelIds,
            Boolean read,
            String q
    ) {
        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            ThreadListResult result = mailSearchQueryService.searchStarredThreadsResult(
                    user.getId(), trimmed, labelIds, read, marker, PageRequest.of(0, size));
            long unreadCount = mailSearchQueryService.countUnreadStarredThreads(user.getId(), trimmed, labelIds, read);
            long totalCount = mailSearchQueryService.countStarredThreads(user.getId(), trimmed, labelIds, read);
            return toMarkerSlice(result, unreadCount, totalCount);
        }
        ThreadListResult result = inboxQueryService.findStarredThreadsResult(
                user.getId(), marker, labelIds, read, PageRequest.of(0, size));
        long unreadCount = inboxQueryService.countUnreadStarred(user.getId(), labelIds, read);
        long totalCount = inboxQueryService.countStarred(user.getId(), labelIds, read);
        return toMarkerSlice(result, unreadCount, totalCount);
    }

    private MarkerSliceResponse<ThreadSummaryResponse> toMarkerSlice(
            ThreadListResult result,
            long unreadCount,
            long totalCount
    ) {
        List<ThreadSummaryResponse> content = result.threads().getContent().stream()
                .map(thread -> ThreadSummaryResponse.from(
                        thread,
                        result.attachmentsByThreadId().getOrDefault(thread.getId(), List.of()),
                        result.contactNameByEmail(),
                        result.labelsByThreadId().getOrDefault(thread.getId(), List.of())))
                .toList();
        UUID nextMarker = result.threads().hasNext() ? result.threads().getContent().getLast().getId() : null;
        return MarkerSliceResponse.of(content, nextMarker, result.threads().hasNext(), unreadCount, totalCount);
    }

    public boolean toggleThreadStar(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        return inboxCommandService.toggleStar(thread);
    }

    public boolean toggleMessageStar(User user, UUID messageId) {
        Message message = inboxQueryService.findActiveMessageById(messageId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), message.getThread());
        return inboxCommandService.toggleMessageStar(message);
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
