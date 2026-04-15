package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class MailFacade {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final long MAX_ATTACHMENT_COUNT = 10;
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    private final MailCommandService mailCommandService;

    public void sendMail(User user, MailSendRequest request) {
        validateSender(request.from());
        validateRecipients(request.to(), request.cc(), request.bcc());
        validateSubjectAndContent(request.subject(), request.content());
        validateAttachments(request.attachments());

        MailSendCommand command = MailSendCommand.from(user, request);
        var persistCommand = mailCommandService.sendMail(command);
        mailCommandService.saveSentMail(persistCommand);
    }

    private void validateSender(String from) {
        if (!isValidEmail(from)) {
            throw new MailSendException(MailSendErrorCode.INVALID_SENDER_ADDRESS);
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
            if (!isValidEmail(recipient)) {
                throw new MailSendException(MailSendErrorCode.INVALID_RECIPIENT_ADDRESS);
            }

            String normalizedRecipient = recipient.trim().toLowerCase();
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

    private boolean isValidEmail(String email) {
        return !isBlank(email) && EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
