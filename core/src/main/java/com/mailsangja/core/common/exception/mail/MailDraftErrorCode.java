package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailDraftErrorCode implements ErrorCode {

    INVALID_REQUEST(400, "MS-MAIL-DRAFT-INVALID-REQUEST", "메일 초안 요청 형식이 올바르지 않습니다."),
    ACCESS_DENIED(403, "MS-MAIL-DRAFT-ACCESS-DENIED", "메일 초안 작성 권한이 없습니다."),
    PROMPT_INJECTION_DETECTED(400, "MS-MAIL-DRAFT-PROMPT-INJECTION-DETECTED", "메일 초안 요청에 허용되지 않은 지시가 포함되어 있습니다."),
    RATE_LIMIT_EXCEEDED(429, "MS-MAIL-DRAFT-RATE-LIMIT-EXCEEDED", "메일 초안 생성 월간 한도를 초과했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
