package com.mailsangja.core.service.trash;

import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrashQueryService 테스트")
class TrashQueryServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @InjectMocks
    private TrashQueryService trashQueryService;

    @Nested
    @DisplayName("삭제된 스레드 조회")
    class FindDeletedThreadById {

        @Test
        @DisplayName("삭제되지 않은 스레드면 예외를 반환한다")
        void findDeletedThreadById_삭제되지않은스레드면예외를반환한다() {
            // given
            UUID threadId = UUID.randomUUID();
            given(threadRepositoryPort.findByIdIncludingDeleted(threadId)).willReturn(Optional.of(createThread()));

            // when
            TrashException exception = assertThrows(
                    TrashException.class,
                    () -> trashQueryService.findDeletedThreadById(threadId)
            );

            // then
            assertEquals("MS-TRASH-THREAD-NOT-DELETED", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("활성 메시지 조회")
    class FindActiveMessageById {

        @Test
        @DisplayName("삭제된 메시지면 예외를 반환한다")
        void findActiveMessageById_삭제된메시지면예외를반환한다() {
            // given
            UUID messageId = UUID.randomUUID();
            Message message = createMessage();
            message.delete();
            given(messageRepositoryPort.findByIdIncludingDeleted(messageId)).willReturn(Optional.of(message));

            // when
            TrashException exception = assertThrows(
                    TrashException.class,
                    () -> trashQueryService.findActiveMessageById(messageId)
            );

            // then
            assertEquals("MS-TRASH-MESSAGE-NOT-FOUND", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("삭제된 메시지 조회")
    class FindDeletedMessageById {

        @Test
        @DisplayName("삭제된 메시지면 그대로 반환한다")
        void findDeletedMessageById_삭제된메시지면그대로반환한다() {
            // given
            UUID messageId = UUID.randomUUID();
            Message message = createMessage();
            message.delete();
            given(messageRepositoryPort.findByIdIncludingDeleted(messageId)).willReturn(Optional.of(message));

            // when
            Message result = trashQueryService.findDeletedMessageById(messageId);

            // then
            assertSame(message, result);
        }

        @Test
        @DisplayName("삭제되지 않은 메시지면 예외를 반환한다")
        void findDeletedMessageById_삭제되지않은메시지면예외를반환한다() {
            // given
            UUID messageId = UUID.randomUUID();
            given(messageRepositoryPort.findByIdIncludingDeleted(messageId)).willReturn(Optional.of(createMessage()));

            // when
            TrashException exception = assertThrows(
                    TrashException.class,
                    () -> trashQueryService.findDeletedMessageById(messageId)
            );

            // then
            assertEquals("MS-TRASH-MESSAGE-NOT-DELETED", exception.getErrorCode().getCode());
        }
    }

    private Thread createThread() {
        return Thread.builder()
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
    }

    private Message createMessage() {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(createThread())
                .gmailMessageId("message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }
}
