package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailAccountErrorCode implements ErrorCode {

    OAUTH_SESSION_NOT_FOUND(401, "MS-MAIL-OAUTH-SESSION-NOT-FOUND", "OAuth 세션 정보를 찾을 수 없습니다."),
    INVALID_OAUTH_STATE(400, "MS-MAIL-INVALID-OAUTH-STATE", "유효하지 않은 OAuth state 입니다."),
    OAUTH_USER_MISMATCH(403, "MS-MAIL-OAUTH-USER-MISMATCH", "OAuth 요청 사용자 정보가 일치하지 않습니다."),
    INVALID_AUTHORIZATION_CODE(400, "MS-MAIL-INVALID-AUTHORIZATION-CODE", "유효하지 않은 OAuth 인가 코드입니다."),
    UNSUPPORTED_MAIL_PROVIDER(400, "MS-MAIL-UNSUPPORTED-PROVIDER", "지원하지 않는 메일 제공자입니다."),
    INVALID_OAUTH_RESULT(400, "MS-MAIL-INVALID-OAUTH-RESULT", "OAuth 응답값이 올바르지 않습니다."),
    MAIL_ACCOUNT_ALREADY_CONNECTED(409, "MS-MAIL-ACCOUNT-ALREADY-CONNECTED", "이미 연결된 메일 계정입니다."),
    MAIL_ACCOUNT_ALREADY_CONNECTED_BY_ANOTHER_USER(409, "MS-MAIL-ACCOUNT-ALREADY-CONNECTED-BY-ANOTHER-USER", "다른 사용자가 이미 연결한 메일 계정입니다."),
    MAIL_ACCOUNT_NOT_FOUND(404, "MS-MAIL-ACCOUNT-NOT-FOUND", "메일 계정을 찾을 수 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
