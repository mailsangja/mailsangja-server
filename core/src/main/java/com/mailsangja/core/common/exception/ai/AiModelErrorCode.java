package com.mailsangja.core.common.exception.ai;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AiModelErrorCode implements ErrorCode {

    INVALID_MODEL(400, "MS-AI-MODEL-INVALID-MODEL", "지원하지 않는 AI 모델입니다.");

    private final int status;
    private final String code;
    private final String message;
}
