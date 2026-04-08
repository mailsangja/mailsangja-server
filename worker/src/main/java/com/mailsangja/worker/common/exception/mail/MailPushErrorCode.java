package com.mailsangja.worker.common.exception.mail;

import com.mailsangja.worker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailPushErrorCode implements ErrorCode {

    INVALID_PUBSUB_PUSH_REQUEST(400, "MS-MAIL-INVALID-PUBSUB-PUSH-REQUEST", "유효하지 않은 Pub/Sub push 요청입니다."),
    INVALID_PUBSUB_MESSAGE_DATA(400, "MS-MAIL-INVALID-PUBSUB-MESSAGE-DATA", "Pub/Sub message data가 올바르지 않습니다."),
    INVALID_GMAIL_PUSH_NOTIFICATION(400, "MS-MAIL-INVALID-GMAIL-PUSH-NOTIFICATION", "Gmail push notification 값이 올바르지 않습니다."),
    INVALID_INITIAL_MAIL_SYNC_COMMAND(400, "MS-MAIL-INVALID-INITIAL-MAIL-SYNC-COMMAND", "초기 메일 동기화 요청값이 올바르지 않습니다."),
    UNSUPPORTED_INITIAL_MAIL_SYNC_PROVIDER(400, "MS-MAIL-UNSUPPORTED-INITIAL-MAIL-SYNC-PROVIDER", "지원하지 않는 초기 메일 동기화 제공자입니다."),
    MAIL_ACCOUNT_NOT_FOUND(404, "MS-MAIL-ACCOUNT-NOT-FOUND", "메일 계정을 찾을 수 없습니다."),
    INVALID_MAIL_ACCOUNT_STATE(400, "MS-MAIL-INVALID-MAIL-ACCOUNT-STATE", "이벤트를 처리할 수 없는 메일 계정 상태입니다."),
    GMAIL_HISTORY_FETCH_FAILED(502, "MS-MAIL-GMAIL-HISTORY-FETCH-FAILED", "Gmail History 조회에 실패했습니다."),
    GMAIL_HISTORY_RESULT_INVALID(502, "MS-MAIL-GMAIL-HISTORY-RESULT-INVALID", "Gmail History 응답값이 올바르지 않습니다."),
    GMAIL_MESSAGES_FETCH_FAILED(502, "MS-MAIL-GMAIL-MESSAGES-FETCH-FAILED", "Gmail 최신 메일 조회에 실패했습니다."),
    GMAIL_MESSAGES_RESULT_INVALID(502, "MS-MAIL-GMAIL-MESSAGES-RESULT-INVALID", "Gmail 최신 메일 응답값이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
