package com.mailsangja.worker.service.label;

import com.mailsangja.db.entity.label.Label;
import com.mailsangja.db.entity.label.MessageLabel;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageLabelCommandServiceTest {

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @InjectMocks
    private MessageLabelCommandService messageLabelCommandService;

    @Test
    void applyLabels_targetLabelsChanged_replacesOnlyTargetLabelsAndPreservesOthers() {
        Label targetOld = label();
        Label targetNew = label();
        Label untouched = label();
        Message message = message();
        message.replaceMessageLabels(List.of(
                MessageLabel.of(message, targetOld),
                MessageLabel.of(message, untouched)
        ));

        messageLabelCommandService.applyLabels(
                List.of(message),
                Map.of(message.getId(), List.of(targetNew)),
                Set.of(targetOld.getId(), targetNew.getId())
        );

        assertEquals(Set.of(targetNew.getId(), untouched.getId()), labelIds(message));
        ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
        verify(messageRepositoryPort).saveAll(captor.capture());
        assertEquals(List.of(message), captor.getValue());
    }

    @Test
    void applyLabels_whenTargetLabelsAlreadySame_doesNotSave() {
        Label target = label();
        Label untouched = label();
        Message message = message();
        message.replaceMessageLabels(List.of(
                MessageLabel.of(message, target),
                MessageLabel.of(message, untouched)
        ));

        messageLabelCommandService.applyLabels(
                List.of(message),
                Map.of(message.getId(), List.of(target)),
                Set.of(target.getId())
        );

        verify(messageRepositoryPort, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
        assertEquals(Set.of(target.getId(), untouched.getId()), labelIds(message));
    }

    @Test
    void applyLabels_emptyTargetLabelIds_returnsWithoutSave() {
        Message message = message();

        messageLabelCommandService.applyLabels(List.of(message), Map.of(), Set.of());

        verify(messageRepositoryPort, never()).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void applyLabelsWithLock_acquiresThreadLockBeforeApplying() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .emailAddress("user@example.com")
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("gmail-thread-1")
                .direction(Direction.INBOUND)
                .build();
        Message message = message(thread);
        Label label = label();

        messageLabelCommandService.applyLabelsWithLock(
                mailAccount,
                "gmail-thread-1",
                List.of(message),
                Map.of(message.getId(), List.of(label)),
                Set.of(label.getId())
        );

        verify(gmailThreadLockRepositoryPort).acquireThreadLock(mailAccount, "gmail-thread-1");
        verify(messageRepositoryPort).saveAll(List.of(message));
        assertEquals(Set.of(label.getId()), labelIds(message));
    }

    private Set<UUID> labelIds(Message message) {
        return message.getMessageLabels().stream()
                .map(messageLabel -> messageLabel.getLabel().getId())
                .collect(java.util.stream.Collectors.toSet());
    }

    private Label label() {
        return Label.builder()
                .id(UUID.randomUUID())
                .name("label")
                .build();
    }

    private Message message() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .emailAddress("user@example.com")
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId(UUID.randomUUID().toString())
                .direction(Direction.INBOUND)
                .build();
        return message(thread);
    }

    private Message message(Thread thread) {
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
