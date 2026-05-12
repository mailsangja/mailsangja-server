package com.mailsangja.core.service.mail;

import com.mailsangja.core.config.properties.InboxProperties;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.AttachmentDisposition;
import com.mailsangja.db.entity.mail.Direction;
import com.mailsangja.db.entity.mail.Message;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InlineImageServiceTest {

    @Test
    void renderInlineImageUrls_인라인이미지cid를전체uri로치환한다() {
        UUID attachmentId = UUID.randomUUID();
        InlineImageService service = createService("https://test-api.mailsangja.com/");
        Message message = createMessage(
                "<p>본문</p><img src=\"cid:inline-1\"><img src=\"cid:normal-1\">",
                List.of(
                        createAttachment(attachmentId, "inline-1", AttachmentDisposition.INLINE),
                        createAttachment(UUID.randomUUID(), "normal-1", AttachmentDisposition.ATTACHMENT)
                )
        );

        String renderedBodyHtml = service.renderInlineImageUrls(message);

        assertEquals(
                "<p>본문</p><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + attachmentId + "\"><img src=\"cid:normal-1\">",
                renderedBodyHtml
        );
    }

    @Test
    void renderInlineImageUrls_cid가다른cid의접두사여도부분치환하지않는다() {
        UUID attachmentId = UUID.randomUUID();
        InlineImageService service = createService("https://test-api.mailsangja.com");
        Message message = createMessage(
                "<p>본문</p><img src=\"cid:abc\"><img src=\"cid:a\">",
                List.of(createAttachment(attachmentId, "a", AttachmentDisposition.INLINE))
        );

        String renderedBodyHtml = service.renderInlineImageUrls(message);

        assertEquals(
                "<p>본문</p><img src=\"cid:abc\"><img src=\"https://test-api.mailsangja.com/api/v1/mail/attachments/" + attachmentId + "\">",
                renderedBodyHtml
        );
    }

    private InlineImageService createService(String apiBaseUri) {
        InboxProperties inboxProperties = new InboxProperties();
        inboxProperties.setApiBaseUri(apiBaseUri);
        return new InlineImageService(inboxProperties);
    }

    private Message createMessage(String bodyHtml, List<Attachment> attachments) {
        return Message.builder()
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
                .bodyHtml(bodyHtml)
                .attachments(attachments)
                .build();
    }

    private Attachment createAttachment(UUID id, String contentId, AttachmentDisposition disposition) {
        return Attachment.builder()
                .id(id)
                .gmailAttachmentId("gmail-attachment-id")
                .filename("image.png")
                .mimeType("image/png")
                .contentId(contentId)
                .disposition(disposition)
                .size(5)
                .build();
    }
}
