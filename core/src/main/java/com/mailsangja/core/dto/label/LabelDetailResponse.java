package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.entity.label.Label;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "라벨 상세 응답")
public record LabelDetailResponse(
        @Schema(description = "라벨 ID", format = "uuid", example = "2fd8a795-5ca5-47ad-b6a4-63c45f6f1c9b")
        UUID id,

        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "알림 활성화 여부", example = "true")
        boolean notificationEnabled,

        @Schema(description = "라벨 자동 분류 규칙")
        LabelRule rule
) {

    public static LabelDetailResponse of(Label label, LabelRule rule) {
        return new LabelDetailResponse(
                label.getId(),
                label.getName(),
                label.getColorCode(),
                label.getDisplayOrder(),
                label.isNotificationEnabled(),
                rule
        );
    }
}
