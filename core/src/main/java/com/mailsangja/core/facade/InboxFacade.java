package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.common.SliceResponse;
import com.mailsangja.core.dto.inbox.ThreadDetailResponse;
import com.mailsangja.core.dto.inbox.ThreadSummaryResponse;
import com.mailsangja.core.config.properties.InboxProperties;
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
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class InboxFacade {

    private final InboxProperties inboxProperties;
    private final InboxQueryService inboxQueryService;
    private final MailAccountQueryService mailAccountQueryService;

    public SliceResponse<ThreadSummaryResponse> getInbox(User user, int page) {
        return getThreadList(user, page, false);
    }

    public SliceResponse<ThreadSummaryResponse> getSent(User user, int page) {
        return getThreadList(user, page, true);
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

    private SliceResponse<ThreadSummaryResponse> getThreadList(User user, int page, boolean isSent) {
        List<UUID> accountIds = mailAccountQueryService.findAllByUserId(user.getId()).stream()
                .map(MailAccount::getId)
                .toList();

        if (accountIds.isEmpty()) {
            return SliceResponse.from(new SliceImpl<>(Collections.emptyList()));
        }

        Pageable pageable = PageRequest.of(page, inboxProperties.getPageSize());
        Slice<Thread> threads = isSent
                ? inboxQueryService.findSentThreadsByAccountIds(accountIds, pageable)
                : inboxQueryService.findInboxThreadsByAccountIds(accountIds, pageable);

        List<UUID> threadIds = threads.getContent().stream().map(Thread::getId).toList();
        Map<UUID, List<Attachment>> attachmentsByThreadId = inboxQueryService.findAttachmentsByThreadIds(threadIds);

        List<ThreadSummaryResponse> content = threads.getContent().stream()
                .map(thread -> ThreadSummaryResponse.from(
                        thread,
                        attachmentsByThreadId.getOrDefault(thread.getId(), List.of())
                ))
                .toList();

        return new SliceResponse<>(content, threads.getNumber(), threads.hasNext());
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
