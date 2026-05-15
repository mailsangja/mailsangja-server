package com.mailsangja.worker.messaging.listener;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.worker.config.properties.LabelReclassifyRabbitProperties;
import com.mailsangja.worker.dto.label.LabelReclassifyMessage;
import com.mailsangja.worker.dto.label.MessageBatch;
import com.mailsangja.worker.handler.label.LabelRuleCompiler;
import com.mailsangja.worker.service.label.LabelQueryService;
import com.mailsangja.worker.service.label.LabelReclassifyJobStore;
import com.mailsangja.worker.service.label.MessageLabelCommandService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LabelReclassificationListenerTest {

    @Mock
    private LabelQueryService labelQueryService;

    @Mock
    private MessageLabelCommandService messageLabelCommandService;

    @Mock
    private LabelRuleCompiler labelRuleCompiler;

    @Mock
    private LabelReclassifyRabbitProperties labelReclassifyRabbitProperties;

    @Mock
    private LabelReclassifyJobStore labelReclassifyJobStore;

    @InjectMocks
    private LabelReclassificationListener listener;

    @Test
    void handle_allLabelsAreStale_skipsBatch() {
        UUID userId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        LabelReclassifyMessage message = new LabelReclassifyMessage(
                userId,
                Set.of(labelId),
                List.of(UUID.randomUUID()),
                "old-job"
        );
        when(labelReclassifyJobStore.getLatestJobId(labelId)).thenReturn("new-job");

        listener.handle(message);

        verify(labelQueryService, never()).findAllActiveByUserId(userId);
        verify(messageLabelCommandService, never()).applyLabelsWithLock(any(), any(), anyList(), any(), any());
    }

    @Test
    void handle_noMatchingActiveLabels_skipsBatch() {
        UUID userId = UUID.randomUUID();
        UUID targetLabelId = UUID.randomUUID();
        Label activeOtherLabel = label(UUID.randomUUID());
        LabelReclassifyMessage message = new LabelReclassifyMessage(
                userId,
                Set.of(targetLabelId),
                List.of(UUID.randomUUID()),
                "job-1"
        );
        when(labelReclassifyJobStore.getLatestJobId(targetLabelId)).thenReturn("job-1");
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of(activeOtherLabel));

        listener.handle(message);

        verify(labelQueryService, never()).findActiveMessagesWithLabelsByThreadIds(anyList());
        verify(messageLabelCommandService, never()).applyLabelsWithLock(any(), any(), anyList(), any(), any());
    }

    @Test
    void handle_noMessages_skipsCompileAndApply() {
        UUID userId = UUID.randomUUID();
        UUID targetLabelId = UUID.randomUUID();
        List<UUID> threadIds = List.of(UUID.randomUUID());
        LabelReclassifyMessage message = new LabelReclassifyMessage(userId, Set.of(targetLabelId), threadIds, "job-1");
        when(labelReclassifyJobStore.getLatestJobId(targetLabelId)).thenReturn("job-1");
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of(label(targetLabelId)));
        when(labelQueryService.findActiveMessagesWithLabelsByThreadIds(threadIds)).thenReturn(List.of());

        listener.handle(message);

        verify(labelRuleCompiler, never()).compile(anyList(), any(MessageBatch.class));
        verify(messageLabelCommandService, never()).applyLabelsWithLock(any(), any(), anyList(), any(), any());
    }

    @Test
    void handle_validBatch_compilesTargetLabelsAndAppliesPerGmailThread() {
        UUID userId = UUID.randomUUID();
        UUID targetLabelId = UUID.randomUUID();
        UUID staleLabelId = UUID.randomUUID();
        Label targetLabel = label(targetLabelId);
        Label staleLabel = label(staleLabelId);
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .emailAddress("user@example.com")
                .build();
        Message firstThreadMessage1 = message(mailAccount, "gmail-thread-1");
        Message firstThreadMessage2 = message(mailAccount, "gmail-thread-1");
        Message secondThreadMessage = message(mailAccount, "gmail-thread-2");
        List<Message> messages = List.of(firstThreadMessage1, firstThreadMessage2, secondThreadMessage);
        List<UUID> threadIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        Set<UUID> messagesWithAttachments = Set.of(firstThreadMessage1.getId());
        Map<UUID, List<Label>> labelsByMessageId = Map.of(firstThreadMessage1.getId(), List.of(targetLabel));
        LabelReclassifyMessage message = new LabelReclassifyMessage(
                userId,
                Set.of(targetLabelId, staleLabelId),
                threadIds,
                "job-1"
        );
        when(labelReclassifyJobStore.getLatestJobId(targetLabelId)).thenReturn("job-1");
        when(labelReclassifyJobStore.getLatestJobId(staleLabelId)).thenReturn("job-2");
        when(labelQueryService.findAllActiveByUserId(userId)).thenReturn(List.of(targetLabel, staleLabel));
        when(labelQueryService.findActiveMessagesWithLabelsByThreadIds(threadIds)).thenReturn(messages);
        when(labelQueryService.findMessageIdsWithAttachmentsByMessages(messages)).thenReturn(messagesWithAttachments);
        when(labelRuleCompiler.compile(eq(List.of(targetLabel)), any(MessageBatch.class))).thenReturn(labelsByMessageId);

        listener.handle(message);

        ArgumentCaptor<MessageBatch> batchCaptor = ArgumentCaptor.forClass(MessageBatch.class);
        verify(labelRuleCompiler).compile(eq(List.of(targetLabel)), batchCaptor.capture());
        assertEquals(messages, batchCaptor.getValue().messages());
        assertEquals(messagesWithAttachments, batchCaptor.getValue().messageIdsWithAttachments());
        verify(messageLabelCommandService, times(2)).applyLabelsWithLock(
                eq(mailAccount),
                org.mockito.ArgumentMatchers.anyString(),
                anyList(),
                eq(labelsByMessageId),
                eq(Set.of(targetLabelId))
        );
    }

    private Label label(UUID id) {
        return Label.builder()
                .id(id)
                .name("label")
                .build();
    }

    private Message message(MailAccount mailAccount, String gmailThreadId) {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(gmailThreadId)
                .direction(Direction.INBOUND)
                .build();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }
}
