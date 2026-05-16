package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailReviewErrorCode implements ErrorCode {

    INVALID_REQUEST(400, "MS-MAIL-REVIEW-INVALID-REQUEST", "메일 검토 요청 형식이 올바르지 않습니다."),
    RATE_LIMIT_EXCEEDED(429, "MS-MAIL-REVIEW-RATE-LIMIT-EXCEEDED", "메일 검토 월간 한도를 초과했습니다."),
    CHAT_MODEL_NOT_AVAILABLE(503, "MS-MAIL-REVIEW-CHAT-MODEL-NOT-AVAILABLE", "메일 검토 모델을 사용할 수 없습니다."),
    AI_RESPONSE_INVALID(502, "MS-MAIL-REVIEW-AI-RESPONSE-INVALID", "메일 검토 AI 응답 형식이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
