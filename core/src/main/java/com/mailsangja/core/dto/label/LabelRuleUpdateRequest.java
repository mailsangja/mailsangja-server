package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라벨 규칙 수정 요청")
public record LabelRuleUpdateRequest(
        @Schema(description = "라벨 자동 분류 규칙")
        LabelRule rule
) {
}
