package com.mailsangja.core.service.mail;

import com.mailsangja.core.config.properties.InboxProperties;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.AttachmentDisposition;
import com.mailsangja.db.entity.mail.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class InlineImageService {

    private final InboxProperties inboxProperties;

    public String renderInlineImageUrls(Message message) {
        String bodyHtml = message.getBodyHtml();
        if (bodyHtml == null || bodyHtml.isBlank() || message.getAttachments() == null || message.getAttachments().isEmpty()) {
            return bodyHtml;
        }

        String renderedBodyHtml = bodyHtml;
        for (Attachment attachment : message.getAttachments()) {
            if (isNotRenderableInlineImage(attachment)) {
                continue;
            }

            renderedBodyHtml = replaceInlineImageSrc(
                    renderedBodyHtml,
                    attachment.getContentId(),
                    buildAttachmentUri(attachment.getId())
            );
        }
        return renderedBodyHtml;
    }

    private boolean isNotRenderableInlineImage(Attachment attachment) {
        return attachment == null
                || attachment.getId() == null
                || attachment.getDisposition() != AttachmentDisposition.INLINE
                || attachment.getContentId() == null
                || attachment.getContentId().isBlank();
    }

    private String replaceInlineImageSrc(String bodyHtml, String contentId, String attachmentUri) {
        Pattern cidSrcPattern = Pattern.compile(
                "(\\bsrc\\s*=\\s*)([\"'])cid:" + Pattern.quote(contentId) + "\\2",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = cidSrcPattern.matcher(bodyHtml);
        return matcher.replaceAll("$1$2" + Matcher.quoteReplacement(attachmentUri) + "$2");
    }

    private String buildAttachmentUri(UUID attachmentId) {
        return normalizeBaseUri(inboxProperties.getApiBaseUri()) + "/api/v1/mail/attachments/" + attachmentId;
    }

    private String normalizeBaseUri(String baseUri) {
        if (baseUri == null || baseUri.isBlank()) {
            return "http://localhost:8080";
        }

        String normalizedBaseUri = baseUri.trim();
        while (normalizedBaseUri.endsWith("/")) {
            normalizedBaseUri = normalizedBaseUri.substring(0, normalizedBaseUri.length() - 1);
        }
        return normalizedBaseUri;
    }
}
