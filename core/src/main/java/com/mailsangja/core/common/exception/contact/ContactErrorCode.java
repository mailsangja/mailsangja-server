package com.mailsangja.core.common.exception.contact;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ContactErrorCode implements ErrorCode {

    INVALID_CONTACT_SYNC_REQUEST(400, "MS-CONTACT-INVALID-SYNC-REQUEST", "주소록 동기화 요청 값이 올바르지 않습니다."),
    GOOGLE_CONTACTS_FETCH_FAILED(502, "MS-CONTACT-GOOGLE-CONTACTS-FETCH-FAILED", "Google Contacts 조회에 실패했습니다."),
    GOOGLE_CONTACTS_RESULT_INVALID(502, "MS-CONTACT-GOOGLE-CONTACTS-RESULT-INVALID", "Google Contacts 응답값이 올바르지 않습니다.");

    private final int status;
    private final String code;
    private final String message;
}
