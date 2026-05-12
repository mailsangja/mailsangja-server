package com.mailsangja.core.dto.label;

import com.mailsangja.db.common.label.NotificationPolicy;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "라벨 수정 요청 (null 필드는 변경하지 않음)")
public record LabelUpdateRequest(
        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "알림 정책 (URGENT/INHERIT/SILENT)", example = "SILENT")
        NotificationPolicy notificationPolicy,

        @Schema(description = "표시 순서", example = "1")
        Integer order
) {
}
