package com.mailsangja.worker.common.exception.common;

import com.mailsangja.worker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum CommonErrorCode implements ErrorCode {

    INTERNAL_FAILURE(500, "MS-COMMON-INTERNAL-FAILURE", "서버 내부 오류입니다.");

    private final int status;
    private final String code;
    private final String message;
}
