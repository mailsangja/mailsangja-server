package com.mailsangja.core.dto.mail;

import com.mailsangja.core.common.exception.BaseException;
import com.mailsangja.core.common.exception.ErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftErrorCode;
import com.mailsangja.core.common.exception.mail.MailDraftException;

public record MailDraftErrorEvent(String code, String message) {

    public MailDraftErrorEvent {
        validateText(code);
        validateText(message);
    }

    public static MailDraftErrorEvent from(Exception exception) {
        if (exception instanceof BaseException baseException) {
            return from(baseException.getErrorCode());
        }
        return new MailDraftErrorEvent("MS-MAIL-DRAFT-STREAM-FAILED", "메일 초안 생성에 실패했습니다.");
    }

    private static MailDraftErrorEvent from(ErrorCode errorCode) {
        return new MailDraftErrorEvent(errorCode.getCode(), errorCode.getMessage());
    }

    private static void validateText(String value) {
        if (value == null || value.isBlank()) {
            throw new MailDraftException(MailDraftErrorCode.INVALID_REQUEST);
        }
    }
}
