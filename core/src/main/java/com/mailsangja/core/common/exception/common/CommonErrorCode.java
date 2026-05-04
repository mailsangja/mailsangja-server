package com.mailsangja.core.common.exception.common;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INVALID_REQUEST(400, "MS-COMMON-INVALID-REQUEST", "요청 값이 올바르지 않습니다."),
    INTERNAL_FAILURE(500, "MS-COMMON-INTERNAL-FAILURE", "서버 내부 오류입니다.");

    private final int status;
    private final String code;
    private final String message;
}
