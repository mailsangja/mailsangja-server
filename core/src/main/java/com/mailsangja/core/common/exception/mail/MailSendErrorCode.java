package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailSendErrorCode implements ErrorCode {

    INVALID_COMPOSE_SESSION_ID(400, "MS-MAIL-INVALID-COMPOSE-SESSION-ID", "유효하지 않은 composeSessionId 입니다."),
    INVALID_SENDER_ADDRESS(400, "MS-MAIL-INVALID-SENDER-ADDRESS", "유효하지 않은 발신 메일 주소입니다."),
    SENDER_MAIL_ACCOUNT_NOT_FOUND(404, "MS-MAIL-SENDER-MAIL-ACCOUNT-NOT-FOUND", "발신 메일 계정을 찾을 수 없습니다."),
    EMPTY_RECIPIENT(400, "MS-MAIL-EMPTY-RECIPIENT", "최소 한 명 이상의 수신자가 필요합니다."),
    INVALID_RECIPIENT_ADDRESS(400, "MS-MAIL-INVALID-RECIPIENT-ADDRESS", "유효하지 않은 수신 메일 주소가 포함되어 있습니다."),
    DUPLICATE_RECIPIENT_ADDRESS(400, "MS-MAIL-DUPLICATE-RECIPIENT-ADDRESS", "중복된 수신 메일 주소는 허용되지 않습니다."),
    EMPTY_SUBJECT_AND_CONTENT(400, "MS-MAIL-EMPTY-SUBJECT-AND-CONTENT", "메일 제목과 본문을 모두 비워둘 수 없습니다."),
    ATTACHMENT_COUNT_EXCEEDED(400, "MS-MAIL-ATTACHMENT-COUNT-EXCEEDED", "첨부파일은 최대 10개까지 업로드할 수 있습니다."),
    ATTACHMENT_SIZE_EXCEEDED(400, "MS-MAIL-ATTACHMENT-SIZE-EXCEEDED", "첨부파일 크기 제한을 초과했습니다."),
    EMPTY_ATTACHMENT_FILE(400, "MS-MAIL-EMPTY-ATTACHMENT-FILE", "비어 있는 첨부파일은 업로드할 수 없습니다."),
    INVALID_ATTACHMENT_FILENAME(400, "MS-MAIL-INVALID-ATTACHMENT-FILENAME", "첨부파일 이름이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
