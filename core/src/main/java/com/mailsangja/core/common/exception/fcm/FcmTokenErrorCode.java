package com.mailsangja.core.common.exception.fcm;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FcmTokenErrorCode implements ErrorCode {

    INVALID_FCM_TOKEN(400, "MS-FCM-INVALID-TOKEN", "유효하지 않은 FCM 토큰입니다.");

    private final int status;
    private final String code;
    private final String message;
}
