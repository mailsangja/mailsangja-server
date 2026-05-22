package com.mailsangja.core.service.trash;

import com.mailsangja.core.common.exception.trash.TrashException;
import com.mailsangja.db.dto.MessageLabelView;
import com.mailsangja.db.dto.ThreadMessageLabelView;
import com.mailsangja.db.entity.contact.Contact;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.mail.Message;
import com.mailsangja.db.entity.mail.Thread;
import com.mailsangja.db.entity.user.User;
import com.mailsangja.db.port.AttachmentRepositoryPort;
import com.mailsangja.db.port.ContactRepositoryPort;
import com.mailsangja.db.port.MessageRepositoryPort;
import com.mailsangja.db.port.ThreadRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrashQueryServiceTest {

    @Mock private ThreadRepositoryPort threadRepositoryPort;
    @Mock private MessageRepositoryPort messageRepositoryPort;
    @Mock private ContactRepositoryPort contactRepositoryPort;
    @Mock private AttachmentRepositoryPort attachmentRepositoryPort;

    @InjectMocks
    private TrashQueryService trashQueryService;

    @Test
    void 삭제_메시지_목록_조회는_라벨_ID에서_null과_중복을_제거해_전달한다() {
        // given
        UUID userId = UUID.randomUUID();
        UUID markerId = UUID.randomUUID();
        UUID labelId = UUID.randomUUID();
        Slice<Message> expected = new SliceImpl<>(List.of(), PageRequest.of(0, 30), false);
        when(messageRepositoryPort.findDeletedByUserIdAndFilters(
                userId, markerId, List.of(labelId), false, PageRequest.of(0, 30)))
                .thenReturn(expected);

        // when
        Slice<Message> result =
                trashQueryService.findDeletedMessagesByUserId(userId, markerId, 30, Arrays.asList(labelId, null, labelId), false);

        // then
        assertEquals(expected, result);
    }

    @Test
    void 삭제된_스레드가_아니면_삭제_스레드_조회에서_예외가_발생한다() {
        // given
        Thread thread = thread(mailAccount(user()), Direction.INBOUND);
        when(threadRepositoryPort.findByIdIncludingDeleted(thread.getId())).thenReturn(Optional.of(thread));

        // when & then
        assertThrows(TrashException.class, () -> trashQueryService.findDeletedThreadById(thread.getId()));
    }

    @Test
    void 삭제된_메시지가_아니면_삭제_메시지_조회에서_예외가_발생한다() {
        // given
        Message message = message(thread(mailAccount(user()), Direction.INBOUND));
        when(messageRepositoryPort.findByIdIncludingDeleted(message.getId())).thenReturn(Optional.of(message));

        // when & then
        assertThrows(TrashException.class, () -> trashQueryService.findDeletedMessageById(message.getId()));
    }

    @Test
    void 스레드_ID가_비어있으면_라벨을_조회하지_않고_빈_맵을_반환한다() {
        // given
        List<UUID> threadIds = List.of();

        // when
        Map<UUID, List<ThreadMessageLabelView>> result = trashQueryService.findLabelsByThreadIds(threadIds);

        // then
        assertEquals(Map.of(), result);
        verify(messageRepositoryPort, never()).findLabelsByThreadIdIn(anyList());
    }

    @Test
    void 메시지_ID가_있으면_메시지_라벨을_메시지별로_그룹핑한다() {
        // given
        UUID messageId = UUID.randomUUID();
        MessageLabelView label = new MessageLabelView(messageId, UUID.randomUUID(), "업무", "#00ff00");
        when(messageRepositoryPort.findMessageLabelsByMessageIds(List.of(messageId))).thenReturn(List.of(label));

        // when
        Map<UUID, List<MessageLabelView>> result = trashQueryService.findMessageLabelsByMessageIds(List.of(messageId));

        // then
        assertEquals(List.of(label), result.get(messageId));
    }

    @Test
    void 이메일이_비어있으면_연락처를_조회하지_않고_빈_맵을_반환한다() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        Map<String, String> result = trashQueryService.findContactNamesByEmails(userId, List.of());

        // then
        assertEquals(Map.of(), result);
        verify(contactRepositoryPort, never()).findAllByUserIdAndEmailInAndDeletedAtIsNull(org.mockito.ArgumentMatchers.any(), anyList());
    }

    @Test
    void 첨부파일을_메시지_ID별로_그룹핑한다() {
        // given
        Message message = message(thread(mailAccount(user()), Direction.INBOUND));
        Attachment attachment = Attachment.builder()
                .id(UUID.randomUUID())
                .message(message)
                .filename("file.pdf")
                .mimeType("application/pdf")
                .build();
        when(attachmentRepositoryPort.findAllByMessageIdIn(List.of(message.getId()))).thenReturn(List.of(attachment));

        // when
        Map<UUID, List<Attachment>> result = trashQueryService.findAttachmentsByMessageIds(List.of(message.getId()));

        // then
        assertEquals(List.of(attachment), result.get(message.getId()));
    }

    @Test
    void 연락처를_이메일별_이름_맵으로_변환한다() {
        // given
        User user = user();
        Contact contact = Contact.create(user, "홍길동", "hong@example.com");
        when(contactRepositoryPort.findAllByUserIdAndEmailInAndDeletedAtIsNull(
                user.getId(), List.of("hong@example.com")))
                .thenReturn(List.of(contact));

        // when
        Map<String, String> result =
                trashQueryService.findContactNamesByEmails(user.getId(), List.of("hong@example.com"));

        // then
        assertEquals("홍길동", result.get("hong@example.com"));
    }

    private User user() {
        return User.builder().id(UUID.randomUUID()).build();
    }

    private MailAccount mailAccount(User user) {
        return MailAccount.builder()
                .id(UUID.randomUUID())
                .user(user)
                .provider(MailProvider.GMAIL)
                .emailAddress("user@example.com")
                .alias("gmail")
                .icon("icon")
                .color("#000000")
                .accessToken("token")
                .build();
    }

    private Thread thread(MailAccount account, Direction direction) {
        return Thread.builder()
                .id(UUID.randomUUID())
                .mailAccount(account)
                .gmailThreadId("gmail-thread")
                .direction(direction)
                .build();
    }

    private Message message(Thread thread) {
        return Message.builder()
                .id(UUID.randomUUID())
                .thread(thread)
                .gmailMessageId("gmail-message-" + UUID.randomUUID())
                .direction(thread.getDirection())
                .fromAddress("from@example.com")
                .build();
    }
}
