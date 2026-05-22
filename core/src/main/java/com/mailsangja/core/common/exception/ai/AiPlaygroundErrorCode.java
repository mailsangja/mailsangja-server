package com.mailsangja.core.common.exception.ai;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiPlaygroundErrorCode implements ErrorCode {

    INVALID_REQUEST(400, "MS-AI-PLAYGROUND-INVALID-REQUEST", "AI 플레이그라운드 요청이 올바르지 않습니다."),
    MAIL_ACCOUNT_REQUIRED(403, "MS-AI-PLAYGROUND-MAIL-ACCOUNT-REQUIRED", "AI 플레이그라운드는 메일 계정을 등록한 사용자만 사용할 수 있습니다."),
    CHAT_MODEL_NOT_AVAILABLE(503, "MS-AI-PLAYGROUND-CHAT-MODEL-NOT-AVAILABLE", "사용 가능한 AI 채팅 모델이 없습니다."),
    PROVIDER_FAILURE(502, "MS-AI-PLAYGROUND-PROVIDER-FAILURE", "AI Provider 호출에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
