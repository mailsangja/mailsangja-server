package com.mailsangja.core.common.exception.inbox;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum InboxErrorCode implements ErrorCode {

    THREAD_NOT_FOUND(404, "MS-INBOX-THREAD-NOT-FOUND", "메일 스레드를 찾을 수 없습니다."),
    THREAD_ACCESS_DENIED(403, "MS-INBOX-THREAD-ACCESS-DENIED", "해당 메일 스레드에 접근할 권한이 없습니다."),
    ATTACHMENT_NOT_FOUND(404, "MS-INBOX-ATTACHMENT-NOT-FOUND", "첨부파일을 찾을 수 없습니다."),
    ATTACHMENT_ACCESS_DENIED(403, "MS-INBOX-ATTACHMENT-ACCESS-DENIED", "해당 첨부파일에 접근할 권한이 없습니다."),
    ATTACHMENT_PROVIDER_NOT_SUPPORTED(501, "MS-INBOX-ATTACHMENT-PROVIDER-NOT-SUPPORTED", "해당 메일 제공자의 첨부파일 조회는 아직 지원하지 않습니다."),
    ATTACHMENT_SOURCE_INVALID(502, "MS-INBOX-ATTACHMENT-SOURCE-INVALID", "첨부파일 원본 정보가 올바르지 않습니다."),
    ATTACHMENT_DOWNLOAD_FAILED(502, "MS-INBOX-ATTACHMENT-DOWNLOAD-FAILED", "첨부파일 다운로드에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
