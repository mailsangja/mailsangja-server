package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleGmailApiService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.trash.TrashCommandService;
import com.mailsangja.core.service.trash.TrashQueryService;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
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

    public void deleteThread(User user, UUID threadId) {
        Thread thread = trashQueryService.findActiveThreadById(threadId);
        validateThreadAccess(user, thread);
        trashCommandService.softDeleteThread(thread);
        googleGmailApiService.trashThread(thread.getMailAccount().getAccessToken(), thread.getGmailThreadId());
    }

    public void deleteMessage(User user, UUID messageId) {
        Message message = trashQueryService.findActiveMessageById(messageId);
        validateThreadAccess(user, message.getThread());
        trashCommandService.softDeleteMessage(message);
        googleGmailApiService.trashMessage(message.getThread().getMailAccount().getAccessToken(), message.getGmailMessageId());
    }

    public List<TrashThreadSummaryResponse> getTrashThreads(User user) {
        List<Thread> threads = trashQueryService.findTrashThreadsByUserId(user.getId());
        return threads.stream()
                .map(TrashThreadSummaryResponse::from)
                .toList();
    }

    public void restoreThread(User user, UUID threadId) {
        Thread thread = trashQueryService.findDeletedThreadById(threadId);
        validateThreadAccess(user, thread);
        trashCommandService.restoreThread(thread);
        googleGmailApiService.untrashThread(thread.getMailAccount().getAccessToken(), thread.getGmailThreadId());
    }

    public void restoreMessage(User user, UUID messageId) {
        Message message = trashQueryService.findDeletedMessageById(messageId);
        validateThreadAccess(user, message.getThread());
        trashCommandService.restoreMessage(message);
        googleGmailApiService.untrashMessage(message.getThread().getMailAccount().getAccessToken(), message.getGmailMessageId());
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
