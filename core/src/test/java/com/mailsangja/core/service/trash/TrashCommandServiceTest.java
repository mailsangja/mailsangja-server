package com.mailsangja.core.service.trash;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.GmailThreadLockRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrashCommandService 테스트")
class TrashCommandServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private GmailThreadLockRepositoryPort gmailThreadLockRepositoryPort;

    @InjectMocks
    private TrashCommandService trashCommandService;

    @Nested
    @DisplayName("메시지 삭제")
    class SoftDeleteMessage {

        @Test
        @DisplayName("활성 메시지가 남지 않으면 스레드도 삭제한다")
        void softDeleteMessage_활성메시지가남지않으면스레드도삭제한다() {
            // given
            Message message = createMessage();
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                    message.getThread().getMailAccount().getId(),
                    message.getThread().getGmailThreadId()
            )).willReturn(List.of());

            // when
            trashCommandService.softDeleteMessage(message);

            // then
            then(gmailThreadLockRepositoryPort).should()
                    .acquireThreadLock(message.getThread().getMailAccount(), message.getThread().getGmailThreadId());
            then(messageRepositoryPort).should().save(message);
            then(threadRepositoryPort).should().bulkSoftDeleteByMailAccountIdAndGmailThreadId(
                    any(UUID.class),
                    any(String.class),
                    any()
            );
        }

        @Test
        @DisplayName("활성 메시지가 남아 있으면 스레드는 삭제하지 않는다")
        void softDeleteMessage_활성메시지가남아있으면스레드는삭제하지않는다() {
            // given
            Message message = createMessage();
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                    message.getThread().getMailAccount().getId(),
                    message.getThread().getGmailThreadId()
            )).willReturn(List.of(createMessage()));

            // when
            trashCommandService.softDeleteMessage(message);

            // then
            then(messageRepositoryPort).should().save(message);
            then(threadRepositoryPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("메시지 복원")
    class RestoreMessage {

        @Test
        @DisplayName("메시지를 복원하면 스레드도 함께 복원한다")
        void restoreMessage_메시지를복원하면스레드도함께복원한다() {
            // given
            Message message = createMessage();
            message.delete();

            // when
            trashCommandService.restoreMessage(message);

            // then
            then(gmailThreadLockRepositoryPort).should()
                    .acquireThreadLock(message.getThread().getMailAccount(), message.getThread().getGmailThreadId());
            then(messageRepositoryPort).should().save(message);
            then(threadRepositoryPort).should().bulkRestoreByMailAccountIdAndGmailThreadId(
                    message.getThread().getMailAccount().getId(),
                    message.getThread().getGmailThreadId()
            );
        }
    }

    private Message createMessage() {
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(MailAccount.builder()
                        .id(UUID.randomUUID())
                        .provider(MailProvider.GMAIL)
                        .emailAddress("user@gmail.com")
                        .alias("업무 메일")
                        .icon("mail")
                        .color("#123ABC")
                        .accessToken("access-token")
                        .build())
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .build();

        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }
}
