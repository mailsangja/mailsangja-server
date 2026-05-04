package com.mailsangja.core.common.exception.mail;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MailSendErrorCode implements ErrorCode {

    INVALID_SENDER_ADDRESS(400, "MS-MAIL-INVALID-SENDER-ADDRESS", "유효하지 않은 발신 메일 주소입니다."),
    INVALID_REPLY_TO_ADDRESS(400, "MS-MAIL-INVALID-REPLY-TO-ADDRESS", "유효하지 않은 답장 받을 메일 주소입니다."),
    SENDER_MAIL_ACCOUNT_NOT_FOUND(404, "MS-MAIL-SENDER-MAIL-ACCOUNT-NOT-FOUND", "발신 메일 계정을 찾을 수 없습니다."),
    EMPTY_RECIPIENT(400, "MS-MAIL-EMPTY-RECIPIENT", "최소 한 명 이상의 수신자가 필요합니다."),
    INVALID_RECIPIENT_ADDRESS(400, "MS-MAIL-INVALID-RECIPIENT-ADDRESS", "유효하지 않은 수신 메일 주소가 포함되어 있습니다."),
    DUPLICATE_RECIPIENT_ADDRESS(400, "MS-MAIL-DUPLICATE-RECIPIENT-ADDRESS", "중복된 수신 메일 주소는 허용되지 않습니다."),
    INVALID_MAIL_SUBJECT(400, "MS-MAIL-INVALID-MAIL-SUBJECT", "메일 제목에 허용되지 않은 문자가 포함되어 있습니다."),
    INVALID_MAIL_REQUEST(400, "MS-MAIL-INVALID-MAIL-REQUEST", "메일 전송 요청 형식이 올바르지 않습니다."),
    EMPTY_SUBJECT_AND_CONTENT(400, "MS-MAIL-EMPTY-SUBJECT-AND-CONTENT", "메일 제목과 본문을 모두 비워둘 수 없습니다."),
    ATTACHMENT_COUNT_EXCEEDED(400, "MS-MAIL-ATTACHMENT-COUNT-EXCEEDED", "첨부파일은 최대 10개까지 업로드할 수 있습니다."),
    ATTACHMENT_SIZE_EXCEEDED(400, "MS-MAIL-ATTACHMENT-SIZE-EXCEEDED", "첨부파일 크기 제한을 초과했습니다."),
    EMPTY_ATTACHMENT_FILE(400, "MS-MAIL-EMPTY-ATTACHMENT-FILE", "비어 있는 첨부파일은 업로드할 수 없습니다."),
    INVALID_ATTACHMENT_FILENAME(400, "MS-MAIL-INVALID-ATTACHMENT-FILENAME", "첨부파일 이름이 올바르지 않습니다."),
    INLINE_IMAGE_COUNT_MISMATCH(400, "MS-MAIL-INLINE-IMAGE-COUNT-MISMATCH", "본문 이미지 파일과 CID 개수가 일치하지 않습니다."),
    INLINE_IMAGE_CID_NOT_FOUND(400, "MS-MAIL-INLINE-IMAGE-CID-NOT-FOUND", "본문에 존재하지 않는 CID가 포함되어 있습니다."),
    DUPLICATE_INLINE_IMAGE_CID(400, "MS-MAIL-DUPLICATE-INLINE-IMAGE-CID", "중복된 본문 이미지 CID는 허용되지 않습니다."),
    INVALID_INLINE_IMAGE_CID(400, "MS-MAIL-INVALID-INLINE-IMAGE-CID", "본문 이미지 CID 형식이 올바르지 않습니다."),
    INVALID_INLINE_IMAGE_TYPE(400, "MS-MAIL-INVALID-INLINE-IMAGE-TYPE", "본문 이미지는 image/* 형식만 업로드할 수 있습니다."),
    REPLY_TARGET_MESSAGE_NOT_FOUND(404, "MS-MAIL-REPLY-TARGET-MESSAGE-NOT-FOUND", "답장 대상 메일 메시지를 찾을 수 없습니다."),
    REPLY_TARGET_MESSAGE_ACCESS_DENIED(403, "MS-MAIL-REPLY-TARGET-MESSAGE-ACCESS-DENIED", "답장 대상 메일 메시지에 접근할 권한이 없습니다."),
    REPLY_SENDER_ACCOUNT_MISMATCH(400, "MS-MAIL-REPLY-SENDER-ACCOUNT-MISMATCH", "답장 발신 계정이 원본 메시지 계정과 일치하지 않습니다."),
    MAIL_MIME_BUILD_FAILED(400, "MS-MAIL-MAIL-MIME-BUILD-FAILED", "메일 전송 요청 본문을 생성하는 중 오류가 발생했습니다."),
    ATTACHMENT_READ_FAILED(500, "MS-MAIL-ATTACHMENT-READ-FAILED", "첨부파일을 읽는 중 오류가 발생했습니다."),
    GOOGLE_MAIL_SEND_FAILED(502, "MS-MAIL-GOOGLE-MAIL-SEND-FAILED", "Google Gmail 메일 전송에 실패했습니다."),
    GOOGLE_MAIL_SEND_RESULT_INVALID(502, "MS-MAIL-GOOGLE-MAIL-SEND-RESULT-INVALID", "Google Gmail 메일 전송 응답값이 올바르지 않습니다."),
    GOOGLE_MAIL_MESSAGE_FETCH_FAILED(502, "MS-MAIL-GOOGLE-MAIL-MESSAGE-FETCH-FAILED", "Google Gmail 메시지 조회에 실패했습니다."),
    GOOGLE_MAIL_MESSAGE_RESULT_INVALID(502, "MS-MAIL-GOOGLE-MAIL-MESSAGE-RESULT-INVALID", "Google Gmail 메시지 조회 응답값이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
