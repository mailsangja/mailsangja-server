package com.mailsangja.core.service.inbox;

import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.inbox.ThreadDetailResult;
import com.mailsangja.core.dto.inbox.ThreadListResult;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("InboxQueryService 테스트")
class InboxQueryServiceTest {

    @Mock
    private ThreadRepositoryPort threadRepositoryPort;

    @Mock
    private MessageRepositoryPort messageRepositoryPort;

    @Mock
    private ContactRepositoryPort contactRepositoryPort;

    @InjectMocks
    private InboxQueryService inboxQueryService;

    @Nested
    @DisplayName("스레드 조회")
    class FindThreadById {

        @Test
        @DisplayName("스레드가 없으면 예외를 반환한다")
        void findThreadById_스레드가없으면예외를반환한다() {
            // given
            UUID threadId = UUID.randomUUID();
            given(threadRepositoryPort.findByIdIncludingDeleted(threadId)).willReturn(Optional.empty());

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> inboxQueryService.findThreadById(threadId)
            );

            // then
            assertEquals("MS-INBOX-THREAD-NOT-FOUND", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("메시지 조회")
    class FindActiveMessageById {

        @Test
        @DisplayName("삭제된 메시지면 예외를 반환한다")
        void findActiveMessageById_삭제된메시지면예외를반환한다() {
            // given
            UUID messageId = UUID.randomUUID();
            Message deletedMessage = createMessage(createThread(), "message-1");
            deletedMessage.delete();
            given(messageRepositoryPort.findByIdIncludingDeleted(messageId)).willReturn(Optional.of(deletedMessage));

            // when
            InboxException exception = assertThrows(
                    InboxException.class,
                    () -> inboxQueryService.findActiveMessageById(messageId)
            );

            // then
            assertEquals("MS-INBOX-MESSAGE-NOT-FOUND", exception.getErrorCode().getCode());
        }
    }

    @Nested
    @DisplayName("스레드 목록 조회")
    class FindInboxThreadsResult {

        @Test
        @DisplayName("첨부파일과 연락처 이름을 함께 조립한다")
        void findInboxThreadsResult_첨부파일과연락처이름을함께조립한다() {
            // given
            UUID userId = UUID.randomUUID();
            Thread thread = createThread();
            Attachment attachment = Attachment.builder()
                    .id(UUID.randomUUID())
                    .filename("guide.pdf")
                    .mimeType("application/pdf")
                    .size(12)
                    .build();
            Message messageWithAttachment = createMessage(thread, "message-1");
            messageWithAttachment.replaceAttachments(List.of(attachment));
            given(threadRepositoryPort.findInboxByUserIdAndDeletedAtIsNull(userId, null, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(thread), PageRequest.of(0, 20), false));
            given(messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(List.of(thread.getId())))
                    .willReturn(List.of(messageWithAttachment));
            given(contactRepositoryPort.findAllByEmailInAndDeletedAtIsNull(List.of("sender@example.com")))
                    .willReturn(List.of(Contact.builder().id(UUID.randomUUID()).email("sender@example.com").name("보낸 사람").build()));

            // when
            ThreadListResult result = inboxQueryService.findInboxThreadsResult(userId, null, PageRequest.of(0, 20));

            // then
            assertEquals(1, result.threads().getContent().size());
            assertEquals(1, result.attachmentsByThreadId().get(thread.getId()).size());
            assertEquals("보낸 사람", result.contactNameByEmail().get("sender@example.com"));
        }

        @Test
        @DisplayName("참여자 이메일이 비어 있으면 연락처 조회를 생략한다")
        void findInboxThreadsResult_참여자이메일이비어있으면연락처조회를생략한다() {
            // given
            UUID userId = UUID.randomUUID();
            Thread thread = Thread.builder()
                    .id(UUID.randomUUID())
                    .mailAccount(createMailAccount())
                    .gmailThreadId("thread-1")
                    .direction(Direction.INBOUND)
                    .latestParticipantAddress(" ")
                    .build();
            given(threadRepositoryPort.findInboxByUserIdAndDeletedAtIsNull(userId, null, PageRequest.of(0, 20)))
                    .willReturn(new SliceImpl<>(List.of(thread), PageRequest.of(0, 20), false));
            given(messageRepositoryPort.findAllByThreadIdInAndDeletedAtIsNull(List.of(thread.getId())))
                    .willReturn(List.of());

            // when
            ThreadListResult result = inboxQueryService.findInboxThreadsResult(userId, null, PageRequest.of(0, 20));

            // then
            assertEquals(Map.of(), result.contactNameByEmail());
        }
    }

    @Nested
    @DisplayName("스레드 상세 조회")
    class FindThreadDetailResult {

        @Test
        @DisplayName("메시지의 발신자와 수신자 연락처 이름을 매핑한다")
        void findThreadDetailResult_메시지의발신자와수신자연락처이름을매핑한다() {
            // given
            Thread thread = createThread();
            Message message = Message.builder()
                    .id(UUID.randomUUID())
                    .thread(thread)
                    .gmailMessageId("message-1")
                    .direction(Direction.INBOUND)
                    .fromAddress("sender@example.com")
                    .toAddresses(List.of("to@example.com"))
                    .ccAddresses(List.of("cc@example.com"))
                    .read(false)
                    .build();
            given(messageRepositoryPort.findAllByMailAccountIdAndGmailThreadIdAndDeletedAtIsNull(
                    thread.getMailAccount().getId(),
                    thread.getGmailThreadId()
            )).willReturn(List.of(message));
            given(contactRepositoryPort.findAllByEmailInAndDeletedAtIsNull(
                    List.of("sender@example.com", "to@example.com", "cc@example.com")
            )).willReturn(List.of(
                    Contact.builder().id(UUID.randomUUID()).email("sender@example.com").name("발신자").build(),
                    Contact.builder().id(UUID.randomUUID()).email("to@example.com").name("수신자").build()
            ));

            // when
            ThreadDetailResult result = inboxQueryService.findThreadDetailResult(thread);

            // then
            assertEquals(1, result.messages().size());
            assertEquals("발신자", result.contactNameByEmail().get("sender@example.com"));
            assertEquals("수신자", result.contactNameByEmail().get("to@example.com"));
            assertEquals(null, result.contactNameByEmail().get("cc@example.com"));
        }
    }

    @Nested
    @DisplayName("안읽은 수 조회")
    class CountUnreadInbox {

        @Test
        @DisplayName("저장소의 안읽은 개수를 반환한다")
        void countUnreadInbox_저장소의안읽은개수를반환한다() {
            // given
            UUID userId = UUID.randomUUID();
            given(threadRepositoryPort.countUnreadInboxByUserId(userId)).willReturn(7L);

            // when
            long count = inboxQueryService.countUnreadInbox(userId);

            // then
            assertEquals(7L, count);
        }
    }

    private Thread createThread() {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(createMailAccount())
                .gmailThreadId("thread-1")
                .direction(Direction.INBOUND)
                .latestParticipantAddress("sender@example.com")
                .lastMessageAt(LocalDateTime.of(2026, 4, 20, 12, 0))
                .build();
    }

    private Message createMessage(Thread thread, String gmailMessageId) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId(gmailMessageId)
                .direction(Direction.INBOUND)
                .fromAddress("sender@example.com")
                .read(false)
                .build();
    }

    private MailAccount createMailAccount() {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .provider(MailProvider.GMAIL)
                .emailAddress("user@gmail.com")
                .alias("업무 메일")
                .icon("mail")
                .color("#123ABC")
                .accessToken("access-token")
                .build();
    }
}
