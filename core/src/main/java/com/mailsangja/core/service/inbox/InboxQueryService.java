package com.mailsangja.core.service.inbox;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InboxQueryService {

    private final ThreadRepositoryPort threadRepositoryPort;
    private final MessageRepositoryPort messageRepositoryPort;

    public Slice<Thread> findInboxThreadsByAccountIds(List<UUID> accountIds, Pageable pageable) {
        return threadRepositoryPort.findInboxByMailAccountIdInAndDeletedAtIsNull(accountIds, pageable);
    }

    public Slice<Thread> findSentThreadsByAccountIds(List<UUID> accountIds, Pageable pageable) {
        return threadRepositoryPort.findSentByMailAccountIdInAndDeletedAtIsNull(accountIds, pageable);
    }

    public Thread findThreadById(UUID threadId) {
        return threadRepositoryPort.findByIdAndDeletedAtIsNull(threadId)
                .orElseThrow(() -> new InboxException(InboxErrorCode.THREAD_NOT_FOUND));
    }

    // 스레드 상세: INBOUND/OUTBOUND 두 행의 메시지를 모두 반환 (전체 대화)
    public List<Message> findAllMessagesByThread(UUID mailAccountId, String gmailThreadId) {
        return messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(mailAccountId, gmailThreadId);
    }

    // 목록용: 여러 스레드의 첨부파일을 한 번에 조회 (N+1 방지)
    public Map<UUID, List<Attachment>> findAttachmentsByThreadIds(List<UUID> threadIds) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }
        List<Message> messages = messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(threadIds);
        return messages.stream()
                .filter(m -> !m.getAttachments().isEmpty())
                .flatMap(m -> m.getAttachments().stream()
                        .map(a -> Map.entry(m.getThread().getId(), a)))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }
}
