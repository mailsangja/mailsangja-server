package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.trash.TrashErrorCode;
import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.core.dto.common.MarkerSliceResponse;
import com.mailsangja.core.dto.trash.TrashThreadDetailResponse;
import com.mailsangja.core.dto.trash.TrashThreadSummaryResponse;
import com.mailsangja.core.service.google.GoogleGmailApiService;
import com.mailsangja.core.service.mail.GoogleAccessTokenEnsureService;
import com.mailsangja.core.service.mail.InlineImageService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.trash.TrashCommandService;
import com.mailsangja.core.service.trash.TrashQueryService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.dto.MessageLabelView;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
    private final InlineImageService inlineImageService;

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

    public MarkerSliceResponse<TrashThreadSummaryResponse> getTrashThreads(
            User user,
            UUID marker,
            int size,
            List<UUID> labelIds,
            Boolean read
    ) {
        Slice<Message> messages = trashQueryService.findDeletedMessagesByUserId(user.getId(), marker, size, labelIds, read);
        long unreadCount = trashQueryService.countUnreadDeletedMessagesByUserId(user.getId(), labelIds, read);
        long totalCount = trashQueryService.countDeletedMessagesByUserId(user.getId(), labelIds, read);

        Map<String, List<Message>> grouped = messages.getContent().stream()
                .collect(Collectors.groupingBy(
                        m -> m.getThread().getMailAccount().getId() + ":" + m.getThread().getGmailThreadId(),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<UUID> representativeThreadIds = grouped.values().stream()
                .map(msgs -> msgs.stream()
                        .map(Message::getThread)
                        .filter(t -> t.getDirection() == Direction.INBOUND)
                        .findFirst()
                        .orElse(msgs.get(0).getThread()))
                .map(Thread::getId)
                .toList();

        Map<UUID, List<ThreadMessageLabelView>> labelsByThreadId =
                trashQueryService.findLabelsByThreadIds(representativeThreadIds);

        List<String> participantEmails = grouped.values().stream()
                .map(msgs -> resolveRepresentative(msgs).getLatestParticipantAddress())
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
        Map<String, String> contactNameByEmail = trashQueryService.findContactNamesByEmails(
                user.getId(),
                participantEmails
        );

        List<UUID> allMessageIds = messages.getContent().stream().map(Message::getId).toList();
        Map<UUID, List<Attachment>> attachmentsByMessageId = trashQueryService.findAttachmentsByMessageIds(allMessageIds);

        List<TrashThreadSummaryResponse> content = grouped.values().stream()
                .map(msgs -> {
                    Thread representative = resolveRepresentative(msgs);
                    List<ThreadMessageLabelView> labelViews =
                            labelsByThreadId.getOrDefault(representative.getId(), List.of());
                    List<Attachment> groupAttachments = msgs.stream()
                            .flatMap(m -> attachmentsByMessageId.getOrDefault(m.getId(), List.of()).stream())
                            .toList();
                    return TrashThreadSummaryResponse.from(representative, groupAttachments, msgs.size(), contactNameByEmail, labelViews);
                })
                .toList();

        UUID nextMarker = messages.hasNext() ? messages.getContent().getLast().getId() : null;
        return MarkerSliceResponse.of(content, nextMarker, messages.hasNext(), unreadCount, totalCount);
    }

    public TrashThreadDetailResponse getTrashThreadDetail(User user, UUID threadId) {
        Thread thread = trashQueryService.findThreadByIdIncludingDeleted(threadId);
        validateThreadAccess(user, thread);
        List<Message> deletedMessages = trashQueryService.findDeletedMessagesByMailAccountIdAndGmailThreadId(
                thread.getMailAccount().getId(), thread.getGmailThreadId());

        List<String> emails = collectEmailsFromMessages(deletedMessages);
        Map<String, String> contactNameByEmail = trashQueryService.findContactNamesByEmails(user.getId(), emails);

        List<UUID> messageIds = deletedMessages.stream().map(Message::getId).toList();
        Map<UUID, List<MessageLabelView>> messageLabelsByMessageId =
                trashQueryService.findMessageLabelsByMessageIds(messageIds);
        return TrashThreadDetailResponse.from(
                thread,
                deletedMessages,
                contactNameByEmail,
                messageLabelsByMessageId,
                renderBodyHtmlByMessageId(deletedMessages)
        );
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

    private Thread resolveRepresentative(List<Message> msgs) {
        return msgs.stream()
                .map(Message::getThread)
                .filter(t -> t.getDirection() == Direction.INBOUND)
                .findFirst()
                .orElse(msgs.get(0).getThread());
    }

    private List<String> collectEmailsFromMessages(List<Message> messages) {
        return messages.stream()
                .flatMap(m -> {
                    List<String> addrs = new ArrayList<>();
                    if (m.getFromAddress() != null) addrs.add(m.getFromAddress());
                    if (m.getToAddresses() != null) addrs.addAll(m.getToAddresses());
                    if (m.getCcAddresses() != null) addrs.addAll(m.getCcAddresses());
                    return addrs.stream();
                })
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .toList();
    }

    private Map<UUID, String> renderBodyHtmlByMessageId(List<Message> messages) {
        return messages.stream()
                .collect(Collectors.toMap(
                        Message::getId,
                        inlineImageService::renderInlineImageUrls
                ));
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
