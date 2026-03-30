package com.mailsangja.core.common.exception.auth;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements ErrorCode {

    UNAUTHORIZED(401, "MS-AUTH-UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(403, "MS-AUTH-FORBIDDEN", "접근 권한이 없습니다."),
    FORBIDDEN_ROLE(403, "MS-AUTH-FORBIDDEN-ROLE", "관리자 권한이 필요합니다.");

    private final int status;
    private final String code;
    private final String message;
}
