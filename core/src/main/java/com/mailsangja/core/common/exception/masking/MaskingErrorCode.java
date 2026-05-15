package com.mailsangja.core.common.exception.masking;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MaskingErrorCode implements ErrorCode {

    INVALID_TEXT(400, "MS-MASKING-INVALID-TEXT", "마스킹 대상 텍스트가 올바르지 않습니다."),
    INVALID_COMMAND(400, "MS-MASKING-INVALID-COMMAND", "마스킹 명령이 올바르지 않습니다."),
    INVALID_RESULT(400, "MS-MASKING-INVALID-RESULT", "마스킹 결과가 올바르지 않습니다."),
    INVALID_SCOPE(400, "MS-MASKING-INVALID-SCOPE", "마스킹 범위가 올바르지 않습니다."),
    INVALID_DETECTION_RANGE(400, "MS-MASKING-INVALID-DETECTION-RANGE", "마스킹 탐지 범위가 올바르지 않습니다."),
    INVALID_TOKEN_TYPE(400, "MS-MASKING-INVALID-TOKEN-TYPE", "마스킹 토큰 타입이 올바르지 않습니다."),
    INVALID_TOKEN(400, "MS-MASKING-INVALID-TOKEN", "마스킹 토큰이 올바르지 않습니다."),
    INVALID_ORIGINAL_VALUE(400, "MS-MASKING-INVALID-ORIGINAL-VALUE", "마스킹 원본 값이 올바르지 않습니다."),
    INVALID_TOKEN_RANGE(400, "MS-MASKING-INVALID-TOKEN-RANGE", "마스킹 토큰 범위가 올바르지 않습니다."),
    PHILEAS_DETECTION_FAILED(500, "MS-MASKING-PHILEAS-DETECTION-FAILED", "Phileas 개인정보 탐지에 실패했습니다."),
    PHILEAS_INITIALIZATION_FAILED(500, "MS-MASKING-PHILEAS-INITIALIZATION-FAILED", "Phileas 초기화에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;
}
