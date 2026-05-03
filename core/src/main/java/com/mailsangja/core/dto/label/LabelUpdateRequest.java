package com.mailsangja.core.dto.label;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라벨 수정 요청 (null 필드는 변경하지 않음)")
public record LabelUpdateRequest(
        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "알림 활성화 여부", example = "false")
        @JsonProperty("notificationEnabled") Boolean notificationEnabled,

        @Schema(description = "표시 순서", example = "1")
        Integer order
) {
}
