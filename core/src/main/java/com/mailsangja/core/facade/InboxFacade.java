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
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.InlineImageService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.search.MailSearchQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
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
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;
    private final InlineImageService inlineImageService;
    private final MailSearchQueryService mailSearchQueryService;

    public MarkerSliceResponse<ThreadSummaryResponse> getInbox(
            User user,
            UUID marker,
            int size,
            List<UUID> labelIds,
            Boolean read,
            String q
    ) {
        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            ThreadListResult result = mailSearchQueryService.searchInboxThreadsResult(
                    user.getId(), trimmed, labelIds, read, marker, PageRequest.of(0, size));
            long unreadCount = mailSearchQueryService.countUnreadInboxThreads(user.getId(), trimmed, labelIds, read);
            long totalCount = mailSearchQueryService.countInboxThreads(user.getId(), trimmed, labelIds, read);
            return toMarkerSlice(result, unreadCount, totalCount);
        }
        ThreadListResult result = inboxQueryService.findInboxThreadsResult(
                user.getId(), marker, labelIds, read, PageRequest.of(0, size));
        long unreadCount = inboxQueryService.countUnreadInbox(user.getId(), labelIds, read);
        long totalCount = inboxQueryService.countInbox(user.getId(), labelIds, read);
        return toMarkerSlice(result, unreadCount, totalCount);
    }

    public MarkerSliceResponse<ThreadSummaryResponse> getSent(
            User user,
            UUID marker,
            int size,
            List<UUID> labelIds,
            Boolean read,
            String q
    ) {
        if (q != null && !q.isBlank()) {
            String trimmed = q.trim();
            ThreadListResult result = mailSearchQueryService.searchSentThreadsResult(
                    user.getId(), trimmed, labelIds, read, marker, PageRequest.of(0, size));
            long unreadCount = mailSearchQueryService.countUnreadSentThreads(user.getId(), trimmed, labelIds, read);
            long totalCount = mailSearchQueryService.countSentThreads(user.getId(), trimmed, labelIds, read);
            return toMarkerSlice(result, unreadCount, totalCount);
        }
        ThreadListResult result = inboxQueryService.findSentThreadsResult(
                user.getId(), marker, labelIds, read, PageRequest.of(0, size));
        long unreadCount = inboxQueryService.countUnreadSent(user.getId(), labelIds, read);
        long totalCount = inboxQueryService.countSent(user.getId(), labelIds, read);
        return toMarkerSlice(result, unreadCount, totalCount);
    }

    public ThreadDetailResponse getThreadDetail(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        ThreadDetailResult result = inboxQueryService.findThreadDetailResult(thread);
        return ThreadDetailResponse.from(
                result.thread(),
                result.messages(),
                result.contactNameByEmail(),
                result.labels(),
                renderBodyHtmlByMessageId(result.messages())
        );
    }

    public void markThreadAsRead(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        if (thread.getMailAccount().getProvider() == MailProvider.GMAIL) {
            googleMailReadCommandService.markThreadAsRead(
                    googleAccessTokenEnsureService.ensureValidGoogleAccessToken(thread.getMailAccount()),
                    thread
            );
        }
        inboxCommandService.markThreadAsRead(thread);
    }

    public void markMessageAsRead(User user, UUID messageId) {
        Message message = inboxQueryService.findActiveMessageById(messageId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), message.getThread());
        if (message.getThread().getMailAccount().getProvider() == MailProvider.GMAIL) {
            googleMailReadCommandService.markMessageAsRead(
                    googleAccessTokenEnsureService.ensureValidGoogleAccessToken(message.getThread().getMailAccount()),
                    message
            );
        }
        inboxCommandService.markMessageAsRead(message);
    }

    public void markMessageAsUnread(User user, UUID messageId) {
        Message message = inboxQueryService.findActiveMessageById(messageId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), message.getThread());
        if (message.getThread().getMailAccount().getProvider() == MailProvider.GMAIL) {
            googleMailReadCommandService.markMessageAsUnread(
                    googleAccessTokenEnsureService.ensureValidGoogleAccessToken(message.getThread().getMailAccount()),
                    message
            );
        }
        inboxCommandService.markMessageAsUnread(message);
    }

    public void markThreadAsUnread(User user, UUID threadId) {
        Thread thread = inboxQueryService.findThreadById(threadId);
        validateThreadAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), thread);
        if (thread.getMailAccount().getProvider() == MailProvider.GMAIL) {
            googleMailReadCommandService.markThreadAsUnread(
                    googleAccessTokenEnsureService.ensureValidGoogleAccessToken(thread.getMailAccount()),
                    thread
            );
        }
        inboxCommandService.markThreadAsUnread(thread);
    }

    public long getUnreadCount(User user) {
        return inboxQueryService.countUnreadInbox(user.getId());
    }

    private MarkerSliceResponse<ThreadSummaryResponse> toMarkerSlice(ThreadListResult result) {
        return toMarkerSlice(result, 0L, 0L);
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

    private java.util.Map<UUID, String> renderBodyHtmlByMessageId(List<Message> messages) {
        return messages.stream()
                .collect(Collectors.toMap(
                        Message::getId,
                        message -> {
                            String rendered = inlineImageService.renderInlineImageUrls(message);
                            return rendered != null ? rendered : "";
                        }
                ));
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
