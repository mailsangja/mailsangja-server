package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailAccountErrorCode implements ErrorCode {

    OAUTH_SESSION_NOT_FOUND(401, "MS-MAIL-OAUTH-SESSION-NOT-FOUND", "OAuth 세션 정보를 찾을 수 없습니다."),
    INVALID_OAUTH_STATE(400, "MS-MAIL-INVALID-OAUTH-STATE", "유효하지 않은 OAuth state 입니다."),
    MAIL_ACCOUNT_ALREADY_CONNECTED(409, "MS-MAIL-ACCOUNT-ALREADY-CONNECTED", "이미 연결된 메일 계정입니다.");

    private final int status;
    private final String code;
    private final String message;
}
