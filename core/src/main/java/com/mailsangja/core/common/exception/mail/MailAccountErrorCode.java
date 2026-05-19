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
    INVALID_MAIL_ACCOUNT_ALIAS(400, "MS-MAIL-INVALID-MAIL-ACCOUNT-ALIAS", "유효하지 않은 메일 계정 별칭입니다."),
    INVALID_MAIL_ACCOUNT_ICON(400, "MS-MAIL-INVALID-MAIL-ACCOUNT-ICON", "유효하지 않은 메일 계정 아이콘입니다."),
    INVALID_MAIL_ACCOUNT_COLOR(400, "MS-MAIL-INVALID-MAIL-ACCOUNT-COLOR", "메일 계정 색상은 HEX 형식으로 입력해주세요."),
    UNSUPPORTED_MAIL_PROVIDER(400, "MS-MAIL-UNSUPPORTED-PROVIDER", "지원하지 않는 메일 제공자입니다."),
    INVALID_OAUTH_RESULT(400, "MS-MAIL-INVALID-OAUTH-RESULT", "OAuth 응답값이 올바르지 않습니다."),
    INVALID_INITIAL_MAIL_SYNC_MESSAGE(400, "MS-MAIL-INVALID-INITIAL-MAIL-SYNC-MESSAGE", "초기 메일 동기화 메시지 값이 올바르지 않습니다."),
    GOOGLE_TOKEN_EXCHANGE_FAILED(502, "MS-MAIL-GOOGLE-TOKEN-EXCHANGE-FAILED", "Google OAuth 토큰 교환에 실패했습니다."),
    GOOGLE_TOKEN_REFRESH_FAILED(502, "MS-MAIL-GOOGLE-TOKEN-REFRESH-FAILED", "Google access token 재발급에 실패했습니다."),
    GOOGLE_USER_INFO_FETCH_FAILED(502, "MS-MAIL-GOOGLE-USER-INFO-FETCH-FAILED", "Google 사용자 정보 조회에 실패했습니다."),
    GOOGLE_MAIL_WATCH_FAILED(502, "MS-MAIL-GOOGLE-MAIL-WATCH-FAILED", "Google Gmail watch 등록에 실패했습니다."),
    GOOGLE_MAIL_READ_MODIFY_FAILED(502, "MS-MAIL-GOOGLE-MAIL-READ-MODIFY-FAILED", "Google Gmail 읽음 처리 동기화에 실패했습니다."),
    GOOGLE_MESSAGE_READ_MODIFY_FAILED(502, "MS-MAIL-GOOGLE-MESSAGE-READ-MODIFY-FAILED", "Google Gmail 메시지 읽음 처리 동기화에 실패했습니다."),
    GOOGLE_MAIL_UNREAD_MODIFY_FAILED(502, "MS-MAIL-GOOGLE-MAIL-UNREAD-MODIFY-FAILED", "Google Gmail 안읽음 처리 동기화에 실패했습니다."),
    GOOGLE_MESSAGE_UNREAD_MODIFY_FAILED(502, "MS-MAIL-GOOGLE-MESSAGE-UNREAD-MODIFY-FAILED", "Google Gmail 메시지 안읽음 처리 동기화에 실패했습니다."),
    GOOGLE_MAIL_WATCH_RESULT_INVALID(502, "MS-MAIL-GOOGLE-MAIL-WATCH-RESULT-INVALID", "Google Gmail watch 응답값이 올바르지 않습니다."),
    GOOGLE_EMAIL_NOT_VERIFIED(400, "MS-MAIL-GOOGLE-EMAIL-NOT-VERIFIED", "Google 계정의 이메일 인증이 확인되지 않았습니다. 인증된 계정으로 다시 시도해주세요."),
    GOOGLE_REFRESH_TOKEN_MISSING(400, "MS-MAIL-GOOGLE-REFRESH-TOKEN-MISSING", "Google 계정 재연동이 필요합니다. 다시 동의하고 계정을 연결해주세요."),
    MAIL_ACCOUNT_ALREADY_CONNECTED(409, "MS-MAIL-ACCOUNT-ALREADY-CONNECTED", "이미 연결된 메일 계정입니다."),
    MAIL_ACCOUNT_ALREADY_CONNECTED_BY_ANOTHER_USER(409, "MS-MAIL-ACCOUNT-ALREADY-CONNECTED-BY-ANOTHER-USER", "다른 사용자가 이미 연결한 메일 계정입니다."),
    MAIL_ACCOUNT_NOT_FOUND(404, "MS-MAIL-ACCOUNT-NOT-FOUND", "메일 계정을 찾을 수 없습니다."),
    MAIL_ACCOUNT_ACCESS_DENIED(403, "MS-MAIL-ACCOUNT-ACCESS-DENIED", "해당 메일 계정에 접근 권한이 없습니다."),
    MAIL_ACCOUNT_NO_FIELD_TO_UPDATE(400, "MS-MAIL-ACCOUNT-NO-FIELD-TO-UPDATE", "변경할 항목을 하나 이상 입력해주세요.");

    private final int status;
    private final String code;
    private final String message;
}
