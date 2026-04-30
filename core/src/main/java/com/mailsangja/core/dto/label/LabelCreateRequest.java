package com.mailsangja.core.dto.label;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mailsangja.db.common.label.LabelRule;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라벨 생성 요청")
public record LabelCreateRequest(
        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "알림 활성화 여부", example = "true")
        @JsonProperty("notificationEnabled") boolean notificationEnabled,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "라벨 자동 분류 규칙")
        LabelRule rule
) {
}
