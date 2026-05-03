package com.mailsangja.core.common.exception.label;

import com.mailsangja.core.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LabelErrorCode implements ErrorCode {

    LABEL_NOT_FOUND(404, "MS-LABEL-NOT-FOUND", "라벨을 찾을 수 없습니다."),
    LABEL_ACCESS_DENIED(403, "MS-LABEL-ACCESS-DENIED", "해당 라벨에 접근할 권한이 없습니다."),
    LABEL_NAME_BLANK(400, "MS-LABEL-NAME-BLANK", "라벨 이름은 비어있을 수 없습니다."),
    LABEL_COLOR_CODE_BLANK(400, "MS-LABEL-COLOR-CODE-BLANK", "라벨 색상 코드는 비어있을 수 없습니다."),
    LABEL_NAME_DUPLICATE(409, "MS-LABEL-NAME-DUPLICATE", "동일한 이름의 라벨이 이미 존재합니다."),
    LABEL_RULE_INVALID_FIELD(400, "MS-LABEL-RULE-INVALID-FIELD", "허용되지 않는 규칙 field 값입니다."),
    LABEL_RULE_INVALID_OPERATOR(400, "MS-LABEL-RULE-INVALID-OPERATOR", "허용되지 않는 규칙 operator 값입니다."),
    LABEL_RULE_FIELD_OPERATOR_MISMATCH(400, "MS-LABEL-RULE-FIELD-OPERATOR-MISMATCH", "field와 operator 조합이 올바르지 않습니다."),
    LABEL_RULE_VALUE_TOO_LONG(400, "MS-LABEL-RULE-VALUE-TOO-LONG", "규칙 value가 허용 길이를 초과했습니다."),
    LABEL_RULE_VALUE_BLANK(400, "MS-LABEL-RULE-VALUE-BLANK", "규칙 value가 비어있습니다."),
    LABEL_RULE_INVALID_JSON(400, "MS-LABEL-RULE-INVALID-JSON", "규칙 JSON 형식이 올바르지 않습니다."),
    LABEL_RULE_INVALID_BOOLEAN_VALUE(400, "MS-LABEL-RULE-INVALID-BOOLEAN-VALUE", "BOOLEAN operator의 value는 'true' 또는 'false'이어야 합니다.");

    private final int status;
    private final String code;
    private final String message;
}
