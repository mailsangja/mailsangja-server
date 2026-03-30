package com.mailsangja.core.common.exception.user;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements ErrorCode {

    USER_NOT_FOUND(404, "MS-USER-NOT-FOUND", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(409, "MS-USER-DUPLICATE-USERNAME", "이미 사용 중인 아이디입니다."),
    INVALID_CREDENTIALS(401, "MS-USER-INVALID-CREDENTIALS", "아이디 또는 비밀번호가 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
