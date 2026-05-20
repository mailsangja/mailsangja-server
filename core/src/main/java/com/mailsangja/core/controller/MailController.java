package com.mailsangja.core.controller;

import com.mailsangja.core.common.auth.AuthUser;
import com.mailsangja.core.controller.docs.MailControllerDocs;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.dto.mail.MailDraftStreamRequest;
import com.mailsangja.core.dto.mail.MailReviewRequest;
import com.mailsangja.core.dto.mail.MailReviewResponse;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionListResponse;
import com.mailsangja.core.dto.mail.ReplyDraftSuggestionResponse;
import com.mailsangja.core.facade.MailDraftFacade;
import com.mailsangja.core.facade.MailFacade;
import com.mailsangja.core.facade.MailReviewFacade;
import com.mailsangja.core.facade.ReplyDraftSuggestionFacade;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MailController implements MailControllerDocs {

    private final MailFacade mailFacade;
    private final MailDraftFacade mailDraftFacade;
    private final MailReviewFacade mailReviewFacade;
    private final ReplyDraftSuggestionFacade replyDraftSuggestionFacade;

    @Override
    @PostMapping(value = "/api/v1/mail/send", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> sendMail(
            @AuthUser User user,
            @RequestParam(required = false) UUID messageId,
            @ModelAttribute MailSendRequest request
    ) {
        if (messageId == null) {
            mailFacade.sendMail(user, request);
        } else {
            mailFacade.replyMail(user, messageId, request);
        }
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping(value = "/api/v1/mail/drafts/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> streamDraft(
            @AuthUser User user,
            @RequestBody MailDraftStreamRequest request
    ) {
        SseEmitter emitter = mailDraftFacade.streamDraft(user, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(emitter);
    }

    @Override
    @PostMapping("/api/v1/mail/reviews")
    public ResponseEntity<MailReviewResponse> reviewMail(
            @AuthUser User user,
            @RequestBody MailReviewRequest request
    ) {
        return ResponseEntity.ok(mailReviewFacade.review(user, request));
    }

    @Override
    @GetMapping("/api/v1/mail/messages/{messageId}/reply-draft-suggestions")
    public ResponseEntity<ReplyDraftSuggestionListResponse> getReplyDraftSuggestions(
            @AuthUser User user,
            @PathVariable UUID messageId
    ) {
        return ResponseEntity.ok(replyDraftSuggestionFacade.findByMessageId(user, messageId));
    }

    @Override
    @PostMapping("/api/v1/mail/reply-draft-suggestions/{suggestionId}/select")
    public ResponseEntity<ReplyDraftSuggestionResponse> selectReplyDraftSuggestion(
            @AuthUser User user,
            @PathVariable UUID suggestionId
    ) {
        return ResponseEntity.ok(replyDraftSuggestionFacade.select(user, suggestionId));
    }

    @Override
    @GetMapping("/api/v1/mail/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@AuthUser User user, @PathVariable UUID attachmentId) {
        MailAttachmentDownloadResult result = mailFacade.getAttachment(user, attachmentId);
        return ResponseEntity.ok()
                .contentType(resolveMediaType(result.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(result.filename(), StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .contentLength(result.bytes().length)
                .body(result.bytes());
    }

    private MediaType resolveMediaType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(mimeType);
        } catch (InvalidMediaTypeException e) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
