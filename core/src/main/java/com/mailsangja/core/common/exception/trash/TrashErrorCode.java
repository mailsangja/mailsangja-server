package com.mailsangja.core.common.exception.trash;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TrashErrorCode implements ErrorCode {

    THREAD_NOT_FOUND(404, "MS-TRASH-THREAD-NOT-FOUND", "메일 스레드를 찾을 수 없습니다."),
    THREAD_NOT_DELETED(409, "MS-TRASH-THREAD-NOT-DELETED", "해당 스레드는 삭제된 상태가 아닙니다."),
    THREAD_ACCESS_DENIED(403, "MS-TRASH-THREAD-ACCESS-DENIED", "해당 메일 스레드에 접근할 권한이 없습니다."),
    MESSAGE_NOT_FOUND(404, "MS-TRASH-MESSAGE-NOT-FOUND", "메일 메시지를 찾을 수 없습니다."),
    MESSAGE_NOT_DELETED(409, "MS-TRASH-MESSAGE-NOT-DELETED", "해당 메시지는 삭제된 상태가 아닙니다."),
    MESSAGE_ACCESS_DENIED(403, "MS-TRASH-MESSAGE-ACCESS-DENIED", "해당 메일 메시지에 접근할 권한이 없습니다."),
    GMAIL_API_FAILED(502, "MS-TRASH-GMAIL-API-FAILED", "Gmail API 호출에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
