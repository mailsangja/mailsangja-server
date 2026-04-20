package com.mailsangja.worker.service.mail;

import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.MessageRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GmailHistoryStateQueryService 테스트")
class GmailHistoryStateQueryServiceTest {

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Nested
    @DisplayName("existsMessage")
    class ExistsMessage {

        @Test
        @DisplayName("유효한 식별자면 메시지 존재 여부를 반환한다")
        void existsMessage_유효한식별자면메시지존재여부를반환한다() {
            // given
            GmailHistoryStateQueryService service = new GmailHistoryStateQueryService(messageRepositoryPort);
            Message message = createMessage();
            UUID mailAccountId = message.getThread().getMailAccount().getId();
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccountId, "thread-1", "message-1"
            )).willReturn(Optional.of(message));

            // when
            boolean result = service.existsMessage(mailAccountId, "thread-1", "message-1");

            // then
            assertTrue(result);
        }

        @Test
        @DisplayName("식별자가 비어 있으면 false를 반환하고 repository를 조회하지 않는다")
        void existsMessage_식별자가비어있으면False를반환하고Repository를조회하지않는다() {
            // given
            GmailHistoryStateQueryService service = new GmailHistoryStateQueryService(messageRepositoryPort);

            // when
            boolean result = service.existsMessage(null, "thread-1", "message-1");

            // then
            assertFalse(result);
            then(messageRepositoryPort).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("findMessage")
    class FindMessage {

        @Test
        @DisplayName("유효한 식별자면 repository 조회 결과를 반환한다")
        void findMessage_유효한식별자면Repository조회결과를반환한다() {
            // given
            GmailHistoryStateQueryService service = new GmailHistoryStateQueryService(messageRepositoryPort);
            Message message = createMessage();
            UUID mailAccountId = message.getThread().getMailAccount().getId();
            given(messageRepositoryPort.findByMailAccountIdAndGmailThreadIdAndGmailMessageIdAndDeletedAtIsNull(
                    mailAccountId, "thread-1", "message-1"
            )).willReturn(Optional.of(message));

            // when
            Optional<Message> result = service.findMessage(mailAccountId, "thread-1", "message-1");

            // then
            assertTrue(result.isPresent());
        }
    }

    private Message createMessage() {
        MailAccount mailAccount = MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .alias("alias")
                .icon("icon")
                .color("#4285F4")
                .accessToken("access-token")
                .accessTokenExpiresAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .refreshToken("refresh-token")
                .active(true)
                .build();
        Thread thread = Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(mailAccount)
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .read(true)
                .messageCount(1)
                .build();
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("message-1")
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .toAddresses(List.of("user@gmail.com"))
                .toNames(List.of("User"))
                .ccAddresses(List.of())
                .ccNames(List.of())
                .read(true)
                .build();
    }
}
