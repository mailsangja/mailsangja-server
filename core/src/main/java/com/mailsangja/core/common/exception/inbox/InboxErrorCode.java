package com.mailsangja.core.common.exception.inbox;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InboxErrorCode implements ErrorCode {

    THREAD_NOT_FOUND(404, "MS-INBOX-THREAD-NOT-FOUND", "메일 스레드를 찾을 수 없습니다."),
    THREAD_ACCESS_DENIED(403, "MS-INBOX-THREAD-ACCESS-DENIED", "해당 메일 스레드에 접근할 권한이 없습니다.");

    private final int status;
    private final String code;
    private final String message;
}
