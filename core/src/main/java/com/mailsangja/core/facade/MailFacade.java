package com.mailsangja.core.facade;

import com.mailsangja.core.common.exception.inbox.InboxErrorCode;
import com.mailsangja.core.common.exception.inbox.InboxException;
import com.mailsangja.core.dto.mail.MailAttachmentDownloadResult;
import com.mailsangja.core.common.exception.mail.MailSendErrorCode;
import com.mailsangja.core.common.exception.mail.MailSendException;
import com.mailsangja.core.dto.mail.MailAddressCommand;
import com.mailsangja.core.dto.mail.MailSendCommand;
import com.mailsangja.core.dto.mail.MailSendRequest;
import com.mailsangja.core.service.google.GoogleMailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailAttachmentQueryService;
import com.mailsangja.core.service.mail.MailAccountQueryService;
import com.mailsangja.core.service.mail.MailCommandService;
import com.mailsangja.db.entity.mail.Attachment;
import com.mailsangja.db.entity.mail.MailAccount;
import com.mailsangja.db.entity.mail.MailProvider;
import com.mailsangja.db.entity.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MailFacade {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private static final long MAX_ATTACHMENT_COUNT = 10;
    private static final long MAX_ATTACHMENT_SIZE = 10L * 1024 * 1024;
    private static final long MAX_TOTAL_ATTACHMENT_SIZE = 20L * 1024 * 1024;

    private final MailAccountQueryService mailAccountQueryService;
    private final MailCommandService mailCommandService;
    private final MailAttachmentQueryService mailAttachmentQueryService;
    private final GoogleMailAttachmentQueryService googleMailAttachmentQueryService;

    public void sendMail(User user, MailSendRequest request, List<MultipartFile> attachments) {
        validateRequest(request);
        validateSender(request.from());
        validateRecipients(request.to(), request.cc(), request.bcc());
        validateSubject(request.subject());
        validateSubjectAndContent(request.subject(), request.content());
        validateAttachments(request.attachments());

        MailSendCommand command = MailSendCommand.from(user, request);
        var persistCommand = mailCommandService.sendMail(command);
        mailCommandService.saveSentMail(persistCommand);
    }

    public MailAttachmentDownloadResult getAttachment(User user, UUID attachmentId) {
        validateAttachmentId(attachmentId);

        Attachment attachment = mailAttachmentQueryService.findById(attachmentId);
        validateAttachmentAccess(mailAccountQueryService.findAllActiveByUserId(user.getId()), attachment);
        validateAttachmentProvider(attachment);

        byte[] attachmentBytes = googleMailAttachmentQueryService.download(
                attachment.getMessage().getThread().getMailAccount(),
                attachment.getMessage(),
                attachment
        );

        return new MailAttachmentDownloadResult(
                attachment.getFilename(),
                attachment.getMimeType(),
                attachmentBytes
        );
    }

    private void validateRequest(MailSendRequest request) {
        if (request == null) {
            throw new MailSendException(MailSendErrorCode.INVALID_SENDER_ADDRESS);
        }
    }

    private void validateSender(String from) {
        MailAddressCommand parsedSender = parseMailAddress(from, MailSendErrorCode.INVALID_SENDER_ADDRESS);
        if (!isValidEmail(parsedSender.address())) {
            throw new MailSendException(MailSendErrorCode.INVALID_SENDER_ADDRESS);
        }
    }

    private void validateAttachmentId(UUID attachmentId) {
        if (attachmentId == null) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_NOT_FOUND);
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

    private void validateAttachmentAccess(List<MailAccount> userAccounts, Attachment attachment) {
        Set<UUID> userAccountIds = userAccounts.stream()
                .map(MailAccount::getId)
                .collect(Collectors.toSet());

        if (!userAccountIds.contains(attachment.getMessage().getThread().getMailAccount().getId())) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_ACCESS_DENIED);
        }
    }

    private void validateAttachmentProvider(Attachment attachment) {
        if (attachment.getMessage().getThread().getMailAccount().getProvider() != MailProvider.GMAIL) {
            throw new InboxException(InboxErrorCode.ATTACHMENT_PROVIDER_NOT_SUPPORTED);
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
