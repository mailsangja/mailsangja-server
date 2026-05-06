package com.mailsangja.core.dto.inbox;

import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.AttachmentDisposition;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageResponseTest {

    @Test
    void from_저장된GmailName을우선하고없으면연락처이름을사용한다() {
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .subject("subject")
                .fromAddress("sender@example.com")
                .fromName("Gmail Sender")
                .replyToAddress("reply@example.com")
                .replyToName("Reply")
                .toAddresses(List.of("first@example.com", "second@example.com"))
                .toNames(Arrays.asList("First Gmail Name", null))
                .ccAddresses(List.of("cc@example.com"))
                .ccNames(Arrays.asList((String) null))
                .snippet("snippet")
                .read(true)
                .build();

        MessageResponse response = MessageResponse.from(
                message,
                Map.of(
                        "sender@example.com", "Contact Sender",
                        "first@example.com", "Contact First",
                        "second@example.com", "Contact Second",
                        "cc@example.com", "Contact Cc"
                ),
                List.of()
        );

        assertEquals("Gmail Sender", response.from().name());
        assertEquals("Reply", response.replyTo().name());
        assertEquals("reply@example.com", response.replyTo().email());
        assertEquals("First Gmail Name", response.to().getFirst().name());
        assertEquals("Contact Second", response.to().get(1).name());
        assertEquals("Contact Cc", response.cc().getFirst().name());
    }

    @Test
    void from_본문html의cid를첨부파일다운로드url로치환한다() {
        UUID inlineAttachmentId = UUID.randomUUID();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .subject("subject")
                .fromAddress("sender@example.com")
                .fromName("sender@example.com")
                .toAddresses(List.of("to@example.com"))
                .toNames(List.of("to@example.com"))
                .snippet("snippet")
                .read(true)
                .bodyHtml("<p>본문</p><img src=\"cid:inline-1\"><img src=\"cid:attachment-1\">")
                .attachments(List.of(
                        Attachment.builder()
                                .id(inlineAttachmentId)
                                .gmailAttachmentId("inline-gmail-attachment-id")
                                .filename("image.png")
                                .mimeType("image/png")
                                .contentId("inline-1")
                                .disposition(AttachmentDisposition.INLINE)
                                .size(5)
                                .build(),
                        Attachment.builder()
                                .id(UUID.randomUUID())
                                .gmailAttachmentId("normal-gmail-attachment-id")
                                .filename("file.txt")
                                .mimeType("text/plain")
                                .contentId("attachment-1")
                                .disposition(AttachmentDisposition.ATTACHMENT)
                                .size(5)
                                .build()
                ))
                .build();

        MessageResponse response = MessageResponse.from(
                message,
                "<p>본문</p><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + inlineAttachmentId + "\"><img src=\"cid:attachment-1\">",
                Map.of(),
                List.of()
        );

        assertEquals(
                "<p>본문</p><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + inlineAttachmentId + "\"><img src=\"cid:attachment-1\">",
                response.bodyHtml()
        );
    }

    @Test
    void from_렌더링된본문html을그대로응답한다() {
        UUID inlineAttachmentId = UUID.randomUUID();
        Message message = Message.builder()
                .id(UUID.randomUUID())
                .gmailMessageId("gmail-message-id")
                .direction(Direction.INBOUND)
                .subject("subject")
                .fromAddress("sender@example.com")
                .fromName("sender@example.com")
                .toAddresses(List.of("to@example.com"))
                .toNames(List.of("to@example.com"))
                .snippet("snippet")
                .read(true)
                .bodyHtml("<p>본문</p><img src=\"cid:abc\"><img src=\"cid:a\">")
                .attachments(List.of(
                        Attachment.builder()
                                .id(inlineAttachmentId)
                                .gmailAttachmentId("inline-gmail-attachment-id")
                                .filename("image.png")
                                .mimeType("image/png")
                                .contentId("a")
                                .disposition(AttachmentDisposition.INLINE)
                                .size(5)
                                .build()
                ))
                .build();

        MessageResponse response = MessageResponse.from(
                message,
                "<p>본문</p><img src=\"cid:abc\"><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + inlineAttachmentId + "\">",
                Map.of(),
                List.of()
        );

        assertEquals(
                "<p>본문</p><img src=\"cid:abc\"><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + inlineAttachmentId + "\">",
                response.bodyHtml()
        );
    }
}
