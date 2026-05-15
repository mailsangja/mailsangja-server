package com.mailsangja.worker.common.exception.embedding;

import com.mailsangja.worker.common.exception.BaseException;

public class EmbeddingException extends BaseException {

    public EmbeddingException(EmbeddingErrorCode errorCode) {
        super(errorCode);
    }

    public EmbeddingException(EmbeddingErrorCode errorCode, String detailMessage) {
        super(errorCode, detailMessage);
    }
}
