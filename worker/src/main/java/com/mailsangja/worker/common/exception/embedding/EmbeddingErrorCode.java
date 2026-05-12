package com.mailsangja.worker.common.exception.embedding;

import com.mailsangja.worker.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmbeddingErrorCode implements ErrorCode {

    MAIL_EMBEDDING_MESSAGE_NOT_FOUND(404, "MS-EMBEDDING-MESSAGE-NOT-FOUND", "임베딩할 메일 메시지를 찾을 수 없습니다."),
    INVALID_MAIL_EMBEDDING_MESSAGE(400, "MS-EMBEDDING-INVALID-MAIL-MESSAGE", "임베딩할 메일 메시지 값이 올바르지 않습니다."),
    INVALID_MAIL_EMBEDDING_DOCUMENT(400, "MS-EMBEDDING-INVALID-MAIL-DOCUMENT", "메일 임베딩 문서 값이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
