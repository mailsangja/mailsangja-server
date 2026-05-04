package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Schema(description = "메일 전송 요청")
public record MailSendRequest(
        @Schema(description = "보내는 사람. `user@example.com` 또는 `\"이름\" <user@example.com>` 형식", example = "\"홍길동\" <sender@gmail.com>")
        String from,

        @Schema(description = "답장 받을 주소. `user@example.com` 또는 `\"이름\" <user@example.com>` 형식", example = "\"홍길동\" <reply@gmail.com>")
        String replyTo,

        @Schema(description = "수신자 목록. multipart/form-data 에서는 to 필드를 반복 전달합니다.")
        List<String> to,

        @Schema(description = "참조 수신자 목록. multipart/form-data 에서는 cc 필드를 반복 전달합니다.")
        List<String> cc,

        @Schema(description = "숨은 참조 수신자 목록. multipart/form-data 에서는 bcc 필드를 반복 전달합니다.")
        List<String> bcc,

        @Schema(description = "메일 제목", example = "회의 자료 전달드립니다.")
        String subject,

        @Schema(
                description = "메일 본문 HTML. 본문 이미지는 `<img src=\"cid:{cid}\">`로 참조합니다.",
                example = "<p>안녕하세요.</p><p>회의 자료 전달드립니다.</p><img src=\"cid:inline-1\" alt=\"회의 자료 이미지\">"
        )
        String content,

        @Schema(description = "첨부파일 목록. multipart/form-data 에서는 attachments 필드를 반복 전달합니다.")
        List<MultipartFile> attachments,

        @Schema(description = "본문 인라인 이미지 목록. multipart/form-data 에서는 inlineImages 필드를 반복 전달합니다.")
        List<MultipartFile> inlineImages,

        @Schema(description = "본문 인라인 이미지 CID 목록. inlineImages와 같은 순서로 inlineImageCids 필드를 반복 전달합니다.")
        List<String> inlineImageCids
) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final Pattern CID_PATTERN = Pattern.compile("^[A-Za-z0-9._-]{1,128}$");
    private static final Pattern CID_REFERENCE_PATTERN = Pattern.compile("cid:([A-Za-z0-9._-]{1,128})");
    private static final long MAX_ATTACHMENT_COUNT = 10;
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    public MailSendRequest(
            String from,
            String replyTo,
            List<String> to,
            List<String> cc,
            List<String> bcc,
            String subject,
            String content,
            List<MultipartFile> attachments
    ) {
        this(from, replyTo, to, cc, bcc, subject, content, attachments, List.of(), List.of());
    }

    public static void validate(MailSendRequest request) {
        if (request == null) {
            throw new MailSendException(MailSendErrorCode.INVALID_MAIL_REQUEST);
        }
        request.validate();
    }

    public void validate() {
        validateSender(from);
        validateReplyTo(replyTo);
        validateRecipients(to, cc, bcc);
        validateSubject(subject);
        validateSubjectAndContent(subject, content);
        validateAttachments(attachments);
        validateInlineImages(content, inlineImages, inlineImageCids);
    }

    private void validateSender(String from) {
        MailAddressCommand parsedSender = parseMailAddress(from, MailSendErrorCode.INVALID_SENDER_ADDRESS);
        if (!isValidEmail(parsedSender.address())) {
            throw new MailSendException(MailSendErrorCode.INVALID_SENDER_ADDRESS);
        }
    }

    private void validateReplyTo(String replyTo) {
        if (isBlank(replyTo)) {
            return;
        }

        MailAddressCommand parsedReplyTo = parseMailAddress(replyTo, MailSendErrorCode.INVALID_REPLY_TO_ADDRESS);
        if (!isValidEmail(parsedReplyTo.address())) {
            throw new MailSendException(MailSendErrorCode.INVALID_REPLY_TO_ADDRESS);
        }
    }

    private void validateRecipients(List<String> to, List<String> cc, List<String> bcc) {
        if (to == null || to.isEmpty()) {
            throw new MailSendException(MailSendErrorCode.EMPTY_RECIPIENT);
        }

        Set<String> normalizedRecipients = new HashSet<>();
        validateRecipientList(to, normalizedRecipients);
        validateRecipientList(cc, normalizedRecipients);
        validateRecipientList(bcc, normalizedRecipients);
    }

    private void validateRecipientList(List<String> recipients, Set<String> normalizedRecipients) {
        if (recipients == null) {
            return;
        }

        for (String recipient : recipients) {
            MailAddressCommand parsedRecipient = parseMailAddress(recipient, MailSendErrorCode.INVALID_RECIPIENT_ADDRESS);
            if (!isValidEmail(parsedRecipient.address())) {
                throw new MailSendException(MailSendErrorCode.INVALID_RECIPIENT_ADDRESS);
            }

            String normalizedRecipient = parsedRecipient.address().trim().toLowerCase();
            if (!normalizedRecipients.add(normalizedRecipient)) {
                throw new MailSendException(MailSendErrorCode.DUPLICATE_RECIPIENT_ADDRESS);
            }
        }
    }

    private void validateSubjectAndContent(String subject, String content) {
        if (isBlank(subject) && isBlank(content)) {
            throw new MailSendException(MailSendErrorCode.EMPTY_SUBJECT_AND_CONTENT);
        }
    }

    private void validateSubject(String subject) {
        if (isBlank(subject)) {
            return;
        }

        if (subject.contains("\r") || subject.contains("\n")) {
            throw new MailSendException(MailSendErrorCode.INVALID_MAIL_SUBJECT);
        }
    }

    private void validateAttachments(List<MultipartFile> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }

        if (attachments.size() > MAX_ATTACHMENT_COUNT) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_COUNT_EXCEEDED);
        }

        long totalAttachmentSize = 0L;
        for (MultipartFile attachment : attachments) {
            if (attachment == null || attachment.isEmpty() || attachment.getSize() <= 0) {
                throw new MailSendException(MailSendErrorCode.EMPTY_ATTACHMENT_FILE);
            }

            if (isBlank(attachment.getOriginalFilename())) {
                throw new MailSendException(MailSendErrorCode.INVALID_ATTACHMENT_FILENAME);
            }

            if (attachment.getSize() > MAX_ATTACHMENT_SIZE) {
                throw new MailSendException(MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
            }

            totalAttachmentSize += attachment.getSize();
        }

        if (totalAttachmentSize > MAX_TOTAL_ATTACHMENT_SIZE) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
        }
    }

    private void validateInlineImages(String content, List<MultipartFile> inlineImages, List<String> inlineImageCids) {
        Set<String> referencedCids = extractReferencedCids(content);
        if ((inlineImages == null || inlineImages.isEmpty()) && (inlineImageCids == null || inlineImageCids.isEmpty())) {
            if (!referencedCids.isEmpty()) {
                throw new MailSendException(MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
            }
            return;
        }

        if (inlineImages == null
                || inlineImageCids == null
                || inlineImages.size() != inlineImageCids.size()) {
            throw new MailSendException(MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
        }

        Set<String> uploadedCids = new HashSet<>();
        long totalInlineImageSize = 0L;

        for (int i = 0; i < inlineImages.size(); i++) {
            MultipartFile inlineImage = inlineImages.get(i);
            String cid = inlineImageCids.get(i);

            validateInlineImageCid(cid, referencedCids, uploadedCids);
            validateAttachmentFile(inlineImage);

            if (isBlank(inlineImage.getContentType()) || !inlineImage.getContentType().startsWith("image/")) {
                throw new MailSendException(MailSendErrorCode.INVALID_INLINE_IMAGE_TYPE);
            }

            totalInlineImageSize += inlineImage.getSize();
        }

        if (totalInlineImageSize > MAX_TOTAL_ATTACHMENT_SIZE) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
        }

        if (!uploadedCids.containsAll(referencedCids)) {
            throw new MailSendException(MailSendErrorCode.INLINE_IMAGE_COUNT_MISMATCH);
        }
    }

    private Set<String> extractReferencedCids(String content) {
        Set<String> referencedCids = new HashSet<>();
        if (isBlank(content)) {
            return referencedCids;
        }

        java.util.regex.Matcher matcher = CID_REFERENCE_PATTERN.matcher(content);
        while (matcher.find()) {
            referencedCids.add(matcher.group(1));
        }
        return referencedCids;
    }

    private void validateInlineImageCid(String cid, Set<String> referencedCids, Set<String> uploadedCids) {
        if (isBlank(cid) || !CID_PATTERN.matcher(cid).matches()) {
            throw new MailSendException(MailSendErrorCode.INVALID_INLINE_IMAGE_CID);
        }

        if (!uploadedCids.add(cid)) {
            throw new MailSendException(MailSendErrorCode.DUPLICATE_INLINE_IMAGE_CID);
        }

        if (!referencedCids.contains(cid)) {
            throw new MailSendException(MailSendErrorCode.INLINE_IMAGE_CID_NOT_FOUND);
        }
    }

    private void validateAttachmentFile(MultipartFile attachment) {
        if (attachment == null || attachment.isEmpty() || attachment.getSize() <= 0) {
            throw new MailSendException(MailSendErrorCode.EMPTY_ATTACHMENT_FILE);
        }

        if (isBlank(attachment.getOriginalFilename())) {
            throw new MailSendException(MailSendErrorCode.INVALID_ATTACHMENT_FILENAME);
        }

        if (attachment.getSize() > MAX_ATTACHMENT_SIZE) {
            throw new MailSendException(MailSendErrorCode.ATTACHMENT_SIZE_EXCEEDED);
        }
    }

    private MailAddressCommand parseMailAddress(String rawValue, MailSendErrorCode errorCode) {
        try {
            MailAddressCommand command = MailAddressCommand.fromRaw(rawValue);
            validateDisplayName(command.name(), errorCode);
            return command;
        } catch (IllegalArgumentException e) {
            throw new MailSendException(errorCode);
        }
    }

    private boolean isValidEmail(String email) {
        return !isBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private void validateDisplayName(String name, MailSendErrorCode errorCode) {
        if (isBlank(name)) {
            return;
        }

        if (name.contains("\r") || name.contains("\n")) {
            throw new MailSendException(errorCode);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
