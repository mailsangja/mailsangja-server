package com.mailsangja.core.dto.label;

import com.mailsangja.db.entity.label.Label;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "라벨 목록 응답")
public record LabelListResponse(
        @Schema(description = "라벨 ID", format = "uuid", example = "2fd8a795-5ca5-47ad-b6a4-63c45f6f1c9b")
        UUID id,

        @Schema(description = "라벨 이름", example = "업무")
        String name,

        @Schema(description = "HEX 색상 코드", example = "#3366FF")
        String colorCode,

        @Schema(description = "표시 순서", example = "0")
        int order,

        @Schema(description = "AI 기능에서 제외할 민감 라벨 여부", example = "false")
        boolean isSensitive,

        @Schema(description = "읽지 않은 스레드 수", example = "12")
        long unreadThreadCount
) {

    public static LabelListResponse of(Label label, long unreadThreadCount) {
        return new LabelListResponse(
                label.getId(),
                label.getName(),
                label.getColorCode(),
                label.getDisplayOrder(),
                label.isSensitive(),
                unreadThreadCount
        );
    }
}
