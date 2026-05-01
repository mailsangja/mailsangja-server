package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.LabelRule;
import com.mailsangja.db.common.label.NotificationPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라벨 생성 요청")
public record LabelCreateRequest(
        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "알림 정책 (URGENT/INHERIT/SILENT)", example = "INHERIT")
        NotificationPolicy notificationPolicy,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "라벨 자동 분류 규칙")
        LabelRule rule
) {
}
