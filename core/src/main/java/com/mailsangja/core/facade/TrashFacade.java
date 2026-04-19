package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadDetailResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleGmailApiService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.trash.TrashCommandService;
import com.mailsangja.core.service.trash.TrashQueryService;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class TrashFacade {

    private final TrashCommandService trashCommandService;
    private final TrashQueryService trashQueryService;
    private final GoogleGmailApiService googleGmailApiService;
    private final MailAccountQueryService mailAccountQueryService;
    private final GoogleAccessTokenEnsureService googleAccessTokenEnsureService;

    public void deleteThread(User user, UUID threadId) {
        Thread thread = trashQueryService.findActiveThreadById(threadId);
        validateThreadAccess(user, thread);
        trashCommandService.softDeleteThread(thread);
        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(thread.getMailAccount());
        googleGmailApiService.trashThread(ensuredMailAccount.getAccessToken(), thread.getGmailThreadId());
    }

    public void deleteMessage(User user, UUID messageId) {
        Message message = trashQueryService.findActiveMessageById(messageId);
        validateThreadAccess(user, message.getThread());
        trashCommandService.softDeleteMessage(message);
        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(message.getThread().getMailAccount());
        googleGmailApiService.trashMessage(ensuredMailAccount.getAccessToken(), message.getGmailMessageId());
    }

    public MarkerSliceResponse<TrashThreadSummaryResponse> getTrashThreads(User user, UUID marker, int size) {
        Slice<Message> messages = trashQueryService.findDeletedMessagesByUserId(user.getId(), marker, size);

        // (mailAccountId + gmailThreadId) 기준으로 메시지를 그룹핑 (삽입 순서 유지)
        Map<String, List<Message>> grouped = messages.getContent().stream()
                .collect(Collectors.groupingBy(
                        m -> m.getThread().getMailAccount().getId() + ":" + m.getThread().getGmailThreadId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<TrashThreadSummaryResponse> content = grouped.values().stream()
                .map(msgs -> {
                    // INBOUND Thread를 대표로 우선 사용, 없으면 첫 번째 Thread 사용
                    Thread representative = msgs.stream()
                            .map(Message::getThread)
                            .filter(t -> t.getDirection() == Direction.INBOUND)
                            .findFirst()
                            .orElse(msgs.get(0).getThread());
                    return TrashThreadSummaryResponse.from(representative, msgs);
                })
                .toList();

        UUID nextMarker = messages.hasNext() ? messages.getContent().getLast().getId() : null;
        return MarkerSliceResponse.of(content, nextMarker, messages.hasNext());
    }

    public TrashThreadDetailResponse getTrashThreadDetail(User user, UUID threadId) {
        Thread thread = trashQueryService.findThreadByIdIncludingDeleted(threadId);
        validateThreadAccess(user, thread);
        List<Message> deletedMessages = trashQueryService.findDeletedMessagesByMailAccountIdAndGmailThreadId(
                thread.getMailAccount().getId(), thread.getGmailThreadId());
        return TrashThreadDetailResponse.from(thread, deletedMessages);
    }

    public void restoreThread(User user, UUID threadId) {
        Thread thread = trashQueryService.findDeletedThreadById(threadId);
        validateThreadAccess(user, thread);
        trashCommandService.restoreThread(thread);
        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(thread.getMailAccount());
        googleGmailApiService.untrashThread(ensuredMailAccount.getAccessToken(), thread.getGmailThreadId());
    }

    public void restoreMessage(User user, UUID messageId) {
        Message message = trashQueryService.findDeletedMessageById(messageId);
        validateThreadAccess(user, message.getThread());
        trashCommandService.restoreMessage(message);
        MailAccount ensuredMailAccount = googleAccessTokenEnsureService.ensureValidGoogleAccessToken(message.getThread().getMailAccount());
        googleGmailApiService.untrashMessage(ensuredMailAccount.getAccessToken(), message.getGmailMessageId());
    }

    private void validateThreadAccess(User user, Thread thread) {
        List<MailAccount> userAccounts = mailAccountQueryService.findAllActiveByUserId(user.getId());
        Set<UUID> userAccountIds = userAccounts.stream()
                .map(MailAccount::getId)
                .collect(Collectors.toSet());

        if (!userAccountIds.contains(thread.getMailAccount().getId())) {
            throw new TrashException(TrashErrorCode.THREAD_ACCESS_DENIED);
        }
    }
}
