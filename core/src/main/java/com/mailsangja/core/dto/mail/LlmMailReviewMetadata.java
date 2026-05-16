package com.mailsangja.core.dto.mail;

import java.util.List;

public record LlmMailReviewMetadata(
        int attachmentCount,
        List<String> attachmentNames
) {

    public LlmMailReviewMetadata {
        attachmentNames = attachmentNames == null ? List.of() : List.copyOf(attachmentNames);
    }

    public static LlmMailReviewMetadata from(MailReviewCommand command) {
        return new LlmMailReviewMetadata(command.attachmentCount(), command.attachmentNames());
    }
}
