package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import com.mailsangja.db.entity.label.Label;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "라벨 제안 응답")
public record LabelSuggestionResponse(
        @Schema(description = "라벨 제안 ID", format = "uuid")
        UUID id,

        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "알림 정책 (URGENT/INHERIT/SILENT)", example = "INHERIT")
        NotificationPolicy notificationPolicy,

        @Schema(description = "라벨 자동 분류 규칙")
        LabelRule rule
) {

    public static LabelSuggestionResponse from(Label label) {
        return new LabelSuggestionResponse(
                label.getId(),
                label.getName(),
                label.getColorCode(),
                label.getDisplayOrder(),
                label.getNotificationPolicy(),
                label.getRule()
        );
    }
}
